package com.bank.docgen.template.web;

import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.DecisionFormConfigView;
import com.bank.docgen.template.api.LifecycleCommentRequest;
import com.bank.docgen.template.api.LifecycleDecisionRequest;
import com.bank.docgen.template.api.LifecycleGovernanceRequest;
import com.bank.docgen.template.api.LifecycleImpactPreviewRequest;
import com.bank.docgen.template.api.LifecycleImpactPreviewView;
import com.bank.docgen.template.api.PublishTemplateRequest;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.service.RiskPromptConfigService;
import com.bank.docgen.template.service.TemplateLifecycleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management/v1/templates")
public class TemplateLifecycleController {

    private final TemplateLifecycleService templateLifecycleService;
    private final RiskPromptConfigService riskPromptConfigService;
    private final TraceIdProvider traceIdProvider;

    public TemplateLifecycleController(
            TemplateLifecycleService templateLifecycleService,
            RiskPromptConfigService riskPromptConfigService,
            TraceIdProvider traceIdProvider
    ) {
        this.templateLifecycleService = templateLifecycleService;
        this.riskPromptConfigService = riskPromptConfigService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping("/{templateId}/lifecycle/decision-form-config")
    public SuccessEnvelope<DecisionFormConfigView> decisionFormConfig(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, riskPromptConfigService.resolveDecisionFormConfig(templateId, session));
    }

    @PostMapping("/{templateId}/lifecycle/submit-test")
    public SuccessEnvelope<TemplateDetailView> submitForTest(
            @PathVariable UUID templateId,
            @Valid @RequestBody LifecycleCommentRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateLifecycleService.submitForTest(templateId, body, session));
    }

    @PostMapping("/{templateId}/lifecycle/test-decision")
    public SuccessEnvelope<TemplateDetailView> testDecision(
            @PathVariable UUID templateId,
            @Valid @RequestBody LifecycleDecisionRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateLifecycleService.recordTestDecision(templateId, body, session));
    }

    @PostMapping("/{templateId}/lifecycle/submit-approval")
    public SuccessEnvelope<TemplateDetailView> submitForApproval(
            @PathVariable UUID templateId,
            @Valid @RequestBody LifecycleCommentRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateLifecycleService.submitForApproval(templateId, body, session));
    }

    @PostMapping("/{templateId}/lifecycle/approval-decision")
    public SuccessEnvelope<TemplateDetailView> approvalDecision(
            @PathVariable UUID templateId,
            @Valid @RequestBody LifecycleDecisionRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateLifecycleService.recordApprovalDecision(templateId, body, session));
    }

    @PostMapping("/{templateId}/lifecycle/publish")
    public SuccessEnvelope<TemplateDetailView> publish(
            @PathVariable UUID templateId,
            @Valid @RequestBody PublishTemplateRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateLifecycleService.publish(templateId, body, session));
    }

    @PostMapping("/{templateId}/lifecycle/stop")
    public SuccessEnvelope<TemplateDetailView> stop(
            @PathVariable UUID templateId,
            @Valid @RequestBody LifecycleGovernanceRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateLifecycleService.stop(templateId, body, session));
    }

    @PostMapping("/{templateId}/lifecycle/restore")
    public SuccessEnvelope<TemplateDetailView> restore(
            @PathVariable UUID templateId,
            @Valid @RequestBody LifecycleGovernanceRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateLifecycleService.restore(templateId, body, session));
    }

    @PostMapping("/{templateId}/lifecycle/deprecate")
    public SuccessEnvelope<TemplateDetailView> deprecate(
            @PathVariable UUID templateId,
            @Valid @RequestBody LifecycleGovernanceRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateLifecycleService.deprecate(templateId, body, session));
    }

    @PostMapping("/{templateId}/lifecycle/impact-preview")
    public SuccessEnvelope<LifecycleImpactPreviewView> impactPreview(
            @PathVariable UUID templateId,
            @Valid @RequestBody LifecycleImpactPreviewRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateLifecycleService.previewImpact(templateId, body, session));
    }

    @PostMapping("/{templateId}/versions/{releaseVersion}/deactivate")
    public SuccessEnvelope<TemplateDetailView> deactivateVersion(
            @PathVariable UUID templateId,
            @PathVariable String releaseVersion,
            @Valid @RequestBody LifecycleGovernanceRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateLifecycleService.deactivateVersion(
                templateId, releaseVersion, body, session));
    }

    @PostMapping("/{templateId}/versions/{releaseVersion}/restore")
    public SuccessEnvelope<TemplateDetailView> restoreVersion(
            @PathVariable UUID templateId,
            @PathVariable String releaseVersion,
            @Valid @RequestBody LifecycleGovernanceRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateLifecycleService.restoreVersion(
                templateId, releaseVersion, body, session));
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
