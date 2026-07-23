package com.bank.docgen.runtime.security;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.runtime.metrics.RateLimitBackendUnavailableMetrics;
import com.bank.docgen.runtime.metrics.RateLimitDeniedMetrics;
import com.bank.docgen.runtime.service.RuntimeGenerationAuditRecorder;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.ErrorDetail;
import com.bank.docgen.sharedkernel.api.ErrorEnvelope;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    /** Fixed Retry-After hint (seconds) when the distributed rate-limit backend is unavailable. */
    static final long BACKEND_UNAVAILABLE_RETRY_AFTER_SECONDS = 1L;

    private final RuntimeRateLimiter rateLimitService;
    private final TraceIdProvider traceIdProvider;
    private final MessageResolver messageResolver;
    private final ObjectMapper objectMapper;
    private final RuntimeGenerationAuditRecorder runtimeGenerationAuditRecorder;
    private final RateLimitDeniedMetrics rateLimitDeniedMetrics;
    private final RateLimitBackendUnavailableMetrics rateLimitBackendUnavailableMetrics;

    public RuntimeRateLimitFilter(
            RuntimeRateLimiter rateLimitService,
            TraceIdProvider traceIdProvider,
            MessageResolver messageResolver,
            ObjectMapper objectMapper,
            RuntimeGenerationAuditRecorder runtimeGenerationAuditRecorder,
            RateLimitDeniedMetrics rateLimitDeniedMetrics,
            RateLimitBackendUnavailableMetrics rateLimitBackendUnavailableMetrics
    ) {
        this.rateLimitService = rateLimitService;
        this.traceIdProvider = traceIdProvider;
        this.messageResolver = messageResolver;
        this.objectMapper = objectMapper;
        this.runtimeGenerationAuditRecorder = runtimeGenerationAuditRecorder;
        this.rateLimitDeniedMetrics = rateLimitDeniedMetrics;
        this.rateLimitBackendUnavailableMetrics = rateLimitBackendUnavailableMetrics;
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
            // LR-B7 / F7-C2: missing credential headers pass through (no IP bucket). Auth fails closed
            // downstream with 401. Shared Redis coordination is opt-in via distributed=true (PQH-F7).
            filterChain.doFilter(request, response);
            return;
        }
        String rateLimitKey = credentialId.trim() + ":" + accessAccount.trim();
        RateLimitDecision decision = rateLimitService.tryConsumeKey(rateLimitKey);
        if (decision.isAllowed()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (decision.isBackendUnavailable()) {
            writeBackendUnavailableResponse(request, response);
            return;
        }
        long retryAfterSeconds = Math.max(1L, TimeUnit.NANOSECONDS.toSeconds(decision.nanosToWaitForRefill()));
        writeRateLimitResponse(request, response, retryAfterSeconds, credentialId.trim(), accessAccount.trim());
    }

    private void writeRateLimitResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            long retryAfterSeconds,
            String credentialId,
            String accessAccount
    ) throws IOException {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        runtimeGenerationAuditRecorder.recordRateLimitDenied(
                resolveEnvironment(request.getRequestURI()),
                credentialId.trim(),
                accessAccount.trim(),
                traceId,
                auditId
        );
        rateLimitDeniedMetrics.record();
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

    private void writeBackendUnavailableResponse(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        rateLimitBackendUnavailableMetrics.record();
        String messageKey = "api.error.runtime.rateLimitBackendUnavailable";
        ErrorDetail error = new ErrorDetail(
                ApiErrorCodes.RATE_LIMIT_BACKEND_UNAVAILABLE,
                ApiErrorCategories.RUNTIME,
                messageResolver.resolve(messageKey),
                messageKey,
                true,
                null
        );
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(BACKEND_UNAVAILABLE_RETRY_AFTER_SECONDS));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getOutputStream(),
                new ErrorEnvelope(Metadata.minimal(auditId, traceId), error)
        );
    }

    static String resolveEnvironment(String requestUri) {
        if (requestUri == null) {
            return "unknown";
        }
        String[] segments = requestUri.split("/");
        if (segments.length >= 3 && "api".equals(segments[1])) {
            return segments[2];
        }
        return "unknown";
    }
}
