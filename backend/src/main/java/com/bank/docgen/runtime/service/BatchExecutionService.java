package com.bank.docgen.runtime.service;

import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.BatchResultItemView;
import com.bank.docgen.runtime.api.BatchResultView;
import com.bank.docgen.runtime.api.BatchSummaryView;
import com.bank.docgen.runtime.api.EncryptionSummaryView;
import com.bank.docgen.runtime.api.OutputOptionsView;
import com.bank.docgen.runtime.domain.TaskStatus;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.sharedkernel.api.ErrorDetail;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.service.TemplateValidationException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BatchExecutionService {

    private final DocumentGenerationEngine documentGenerationEngine;
    private final IdempotencyService idempotencyService;
    private final MessageResolver messageResolver;

    public BatchExecutionService(
            DocumentGenerationEngine documentGenerationEngine,
            IdempotencyService idempotencyService,
            MessageResolver messageResolver
    ) {
        this.documentGenerationEngine = documentGenerationEngine;
        this.idempotencyService = idempotencyService;
        this.messageResolver = messageResolver;
    }

    public record BatchExecutionOutcome(BatchResultView batchResult, TaskStatus taskStatus) {
    }

    public BatchExecutionOutcome execute(
            TemplateEntity template,
            String releaseVersion,
            BatchGenerateRequestBody request,
            String batchId,
            boolean continueOnItemFailure
    ) {
        List<BatchResultItemView> items = new ArrayList<>();
        List<DocumentGenerationEngine.GeneratedDocument> successfulDocuments = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (BatchGenerateRequestBody.BatchGenerateItemBody item : request.items()) {
            OutputOptionsView output = item.output() != null ? item.output() : request.output();
            EncryptionOptionsView encryption = item.encryption() != null ? item.encryption() : request.encryption();
            try {
                DocumentGenerationEngine.GeneratedDocument generated = documentGenerationEngine.generate(
                        template,
                        releaseVersion,
                        item.variables(),
                        output.format(),
                        encryption
                );
                if (continueOnItemFailure) {
                    idempotencyService.registerDownloadableDocument(
                            template.getId(),
                            generated.documentId(),
                            generated.storageKey()
                    );
                } else {
                    successfulDocuments.add(generated);
                }
                items.add(new BatchResultItemView(
                        item.itemId(),
                        "SUCCEEDED",
                        output,
                        EncryptionSummaryView.fromRequest(output.format(), encryption),
                        generated.documentId(),
                        generated.fidelityWarningCodes()
                ));
                successCount++;
            } catch (RuntimeException ex) {
                items.add(new BatchResultItemView(
                        item.itemId(),
                        "FAILED",
                        output,
                        EncryptionSummaryView.fromRequest(output.format(), encryption),
                        null,
                        List.of(),
                        toItemError(ex)
                ));
                failureCount++;
            }
        }

        BatchSummaryView summary = new BatchSummaryView(
                items.size(),
                items.size(),
                successCount,
                failureCount,
                0
        );
        BatchResultView batchResult = new BatchResultView(batchId, summary, items);

        if (!continueOnItemFailure && failureCount > 0) {
            throw new SyncBatchFailureException(batchResult);
        }

        if (!continueOnItemFailure) {
            for (DocumentGenerationEngine.GeneratedDocument generated : successfulDocuments) {
                idempotencyService.registerDownloadableDocument(
                        template.getId(),
                        generated.documentId(),
                        generated.storageKey()
                );
            }
        }

        TaskStatus taskStatus;
        if (failureCount == 0) {
            taskStatus = TaskStatus.SUCCEEDED;
        } else if (successCount == 0) {
            taskStatus = TaskStatus.FAILED;
        } else {
            taskStatus = TaskStatus.PARTIAL_SUCCEEDED;
        }
        return new BatchExecutionOutcome(batchResult, taskStatus);
    }

    private ErrorDetail toItemError(RuntimeException ex) {
        String messageKey = ex instanceof TemplateValidationException validation
                ? validation.messageKey()
                : "api.error.rendering.generationFailed";
        return new ErrorDetail(
                ApiErrorCodes.RENDERING_FAILED,
                ApiErrorCategories.RENDERING,
                messageResolver.resolve(messageKey),
                messageKey,
                false,
                null
        );
    }
}
