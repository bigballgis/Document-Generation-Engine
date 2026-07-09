package com.bank.docgen.audit.web;

import com.bank.docgen.audit.api.GenerationAuditQueryResult;
import com.bank.docgen.audit.service.AuditQueryService;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management/v1/audit")
public class GenerationAuditController {

    private final AuditQueryService auditQueryService;
    private final TraceIdProvider traceIdProvider;

    public GenerationAuditController(AuditQueryService auditQueryService, TraceIdProvider traceIdProvider) {
        this.auditQueryService = auditQueryService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping("/generation")
    public SuccessEnvelope<GenerationAuditQueryResult> getGenerationEvents(
            @RequestParam String templateExternalId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        GenerationAuditQueryResult result = auditQueryService.queryGenerationEventsByExternalId(
                session,
                templateExternalId,
                page,
                size
        );
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
