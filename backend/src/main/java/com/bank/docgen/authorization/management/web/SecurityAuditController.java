package com.bank.docgen.authorization.management.web;

import com.bank.docgen.authorization.management.api.RouteAccessDeniedRequest;
import com.bank.docgen.authorization.management.service.SecurityAuditSummaryService;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * LR-D7: thin authenticated report endpoint for frontend forbidden-route denials.
 * Persistence is fail-safe; callers receive 204 even when durable write fails.
 */
@RestController
@RequestMapping("/api/management/v1/security-audit")
public class SecurityAuditController {

    private final SecurityAuditSummaryService securityAuditSummaryService;
    private final TraceIdProvider traceIdProvider;

    public SecurityAuditController(
            SecurityAuditSummaryService securityAuditSummaryService,
            TraceIdProvider traceIdProvider
    ) {
        this.securityAuditSummaryService = securityAuditSummaryService;
        this.traceIdProvider = traceIdProvider;
    }

    @PostMapping("/route-access-denied")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reportRouteAccessDenied(
            @AuthenticationPrincipal ManagementSessionClaims session,
            @Valid @RequestBody RouteAccessDeniedRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientTraceId = request.traceId();
        String traceId = traceIdProvider.currentOrNew(
                clientTraceId != null && !clientTraceId.isBlank()
                        ? clientTraceId
                        : httpRequest.getHeader("X-Trace-Id")
        );
        String auditId = traceIdProvider.newAuditId();
        securityAuditSummaryService.recordRouteAccessDenied(
                session.username(),
                request.routeKey(),
                SecurityAuditSummaryService.REASON_ROUTE_NOT_VISIBLE,
                auditId,
                traceId
        );
    }
}
