package com.bank.docgen.template.web;

import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.AnchorBindingView;
import com.bank.docgen.template.api.BindingValidationView;
import com.bank.docgen.template.api.CompositionRuleView;
import com.bank.docgen.template.api.CreateTemplateRequest;
import com.bank.docgen.template.api.LifecycleCommentRequest;
import com.bank.docgen.template.api.LifecycleDecisionRequest;
import com.bank.docgen.template.api.LifecycleGovernanceRequest;
import com.bank.docgen.template.api.LifecycleImpactPreviewRequest;
import com.bank.docgen.template.api.LifecycleImpactPreviewView;
import com.bank.docgen.template.api.PublishTemplateRequest;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.api.TemplateReleaseVersionView;
import com.bank.docgen.template.api.TemplateSummaryView;
import com.bank.docgen.template.api.UpsertAnchorBindingRequest;
import com.bank.docgen.template.api.UpsertVariableSchemaRequest;
import com.bank.docgen.template.api.VariableSchemaView;
import com.bank.docgen.template.api.TemplateRuleValidationRequest;
import com.bank.docgen.template.api.TemplateRuleValidationView;
import com.bank.docgen.template.api.UpdateTemplateRequest;
import com.bank.docgen.template.service.TemplateLifecycleService;
import com.bank.docgen.template.service.TemplateDeleteService;
import com.bank.docgen.template.service.TemplateRuleValidationService;
import com.bank.docgen.template.service.TemplateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management/v1/templates")
public class TemplateController {

    private final TemplateService templateService;
    private final TemplateLifecycleService templateLifecycleService;
    private final TemplateDeleteService templateDeleteService;
    private final TemplateRuleValidationService templateRuleValidationService;
    private final TraceIdProvider traceIdProvider;

    public TemplateController(
            TemplateService templateService,
            TemplateLifecycleService templateLifecycleService,
            TemplateDeleteService templateDeleteService,
            TemplateRuleValidationService templateRuleValidationService,
            TraceIdProvider traceIdProvider
    ) {
        this.templateService = templateService;
        this.templateLifecycleService = templateLifecycleService;
        this.templateDeleteService = templateDeleteService;
        this.templateRuleValidationService = templateRuleValidationService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping
    public SuccessEnvelope<List<TemplateSummaryView>> list(
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateService.list(session));
    }

    @GetMapping("/{templateId}")
    public SuccessEnvelope<TemplateDetailView> get(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateService.get(templateId, session));
    }

    @GetMapping("/{templateId}/release-versions")
    public SuccessEnvelope<List<TemplateReleaseVersionView>> listReleaseVersions(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateService.listReleaseVersions(templateId, session));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessEnvelope<TemplateDetailView> create(
            @Valid @RequestBody CreateTemplateRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateService.create(body, session));
    }

    @PatchMapping("/{templateId}")
    public SuccessEnvelope<TemplateDetailView> updateMetadata(
            @PathVariable UUID templateId,
            @Valid @RequestBody UpdateTemplateRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateService.updateMetadata(templateId, body, session));
    }

    @PutMapping("/{templateId}/variables/{variableKey}")
    public SuccessEnvelope<VariableSchemaView> upsertVariable(
            @PathVariable UUID templateId,
            @PathVariable String variableKey,
            @Valid @RequestBody UpsertVariableSchemaRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        UpsertVariableSchemaRequest normalized = new UpsertVariableSchemaRequest(
                variableKey,
                body.variableType(),
                body.required(),
                body.defaultValue(),
                body.enumValues(),
                body.description()
        );
        return envelope(request, templateService.upsertVariable(templateId, normalized, session));
    }

    @DeleteMapping("/{templateId}/variables/{variableKey}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVariable(
            @PathVariable UUID templateId,
            @PathVariable String variableKey,
            @AuthenticationPrincipal ManagementSessionClaims session
    ) {
        templateService.deleteVariable(templateId, variableKey, session);
    }

    @DeleteMapping("/{templateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemplate(
            @PathVariable UUID templateId,
            @Valid @RequestBody LifecycleGovernanceRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session
    ) {
        templateDeleteService.deleteTemplate(templateId, body, session);
    }

    @PutMapping("/{templateId}/bindings/{anchorId}")
    public SuccessEnvelope<AnchorBindingView> upsertBinding(
            @PathVariable UUID templateId,
            @PathVariable String anchorId,
            @Valid @RequestBody UpsertAnchorBindingRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        UpsertAnchorBindingRequest normalized = new UpsertAnchorBindingRequest(
                anchorId,
                body.declaredContentType(),
                body.structuredContentJson()
        );
        return envelope(request, templateService.upsertBinding(templateId, normalized, session));
    }

    @PostMapping("/{templateId}/bindings/validate")
    public SuccessEnvelope<BindingValidationView> validateBindings(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateService.validateBindings(templateId, session));
    }

    @PutMapping("/{templateId}/rules")
    public SuccessEnvelope<List<CompositionRuleView>> saveRules(
            @PathVariable UUID templateId,
            @Valid @RequestBody TemplateRuleValidationRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        List<CompositionRuleView> rules = body.rules().stream()
                .map(rule -> new CompositionRuleView(
                        rule.ruleId(),
                        rule.conditionExpression(),
                        rule.targetAnchorId(),
                        rule.trueBranchRuleId(),
                        rule.falseBranchRuleId()
                ))
                .toList();
        return envelope(request, templateService.saveRules(templateId, rules, session));
    }

    @PostMapping("/{templateId}/rules/validate")
    public SuccessEnvelope<TemplateRuleValidationView> validateRules(
            @PathVariable UUID templateId,
            @Valid @RequestBody TemplateRuleValidationRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateRuleValidationService.validateRules(templateId, body, session));
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
