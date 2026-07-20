package com.bank.docgen.rendering.web;

import com.bank.docgen.infrastructure.storage.ObjectStorageException;
import com.bank.docgen.rendering.DocxAssemblyException;
import com.bank.docgen.rendering.PdfArchivalEncryptionMutexException;
import com.bank.docgen.rendering.RenderingOperationException;
import com.bank.docgen.rendering.PdfConversionCapacityExceededException;
import com.bank.docgen.rendering.service.BatchTestRunNotFoundException;
import com.bank.docgen.rendering.service.PreviewArtifactExpiredException;
import com.bank.docgen.rendering.service.PreviewArtifactNotAvailableException;
import com.bank.docgen.rendering.service.PreviewConcurrencyLimitException;
import com.bank.docgen.rendering.service.PreviewGenerationException;
import com.bank.docgen.rendering.service.PreviewNotFoundException;
import com.bank.docgen.rendering.service.PreviewValidationException;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.ErrorEnvelope;
import com.bank.docgen.sharedkernel.api.ErrorEnvelopeFactory;
import com.bank.docgen.documentbrand.service.DocumentBrandResolveException;
import com.bank.docgen.template.port.CompositionInclusionUnsatisfiedException;
import com.bank.docgen.template.port.ContentModuleJurisdictionMismatchException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(0)
public class RenderingExceptionAdvice {

    private final ErrorEnvelopeFactory errorEnvelopeFactory;

    public RenderingExceptionAdvice(ErrorEnvelopeFactory errorEnvelopeFactory) {
        this.errorEnvelopeFactory = errorEnvelopeFactory;
    }

    @ExceptionHandler(PdfConversionCapacityExceededException.class)
    public ResponseEntity<ErrorEnvelope> handlePdfConversionCapacityExceeded(
            HttpServletRequest request,
            PdfConversionCapacityExceededException ex
    ) {
        return errorEnvelopeFactory.pdfConversionCapacityExceeded(request, ex);
    }

    @ExceptionHandler(PdfArchivalEncryptionMutexException.class)
    public ResponseEntity<ErrorEnvelope> handlePdfArchivalEncryptionMutex(
            HttpServletRequest request,
            PdfArchivalEncryptionMutexException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.BAD_REQUEST,
                ApiErrorCodes.PDF_ARCHIVAL_ENCRYPTION_MUTEX,
                ApiErrorCategories.GENERATION,
                ex.messageKey()
        );
    }

    @ExceptionHandler(PreviewNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handlePreviewNotFound(HttpServletRequest request) {
        return errorEnvelopeFactory.renderingDomainError(
                request,
                HttpStatus.NOT_FOUND,
                ApiErrorCodes.PREVIEW_NOT_FOUND,
                "api.error.rendering.previewNotFound"
        );
    }

    @ExceptionHandler(PreviewArtifactNotAvailableException.class)
    public ResponseEntity<ErrorEnvelope> handlePreviewArtifactNotAvailable(HttpServletRequest request) {
        return errorEnvelopeFactory.renderingDomainError(
                request,
                HttpStatus.NOT_FOUND,
                ApiErrorCodes.PREVIEW_NOT_FOUND,
                "api.error.rendering.previewArtifactNotAvailable"
        );
    }

    @ExceptionHandler(PreviewArtifactExpiredException.class)
    public ResponseEntity<ErrorEnvelope> handlePreviewArtifactExpired(HttpServletRequest request) {
        return errorEnvelopeFactory.renderingDomainError(
                request,
                HttpStatus.GONE,
                ApiErrorCodes.PREVIEW_ARTIFACT_EXPIRED,
                "api.error.rendering.previewArtifactExpired"
        );
    }

    @ExceptionHandler(PreviewConcurrencyLimitException.class)
    public ResponseEntity<ErrorEnvelope> handlePreviewConcurrencyLimit(HttpServletRequest request) {
        return errorEnvelopeFactory.renderingDomainError(
                request,
                HttpStatus.TOO_MANY_REQUESTS,
                ApiErrorCodes.PREVIEW_CONCURRENCY_LIMIT_EXCEEDED,
                "api.error.rendering.previewConcurrencyLimitExceeded"
        );
    }

    @ExceptionHandler(BatchTestRunNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handleBatchTestRunNotFound(HttpServletRequest request) {
        return errorEnvelopeFactory.renderingDomainError(
                request,
                HttpStatus.NOT_FOUND,
                ApiErrorCodes.BATCH_TEST_RUN_NOT_FOUND,
                "api.error.rendering.batchTestRunNotFound"
        );
    }

    @ExceptionHandler(PreviewValidationException.class)
    public ResponseEntity<ErrorEnvelope> handlePreviewValidation(
            HttpServletRequest request,
            PreviewValidationException ex
    ) {
        return errorEnvelopeFactory.renderingDomainError(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                ApiErrorCodes.REQUEST_BODY_INVALID,
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

    @ExceptionHandler(PreviewGenerationException.class)
    public ResponseEntity<ErrorEnvelope> handlePreviewGeneration(
            HttpServletRequest request,
            PreviewGenerationException ex
    ) {
        Throwable cause = ex.getCause();
        if (cause instanceof com.bank.docgen.sharedkernel.document.variable.VariableValidationException validationEx) {
            return errorEnvelopeFactory.variableValidationFailed(request, validationEx);
        }
        if (cause instanceof com.bank.docgen.sharedkernel.document.compute.VariableComputeException computeEx) {
            return errorEnvelopeFactory.variableComputeFailed(request, computeEx);
        }
        if (cause instanceof CompositionInclusionUnsatisfiedException inclusionEx) {
            return handleCompositionInclusionUnsatisfied(request, inclusionEx);
        }
        if (cause instanceof ContentModuleJurisdictionMismatchException mismatchEx) {
            return handleContentModuleJurisdictionMismatch(request, mismatchEx);
        }
        if (cause instanceof DocxAssemblyException assemblyException) {
            return errorEnvelopeFactory.domainError(
                    request,
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    assemblyException.errorCode(),
                    assemblyException.category(),
                    assemblyException.messageKey()
            );
        }
        return errorEnvelopeFactory.renderingDomainError(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                ApiErrorCodes.RENDERING_FAILED,
                ex.messageKey()
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

    @ExceptionHandler(DocxAssemblyException.class)
    public ResponseEntity<ErrorEnvelope> handleDocxAssembly(
            HttpServletRequest request,
            DocxAssemblyException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.errorCode(),
                ex.category(),
                ex.messageKey()
        );
    }

    @ExceptionHandler(RenderingOperationException.class)
    public ResponseEntity<ErrorEnvelope> handleRenderingOperation(
            HttpServletRequest request,
            RenderingOperationException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                ApiErrorCodes.RENDERING_FAILED,
                ApiErrorCategories.GENERATION,
                ex.messageKey()
        );
    }

    @ExceptionHandler(ObjectStorageException.class)
    public ResponseEntity<ErrorEnvelope> handleObjectStorage(
            HttpServletRequest request,
            ObjectStorageException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorCodes.INTERNAL_ERROR,
                ApiErrorCategories.GENERATION,
                ex.messageKey()
        );
    }
}
