package com.bank.docgen.runtime.web;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.infrastructure.resilience.GenerationServiceUnavailableException;
import com.bank.docgen.infrastructure.resilience.GenerationTimeoutException;
import com.bank.docgen.rendering.EncryptionFailedException;
import com.bank.docgen.rendering.PdfConversionCapacityExceededException;
import com.bank.docgen.rendering.RenderingOperationException;
import com.bank.docgen.runtime.domain.InvocationErrorEnvelope;
import com.bank.docgen.runtime.service.OriginalBatchIdFormatException;
import com.bank.docgen.runtime.service.OriginalBatchNotFoundException;
import com.bank.docgen.runtime.service.RuntimeBatchValidationException;
import com.bank.docgen.runtime.service.RuntimeEncryptionValidationException;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.document.compute.VariableComputeException;
import com.bank.docgen.runtime.service.TemplateLocaleMismatchException;
import com.bank.docgen.template.port.CompositionInclusionUnsatisfiedException;
import com.bank.docgen.template.port.ContentModuleJurisdictionMismatchException;
import com.bank.docgen.template.service.TemplateValidationException;
import com.bank.docgen.sharedkernel.document.variable.VariableValidationException;

/**
 * Maps runtime sync-generate failures to the same unified error envelope fields returned
 * by platform {@code ErrorEnvelope} / exception advice (CE-U11 IRC-006).
 */
final class FailedSyncInvocationErrorMapper {

    private FailedSyncInvocationErrorMapper() {
    }

    static InvocationErrorEnvelope from(Throwable throwable, MessageResolver messageResolver) {
        if (throwable instanceof TemplateLocaleMismatchException ex) {
            return envelope(
                    ex.errorCode(),
                    ApiErrorCategories.TEMPLATE,
                    ex.messageKey(),
                    false,
                    messageResolver
            );
        }
        if (throwable instanceof CompositionInclusionUnsatisfiedException ex) {
            return envelope(
                    ex.errorCode(),
                    ApiErrorCategories.TEMPLATE,
                    ex.messageKey(),
                    false,
                    messageResolver
            );
        }
        if (throwable instanceof ContentModuleJurisdictionMismatchException ex) {
            return envelope(
                    ex.errorCode(),
                    ApiErrorCategories.TEMPLATE,
                    ex.messageKey(),
                    false,
                    messageResolver
            );
        }
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
        if (throwable instanceof VariableValidationException ex) {
            return envelope(
                    ApiErrorCodes.VARIABLE_VALIDATION_FAILED,
                    ApiErrorCategories.VALIDATION,
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
        InvocationErrorEnvelope generationEnvelope = mapGenerationTaxonomy(throwable, messageResolver);
        if (generationEnvelope != null) {
            return generationEnvelope;
        }
        return null;
    }

    /**
     * PRR-D01b: align IRC persistence with D01A HTTP taxonomy (walk cause chain).
     */
    private static InvocationErrorEnvelope mapGenerationTaxonomy(
            Throwable throwable,
            MessageResolver messageResolver
    ) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof GenerationServiceUnavailableException ex) {
                return envelope(
                        ApiErrorCodes.GENERATION_SERVICE_UNAVAILABLE,
                        ApiErrorCategories.GENERATION,
                        ex.messageKey(),
                        true,
                        messageResolver
                );
            }
            if (current instanceof GenerationTimeoutException ex) {
                return envelope(
                        ApiErrorCodes.GENERATION_TIMEOUT,
                        ApiErrorCategories.GENERATION,
                        ex.messageKey(),
                        true,
                        messageResolver
                );
            }
            if (current instanceof PdfConversionCapacityExceededException ex) {
                return envelope(
                        ApiErrorCodes.PDF_CONVERSION_CAPACITY_EXCEEDED,
                        ApiErrorCategories.GENERATION,
                        ex.messageKey(),
                        true,
                        messageResolver
                );
            }
            current = current.getCause();
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
