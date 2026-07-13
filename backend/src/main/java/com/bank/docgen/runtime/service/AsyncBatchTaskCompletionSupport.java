package com.bank.docgen.runtime.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.BatchResultView;
import com.bank.docgen.runtime.api.BatchSummaryView;
import com.bank.docgen.runtime.domain.TaskStatus;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskEntity;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskRepository;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import java.util.List;

/**
 * Package-private completion / failure / invocation helpers for async batch tasks.
 */
final class AsyncBatchTaskCompletionSupport {

    private final GenerationAsyncTaskRepository asyncTaskRepository;
    private final TemplateRepository templateRepository;
    private final RuntimeGenerationAuditRecorder runtimeGenerationAuditRecorder;
    private final ApiPolicyRepository apiPolicyRepository;
    private final InvocationRecordService invocationRecordService;
    private final TraceIdProvider traceIdProvider;
    private final AsyncBatchPayloadScrubber payloadScrubber;
    private final BatchGenerationJsonSupport jsonSupport;

    AsyncBatchTaskCompletionSupport(
            GenerationAsyncTaskRepository asyncTaskRepository,
            TemplateRepository templateRepository,
            RuntimeGenerationAuditRecorder runtimeGenerationAuditRecorder,
            ApiPolicyRepository apiPolicyRepository,
            InvocationRecordService invocationRecordService,
            TraceIdProvider traceIdProvider,
            AsyncBatchPayloadScrubber payloadScrubber,
            BatchGenerationJsonSupport jsonSupport
    ) {
        this.asyncTaskRepository = asyncTaskRepository;
        this.templateRepository = templateRepository;
        this.runtimeGenerationAuditRecorder = runtimeGenerationAuditRecorder;
        this.apiPolicyRepository = apiPolicyRepository;
        this.invocationRecordService = invocationRecordService;
        this.traceIdProvider = traceIdProvider;
        this.payloadScrubber = payloadScrubber;
        this.jsonSupport = jsonSupport;
    }

    void handleFailure(GenerationAsyncTaskEntity task, RuntimeException ex) {
        task.markFailed();
        BatchGenerateRequestBody request = null;
        TemplateEntity template = templateRepository.findByIdAndDeletedAtIsNull(task.getTemplateId()).orElse(null);
        if (template != null) {
            request = jsonSupport.readRequestPayload(task.getRequestPayloadJson());
            scrubTerminalPayload(task, request);
            runtimeGenerationAuditRecorder.recordBatchAsyncCompletedFromTask(
                    template,
                    task,
                    request,
                    RuntimeGenerationAuditRecorder.OUTCOME_FAILURE,
                    null,
                    AsyncBatchTaskRunner.summarizeFailure(ex)
            );
            completeAsyncInvocation(template, task, request, null, TaskStatus.FAILED, ex);
        } else {
            scrubTerminalPayload(task, request);
        }
        asyncTaskRepository.save(task);
    }

    void applyOutcome(
            GenerationAsyncTaskEntity task,
            TemplateEntity template,
            BatchGenerateRequestBody request,
            BatchExecutionService.BatchExecutionOutcome outcome
    ) {
        String batchResultJson = jsonSupport.writeBatchResult(outcome.batchResult());
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
}
