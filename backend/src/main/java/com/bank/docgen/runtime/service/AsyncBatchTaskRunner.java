package com.bank.docgen.runtime.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.runtime.domain.TaskStatus;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskEntity;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.service.TemplateNotFoundException;
import com.bank.docgen.rendering.RenderingOperationException;
import com.bank.docgen.template.service.TemplateValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final AsyncBatchTaskStaleReclaimService staleReclaimService;
    private final BatchGenerationJsonSupport jsonSupport;
    private final AsyncBatchTaskCompletionSupport completion;

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
        this.staleReclaimService = staleReclaimService;
        this.jsonSupport = new BatchGenerationJsonSupport(objectMapper);
        this.completion = new AsyncBatchTaskCompletionSupport(
                asyncTaskRepository,
                templateRepository,
                runtimeGenerationAuditRecorder,
                apiPolicyRepository,
                invocationRecordService,
                traceIdProvider,
                payloadScrubber,
                jsonSupport
        );
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
            BatchGenerateRequestBody request = jsonSupport.readRequestPayload(task.getRequestPayloadJson());
            BatchExecutionService.BatchExecutionOutcome outcome = batchExecutionService.execute(
                    template,
                    task.getReleaseVersion(),
                    request,
                    task.getBatchExternalId(),
                    true,
                    "async"
            );
            completion.applyOutcome(task, template, request, outcome);
            asyncTaskRepository.save(task);
        } catch (RenderingOperationException | TemplateValidationException ex) {
            completion.handleFailure(task, ex);
        } catch (RuntimeException ex) {
            completion.handleFailure(task, ex);
        }
    }

    static boolean isTerminalStatus(TaskStatus status) {
        return status == TaskStatus.SUCCEEDED
                || status == TaskStatus.FAILED
                || status == TaskStatus.PARTIAL_SUCCEEDED
                || status == TaskStatus.EXPIRED;
    }

    static String summarizeFailure(RuntimeException ex) {
        if (ex instanceof RenderingOperationException renderingException) {
            return renderingException.messageKey();
        }
        if (ex instanceof TemplateValidationException validationException) {
            return validationException.messageKey();
        }
        return ex.getClass().getSimpleName();
    }
}
