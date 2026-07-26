package com.bank.docgen.runtime.web;

import com.bank.docgen.rendering.EncryptionFailedException;
import com.bank.docgen.runtime.service.AsyncTaskCancellationNotAllowedException;
import com.bank.docgen.runtime.service.AsyncTaskExpiredException;
import com.bank.docgen.runtime.service.AsyncTaskNotFoundException;
import com.bank.docgen.runtime.service.IdempotencyConflictException;
import com.bank.docgen.runtime.service.IdempotencyDigestException;
import com.bank.docgen.runtime.service.OriginalBatchIdFormatException;
import com.bank.docgen.runtime.service.OriginalBatchNotFoundException;
import com.bank.docgen.runtime.service.RuntimeAccessDeniedException;
import com.bank.docgen.runtime.service.RuntimeBatchValidationException;
import com.bank.docgen.runtime.service.RuntimeDocumentNotFoundException;
import com.bank.docgen.runtime.service.RuntimeDownloadExpiredException;
import com.bank.docgen.runtime.service.RuntimeEncryptionValidationException;
import com.bank.docgen.runtime.service.GenerateRequestShapeException;
import com.bank.docgen.runtime.service.SyncBatchFailureException;
import com.bank.docgen.documentbrand.service.DocumentBrandResolveException;
import com.bank.docgen.runtime.service.TemplateLocaleMismatchException;
import com.bank.docgen.template.port.CompositionInclusionUnsatisfiedException;
import com.bank.docgen.template.port.ContentModuleJurisdictionMismatchException;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.ErrorEnvelope;
import com.bank.docgen.sharedkernel.api.ErrorEnvelopeFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(0)
public class RuntimeExceptionAdvice {

    private final ErrorEnvelopeFactory errorEnvelopeFactory;

    public RuntimeExceptionAdvice(ErrorEnvelopeFactory errorEnvelopeFactory) {
        this.errorEnvelopeFactory = errorEnvelopeFactory;
    }

