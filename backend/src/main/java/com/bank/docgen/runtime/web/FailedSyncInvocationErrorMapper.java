package com.bank.docgen.runtime.web;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.rendering.EncryptionFailedException;
import com.bank.docgen.rendering.RenderingOperationException;
import com.bank.docgen.runtime.domain.InvocationErrorEnvelope;
import com.bank.docgen.runtime.service.OriginalBatchIdFormatException;
import com.bank.docgen.runtime.service.OriginalBatchNotFoundException;
import com.bank.docgen.runtime.service.RuntimeBatchValidationException;
import com.bank.docgen.runtime.service.RuntimeEncryptionValidationException;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.document.compute.VariableComputeException;
import com.bank.docgen.template.service.TemplateValidationException;

/**
 * Maps runtime sync-generate failures to the same unified error envelope fields returned
 * by platform {@code ErrorEnvelope} / exception advice (CE-U11 IRC-006).
 */
final class FailedSyncInvocationErrorMapper {

    private FailedSyncInvocationErrorMapper() {
    }

    static InvocationErrorEnvelope from(Throwable throwable, MessageResolver messageResolver) {
        if (throwable instanceof TemplateValidationException ex) {
            if ("api.error.validation.requestBodyInvalid".equals(ex.messageKey())) {
                return envelope(
                        ApiErrorCodes.REQUEST_BODY_INVALID,
                        ApiErrorCategories.VALIDATION,
                        ex.messageKey(),
                        false,
                        messageResolver
                );
            }
            return envelope(
                    ApiErrorCodes.TEMPLATE_VALIDATION_FAILED,
                    ApiErrorCategories.TEMPLATE,
                    ex.messageKey(),
                    false,
                    messageResolver
            );
        }
        if (throwable instanceof RenderingOperationException ex) {
            return envelope(
                    ApiErrorCodes.RENDERING_FAILED,
                    ApiErrorCategories.GENERATION,
                    ex.messageKey(),
                    false,
                    messageResolver
            );
        }
        if (throwable instanceof RuntimeEncryptionValidationException ex) {
            String code = "api.error.encryption.encryptionNotAllowed".equals(ex.messageKey())
                    ? ApiErrorCodes.ENCRYPTION_NOT_ALLOWED
                    : ApiErrorCodes.ENCRYPTION_PARAMETER_INVALID;
            return envelope(code, ApiErrorCategories.ENCRYPTION, ex.messageKey(), false, messageResolver);
        }
        if (throwable instanceof com.bank.docgen.rendering.PdfArchivalEncryptionMutexException ex) {
            return envelope(
                    ApiErrorCodes.PDF_ARCHIVAL_ENCRYPTION_MUTEX,
                    ApiErrorCategories.GENERATION,
                    ex.messageKey(),
                    false,
                    messageResolver
            );
        }
        if (throwable instanceof EncryptionFailedException ex) {
            return envelope(
                    ApiErrorCodes.ENCRYPTION_FAILED,
                    ApiErrorCategories.ENCRYPTION,
                    ex.messageKey(),
                    false,
                    messageResolver
            );
        }
        if (throwable instanceof VariableComputeException ex) {
            return envelope(
                    ApiErrorCodes.VARIABLE_COMPUTE_FAILED,
                    ApiErrorCategories.GENERATION,
                    ex.messageKey(),
                    false,
                    messageResolver
            );
        }
        if (throwable instanceof RuntimeBatchValidationException ex) {
            return envelope(
                    ex.errorCode(),
                    ApiErrorCategories.RUNTIME,
                    ex.messageKey(),
                    false,
                    messageResolver
            );
        }
        if (throwable instanceof OriginalBatchNotFoundException ex) {
            return envelope(
                    ex.errorCode(),
                    ApiErrorCategories.BATCH,
                    ex.messageKey(),
                    false,
                    messageResolver
            );
        }
        if (throwable instanceof OriginalBatchIdFormatException ex) {
            return envelope(
                    ApiErrorCodes.REQUEST_BODY_INVALID,
                    ApiErrorCategories.VALIDATION,
                    ex.messageKey(),
                    false,
                    messageResolver
            );
        }
        return null;
    }

    private static InvocationErrorEnvelope envelope(
            String code,
            String category,
            String messageKey,
            boolean retryable,
            MessageResolver messageResolver
    ) {
        String message = null;
        if (messageResolver != null && messageKey != null) {
            try {
                message = messageResolver.resolve(messageKey);
            } catch (RuntimeException ignored) {
                message = null;
            }
        }
        return new InvocationErrorEnvelope(code, category, messageKey, retryable, message);
    }
}
