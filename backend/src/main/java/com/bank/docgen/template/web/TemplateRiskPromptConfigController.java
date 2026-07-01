package com.bank.docgen.template.web;

import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.TemplateRiskPromptConfigView;
import com.bank.docgen.template.api.UpsertTemplateRiskPromptConfigRequest;
import com.bank.docgen.template.service.RiskPromptConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management/v1/templates/{templateId}/risk-prompt-config")
public class TemplateRiskPromptConfigController {

    private final RiskPromptConfigService riskPromptConfigService;
    private final TraceIdProvider traceIdProvider;

    public TemplateRiskPromptConfigController(
            RiskPromptConfigService riskPromptConfigService,
            TraceIdProvider traceIdProvider
    ) {
        this.riskPromptConfigService = riskPromptConfigService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping
    public SuccessEnvelope<TemplateRiskPromptConfigView> get(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, riskPromptConfigService.getTemplateConfig(templateId, session));
    }

    @PutMapping
    public SuccessEnvelope<TemplateRiskPromptConfigView> upsert(
            @PathVariable UUID templateId,
            @Valid @RequestBody UpsertTemplateRiskPromptConfigRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, riskPromptConfigService.upsertTemplateConfig(templateId, body, session));
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