    @ExceptionHandler(RuntimeDocumentNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handleRuntimeDocumentNotFound(HttpServletRequest request) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.NOT_FOUND,
                ApiErrorCodes.DOCUMENT_NOT_FOUND,
                ApiErrorCategories.RUNTIME,
                "api.error.runtime.documentNotFound"
        );
    }

    @ExceptionHandler(RuntimeDownloadExpiredException.class)
    public ResponseEntity<ErrorEnvelope> handleRuntimeDownloadExpired(HttpServletRequest request) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.GONE,
                ApiErrorCodes.DOWNLOAD_URL_EXPIRED,
                ApiErrorCategories.RUNTIME,
                "api.error.runtime.downloadUrlExpired"
        );
    }

    @ExceptionHandler(RuntimeAccessDeniedException.class)
    public ResponseEntity<ErrorEnvelope> handleRuntimeAccessDenied(HttpServletRequest request) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.FORBIDDEN,
                ApiErrorCodes.ACCESS_DENIED,
                ApiErrorCategories.RUNTIME,
                "api.error.authorization.accessDenied"
        );
    }

    @ExceptionHandler(RuntimeBatchValidationException.class)
    public ResponseEntity<ErrorEnvelope> handleRuntimeBatchValidation(
            HttpServletRequest request,
            RuntimeBatchValidationException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.errorCode(),
                ApiErrorCategories.RUNTIME,
                ex.messageKey()
        );
    }

    @ExceptionHandler(TemplateLocaleMismatchException.class)
    public ResponseEntity<ErrorEnvelope> handleTemplateLocaleMismatch(
            HttpServletRequest request,
            TemplateLocaleMismatchException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.errorCode(),
                ApiErrorCategories.TEMPLATE,
                ex.messageKey()
        );
    }

    @ExceptionHandler(CompositionInclusionUnsatisfiedException.class)
    public ResponseEntity<ErrorEnvelope> handleCompositionInclusionUnsatisfied(
            HttpServletRequest request,
            CompositionInclusionUnsatisfiedException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.errorCode(),
                ApiErrorCategories.TEMPLATE,
                ex.messageKey()
        );
    }

    @ExceptionHandler(DocumentBrandResolveException.class)
    public ResponseEntity<ErrorEnvelope> handleDocumentBrandResolve(
            HttpServletRequest request,
            DocumentBrandResolveException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.errorCode(),
                ApiErrorCategories.VALIDATION,
                ex.messageKey()
        );
    }

    @ExceptionHandler(ContentModuleJurisdictionMismatchException.class)
    public ResponseEntity<ErrorEnvelope> handleContentModuleJurisdictionMismatch(
            HttpServletRequest request,
            ContentModuleJurisdictionMismatchException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.errorCode(),
                ApiErrorCategories.TEMPLATE,
                ex.messageKey()
        );
    }

    @ExceptionHandler(OriginalBatchNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handleOriginalBatchNotFound(
            HttpServletRequest request,
            OriginalBatchNotFoundException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.NOT_FOUND,
                ex.errorCode(),
                ApiErrorCategories.BATCH,
                ex.messageKey()
        );
    }

    @ExceptionHandler(OriginalBatchIdFormatException.class)
    public ResponseEntity<ErrorEnvelope> handleOriginalBatchIdFormat(
            HttpServletRequest request,
            OriginalBatchIdFormatException ex
    ) {
        return errorEnvelopeFactory.validationError(
                request,
                ex.messageKey(),
                ex.fieldErrors()
        );
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorEnvelope> handleIdempotencyConflict(
            HttpServletRequest request,
            IdempotencyConflictException ex
    ) {
        return errorEnvelopeFactory.idempotencyConflict(request, ex);
    }

    @ExceptionHandler(IdempotencyDigestException.class)
    public ResponseEntity<ErrorEnvelope> handleIdempotencyDigestFailure(
            HttpServletRequest request,
            IdempotencyDigestException ex
    ) {
        return errorEnvelopeFactory.idempotencyDigestFailure(request, ex);
    }

    @ExceptionHandler(AsyncTaskNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handleAsyncTaskNotFound(HttpServletRequest request) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.NOT_FOUND,
                ApiErrorCodes.ASYNC_TASK_NOT_FOUND,
                ApiErrorCategories.RUNTIME,
                "api.error.runtime.asyncTaskNotFound"
        );
    }

    @ExceptionHandler(AsyncTaskCancellationNotAllowedException.class)
    public ResponseEntity<ErrorEnvelope> handleAsyncTaskCancellationNotAllowed(HttpServletRequest request) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.CONFLICT,
                ApiErrorCodes.ASYNC_TASK_CANCELLATION_NOT_ALLOWED,
                ApiErrorCategories.RUNTIME,
                "api.error.runtime.asyncTaskCancellationNotAllowed"
        );
    }

    @ExceptionHandler(AsyncTaskExpiredException.class)
    public ResponseEntity<ErrorEnvelope> handleAsyncTaskExpired(HttpServletRequest request) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.GONE,
                ApiErrorCodes.ASYNC_TASK_EXPIRED,
                ApiErrorCategories.RUNTIME,
                "api.error.runtime.asyncTaskExpired"
        );
    }

    @ExceptionHandler(SyncBatchFailureException.class)
    public ResponseEntity<ErrorEnvelope> handleSyncBatchFailure(
            HttpServletRequest request,
            SyncBatchFailureException ex
    ) {
        return errorEnvelopeFactory.syncBatchFailure(request, ex);
    }

    @ExceptionHandler(RuntimeEncryptionValidationException.class)
    public ResponseEntity<ErrorEnvelope> handleRuntimeEncryptionValidation(
            HttpServletRequest request,
            RuntimeEncryptionValidationException ex
    ) {
        String code = "api.error.encryption.encryptionNotAllowed".equals(ex.messageKey())
                ? ApiErrorCodes.ENCRYPTION_NOT_ALLOWED
                : ApiErrorCodes.ENCRYPTION_PARAMETER_INVALID;
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.BAD_REQUEST,
                code,
                ApiErrorCategories.ENCRYPTION,
                ex.messageKey()
        );
    }

    @ExceptionHandler(com.bank.docgen.rendering.PdfArchivalEncryptionMutexException.class)
    public ResponseEntity<ErrorEnvelope> handlePdfArchivalEncryptionMutex(
            HttpServletRequest request,
            com.bank.docgen.rendering.PdfArchivalEncryptionMutexException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.BAD_REQUEST,
                ApiErrorCodes.PDF_ARCHIVAL_ENCRYPTION_MUTEX,
                ApiErrorCategories.GENERATION,
                ex.messageKey()
        );
    }

    @ExceptionHandler(EncryptionFailedException.class)
    public ResponseEntity<ErrorEnvelope> handleEncryptionFailed(
            HttpServletRequest request,
            EncryptionFailedException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorCodes.ENCRYPTION_FAILED,
                ApiErrorCategories.ENCRYPTION,
                ex.messageKey()
        );
    }

    @ExceptionHandler(GenerateRequestShapeException.class)
    public ResponseEntity<ErrorEnvelope> handleGenerateRequestShape(
            HttpServletRequest request,
            GenerateRequestShapeException ex
    ) {
        return errorEnvelopeFactory.validationError(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.messageKey(),
                ex.fieldErrors()
        );
    }

    @ExceptionHandler(com.bank.docgen.sharedkernel.document.variable.VariableValidationException.class)
    public ResponseEntity<ErrorEnvelope> handleVariableValidationFailed(
            HttpServletRequest request,
            com.bank.docgen.sharedkernel.document.variable.VariableValidationException ex
    ) {
        return errorEnvelopeFactory.variableValidationFailed(request, ex);
    }

    @ExceptionHandler(com.bank.docgen.sharedkernel.document.compute.VariableComputeException.class)
    public ResponseEntity<ErrorEnvelope> handleVariableComputeFailed(
            HttpServletRequest request,
            com.bank.docgen.sharedkernel.document.compute.VariableComputeException ex
    ) {
        return errorEnvelopeFactory.variableComputeFailed(request, ex);
    }
}
