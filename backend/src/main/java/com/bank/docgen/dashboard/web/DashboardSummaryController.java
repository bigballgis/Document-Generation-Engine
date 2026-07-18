package com.bank.docgen.dashboard.web;

import com.bank.docgen.dashboard.api.DashboardSummaryView;
import com.bank.docgen.dashboard.service.DashboardSummaryService;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Management Dashboard Overview summary (PRR-D01c / BDD-PRR-D01C).
 */
@RestController
@RequestMapping("/api/management/v1/dashboard")
public class DashboardSummaryController {

    private final DashboardSummaryService dashboardSummaryService;
    private final TraceIdProvider traceIdProvider;

    public DashboardSummaryController(
            DashboardSummaryService dashboardSummaryService,
            TraceIdProvider traceIdProvider
    ) {
        this.dashboardSummaryService = dashboardSummaryService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping("/summary")
    public SuccessEnvelope<DashboardSummaryView> summary(
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(
                Metadata.minimal(auditId, traceId),
                dashboardSummaryService.summarize(session)
        );
    }
}
