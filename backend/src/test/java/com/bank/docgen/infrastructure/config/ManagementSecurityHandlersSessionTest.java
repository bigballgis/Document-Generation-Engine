package com.bank.docgen.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.web.JwtAuthenticationFilter;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

class ManagementSecurityHandlersSessionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ManagementSecurityHandlers handlers;

    @BeforeEach
    void setUp() {
        MessageResolver messageResolver = mock(MessageResolver.class);
        when(messageResolver.resolve(anyString())).thenAnswer(invocation -> "msg:" + invocation.getArgument(0));
        handlers = new ManagementSecurityHandlers(new TraceIdProvider(), messageResolver, objectMapper);
    }

    @Test
    void commenceWritesSessionRevokedEnvelopeWhenFilterFlaggedRevocation() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/management/v1/auth/session");
        request.setAttribute(
                JwtAuthenticationFilter.SESSION_VALIDATION_FAILURE_ATTRIBUTE, ApiErrorCodes.SESSION_REVOKED);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handlers.commence(request, response, new InsufficientAuthenticationException("unauthenticated"));

        assertThat(response.getStatus()).isEqualTo(401);
        JsonNode error = objectMapper.readTree(response.getContentAsString()).path("error");
        assertThat(error.path("code").asText()).isEqualTo("SESSION_REVOKED");
        assertThat(error.path("category").asText()).isEqualTo("AUTHENTICATION");
        assertThat(error.path("retryable").asBoolean()).isFalse();
        assertThat(error.path("messageKey").asText()).isEqualTo("api.error.authentication.sessionRevoked");
    }

    @Test
    void commenceWritesRetryableUnavailableEnvelopeWhenValidationFailedClosed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/management/v1/auth/renew");
        request.setAttribute(
                JwtAuthenticationFilter.SESSION_VALIDATION_FAILURE_ATTRIBUTE,
                ApiErrorCodes.SESSION_VALIDATION_UNAVAILABLE);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handlers.commence(request, response, new InsufficientAuthenticationException("unauthenticated"));

        assertThat(response.getStatus()).isEqualTo(401);
        JsonNode error = objectMapper.readTree(response.getContentAsString()).path("error");
        assertThat(error.path("code").asText()).isEqualTo("SESSION_VALIDATION_UNAVAILABLE");
        assertThat(error.path("retryable").asBoolean()).isTrue();
        assertThat(error.path("messageKey").asText())
                .isEqualTo("api.error.authentication.sessionValidationUnavailable");
    }

    @Test
    void commenceDefaultsToSessionExpiredWithoutFilterFlag() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/management/v1/auth/session");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handlers.commence(request, response, new InsufficientAuthenticationException("unauthenticated"));

        assertThat(response.getStatus()).isEqualTo(401);
        JsonNode error = objectMapper.readTree(response.getContentAsString()).path("error");
        assertThat(error.path("code").asText()).isEqualTo("SESSION_EXPIRED");
        assertThat(error.path("retryable").asBoolean()).isFalse();
    }
}
