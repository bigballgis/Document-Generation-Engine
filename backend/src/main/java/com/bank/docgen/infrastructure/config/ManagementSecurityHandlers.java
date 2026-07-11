package com.bank.docgen.infrastructure.config;

import com.bank.docgen.authorization.management.service.SecurityAuditSummaryService;
import com.bank.docgen.authorization.management.web.JwtAuthenticationFilter;
import com.bank.docgen.authorization.management.web.ManagementAuthentication;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.ErrorDetail;
import com.bank.docgen.sharedkernel.api.ErrorEnvelope;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ManagementSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final TraceIdProvider traceIdProvider;
    private final MessageResolver messageResolver;
    private final ObjectMapper objectMapper;
    private final SecurityAuditSummaryService securityAuditSummaryService;

    public ManagementSecurityHandlers(
            TraceIdProvider traceIdProvider,
            MessageResolver messageResolver,
            ObjectMapper objectMapper,
            SecurityAuditSummaryService securityAuditSummaryService
    ) {
        this.traceIdProvider = traceIdProvider;
        this.messageResolver = messageResolver;
        this.objectMapper = objectMapper;
        this.securityAuditSummaryService = securityAuditSummaryService;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        // LR-B6: the JWT filter flags revocation-list outcomes so 401s stay distinguishable
        // by error code (SESSION_REVOKED / SESSION_VALIDATION_UNAVAILABLE vs SESSION_EXPIRED).
        Object sessionValidationFailure =
                request.getAttribute(JwtAuthenticationFilter.SESSION_VALIDATION_FAILURE_ATTRIBUTE);
        if (ApiErrorCodes.SESSION_REVOKED.equals(sessionValidationFailure)) {
            writeError(
                    response,
                    request,
                    HttpStatus.UNAUTHORIZED,
                    ApiErrorCodes.SESSION_REVOKED,
                    ApiErrorCategories.AUTHENTICATION,
                    "api.error.authentication.sessionRevoked",
                    false
            );
            return;
        }
        if (ApiErrorCodes.SESSION_VALIDATION_UNAVAILABLE.equals(sessionValidationFailure)) {
            writeError(
                    response,
                    request,
                    HttpStatus.UNAUTHORIZED,
                    ApiErrorCodes.SESSION_VALIDATION_UNAVAILABLE,
                    ApiErrorCategories.AUTHENTICATION,
                    "api.error.authentication.sessionValidationUnavailable",
                    true
            );
            return;
        }
        writeError(
                response,
                request,
                HttpStatus.UNAUTHORIZED,
                ApiErrorCodes.SESSION_EXPIRED,
                ApiErrorCategories.AUTHENTICATION,
                "api.error.authentication.sessionExpired",
                false
        );
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        recordRouteAccessDeniedIfAuthenticated(request, auditId, traceId);
        writeError(
                response,
                HttpStatus.FORBIDDEN,
                ApiErrorCodes.ACCESS_DENIED,
                ApiErrorCategories.AUTHORIZATION,
                "api.error.authorization.accessDenied",
                false,
                auditId,
                traceId
        );
    }

    private void recordRouteAccessDeniedIfAuthenticated(
            HttpServletRequest request,
            String auditId,
            String traceId
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof ManagementAuthentication managementAuthentication)) {
            return;
        }
        Object principal = managementAuthentication.getPrincipal();
        if (!(principal instanceof ManagementSessionClaims claims)) {
            return;
        }
        String routeKey = summarizeRouteKey(request);
        securityAuditSummaryService.recordRouteAccessDenied(
                claims.username(),
                routeKey,
                SecurityAuditSummaryService.REASON_ACCESS_DENIED,
                auditId,
                traceId
        );
    }

    private static String summarizeRouteKey(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isBlank()) {
            path = request.getRequestURI();
        }
        if (path == null || path.isBlank()) {
            return "unknown";
        }
        int queryIndex = path.indexOf('?');
        return queryIndex >= 0 ? path.substring(0, queryIndex) : path;
    }

    private void writeError(
            HttpServletResponse response,
            HttpServletRequest request,
            HttpStatus status,
            String code,
            String category,
            String messageKey,
            boolean retryable
    ) throws IOException {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        writeError(response, status, code, category, messageKey, retryable, auditId, traceId);
    }

    private void writeError(
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String category,
            String messageKey,
            boolean retryable,
            String auditId,
            String traceId
    ) throws IOException {
        ErrorDetail error = new ErrorDetail(
                code,
                category,
                messageResolver.resolve(messageKey),
                messageKey,
                retryable,
                null
        );
        ErrorEnvelope envelope = new ErrorEnvelope(Metadata.minimal(auditId, traceId), error);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), envelope);
    }
}
