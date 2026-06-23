package com.bank.docgen.runtime.security;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.ErrorDetail;
import com.bank.docgen.sharedkernel.api.ErrorEnvelope;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RuntimeRateLimitFilter extends OncePerRequestFilter {

    private final RuntimeRateLimitService rateLimitService;
    private final TraceIdProvider traceIdProvider;
    private final MessageResolver messageResolver;
    private final ObjectMapper objectMapper;

    public RuntimeRateLimitFilter(
            RuntimeRateLimitService rateLimitService,
            TraceIdProvider traceIdProvider,
            MessageResolver messageResolver,
            ObjectMapper objectMapper
    ) {
        this.rateLimitService = rateLimitService;
        this.traceIdProvider = traceIdProvider;
        this.messageResolver = messageResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!rateLimitService.enabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return path.startsWith("/api/management/") || !path.matches("/api/[^/]+/v1/.*");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String credentialId = request.getHeader(ApiCredentialAuthenticationFilter.HEADER_CREDENTIAL_ID);
        String accessAccount = request.getHeader(ApiCredentialAuthenticationFilter.HEADER_ACCESS_ACCOUNT);
        if (credentialId == null || credentialId.isBlank() || accessAccount == null || accessAccount.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        ConsumptionProbe probe = rateLimitService.tryConsume(credentialId.trim(), accessAccount.trim());
        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }
        long retryAfterSeconds = Math.max(1L, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
        writeRateLimitResponse(request, response, retryAfterSeconds);
    }

    private void writeRateLimitResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            long retryAfterSeconds
    ) throws IOException {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        String messageKey = "api.error.runtime.rateLimitExceeded";
        ErrorDetail error = new ErrorDetail(
                ApiErrorCodes.RATE_LIMIT_EXCEEDED,
                ApiErrorCategories.RUNTIME,
                messageResolver.resolve(messageKey),
                messageKey,
                true,
                null
        );
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getOutputStream(),
                new ErrorEnvelope(Metadata.minimal(auditId, traceId), error)
        );
    }
}
