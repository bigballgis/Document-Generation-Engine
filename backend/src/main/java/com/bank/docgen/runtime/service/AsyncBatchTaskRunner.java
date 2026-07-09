package com.bank.docgen.runtime.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.BatchResultView;
import com.bank.docgen.runtime.api.BatchSummaryView;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.runtime.domain.TaskStatus;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskEntity;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.service.TemplateNotFoundException;
import com.bank.docgen.template.service.TemplateValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AsyncBatchTaskRunner {

    private final GenerationAsyncTaskRepository asyncTaskRepository;
    private final TemplateRepository templateRepository;
    private final BatchExecutionService batchExecutionService;
    private final RuntimeGenerationAuditRecorder runtimeGenerationAuditRecorder;
    private final ObjectMapper objectMapper;
    private final ApiPolicyRepository apiPolicyRepository;
    private final InvocationRecordService invocationRecordService;
    private final TraceIdProvider traceIdProvider;
    private final AsyncBatchTaskStaleReclaimService staleReclaimService;
    private final AsyncBatchPayloadScrubber payloadScrubber;

    public AsyncBatchTaskRunner(
            GenerationAsyncTaskRepository asyncTaskRepository,
            TemplateRepository templateRepository,
            BatchExecutionService batchExecutionService,
            RuntimeGenerationAuditRecorder runtimeGenerationAuditRecorder,
            ObjectMapper objectMapper,
            ApiPolicyRepository apiPolicyRepository,
            @Lazy InvocationRecordService invocationRecordService,
            TraceIdProvider traceIdProvider,
            AsyncBatchTaskStaleReclaimService staleReclaimService,
            AsyncBatchPayloadScrubber payloadScrubber
    ) {
        this.asyncTaskRepository = asyncTaskRepository;
        this.templateRepository = templateRepository;
        this.batchExecutionService = batchExecutionService;
        this.runtimeGenerationAuditRecorder = runtimeGenerationAuditRecorder;
        this.objectMapper = objectMapper;
        this.apiPolicyRepository = apiPolicyRepository;
        this.invocationRecordService = invocationRecordService;
        this.traceIdProvider = traceIdProvider;
        this.staleReclaimService = staleReclaimService;
        this.payloadScrubber = payloadScrubber;
    }

    @Async("asyncTaskExecutor")
    public void run(UUID taskUuid) {
        processTask(taskUuid);
    }

    @Transactional
    public void processTask(UUID taskUuid) {
        GenerationAsyncTaskEntity task = asyncTaskRepository.findById(taskUuid).orElseThrow();
        if (task.getStatus() == TaskStatus.CANCELLED) {
            return;
        }
        if (isTerminalStatus(task.getStatus())) {
            return;
        }
        if (task.getStatus() == TaskStatus.PROCESSING) {
            if (!staleReclaimService.isStale(task)) {
                return;
            }
            staleReclaimService.reclaimTask(task.getId());
            return;
        }
        task.markProcessing();
        asyncTaskRepository.save(task);
        try {
            TemplateEntity template = templateRepository.findByIdAndDeletedAtIsNull(task.getTemplateId())
                    .orElseThrow(TemplateNotFoundException::new);
            BatchGenerateRequestBody request = readRequestPayload(task.getRequestPayloadJson());
            BatchExecutionService.BatchExecutionOutcome outcome = batchExecutionService.execute(
                    template,
                    task.getReleaseVersion(),
                    request,
                    task.getBatchExternalId(),
                    true,
                    "async"
            );
            applyOutcome(task, template, request, outcome);
            asyncTaskRepository.save(task);
        } catch (TemplateValidationException ex) {
            handleFailure(task, ex);
        } catch (RuntimeException ex) {
            handleFailure(task, ex);
        }
    }

    static boolean isTerminalStatus(TaskStatus status) {
        return status == TaskStatus.SUCCEEDED
                || status == TaskStatus.FAILED
                || status == TaskStatus.PARTIAL_SUCCEEDED
                || status == TaskStatus.EXPIRED;
    }

    private void handleFailure(GenerationAsyncTaskEntity task, RuntimeException ex) {
        task.markFailed();
        BatchGenerateRequestBody request = null;
        TemplateEntity template = templateRepository.findByIdAndDeletedAtIsNull(task.getTemplateId()).orElse(null);
        if (template != null) {
            request = readRequestPayload(task.getRequestPayloadJson());
            scrubTerminalPayload(task, request);
            runtimeGenerationAuditRecorder.recordBatchAsyncCompletedFromTask(
                    template,
                    task,
                    request,
                    RuntimeGenerationAuditRecorder.OUTCOME_FAILURE,
                    null,
                    summarizeFailure(ex)
            );
            completeAsyncInvocation(template, task, request, null, TaskStatus.FAILED, ex);
        } else {
            scrubTerminalPayload(task, request);
        }
        asyncTaskRepository.save(task);
    }

    private void applyOutcome(
            GenerationAsyncTaskEntity task,
            TemplateEntity template,
            BatchGenerateRequestBody request,
            BatchExecutionService.BatchExecutionOutcome outcome
    ) {
        String batchResultJson = writeBatchResult(outcome.batchResult());
        String outcomeLabel;
        switch (outcome.taskStatus()) {
            case PARTIAL_SUCCEEDED -> {
                task.markPartialSucceeded(batchResultJson);
                outcomeLabel = RuntimeGenerationAuditRecorder.OUTCOME_FAILURE;
            }
            case FAILED -> {
                task.markFailed(batchResultJson);
                outcomeLabel = RuntimeGenerationAuditRecorder.OUTCOME_FAILURE;
            }
            default -> {
                task.markSucceeded(batchResultJson);
                outcomeLabel = RuntimeGenerationAuditRecorder.OUTCOME_SUCCESS;
            }
        }
        scrubTerminalPayload(task, request);
        runtimeGenerationAuditRecorder.recordBatchAsyncCompletedFromTask(
                template,
                task,
                request,
                outcomeLabel,
                "Batch " + outcome.taskStatus().name(),
                RuntimeGenerationAuditRecorder.OUTCOME_FAILURE.equals(outcomeLabel)
                        ? outcome.taskStatus().name()
                        : null
        );
        completeAsyncInvocation(
                template,
                task,
                request,
                outcome.batchResult(),
                outcome.taskStatus(),
                null
        );
    }

    private void scrubTerminalPayload(GenerationAsyncTaskEntity task, BatchGenerateRequestBody request) {
        if (request == null) {
            return;
        }
        task.scrubRequestPayload(payloadScrubber.scrub(request));
    }

    private void completeAsyncInvocation(
            TemplateEntity template,
            GenerationAsyncTaskEntity task,
            BatchGenerateRequestBody request,
            BatchResultView batchResult,
            TaskStatus taskStatus,
            RuntimeException failure
    ) {
        ApiPolicyEntity policy = apiPolicyRepository.findByTemplateId(template.getId()).orElse(null);
        if (policy == null) {
            return;
        }
        BatchResultView resolvedBatchResult = batchResult;
        if (resolvedBatchResult == null && failure != null) {
            resolvedBatchResult = new BatchResultView(
                    task.getBatchExternalId(),
                    new BatchSummaryView(0, 0, 0, 0, 0),
                    List.of()
            );
        }
        if (resolvedBatchResult == null) {
            return;
        }
        String outcome = taskStatus == TaskStatus.FAILED || taskStatus == TaskStatus.PARTIAL_SUCCEEDED
                ? RuntimeGenerationAuditRecorder.OUTCOME_FAILURE
                : RuntimeGenerationAuditRecorder.OUTCOME_SUCCESS;
        invocationRecordService.completeAsyncBatch(
                template,
                policy,
                RuntimeGenerationAuditRecorder.ASYNC_ENVIRONMENT,
                task.getRouteType(),
                null,
                task.getReleaseVersion(),
                request,
                task,
                resolvedBatchResult,
                taskStatus,
                outcome,
                traceIdProvider.newAuditId()
        );
    }

    static String summarizeFailure(RuntimeException ex) {
        if (ex instanceof TemplateValidationException validationException) {
            return validationException.messageKey();
        }
        return ex.getClass().getSimpleName();
    }

    private String writeBatchResult(com.bank.docgen.runtime.api.BatchResultView batchResult) {
        try {
            return objectMapper.writeValueAsString(batchResult);
        } catch (JsonProcessingException ex) {
            throw new TemplateValidationException("api.error.rendering.generationFailed");
        }
    }

    private BatchGenerateRequestBody readRequestPayload(String json) {
        try {
            return objectMapper.readValue(json, BatchGenerateRequestBody.class);
        } catch (JsonProcessingException ex) {
            throw new TemplateValidationException("api.error.validation.requestBodyInvalid");
        }
    }
}
