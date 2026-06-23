package com.bank.docgen.runtime.service;

import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.BatchResultItemView;
import com.bank.docgen.runtime.api.BatchResultView;
import com.bank.docgen.runtime.api.BatchSummaryView;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.runtime.api.EncryptionSummaryView;
import com.bank.docgen.runtime.api.OutputOptionsView;
import com.bank.docgen.runtime.domain.TaskStatus;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskEntity;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.service.TemplateNotFoundException;
import com.bank.docgen.template.service.TemplateValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AsyncBatchTaskRunner {

    private final GenerationAsyncTaskRepository asyncTaskRepository;
    private final TemplateRepository templateRepository;
    private final DocumentGenerationEngine documentGenerationEngine;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public AsyncBatchTaskRunner(
            GenerationAsyncTaskRepository asyncTaskRepository,
            TemplateRepository templateRepository,
            DocumentGenerationEngine documentGenerationEngine,
            IdempotencyService idempotencyService,
            ObjectMapper objectMapper
    ) {
        this.asyncTaskRepository = asyncTaskRepository;
        this.templateRepository = templateRepository;
        this.documentGenerationEngine = documentGenerationEngine;
        this.idempotencyService = idempotencyService;
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
        if (task.getStatus() == TaskStatus.SUCCEEDED || task.getStatus() == TaskStatus.PROCESSING) {
            return;
        }
        task.markProcessing();
        asyncTaskRepository.save(task);
        TemplateEntity template = templateRepository.findByIdAndDeletedAtIsNull(task.getTemplateId())
                .orElseThrow(TemplateNotFoundException::new);
        BatchGenerateRequestBody request = readRequestPayload(task.getRequestPayloadJson());
        BatchResultView batchResult = executeBatch(template, task.getReleaseVersion(), request);
        task.markSucceeded(writeBatchResult(batchResult));
        asyncTaskRepository.save(task);
    }

    private BatchResultView executeBatch(
            TemplateEntity template,
            String releaseVersion,
            BatchGenerateRequestBody request
    ) {
        String batchId = taskBatchId();
        List<BatchResultItemView> items = new ArrayList<>();
        for (BatchGenerateRequestBody.BatchGenerateItemBody item : request.items()) {
            OutputOptionsView output = item.output() != null ? item.output() : request.output();
            EncryptionOptionsView encryption = item.encryption() != null ? item.encryption() : request.encryption();
            DocumentGenerationEngine.GeneratedDocument generated = documentGenerationEngine.generate(
                    template,
                    releaseVersion,
                    item.variables(),
                    output.format(),
                    encryption
            );
            idempotencyService.registerDownloadableDocument(
                    template.getId(),
                    generated.documentId(),
                    generated.storageKey()
            );
            items.add(new BatchResultItemView(
                    item.itemId(),
                    "SUCCEEDED",
                    output,
                    EncryptionSummaryView.fromRequest(output.format(), encryption),
                    generated.documentId(),
                    generated.fidelityWarningCodes()
            ));
        }
        return new BatchResultView(
                batchId,
                new BatchSummaryView(items.size(), items.size(), items.size(), 0, 0),
                items
        );
    }

    private String taskBatchId() {
        return "BATCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String writeBatchResult(BatchResultView batchResult) {
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
