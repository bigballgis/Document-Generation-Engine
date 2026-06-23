package com.bank.docgen.runtime.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.config.RuntimeRateLimitProperties;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.ConsumptionProbe;
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
    private RuntimeRateLimitService rateLimitService;
    @Mock
    private MessageResolver messageResolver;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TraceIdProvider traceIdProvider = new TraceIdProvider();

    private RuntimeRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RuntimeRateLimitFilter(
                rateLimitService,
                traceIdProvider,
                messageResolver,
                objectMapper
        );
    }

    @Test
    void exceededLimitWrites429WithRetryAfter() throws Exception {
        when(rateLimitService.enabled()).thenReturn(true);
        when(messageResolver.resolve("api.error.runtime.rateLimitExceeded"))
                .thenReturn("Too many requests. Please retry later.");
        ConsumptionProbe rejected = mock(ConsumptionProbe.class);
        when(rejected.isConsumed()).thenReturn(false);
        when(rejected.getNanosToWaitForRefill()).thenReturn(2_000_000_000L);
        when(rateLimitService.tryConsume("CRED-1", "svc-caller")).thenReturn(rejected);

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
    }

    @Test
    void withinLimitContinuesFilterChain() throws Exception {
        when(rateLimitService.enabled()).thenReturn(true);
        ConsumptionProbe accepted = mock(ConsumptionProbe.class);
        when(accepted.isConsumed()).thenReturn(true);
        when(rateLimitService.tryConsume("CRED-1", "svc-caller")).thenReturn(accepted);

        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/dev/v1/templates/TPL-001/generate");
        request.addHeader(ApiCredentialAuthenticationFilter.HEADER_CREDENTIAL_ID, "CRED-1");
        request.addHeader(ApiCredentialAuthenticationFilter.HEADER_ACCESS_ACCOUNT, "svc-caller");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
