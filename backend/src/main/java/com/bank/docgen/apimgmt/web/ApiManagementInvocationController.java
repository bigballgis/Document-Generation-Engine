package com.bank.docgen.apimgmt.web;

import com.bank.docgen.apimgmt.api.ManagementInvocationDetailView;
import com.bank.docgen.apimgmt.api.ManagementInvocationSummaryView;
import com.bank.docgen.apimgmt.service.ManagementInvocationFilters;
import com.bank.docgen.apimgmt.service.ManagementInvocationQueryService;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management/v1/templates/{templateId}/api")
public class ApiManagementInvocationController {

    private final ManagementInvocationQueryService managementInvocationQueryService;
    private final ApiManagementWebEnvelopeSupport envelopes;

    public ApiManagementInvocationController(
            ManagementInvocationQueryService managementInvocationQueryService,
            TraceIdProvider traceIdProvider
    ) {
        this.managementInvocationQueryService = managementInvocationQueryService;
        this.envelopes = new ApiManagementWebEnvelopeSupport(traceIdProvider);
    }

    @GetMapping("/invocations/recent")
    public SuccessEnvelope<List<ManagementInvocationSummaryView>> listRecentInvocations(
            @PathVariable UUID templateId,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelopes.envelope(
                request,
                managementInvocationQueryService.listRecentInvocations(templateId, limit, session)
        );
    }

    @GetMapping("/invocations")
    public SuccessEnvelope<PageView<ManagementInvocationSummaryView>> listInvocations(
            @PathVariable UUID templateId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String invocationKind,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) Instant createdAfter,
            @RequestParam(required = false) Instant createdBefore,
            @RequestParam(required = false) UUID credentialId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        ManagementInvocationFilters filters = new ManagementInvocationFilters(
                status,
                invocationKind,
                requestId,
                createdAfter,
                createdBefore,
                credentialId
        );
        return envelopes.envelope(request, managementInvocationQueryService.listInvocations(
                templateId,
                session,
                page,
                size,
                filters
        ));
    }

    @GetMapping("/invocations/{invocationId}")
    public SuccessEnvelope<ManagementInvocationDetailView> getInvocationDetail(
            @PathVariable UUID templateId,
            @PathVariable String invocationId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelopes.envelope(request, managementInvocationQueryService.getInvocationDetail(
                templateId,
                invocationId,
                session
        ));
    }
}
