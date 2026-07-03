package com.bank.docgen.runtime.web;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.runtime.domain.InvocationViewValidationException;
import com.bank.docgen.runtime.service.InvocationNotFoundException;
import com.bank.docgen.runtime.service.InvocationRecordExpiredException;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.ErrorDetail;
import com.bank.docgen.sharedkernel.api.ErrorEnvelope;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RuntimeInvocationExceptionAdvice {

    private final TraceIdProvider traceIdProvider;
    private final MessageResolver messageResolver;

    public RuntimeInvocationExceptionAdvice(TraceIdProvider traceIdProvider, MessageResolver messageResolver) {
        this.traceIdProvider = traceIdProvider;
        this.messageResolver = messageResolver;
    }

    @ExceptionHandler(InvocationNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handleInvocationNotFound(HttpServletRequest request) {
        return domainError(
                request,
                HttpStatus.NOT_FOUND,
                ApiErrorCodes.INVOCATION_NOT_FOUND,
                ApiErrorCategories.RUNTIME,
                "api.error.runtime.invocationNotFound"
        );
    }

    @ExceptionHandler(InvocationRecordExpiredException.class)
    public ResponseEntity<ErrorEnvelope> handleInvocationRecordExpired(HttpServletRequest request) {
        return domainError(
                request,
                HttpStatus.GONE,
                ApiErrorCodes.INVOCATION_RECORD_EXPIRED,
                ApiErrorCategories.RUNTIME,
                "api.error.runtime.invocationRecordExpired"
        );
    }

    @ExceptionHandler(InvocationViewValidationException.class)
    public ResponseEntity<ErrorEnvelope> handleInvocationViewInvalid(HttpServletRequest request) {
        return domainError(
                request,
                HttpStatus.BAD_REQUEST,
                ApiErrorCodes.INVOCATION_VIEW_INVALID,
                ApiErrorCategories.VALIDATION,
                "api.error.runtime.invocationViewInvalid"
        );
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
}
