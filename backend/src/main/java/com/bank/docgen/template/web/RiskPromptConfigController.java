package com.bank.docgen.template.web;

import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.RiskPromptConfigView;
import com.bank.docgen.template.api.UpsertGlobalRiskPromptConfigRequest;
import com.bank.docgen.template.service.RiskPromptConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management/v1/risk-prompt-config")
public class RiskPromptConfigController {

    private final RiskPromptConfigService riskPromptConfigService;
    private final TraceIdProvider traceIdProvider;

    public RiskPromptConfigController(
            RiskPromptConfigService riskPromptConfigService,
            TraceIdProvider traceIdProvider
    ) {
        this.riskPromptConfigService = riskPromptConfigService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping
    public SuccessEnvelope<RiskPromptConfigView> get(
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, riskPromptConfigService.getGlobal(session));
    }

    @PutMapping
    public SuccessEnvelope<RiskPromptConfigView> upsert(
            @Valid @RequestBody UpsertGlobalRiskPromptConfigRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, riskPromptConfigService.upsertGlobal(body, session));
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
