package com.bank.docgen.infrastructure.config;

import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.ErrorDetail;
import com.bank.docgen.sharedkernel.api.ErrorEnvelope;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ManagementSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final TraceIdProvider traceIdProvider;
    private final MessageResolver messageResolver;
    private final ObjectMapper objectMapper;

    public ManagementSecurityHandlers(
            TraceIdProvider traceIdProvider,
            MessageResolver messageResolver,
            ObjectMapper objectMapper
    ) {
        this.traceIdProvider = traceIdProvider;
        this.messageResolver = messageResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
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
        writeError(
                response,
                request,
                HttpStatus.FORBIDDEN,
                ApiErrorCodes.ACCESS_DENIED,
                ApiErrorCategories.AUTHORIZATION,
                "api.error.authorization.accessDenied",
                false
        );
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
