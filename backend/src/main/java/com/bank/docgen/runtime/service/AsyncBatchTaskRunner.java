package com.bank.docgen.runtime.service;

import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.domain.TaskStatus;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskEntity;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.service.TemplateNotFoundException;
import com.bank.docgen.template.service.TemplateValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
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

    public AsyncBatchTaskRunner(
            GenerationAsyncTaskRepository asyncTaskRepository,
            TemplateRepository templateRepository,
            BatchExecutionService batchExecutionService,
            RuntimeGenerationAuditRecorder runtimeGenerationAuditRecorder,
            ObjectMapper objectMapper
    ) {
        this.asyncTaskRepository = asyncTaskRepository;
        this.templateRepository = templateRepository;
        this.batchExecutionService = batchExecutionService;
        this.runtimeGenerationAuditRecorder = runtimeGenerationAuditRecorder;
        this.objectMapper = objectMapper;
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
                    true
            );
            applyOutcome(task, template, request, outcome);
            asyncTaskRepository.save(task);
        } catch (RuntimeException ex) {
            task.markFailed();
            asyncTaskRepository.save(task);
            TemplateEntity template = templateRepository.findByIdAndDeletedAtIsNull(task.getTemplateId()).orElse(null);
            if (template != null) {
                BatchGenerateRequestBody request = readRequestPayload(task.getRequestPayloadJson());
                runtimeGenerationAuditRecorder.recordBatchAsyncCompletedFromTask(
                        template,
                        task,
                        request,
                        RuntimeGenerationAuditRecorder.OUTCOME_FAILURE,
                        null,
                        summarizeFailure(ex)
                );
            }
        }
    }

    private boolean isTerminalStatus(TaskStatus status) {
        return status == TaskStatus.SUCCEEDED
                || status == TaskStatus.FAILED
                || status == TaskStatus.PARTIAL_SUCCEEDED
                || status == TaskStatus.EXPIRED
                || status == TaskStatus.PROCESSING;
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
    }

    private String summarizeFailure(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null ? ex.getClass().getSimpleName() : message;
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
