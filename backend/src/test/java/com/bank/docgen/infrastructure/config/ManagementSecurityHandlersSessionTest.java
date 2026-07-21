package com.bank.docgen.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.SecurityAuditSummaryService;
import com.bank.docgen.authorization.management.web.JwtAuthenticationFilter;
import com.bank.docgen.authorization.management.web.ManagementAuthentication;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * BDD-LRP-D7-004 — management AccessDeniedHandler persists SECURITY_ROUTE_ACCESS_DENIED.
 */
class ManagementSecurityHandlersSessionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SecurityAuditSummaryService securityAuditSummaryService;
    private ManagementSecurityHandlers handlers;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        MessageResolver messageResolver = mock(MessageResolver.class);
        when(messageResolver.resolve(anyString())).thenAnswer(invocation -> "msg:" + invocation.getArgument(0));
        securityAuditSummaryService = mock(SecurityAuditSummaryService.class);
        handlers = new ManagementSecurityHandlers(
                new TraceIdProvider(),
                messageResolver,
                objectMapper,
                securityAuditSummaryService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
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

    @Test
    void handlePersistsRouteAccessDeniedForAuthenticatedCaller() throws Exception {
        ManagementSessionClaims session = new ManagementSessionClaims(
                "10000003",
                "Author",
                "author@bank.test",
                AuthSource.LOCAL,
                List.of("DOCUMENT_AUTHOR"),
                List.of("RETAIL"),
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                "jti-1",
                Instant.parse("2026-07-11T00:00:00Z"),
                Instant.parse("2030-01-01T00:00:00Z")
        );
        SecurityContextHolder.getContext().setAuthentication(new ManagementAuthentication(session));

        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/management/v1/admin/audit/management-events");
        request.setServletPath("/api/management/v1/admin/audit/management-events");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handlers.handle(request, response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        JsonNode error = objectMapper.readTree(response.getContentAsString()).path("error");
        assertThat(error.path("code").asText()).isEqualTo("ACCESS_DENIED");
        verify(securityAuditSummaryService).recordRouteAccessDenied(
                eq("10000003"),
                eq("/api/management/v1/admin/audit/management-events"),
                eq(SecurityAuditSummaryService.REASON_ACCESS_DENIED),
                anyString(),
                anyString()
        );
    }
}
