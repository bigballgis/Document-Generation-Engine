package com.bank.docgen.template.web;

import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.AnchorBindingView;
import com.bank.docgen.template.api.BindingValidationView;
import com.bank.docgen.template.api.ComputeExpressionEvaluateRequest;
import com.bank.docgen.template.api.ComputeExpressionEvaluateView;
import com.bank.docgen.template.api.ComputeExpressionValidateRequest;
import com.bank.docgen.template.api.ComputeExpressionValidateView;
import com.bank.docgen.template.api.CompositionRuleView;
import com.bank.docgen.template.api.ContentModuleReferenceView;
import com.bank.docgen.template.api.MasterStyleCatalogView;
import com.bank.docgen.template.api.PasteCleanRequest;
import com.bank.docgen.template.api.PasteCleanResultView;
import com.bank.docgen.template.api.TemplateRuleValidationRequest;
import com.bank.docgen.template.api.TemplateRuleValidationView;
import com.bank.docgen.template.api.UpsertAnchorBindingRequest;
import com.bank.docgen.template.api.UpsertContentModuleReferenceRequest;
import com.bank.docgen.template.api.UpsertVariableSchemaRequest;
import com.bank.docgen.template.api.VariableSchemaView;
import com.bank.docgen.template.service.TemplateContentModuleReferenceService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management/v1/templates")
public class TemplateAuthoringController {

    private final TemplateService templateService;
    private final TemplateRuleValidationService templateRuleValidationService;
    private final TemplateContentModuleReferenceService contentModuleReferenceService;
    private final TraceIdProvider traceIdProvider;

    public TemplateAuthoringController(
            TemplateService templateService,
            TemplateRuleValidationService templateRuleValidationService,
            TemplateContentModuleReferenceService contentModuleReferenceService,
            TraceIdProvider traceIdProvider
    ) {
        this.templateService = templateService;
        this.templateRuleValidationService = templateRuleValidationService;
        this.contentModuleReferenceService = contentModuleReferenceService;
        this.traceIdProvider = traceIdProvider;
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
                body.description(),
                body.computeExpression(),
                body.piiCategory()
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
                body.structuredContentJson(),
                body.pasteCleaningEvidence(),
                body.clearPasteCleaningEvidence()
        );
        return envelope(request, templateService.upsertBinding(templateId, normalized, session));
    }

    @GetMapping("/{templateId}/content-module-references")
    public SuccessEnvelope<List<ContentModuleReferenceView>> listContentModuleReferences(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, contentModuleReferenceService.listReferences(templateId, session));
    }

    @PutMapping("/{templateId}/content-module-references/{referenceKey}")
    public SuccessEnvelope<ContentModuleReferenceView> upsertContentModuleReference(
            @PathVariable UUID templateId,
            @PathVariable String referenceKey,
            @Valid @RequestBody UpsertContentModuleReferenceRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        UpsertContentModuleReferenceRequest normalized = new UpsertContentModuleReferenceRequest(
                referenceKey,
                body.moduleId(),
                body.semanticVersion()
        );
        return envelope(request, contentModuleReferenceService.upsertReference(templateId, normalized, session));
    }

    @PostMapping("/{templateId}/bindings/validate")
    public SuccessEnvelope<BindingValidationView> validateBindings(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateService.validateBindings(templateId, session));
    }

    @GetMapping("/{templateId}/master-style-catalog")
    public SuccessEnvelope<MasterStyleCatalogView> masterStyleCatalog(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateService.getMasterStyleCatalog(templateId, session));
    }

    @PostMapping("/{templateId}/paste-clean")
    public SuccessEnvelope<PasteCleanResultView> pasteClean(
            @PathVariable UUID templateId,
            @Valid @RequestBody PasteCleanRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateService.pasteClean(templateId, body, session));
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

    @PostMapping("/{templateId}/compute-expressions/validate")
    public SuccessEnvelope<ComputeExpressionValidateView> validateComputeExpression(
            @PathVariable UUID templateId,
            @RequestBody ComputeExpressionValidateRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateService.validateComputeExpression(templateId, body, session));
    }

    @PostMapping("/{templateId}/compute-expressions/evaluate")
    public SuccessEnvelope<ComputeExpressionEvaluateView> evaluateComputeExpression(
            @PathVariable UUID templateId,
            @RequestBody ComputeExpressionEvaluateRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateService.evaluateComputeExpression(templateId, body, session));
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
