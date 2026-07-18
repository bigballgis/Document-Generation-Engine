package com.bank.docgen.sharedkernel.api;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.infrastructure.resilience.GenerationServiceUnavailableException;
import com.bank.docgen.infrastructure.resilience.GenerationTimeoutException;
import com.bank.docgen.rendering.PdfConversionCapacityExceededException;
import com.bank.docgen.runtime.service.IdempotencyConflictException;
import com.bank.docgen.runtime.service.IdempotencyDigestException;
import com.bank.docgen.runtime.service.SyncBatchFailureException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ErrorEnvelopeFactory {

    private final TraceIdProvider traceIdProvider;
    private final MessageResolver messageResolver;

    public ErrorEnvelopeFactory(TraceIdProvider traceIdProvider, MessageResolver messageResolver) {
        this.traceIdProvider = traceIdProvider;
        this.messageResolver = messageResolver;
    }

    public ResponseEntity<ErrorEnvelope> domainError(
            HttpServletRequest request,
            HttpStatus status,
            String code,
            String category,
            String messageKey
    ) {
        return domainError(request, status, code, category, messageKey, false);
    }

    public ResponseEntity<ErrorEnvelope> domainError(
            HttpServletRequest request,
            HttpStatus status,
            String code,
            String category,
            String messageKey,
            boolean retryable
    ) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        ErrorDetail error = new ErrorDetail(
                code,
                category,
                messageResolver.resolve(messageKey),
                messageKey,
                retryable,
                null
        );
        return ResponseEntity.status(status)
                .body(new ErrorEnvelope(Metadata.minimal(auditId, traceId), error));
    }

    public ResponseEntity<ErrorEnvelope> authenticationError(
            HttpServletRequest request,
            String code,
            String messageKey
    ) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        ErrorDetail error = new ErrorDetail(
                code,
                ApiErrorCategories.AUTHENTICATION,
                messageResolver.resolve(messageKey),
                messageKey,
                false,
                null
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorEnvelope(Metadata.minimal(auditId, traceId), error));
    }

    public ResponseEntity<ErrorEnvelope> renderingDomainError(
            HttpServletRequest request,
            HttpStatus status,
            String code,
            String messageKey
    ) {
        return domainError(request, status, code, ApiErrorCategories.RENDERING, messageKey);
    }

    public ResponseEntity<ErrorEnvelope> validationError(
            HttpServletRequest request,
            String messageKey,
            List<FieldError> fieldErrors
    ) {
        return validationError(request, HttpStatus.BAD_REQUEST, messageKey, fieldErrors);
    }

    public ResponseEntity<ErrorEnvelope> validationError(
            HttpServletRequest request,
            HttpStatus status,
            String messageKey,
            List<FieldError> fieldErrors
    ) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        ErrorDetail error = new ErrorDetail(
                ApiErrorCodes.REQUEST_BODY_INVALID,
                ApiErrorCategories.VALIDATION,
                messageResolver.resolve(messageKey),
                messageKey,
                false,
                fieldErrors
        );
        return ResponseEntity.status(status)
                .body(new ErrorEnvelope(Metadata.minimal(auditId, traceId), error));
    }

    public ResponseEntity<ErrorEnvelope> idempotencyConflict(
            HttpServletRequest request,
            IdempotencyConflictException ex
    ) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        String messageKey = ex.messageKey();
        Map<String, Object> conflictSummary = new LinkedHashMap<>();
        conflictSummary.put("idempotencyKey", ex.idempotencyKey());
        conflictSummary.put("conflictType", ex.conflictType());
        if (ex.originalResolvedReleaseVersion() != null) {
            conflictSummary.put("originalResolvedReleaseVersion", ex.originalResolvedReleaseVersion());
        }
        ErrorDetail error = new ErrorDetail(
                ApiErrorCodes.IDEMPOTENCY_KEY_CONFLICT,
                ApiErrorCategories.IDEMPOTENCY,
                messageResolver.resolve(messageKey),
                messageKey,
                false,
                null,
                conflictSummary
        );
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorEnvelope(Metadata.minimal(auditId, traceId), error));
    }

    public ResponseEntity<ErrorEnvelope> idempotencyDigestFailure(
            HttpServletRequest request,
            IdempotencyDigestException ex
    ) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        String messageKey = ex.messageKey();
        ErrorDetail error = new ErrorDetail(
                ApiErrorCodes.IDEMPOTENCY_DIGEST_FAILED,
                ApiErrorCategories.GENERATION,
                messageResolver.resolve(messageKey),
                messageKey,
                true,
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorEnvelope(Metadata.minimal(auditId, traceId), error));
    }

    public ResponseEntity<ErrorEnvelope> syncBatchFailure(
            HttpServletRequest request,
            SyncBatchFailureException ex
    ) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        String messageKey = ex.messageKey();
        List<BatchErrorItemView> errorItems = ex.batchResult().items().stream()
                .map(item -> new BatchErrorItemView(item.itemId(), item.status(), item.error()))
                .toList();
        ErrorDetail error = new ErrorDetail(
                ApiErrorCodes.BATCH_PROCESSING_FAILED,
                ApiErrorCategories.RUNTIME,
                messageResolver.resolve(messageKey),
                messageKey,
                false,
                null,
                null,
                errorItems
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorEnvelope(Metadata.minimal(auditId, traceId), error));
    }

    public ResponseEntity<ErrorEnvelope> pdfConversionCapacityExceeded(
            HttpServletRequest request,
            PdfConversionCapacityExceededException ex
    ) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        String messageKey = ex.messageKey();
        ErrorDetail error = new ErrorDetail(
                ApiErrorCodes.PDF_CONVERSION_CAPACITY_EXCEEDED,
                ApiErrorCategories.GENERATION,
                messageResolver.resolve(messageKey),
                messageKey,
                true,
                null
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorEnvelope(Metadata.minimal(auditId, traceId), error));
    }

    public ResponseEntity<ErrorEnvelope> generationServiceUnavailable(
            HttpServletRequest request,
            GenerationServiceUnavailableException ex
    ) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        String messageKey = ex.messageKey();
        ErrorDetail error = new ErrorDetail(
                ApiErrorCodes.GENERATION_SERVICE_UNAVAILABLE,
                ApiErrorCategories.GENERATION,
                messageResolver.resolve(messageKey),
                messageKey,
                true,
                null
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorEnvelope(Metadata.minimal(auditId, traceId), error));
    }

    public ResponseEntity<ErrorEnvelope> generationTimeout(
            HttpServletRequest request,
            GenerationTimeoutException ex
    ) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        String messageKey = ex.messageKey();
        ErrorDetail error = new ErrorDetail(
                ApiErrorCodes.GENERATION_TIMEOUT,
                ApiErrorCategories.GENERATION,
                messageResolver.resolve(messageKey),
                messageKey,
                true,
                null
        );
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(new ErrorEnvelope(Metadata.minimal(auditId, traceId), error));
    }

    public ResponseEntity<ErrorEnvelope> variableValidationFailed(
            HttpServletRequest request,
            com.bank.docgen.sharedkernel.document.variable.VariableValidationException ex
    ) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        String messageKey = ex.messageKey();
        ErrorDetail error = new ErrorDetail(
                ApiErrorCodes.VARIABLE_VALIDATION_FAILED,
                ApiErrorCategories.VALIDATION,
                messageResolver.resolve(messageKey),
                messageKey,
                false,
                ex.fieldErrors()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorEnvelope(Metadata.minimal(auditId, traceId), error));
    }

    public ResponseEntity<ErrorEnvelope> variableComputeFailed(
            HttpServletRequest request,
            com.bank.docgen.sharedkernel.document.compute.VariableComputeException ex
    ) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        String messageKey = ex.messageKey();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("variableKey", ex.variableKey());
        details.put("expressionSummary", ex.expressionSummary());
        List<FieldError> fieldErrors = List.of(
                new FieldError("variableKey", "COMPUTE_FAILED", ex.variableKey()),
                new FieldError("expressionSummary", "COMPUTE_FAILED", ex.expressionSummary())
        );
        ErrorDetail error = new ErrorDetail(
                ApiErrorCodes.VARIABLE_COMPUTE_FAILED,
                ApiErrorCategories.GENERATION,
                messageResolver.resolve(messageKey),
                messageKey,
                false,
                fieldErrors,
                details
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorEnvelope(Metadata.minimal(auditId, traceId), error));
    }

    public ResponseEntity<ErrorEnvelope> masterRevisionInUse(
            HttpServletRequest request,
            com.bank.docgen.master.service.MasterRevisionInUseException ex
    ) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        String messageKey = "api.error.master.revisionInUseByPublishedRelease";
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("referencedReleases", ex.references());
        ErrorDetail error = new ErrorDetail(
                ApiErrorCodes.MASTER_REVISION_IN_USE_BY_PUBLISHED_RELEASE,
                ApiErrorCategories.CONFLICT,
                messageResolver.resolve(messageKey),
                messageKey,
                false,
                null,
                details
        );
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorEnvelope(Metadata.minimal(auditId, traceId), error));
    }

    public ResponseEntity<ErrorEnvelope> importDependenciesUnsatisfied(
            HttpServletRequest request,
            Object dependencyReport
    ) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        String messageKey = "api.error.template.importDependenciesUnsatisfied";
        ErrorDetail error = new ErrorDetail(
                ApiErrorCodes.IMPORT_DEPENDENCIES_UNSATISFIED,
                ApiErrorCategories.TEMPLATE,
                messageResolver.resolve(messageKey),
                messageKey,
                false,
                null,
                null,
                null,
                dependencyReport
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorEnvelope(Metadata.minimal(auditId, traceId), error));
    }

    public ResponseEntity<ErrorEnvelope> unexpectedError(HttpServletRequest request) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        String messageKey = "api.error.generation.internalError";
        ErrorDetail error = new ErrorDetail(
                ApiErrorCodes.INTERNAL_ERROR,
                ApiErrorCategories.GENERATION,
                messageResolver.resolveOrDefault(messageKey, "An internal error occurred."),
                messageKey,
                true,
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorEnvelope(Metadata.minimal(auditId, traceId), error));
    }
}
