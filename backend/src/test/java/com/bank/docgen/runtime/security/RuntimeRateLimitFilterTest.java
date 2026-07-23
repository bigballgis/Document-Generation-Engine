package com.bank.docgen.runtime.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.runtime.metrics.RateLimitBackendUnavailableMetrics;
import com.bank.docgen.runtime.metrics.RateLimitDeniedMetrics;
import com.bank.docgen.runtime.service.RuntimeGenerationAuditRecorder;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class RuntimeRateLimitFilterTest {

    @Mock
    private RuntimeRateLimiter rateLimitService;
    @Mock
    private MessageResolver messageResolver;
    @Mock
    private RuntimeGenerationAuditRecorder runtimeGenerationAuditRecorder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TraceIdProvider traceIdProvider = new TraceIdProvider();
    private SimpleMeterRegistry meterRegistry;
    private RateLimitDeniedMetrics rateLimitDeniedMetrics;
    private RateLimitBackendUnavailableMetrics rateLimitBackendUnavailableMetrics;

    private RuntimeRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        rateLimitDeniedMetrics = new RateLimitDeniedMetrics(meterRegistry);
        rateLimitBackendUnavailableMetrics = new RateLimitBackendUnavailableMetrics(meterRegistry);
        filter = new RuntimeRateLimitFilter(
                rateLimitService,
                traceIdProvider,
                messageResolver,
                objectMapper,
                runtimeGenerationAuditRecorder,
                rateLimitDeniedMetrics,
                rateLimitBackendUnavailableMetrics
        );
    }

    @Test
    void exceededLimitWrites429WithRetryAfter() throws Exception {
        when(rateLimitService.enabled()).thenReturn(true);
        when(messageResolver.resolve("api.error.runtime.rateLimitExceeded"))
                .thenReturn("Too many requests. Please retry later.");
        when(rateLimitService.tryConsumeKey("CRED-1:svc-caller"))
                .thenReturn(RateLimitDecision.denied(2_000_000_000L));

        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/dev/v1/templates/TPL-001/generate");
        request.addHeader(ApiCredentialAuthenticationFilter.HEADER_CREDENTIAL_ID, "CRED-1");
        request.addHeader(ApiCredentialAuthenticationFilter.HEADER_ACCESS_ACCOUNT, "svc-caller");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("2");
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.path("error").path("code").asText()).isEqualTo(ApiErrorCodes.RATE_LIMIT_EXCEEDED);
        assertThat(body.path("error").path("retryable").asBoolean()).isTrue();
        assertThat(meterRegistry.find("docgen.http.rate_limit.denied").counter().count()).isEqualTo(1.0);
        verify(runtimeGenerationAuditRecorder).recordRateLimitDenied(
                "dev",
                "CRED-1",
                "svc-caller",
                body.path("metadata").path("traceId").asText(),
                body.path("metadata").path("auditId").asText()
        );
    }

    @Test
    void backendUnavailableWrites503Not429() throws Exception {
        when(rateLimitService.enabled()).thenReturn(true);
        when(messageResolver.resolve("api.error.runtime.rateLimitBackendUnavailable"))
                .thenReturn("Rate-limit service is temporarily unavailable. Please retry later.");
        when(rateLimitService.tryConsumeKey("CRED-1:svc-caller"))
                .thenReturn(RateLimitDecision.backendUnavailable());

        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/dev/v1/templates/TPL-001/generate");
        request.addHeader(ApiCredentialAuthenticationFilter.HEADER_CREDENTIAL_ID, "CRED-1");
        request.addHeader(ApiCredentialAuthenticationFilter.HEADER_ACCESS_ACCOUNT, "svc-caller");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getHeader("Retry-After")).isEqualTo("1");
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.path("error").path("code").asText())
                .isEqualTo(ApiErrorCodes.RATE_LIMIT_BACKEND_UNAVAILABLE);
        assertThat(body.path("error").path("category").asText()).isEqualTo("RUNTIME");
        assertThat(body.path("error").path("retryable").asBoolean()).isTrue();
        assertThat(body.path("error").path("messageKey").asText())
                .isEqualTo("api.error.runtime.rateLimitBackendUnavailable");
        assertThat(meterRegistry.find("docgen.http.rate_limit.backend_unavailable").counter().count())
                .isEqualTo(1.0);
        assertThat(meterRegistry.find("docgen.http.rate_limit.denied").counter().count()).isZero();
        verify(runtimeGenerationAuditRecorder, never()).recordRateLimitDenied(
                any(), any(), any(), any(), any()
        );
        verify(chain, never()).doFilter(any(), any());
    }

    /**
     * LR-B7 recorded decision (ADR-0031 / ADR-0044 / ledger seam «Runtime rate limit»):
     * requests without credential headers are not counted against any bucket and pass
     * through the limiter; the credential authentication filter rejects them immediately
     * downstream (fail-closed at the auth layer, 401).
     */
    @Test
    void missingCredentialHeadersSkipBucketAndAreRejectedByAuthLayer() throws Exception {
        when(rateLimitService.enabled()).thenReturn(true);
        when(messageResolver.resolve("api.error.runtime.invalidCredentials"))
                .thenReturn("Invalid API credentials.");
        ApiCredentialAuthenticationFilter authFilter = new ApiCredentialAuthenticationFilter(
                mock(com.bank.docgen.apimgmt.persistence.ApiCredentialRepository.class),
                mock(com.bank.docgen.apimgmt.persistence.ApiPolicyRepository.class),
                mock(com.bank.docgen.template.persistence.TemplateRepository.class),
                mock(com.bank.docgen.sharedkernel.security.PasswordHashService.class),
                mock(com.bank.docgen.apimgmt.service.AdGroupResolver.class),
                mock(com.bank.docgen.apimgmt.service.TemplateAdGroupAuthorizationCache.class),
                objectMapper,
                traceIdProvider,
                messageResolver
        );

        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/dev/v1/templates/TPL-001/generate");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chainToAuthLayer = (req, resp) -> {
            try {
                authFilter.doFilterInternal(
                        (jakarta.servlet.http.HttpServletRequest) req,
                        (jakarta.servlet.http.HttpServletResponse) resp,
                        mock(FilterChain.class)
                );
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        };

        filter.doFilter(request, response, chainToAuthLayer);

        verify(rateLimitService, never()).tryConsume(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.path("error").path("code").asText()).isEqualTo(ApiErrorCodes.INVALID_CREDENTIALS);
    }

    @Test
    void withinLimitContinuesFilterChain() throws Exception {
        when(rateLimitService.enabled()).thenReturn(true);
        when(rateLimitService.tryConsumeKey("CRED-1:svc-caller")).thenReturn(RateLimitDecision.allowed());

        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/dev/v1/templates/TPL-001/generate");
        request.addHeader(ApiCredentialAuthenticationFilter.HEADER_CREDENTIAL_ID, "CRED-1");
        request.addHeader(ApiCredentialAuthenticationFilter.HEADER_ACCESS_ACCOUNT, "svc-caller");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void managementPathsAreNotFiltered() throws Exception {
        when(rateLimitService.enabled()).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/management/templates");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(rateLimitService, never()).tryConsumeKey(any());
    }
}
