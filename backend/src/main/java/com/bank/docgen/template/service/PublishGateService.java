package com.bank.docgen.template.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authoring.structured.NodeMatrixValidationService;
import com.bank.docgen.template.port.PreviewEvidencePort;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.BindingValidationView;
import com.bank.docgen.template.api.ChangeDiffView;
import com.bank.docgen.template.api.CoverageSummaryView;
import com.bank.docgen.template.api.PublishGateChecklistView;
import com.bank.docgen.template.api.TemplateRuleValidationItemRequest;
import com.bank.docgen.template.api.TemplateRuleValidationRequest;
import com.bank.docgen.template.api.TemplateRuleValidationView;
import com.bank.docgen.template.domain.PublishGatePhase;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublishGateService {

    private final TemplateService templateService;
    private final TemplateVersionRepository templateVersionRepository;
    private final CoverageComputationService coverageComputationService;
    private final ChangeDiffService changeDiffService;
    private final TemplateRuleValidationService templateRuleValidationService;
    private final TemplateCurrentVersionResolver templateVersionSupport;
    private final PublishGateChecklistSupport checklist;

    public PublishGateService(
            TemplateService templateService,
            TemplateVersionRepository templateVersionRepository,
            TemplateLifecycleRecordRepository lifecycleRecordRepository,
            ApiPolicyRepository apiPolicyRepository,
            PreviewEvidencePort previewEvidencePort,
            CoverageComputationService coverageComputationService,
            ChangeDiffService changeDiffService,
            TemplateRuleValidationService templateRuleValidationService,
            VariableSchemaRepository variableSchemaRepository,
            TemplateContentModuleReferenceService contentModuleReferenceService,
            TemplateCurrentVersionResolver templateVersionSupport,
            AnchorBindingRepository anchorBindingRepository,
            NodeMatrixValidationService nodeMatrixValidationService,
            ObjectMapper objectMapper
    ) {
        this.templateService = templateService;
        this.templateVersionRepository = templateVersionRepository;
        this.coverageComputationService = coverageComputationService;
        this.changeDiffService = changeDiffService;
        this.templateRuleValidationService = templateRuleValidationService;
        this.templateVersionSupport = templateVersionSupport;
        PublishGateCheckItemSupport checkItems = new PublishGateCheckItemSupport(
                lifecycleRecordRepository,
                apiPolicyRepository,
                previewEvidencePort,
                variableSchemaRepository,
                contentModuleReferenceService,
                anchorBindingRepository,
                nodeMatrixValidationService,
                objectMapper
        );
        this.checklist = new PublishGateChecklistSupport(checkItems);
    }

    @Transactional(readOnly = true)
    public PublishGateChecklistView evaluate(UUID templateId, ManagementSessionClaims session) {
        return evaluate(templateId, session, PublishGatePhase.PUBLISH);
    }

    @Transactional(readOnly = true)
    public PublishGateChecklistView evaluate(
            UUID templateId,
            ManagementSessionClaims session,
            PublishGatePhase phase
    ) {
        templateService.requireReadableTemplate(templateId, session);
        TemplateVersionEntity version = templateVersionSupport.requireInFlightDevVersion(templateId);
        BindingValidationView bindings = templateService.validateBindings(templateId, session);
        CoverageSummaryView coverage = coverageComputationService.compute(templateId, session);
        ChangeDiffView changeDiff = changeDiffService.compute(templateId, session);
        TemplateRuleValidationView ruleValidation = validateCurrentRules(templateId, version, session);
        return checklist.buildChecklist(templateId, version, phase, bindings, coverage, changeDiff, ruleValidation);
    }

    /**
     * Live publish-gate evaluation against a published release version entity
     * (not the in-flight DEV line). Reuses the same checklist builders as {@link #evaluate}.
     */
    @Transactional(readOnly = true)
    public PublishGateChecklistView evaluateForRelease(
            UUID templateId,
            String releaseVersion,
            ManagementSessionClaims session
    ) {
        templateService.requireReadableTemplate(templateId, session);
        TemplateVersionEntity version = templateVersionRepository
                .findByTemplateIdAndReleaseVersion(templateId, releaseVersion)
                .orElseThrow(TemplateNotFoundException::new);
        if (version.getReleaseVersion() == null || version.getReleaseVersion().isBlank()) {
            throw new TemplateNotFoundException();
        }
        BindingValidationView bindings = templateService.validateBindingsForVersion(templateId, version, session);
        CoverageSummaryView coverage = coverageComputationService.computeForVersion(templateId, version, session);
        ChangeDiffView changeDiff = changeDiffService.computeForVersion(templateId, version, session);
        TemplateRuleValidationView ruleValidation = validateRulesForVersion(templateId, version, session);
        return checklist.buildChecklist(
                templateId,
                version,
                PublishGatePhase.PUBLISH,
                bindings,
                coverage,
                changeDiff,
                ruleValidation
        );
    }

    @Transactional(readOnly = true)
    public void assertReady(UUID templateId, ManagementSessionClaims session) {
        assertReady(templateId, session, PublishGatePhase.PUBLISH);
    }

    @Transactional(readOnly = true)
    public void assertReady(UUID templateId, ManagementSessionClaims session, PublishGatePhase phase) {
        PublishGateChecklistView gateChecklist = evaluate(templateId, session, phase);
        if (!gateChecklist.ready()) {
            String messageKey = phase == PublishGatePhase.SUBMIT_FOR_APPROVAL
                    ? "api.error.template.submitForApprovalGateBlocked"
                    : "api.error.template.publishGateBlocked";
            throw new TemplateValidationException(messageKey);
        }
    }

    @Transactional(readOnly = true)
    public void assertReadyForSubmitForApproval(UUID templateId, ManagementSessionClaims session) {
        assertReady(templateId, session, PublishGatePhase.SUBMIT_FOR_APPROVAL);
    }

    private TemplateRuleValidationView validateCurrentRules(
            UUID templateId,
            TemplateVersionEntity version,
            ManagementSessionClaims session
    ) {
        return templateRuleValidationService.validateRules(
                templateId,
                new TemplateRuleValidationRequest(toRuleValidationItems(version)),
                session
        );
    }

    private TemplateRuleValidationView validateRulesForVersion(
            UUID templateId,
            TemplateVersionEntity version,
            ManagementSessionClaims session
    ) {
        return templateRuleValidationService.validateRulesForVersion(
                templateId,
                version,
                new TemplateRuleValidationRequest(toRuleValidationItems(version)),
                session
        );
    }

    private List<TemplateRuleValidationItemRequest> toRuleValidationItems(TemplateVersionEntity version) {
        return templateService.loadRules(version).stream()
                .map(rule -> new TemplateRuleValidationItemRequest(
                        rule.ruleId(),
                        rule.conditionExpression(),
                        rule.targetAnchorId(),
                        rule.trueBranchRuleId(),
                        rule.falseBranchRuleId()
                ))
                .toList();
    }
}
