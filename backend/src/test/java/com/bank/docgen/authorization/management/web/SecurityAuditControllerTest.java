package com.bank.docgen.authorization.management.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bank.docgen.authorization.management.api.RouteAccessDeniedRequest;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.SecurityAuditSummaryService;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * BDD-LRP-D7-003 — authenticated route-access-denied report API.
 */
class SecurityAuditControllerTest {

    private SecurityAuditSummaryService securityAuditSummaryService;
    private SecurityAuditController controller;

    @BeforeEach
    void setUp() {
        securityAuditSummaryService = mock(SecurityAuditSummaryService.class);
        controller = new SecurityAuditController(securityAuditSummaryService, new TraceIdProvider());
    }

    @Test
    void routeAccessDeniedPersistsForAuthenticatedCaller() {
        controller.reportRouteAccessDenied(
                session(),
                new RouteAccessDeniedRequest("route.audit-console", "client-trace-1"),
                new MockHttpServletRequest("POST", "/api/management/v1/security-audit/route-access-denied")
        );

        verify(securityAuditSummaryService).recordRouteAccessDenied(
                eq("10000003"),
                eq("route.audit-console"),
                eq(SecurityAuditSummaryService.REASON_ROUTE_NOT_VISIBLE),
                anyString(),
                eq("client-trace-1")
        );
    }

    private static ManagementSessionClaims session() {
        return new ManagementSessionClaims(
                "10000003",
                "Author",
                "author@bank.test",
                AuthSource.LOCAL,
                List.of("TEMPLATE_AUTHOR"),
                List.of("RETAIL"),
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                "jti-1",
                Instant.parse("2026-07-11T00:00:00Z"),
                Instant.parse("2030-01-01T00:00:00Z")
        );
    }
}
