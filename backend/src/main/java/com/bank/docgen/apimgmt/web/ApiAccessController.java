package com.bank.docgen.apimgmt.web;

import com.bank.docgen.apimgmt.api.ApiAccessAlertView;
import com.bank.docgen.apimgmt.service.ApiAccessAlertQueryService;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({
        "/api/management/v1/api-access",
        "/api/management/v1/api-policies"
})
public class ApiAccessController {

    private final ApiAccessAlertQueryService apiAccessAlertQueryService;
    private final TraceIdProvider traceIdProvider;

    public ApiAccessController(
            ApiAccessAlertQueryService apiAccessAlertQueryService,
            TraceIdProvider traceIdProvider
    ) {
        this.apiAccessAlertQueryService = apiAccessAlertQueryService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping("/alerts")
    public SuccessEnvelope<List<ApiAccessAlertView>> listAlerts(
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(
                Metadata.minimal(auditId, traceId),
                apiAccessAlertQueryService.listAlerts(session)
        );
    }
}
