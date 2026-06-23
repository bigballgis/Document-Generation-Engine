package com.bank.docgen.sharedkernel.api;

import com.bank.docgen.authorization.management.service.InvalidCredentialsException;
import com.bank.docgen.authorization.management.service.ManagementConflictException;
import com.bank.docgen.authorization.management.service.ManagementForbiddenException;
import com.bank.docgen.authorization.management.service.ManagementNotFoundException;
import com.bank.docgen.authorization.management.service.SessionExpiredException;
import com.bank.docgen.apimgmt.service.ApiManagementAccessDeniedException;
import com.bank.docgen.apimgmt.service.ApiManagementNotFoundException;
import com.bank.docgen.audit.service.AuditAccessDeniedException;
import com.bank.docgen.audit.service.AuditValidationException;
import com.bank.docgen.runtime.service.AsyncTaskCancellationNotAllowedException;
import com.bank.docgen.runtime.service.AsyncTaskNotFoundException;
import com.bank.docgen.runtime.service.IdempotencyConflictException;
import com.bank.docgen.runtime.service.RuntimeAccessDeniedException;
import com.bank.docgen.runtime.service.RuntimeBatchValidationException;
import com.bank.docgen.runtime.service.RuntimeDocumentNotFoundException;
import com.bank.docgen.runtime.service.RuntimeDownloadExpiredException;
import com.bank.docgen.rendering.EncryptionFailedException;
import com.bank.docgen.runtime.service.RuntimeEncryptionValidationException;
import com.bank.docgen.rendering.service.PreviewGenerationException;
import com.bank.docgen.rendering.service.PreviewNotFoundException;
import com.bank.docgen.rendering.DocxAssemblyException;
import com.bank.docgen.infrastructure.storage.ObjectStorageException;
import com.bank.docgen.template.service.TemplateAccessDeniedException;
import com.bank.docgen.template.service.TemplateNotFoundException;
import com.bank.docgen.template.service.TestDataSetNotFoundException;
import com.bank.docgen.template.service.TemplateValidationException;
import com.bank.docgen.master.service.MasterAccessDeniedException;
import com.bank.docgen.master.service.MasterNotFoundException;
import com.bank.docgen.master.service.MasterValidationException;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final TraceIdProvider traceIdProvider;
    private final MessageResolver messageResolver;

    public GlobalExceptionHandler(TraceIdProvider traceIdProvider, MessageResolver messageResolver) {
        this.traceIdProvider = traceIdProvider;
        this.messageResolver = messageResolver;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorEnvelope> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        String messageKey = "api.error.validation.requestBodyInvalid";
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        ErrorDetail error = new ErrorDetail(
                ApiErrorCodes.REQUEST_BODY_INVALID,
                ApiErrorCategories.VALIDATION,
                messageResolver.resolve(messageKey),
                messageKey,
                false,
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorEnvelope(Metadata.minimal(auditId, traceId), error));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorEnvelope> handleInvalidCredentials(HttpServletRequest request) {
        return authenticationError(
                request,
                ApiErrorCodes.INVALID_CREDENTIALS,
                "api.error.authentication.invalidCredentials"
        );
    }

    @ExceptionHandler(SessionExpiredException.class)
    public ResponseEntity<ErrorEnvelope> handleSessionExpired(HttpServletRequest request) {
        return authenticationError(
                request,
                ApiErrorCodes.SESSION_EXPIRED,
                "api.error.authentication.sessionExpired"
        );
    }

    @ExceptionHandler(MasterNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handleMasterNotFound(HttpServletRequest request) {
        return domainError(
                request,
                HttpStatus.NOT_FOUND,
                ApiErrorCodes.MASTER_NOT_FOUND,
                ApiErrorCategories.MASTER,
                "api.error.master.notFound"
        );
    }

    @ExceptionHandler(MasterAccessDeniedException.class)
    public ResponseEntity<ErrorEnvelope> handleMasterAccessDenied(HttpServletRequest request) {
        return domainError(
                request,
                HttpStatus.FORBIDDEN,
                ApiErrorCodes.ACCESS_DENIED,
                ApiErrorCategories.MASTER,
                "api.error.master.accessDenied"
        );
    }

    @ExceptionHandler(MasterValidationException.class)
    public ResponseEntity<ErrorEnvelope> handleMasterValidation(
            HttpServletRequest request,
            MasterValidationException ex
    ) {
        return domainError(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                ApiErrorCodes.MASTER_VALIDATION_FAILED,
                ApiErrorCategories.MASTER,
                ex.messageKey()
        );
    }

    @ExceptionHandler(TemplateNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handleTemplateNotFound(HttpServletRequest request) {
        return domainError(
                request,
                HttpStatus.NOT_FOUND,
                ApiErrorCodes.TEMPLATE_NOT_FOUND,
                ApiErrorCategories.TEMPLATE,
                "api.error.template.notFound"
        );
    }

    @ExceptionHandler(TestDataSetNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handleTestDataSetNotFound(HttpServletRequest request) {
        return domainError(
                request,
                HttpStatus.NOT_FOUND,
                ApiErrorCodes.TEST_DATA_SET_NOT_FOUND,
                ApiErrorCategories.TEMPLATE,
                "api.error.template.testDataSetNotFound"
        );
    }

    @ExceptionHandler(TemplateAccessDeniedException.class)
    public ResponseEntity<ErrorEnvelope> handleTemplateAccessDenied(HttpServletRequest request) {
        return domainError(
                request,
                HttpStatus.FORBIDDEN,
                ApiErrorCodes.ACCESS_DENIED,
                ApiErrorCategories.TEMPLATE,
                "api.error.template.accessDenied"
        );
    }

    @ExceptionHandler(TemplateValidationException.class)
    public ResponseEntity<ErrorEnvelope> handleTemplateValidation(
            HttpServletRequest request,
            TemplateValidationException ex
    ) {
        return domainError(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                ApiErrorCodes.TEMPLATE_VALIDATION_FAILED,
                ApiErrorCategories.TEMPLATE,
                ex.messageKey()
        );
    }

    @ExceptionHandler(PreviewNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handlePreviewNotFound(HttpServletRequest request) {
        return domainError(
                request,
                HttpStatus.NOT_FOUND,
                ApiErrorCodes.PREVIEW_NOT_FOUND,
                ApiErrorCategories.RENDERING,
                "api.error.rendering.previewNotFound"
        );
    }

    @ExceptionHandler(PreviewGenerationException.class)
    public ResponseEntity<ErrorEnvelope> handlePreviewGeneration(
            HttpServletRequest request,
            PreviewGenerationException ex
    ) {
        return domainError(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                ApiErrorCodes.RENDERING_FAILED,
                ApiErrorCategories.RENDERING,
                ex.messageKey()
        );
    }

    @ExceptionHandler(ApiManagementNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handleApiPolicyNotFound(HttpServletRequest request) {
        return domainError(
                request,
                HttpStatus.NOT_FOUND,
                ApiErrorCodes.API_POLICY_NOT_FOUND,
                ApiErrorCategories.APIMGMT,
                "api.error.apimgmt.policyNotFound"
        );
    }

    @ExceptionHandler(ApiManagementAccessDeniedException.class)
    public ResponseEntity<ErrorEnvelope> handleApiManagementAccessDenied(HttpServletRequest request) {
        return domainError(
                request,
                HttpStatus.FORBIDDEN,
                ApiErrorCodes.ACCESS_DENIED,
                ApiErrorCategories.APIMGMT,
                "api.error.apimgmt.accessDenied"
        );
    }

    @ExceptionHandler(AuditAccessDeniedException.class)
    public ResponseEntity<ErrorEnvelope> handleAuditAccessDenied(HttpServletRequest request) {
        return domainError(
                request,
                HttpStatus.FORBIDDEN,
                ApiErrorCodes.ACCESS_DENIED,
                ApiErrorCategories.AUDIT,
                "api.error.authorization.accessDenied"
        );
    }

    @ExceptionHandler(AuditValidationException.class)
    public ResponseEntity<ErrorEnvelope> handleAuditValidation(
            HttpServletRequest request,
            AuditValidationException ex
    ) {
        return domainError(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.errorCode(),
                ApiErrorCategories.AUDIT,
                ex.messageKey()
        );
    }

    @ExceptionHandler(RuntimeDocumentNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handleRuntimeDocumentNotFound(HttpServletRequest request) {
        return domainError(
                request,
                HttpStatus.NOT_FOUND,
                ApiErrorCodes.DOCUMENT_NOT_FOUND,
                ApiErrorCategories.RUNTIME,
                "api.error.runtime.documentNotFound"
        );
    }

    @ExceptionHandler(RuntimeDownloadExpiredException.class)
    public ResponseEntity<ErrorEnvelope> handleRuntimeDownloadExpired(HttpServletRequest request) {
        return domainError(
                request,
                HttpStatus.GONE,
                ApiErrorCodes.DOWNLOAD_URL_EXPIRED,
                ApiErrorCategories.RUNTIME,
                "api.error.runtime.downloadUrlExpired"
        );
    }

    @ExceptionHandler(RuntimeAccessDeniedException.class)
    public ResponseEntity<ErrorEnvelope> handleRuntimeAccessDenied(HttpServletRequest request) {
        return domainError(
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
        return domainError(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.errorCode(),
                ApiErrorCategories.RUNTIME,
                ex.messageKey()
        );
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorEnvelope> handleIdempotencyConflict(
            HttpServletRequest request,
            IdempotencyConflictException ex
    ) {
        return domainError(
                request,
                HttpStatus.CONFLICT,
                ApiErrorCodes.IDEMPOTENCY_KEY_CONFLICT,
                ApiErrorCategories.RUNTIME,
                ex.messageKey()
        );
    }

    @ExceptionHandler(AsyncTaskNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handleAsyncTaskNotFound(HttpServletRequest request) {
        return domainError(
                request,
                HttpStatus.NOT_FOUND,
                ApiErrorCodes.ASYNC_TASK_NOT_FOUND,
                ApiErrorCategories.RUNTIME,
                "api.error.runtime.asyncTaskNotFound"
        );
    }

    @ExceptionHandler(AsyncTaskCancellationNotAllowedException.class)
    public ResponseEntity<ErrorEnvelope> handleAsyncTaskCancellationNotAllowed(HttpServletRequest request) {
        return domainError(
                request,
                HttpStatus.CONFLICT,
                ApiErrorCodes.ASYNC_TASK_CANCELLATION_NOT_ALLOWED,
                ApiErrorCategories.RUNTIME,
                "api.error.runtime.asyncTaskCancellationNotAllowed"
        );
    }

    @ExceptionHandler(RuntimeEncryptionValidationException.class)
    public ResponseEntity<ErrorEnvelope> handleRuntimeEncryptionValidation(
            HttpServletRequest request,
            RuntimeEncryptionValidationException ex
    ) {
        String code = "api.error.encryption.encryptionNotAllowed".equals(ex.messageKey())
                ? ApiErrorCodes.ENCRYPTION_NOT_ALLOWED
                : ApiErrorCodes.ENCRYPTION_PARAMETER_INVALID;
        return domainError(
                request,
                HttpStatus.BAD_REQUEST,
                code,
                ApiErrorCategories.ENCRYPTION,
                ex.messageKey()
        );
    }

    @ExceptionHandler(EncryptionFailedException.class)
    public ResponseEntity<ErrorEnvelope> handleEncryptionFailed(
            HttpServletRequest request,
            EncryptionFailedException ex
    ) {
        return domainError(
                request,
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorCodes.ENCRYPTION_FAILED,
                ApiErrorCategories.ENCRYPTION,
                ex.messageKey()
        );
    }

    @ExceptionHandler(ManagementNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handleManagementNotFound(
            HttpServletRequest request,
            ManagementNotFoundException ex
    ) {
        return domainError(
                request,
                HttpStatus.NOT_FOUND,
                ex.errorCode(),
                ApiErrorCategories.NOT_FOUND,
                ex.messageKey()
        );
    }

    @ExceptionHandler(ManagementConflictException.class)
    public ResponseEntity<ErrorEnvelope> handleManagementConflict(
            HttpServletRequest request,
            ManagementConflictException ex
    ) {
        return domainError(
                request,
                HttpStatus.CONFLICT,
                ex.errorCode(),
                ApiErrorCategories.CONFLICT,
                ex.messageKey()
        );
    }

    @ExceptionHandler(ManagementForbiddenException.class)
    public ResponseEntity<ErrorEnvelope> handleManagementForbidden(
            HttpServletRequest request,
            ManagementForbiddenException ex
    ) {
        return domainError(
                request,
                HttpStatus.FORBIDDEN,
                ex.errorCode(),
                ApiErrorCategories.AUTHORIZATION,
                ex.messageKey()
        );
    }

    @ExceptionHandler(ObjectStorageException.class)
    public ResponseEntity<ErrorEnvelope> handleObjectStorage(
            HttpServletRequest request,
            ObjectStorageException ex
    ) {
        return domainError(
                request,
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorCodes.INTERNAL_ERROR,
                ApiErrorCategories.GENERATION,
                ex.messageKey()
        );
    }

    @ExceptionHandler(DocxAssemblyException.class)
    public ResponseEntity<ErrorEnvelope> handleDocxAssembly(
            HttpServletRequest request,
            DocxAssemblyException ex
    ) {
        return domainError(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                ApiErrorCodes.RENDERING_FAILED,
                ApiErrorCategories.RENDERING,
                ex.messageKey()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorEnvelope> handleIllegalState(HttpServletRequest request) {
        return domainError(
                request,
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorCodes.INTERNAL_ERROR,
                ApiErrorCategories.GENERATION,
                "api.error.generation.internalError"
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorEnvelope> handleUnexpected(HttpServletRequest request, Exception ex) {
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

    private ResponseEntity<ErrorEnvelope> authenticationError(
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

    private ResponseEntity<ErrorEnvelope> domainError(
            HttpServletRequest request,
            HttpStatus status,
            String code,
            String category,
            String messageKey
    ) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        ErrorDetail error = new ErrorDetail(
                code,
                category,
                messageResolver.resolve(messageKey),
                messageKey,
                false,
                null
        );
        return ResponseEntity.status(status)
                .body(new ErrorEnvelope(Metadata.minimal(auditId, traceId), error));
    }

    private FieldError toFieldError(org.springframework.validation.FieldError error) {
        String messageKey = validationMessageKey(error);
        return new FieldError(
                error.getField(),
                validationReason(error),
                messageResolver.resolveOrDefault(messageKey, error.getDefaultMessage())
        );
    }

    private String validationMessageKey(org.springframework.validation.FieldError error) {
        String code = error.getCode();
        if (code == null) {
            return "api.error.validation.fieldInvalid";
        }
        if (isRequiredConstraint(code)) {
            return "api.error.validation.fieldRequired";
        }
        if ("Size".equals(code)) {
            return "api.error.validation.fieldSizeInvalid";
        }
        if ("Pattern".equals(code)) {
            return "api.error.validation.fieldPatternInvalid";
        }
        return "api.error.validation.fieldInvalid";
    }

    private boolean isRequiredConstraint(String code) {
        return "NotBlank".equals(code) || "NotNull".equals(code) || "NotEmpty".equals(code);
    }

    private String validationReason(org.springframework.validation.FieldError error) {
        String code = error.getCode();
        if (code == null) {
            return "RULE_FAILED";
        }
        if (isRequiredConstraint(code)) {
            return "REQUIRED";
        }
        if ("Size".equals(code)) {
            Object[] arguments = error.getArguments();
            if (arguments != null && arguments.length >= 2
                    && error.getRejectedValue() instanceof String rejected
                    && rejected.length() > ((Number) arguments[1]).intValue()) {
                return "TOO_LONG";
            }
            return "TOO_SHORT";
        }
        if ("Pattern".equals(code)) {
            return "PATTERN_MISMATCH";
        }
        return "RULE_FAILED";
    }
}
