package com.bank.docgen.authorization.management.web;

import com.bank.docgen.authorization.management.service.SessionAbsoluteLimitReachedException;
import com.bank.docgen.authorization.management.session.SessionRevocationUnavailableException;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.ErrorDetail;
import com.bank.docgen.sharedkernel.api.ErrorEnvelope;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Envelope mappings for LR-B6 session renewal/revocation failures raised inside controllers
 * (filter-stage failures go through the authentication entry point instead).
 *
 * <p>Highest precedence so {@code GlobalExceptionHandler}'s catch-all {@code Exception}
 * handler never shadows these mappings.</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ManagementSessionExceptionHandler {

    private final TraceIdProvider traceIdProvider;
    private final MessageResolver messageResolver;

    public ManagementSessionExceptionHandler(TraceIdProvider traceIdProvider, MessageResolver messageResolver) {
        this.traceIdProvider = traceIdProvider;
        this.messageResolver = messageResolver;
    }

    /**
     * Scenario 8.2: renewal past the absolute session limit requires a fresh login.
     */
    @ExceptionHandler(SessionAbsoluteLimitReachedException.class)
    public ResponseEntity<ErrorEnvelope> handleSessionAbsoluteLimitReached(HttpServletRequest request) {
        return errorEnvelope(
                request,
                HttpStatus.UNAUTHORIZED,
                ApiErrorCodes.SESSION_ABSOLUTE_LIMIT_REACHED,
                "api.error.authentication.sessionAbsoluteLimitReached",
                false
        );
    }

    /**
     * Boundary B6: a failed revocation write must not pretend logout/renewal worked.
     */
    @ExceptionHandler(SessionRevocationUnavailableException.class)
    public ResponseEntity<ErrorEnvelope> handleSessionRevocationUnavailable(HttpServletRequest request) {
        return errorEnvelope(
                request,
                HttpStatus.SERVICE_UNAVAILABLE,
                ApiErrorCodes.SESSION_VALIDATION_UNAVAILABLE,
                "api.error.authentication.sessionValidationUnavailable",
                true
        );
    }

    private ResponseEntity<ErrorEnvelope> errorEnvelope(
            HttpServletRequest request,
            HttpStatus status,
            String code,
            String messageKey,
            boolean retryable
    ) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        ErrorDetail error = new ErrorDetail(
                code,
                ApiErrorCategories.AUTHENTICATION,
                messageResolver.resolve(messageKey),
                messageKey,
                retryable,
                null
        );
        return ResponseEntity.status(status)
                .body(new ErrorEnvelope(Metadata.minimal(auditId, traceId), error));
    }
}
