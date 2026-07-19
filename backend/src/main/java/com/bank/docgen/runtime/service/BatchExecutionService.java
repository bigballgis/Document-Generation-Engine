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
import com.bank.docgen.sharedkernel.api.FieldError;
import com.bank.docgen.sharedkernel.document.compute.VariableComputeException;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.rendering.RenderingOperationException;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.service.TemplateValidationException;
import com.bank.docgen.sharedkernel.document.variable.VariableValidationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class BatchExecutionService {

    private final DocumentGenerationEngine documentGenerationEngine;
    private final IdempotencyService idempotencyService;
    private final MessageResolver messageResolver;
    private final RuntimeFidelityWarningMapper fidelityWarningMapper;

    public BatchExecutionService(
            DocumentGenerationEngine documentGenerationEngine,
            @Lazy IdempotencyService idempotencyService,
            MessageResolver messageResolver,
            RuntimeFidelityWarningMapper fidelityWarningMapper
    ) {
        this.documentGenerationEngine = documentGenerationEngine;
        this.idempotencyService = idempotencyService;
        this.messageResolver = messageResolver;
        this.fidelityWarningMapper = fidelityWarningMapper;
    }

    public record BatchExecutionOutcome(BatchResultView batchResult, TaskStatus taskStatus) {
        public BatchExecutionOutcome {
            batchResult = new BatchResultView(
                    batchResult.batchId(),
                    batchResult.summary(),
                    batchResult.items(),
                    batchResult.originalBatchId()
            );
        }
    }

    public BatchExecutionOutcome execute(
            TemplateEntity template,
            String releaseVersion,
            BatchGenerateRequestBody request,
            String batchId,
            boolean continueOnItemFailure
    ) {
        return execute(template, releaseVersion, request, batchId, continueOnItemFailure, "sync");
    }

    public BatchExecutionOutcome execute(
            TemplateEntity template,
            String releaseVersion,
            BatchGenerateRequestBody request,
            String batchId,
            boolean continueOnItemFailure,
            String mode
    ) {
        List<BatchResultItemView> items = new ArrayList<>();
        List<DocumentGenerationEngine.GeneratedDocument> successfulDocuments = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        String requestLocale = request.context() == null ? null : request.context().locale();
        TemplateLocaleCompatibilitySupport.assertRequestLocaleCompatible(template, requestLocale);
        for (BatchGenerateRequestBody.BatchGenerateItemBody item : request.items()) {
            OutputOptionsView output = item.output() != null ? item.output() : request.output();
            EncryptionOptionsView encryption = item.encryption() != null ? item.encryption() : request.encryption();
            try {
                DocumentGenerationEngine.GeneratedDocument generated = documentGenerationEngine.generate(
                        template,
                        releaseVersion,
                        item.variables(),
                        output.format(),
                        encryption,
                        com.bank.docgen.authoring.structured.CallerRenderOverride.empty(),
                        mode,
                        requestLocale
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
                        fidelityWarningMapper.toWarnings(generated.fidelityWarningCodes())
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
        BatchResultView batchResult = new BatchResultView(
                batchId,
                summary,
                items,
                request.originalBatchId()
        );

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
        VariableValidationException validationFailed = findVariableValidation(ex);
        if (validationFailed != null) {
            String messageKey = validationFailed.messageKey();
            return new ErrorDetail(
                    ApiErrorCodes.VARIABLE_VALIDATION_FAILED,
                    ApiErrorCategories.VALIDATION,
                    messageResolver.resolve(messageKey),
                    messageKey,
                    false,
                    validationFailed.fieldErrors()
            );
        }
        VariableComputeException compute = findVariableCompute(ex);
        if (compute != null) {
            String messageKey = compute.messageKey();
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("variableKey", compute.variableKey());
            details.put("expressionSummary", compute.expressionSummary());
            List<FieldError> fieldErrors = List.of(
                    new FieldError("variableKey", "COMPUTE_FAILED", compute.variableKey()),
                    new FieldError("expressionSummary", "COMPUTE_FAILED", compute.expressionSummary())
            );
            return new ErrorDetail(
                    ApiErrorCodes.VARIABLE_COMPUTE_FAILED,
                    ApiErrorCategories.GENERATION,
                    messageResolver.resolve(messageKey),
                    messageKey,
                    false,
                    fieldErrors,
                    details
            );
        }
        String messageKey = ex instanceof TemplateValidationException validation
                ? validation.messageKey()
                : ex instanceof RenderingOperationException rendering
                        ? rendering.messageKey()
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

    private static VariableValidationException findVariableValidation(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof VariableValidationException validation) {
                return validation;
            }
            current = current.getCause();
        }
        return null;
    }

    private static VariableComputeException findVariableCompute(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof VariableComputeException compute) {
                return compute;
            }
            current = current.getCause();
        }
        return null;
    }
}
