package com.bank.docgen.template.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.template.port.PreviewEvidencePort;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.BindingValidationView;
import com.bank.docgen.template.api.ChangeDiffView;
import com.bank.docgen.template.api.CoverageSummaryView;
import com.bank.docgen.template.api.PublishGateChecklistView;
import com.bank.docgen.template.api.PublishGateItemView;
import com.bank.docgen.template.api.TemplateRuleValidationItemRequest;
import com.bank.docgen.template.api.TemplateRuleValidationRequest;
import com.bank.docgen.template.api.TemplateRuleValidationView;
import com.bank.docgen.template.domain.LifecycleAction;
import com.bank.docgen.template.domain.LifecycleDecision;
import com.bank.docgen.template.domain.PublishGateCheckCode;
import com.bank.docgen.template.domain.PublishGatePhase;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublishGateService {

    private final TemplateService templateService;
    private final TemplateVersionRepository templateVersionRepository;
    private final TemplateLifecycleRecordRepository lifecycleRecordRepository;
    private final ApiPolicyRepository apiPolicyRepository;
    private final PreviewEvidencePort previewEvidencePort;
    private final CoverageComputationService coverageComputationService;
    private final ChangeDiffService changeDiffService;
    private final TemplateRuleValidationService templateRuleValidationService;
    private final VariableSchemaRepository variableSchemaRepository;
    private final TemplateContentModuleReferenceService contentModuleReferenceService;
    private final TemplateCurrentVersionResolver templateVersionSupport;

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
            TemplateCurrentVersionResolver templateVersionSupport
    ) {
        this.templateService = templateService;
        this.templateVersionRepository = templateVersionRepository;
        this.lifecycleRecordRepository = lifecycleRecordRepository;
        this.apiPolicyRepository = apiPolicyRepository;
        this.previewEvidencePort = previewEvidencePort;
        this.coverageComputationService = coverageComputationService;
        this.changeDiffService = changeDiffService;
        this.templateRuleValidationService = templateRuleValidationService;
        this.variableSchemaRepository = variableSchemaRepository;
        this.contentModuleReferenceService = contentModuleReferenceService;
        this.templateVersionSupport = templateVersionSupport;
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

        List<PublishGateItemView> items = new ArrayList<>();
        items.add(anchorIntegrityItem(bindings));
        items.add(variableSchemaItem(version.getId()));
        items.add(ruleBoundsItem(ruleValidation));
        items.add(testResultsItem(templateId));
        items.add(previewPresentItem(templateId, version.getId()));
        items.add(changeDiffItem(changeDiff));
        items.add(approvalSummaryItem(templateId));
        items.add(coverageThresholdsItem(coverage));
        items.add(apiPolicyItem(templateId));
        items.add(contentModuleReferencesItem(version.getId()));
        items.add(blockerStatusItem(templateId, version.getId(), bindings, coverage));

        List<PublishGateItemView> phaseItems = filterForPhase(items, phase);
        int blockerCount = (int) phaseItems.stream().filter(PublishGateItemView::blocker).count();
        boolean ready = blockerCount == 0;
        return new PublishGateChecklistView(templateId.toString(), ready, blockerCount, phaseItems);
    }

    @Transactional(readOnly = true)
    public void assertReady(UUID templateId, ManagementSessionClaims session) {
        assertReady(templateId, session, PublishGatePhase.PUBLISH);
    }

    @Transactional(readOnly = true)
    public void assertReady(UUID templateId, ManagementSessionClaims session, PublishGatePhase phase) {
        PublishGateChecklistView checklist = evaluate(templateId, session, phase);
        if (!checklist.ready()) {
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

    private List<PublishGateItemView> filterForPhase(List<PublishGateItemView> items, PublishGatePhase phase) {
        if (phase == PublishGatePhase.PUBLISH) {
            return items;
        }
        return items.stream()
                .filter(item -> item.checkCode() != PublishGateCheckCode.APPROVAL_SUMMARY
                        && item.checkCode() != PublishGateCheckCode.API_POLICY)
                .toList();
    }

    private PublishGateItemView anchorIntegrityItem(BindingValidationView bindings) {
        boolean blocking = bindings.summary().blocking();
        return new PublishGateItemView(
                PublishGateCheckCode.ANCHOR_INTEGRITY,
                !blocking,
                blocking,
                blocking ? "api.publishGate.anchorIntegrity.blocked" : "api.publishGate.anchorIntegrity.ready",
                "blocking=" + bindings.summary().blocking()
        );
    }

    private PublishGateItemView variableSchemaItem(UUID versionId) {
        int variableCount = variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(versionId).size();
        boolean ready = variableCount > 0;
        return new PublishGateItemView(
                PublishGateCheckCode.VARIABLE_SCHEMA,
                ready,
                !ready,
                ready ? "api.publishGate.variableSchema.ready" : "api.publishGate.variableSchema.missing",
                "variableCount=" + variableCount
        );
    }

    private PublishGateItemView ruleBoundsItem(TemplateRuleValidationView ruleValidation) {
        boolean blocking = ruleValidation.summary().blocking();
        return new PublishGateItemView(
                PublishGateCheckCode.RULE_BOUNDS,
                !blocking,
                blocking,
                blocking ? "api.publishGate.ruleBounds.blocked" : "api.publishGate.ruleBounds.ready",
                "invalidRules=" + (ruleValidation.summary().totalRules() - ruleValidation.summary().validCount())
        );
    }

    private PublishGateItemView testResultsItem(UUID templateId) {
        var latest = previewEvidencePort.latestBatchTestRun(templateId);
        boolean hasRun = latest.isPresent();
        int blockerCount = latest.map(snapshot -> snapshot.blockerCount()).orElse(0);
        boolean blocking = !hasRun || blockerCount > 0;
        return new PublishGateItemView(
                PublishGateCheckCode.TEST_RESULTS,
                hasRun && blockerCount == 0,
                blocking,
                hasRun ? "api.publishGate.testResults.ready" : "api.publishGate.testResults.missing",
                hasRun ? "blockerCount=" + blockerCount : "noBatchRun"
        );
    }

    private PublishGateItemView previewPresentItem(UUID templateId, UUID versionId) {
        int previewCount = previewEvidencePort.countSuccessfulPreviews(templateId, versionId);
        boolean ready = previewCount > 0;
        return new PublishGateItemView(
                PublishGateCheckCode.PREVIEW_PRESENT,
                ready,
                !ready,
                ready ? "api.publishGate.previewPresent.ready" : "api.publishGate.previewPresent.missing",
                "successfulPreviews=" + previewCount
        );
    }

    private PublishGateItemView changeDiffItem(ChangeDiffView changeDiff) {
        return new PublishGateItemView(
                PublishGateCheckCode.CHANGE_DIFF,
                true,
                false,
                "api.publishGate.changeDiff.ready",
                "changeCount=" + changeDiff.totalChangeCount()
        );
    }

    private PublishGateItemView approvalSummaryItem(UUID templateId) {
        boolean approved = lifecycleRecordRepository.findByTemplateIdOrderByCreatedAtDesc(templateId).stream()
                .anyMatch(record -> record.getAction() == LifecycleAction.RECORD_APPROVAL_DECISION
                        && record.getDecision() == LifecycleDecision.APPROVED);
        return new PublishGateItemView(
                PublishGateCheckCode.APPROVAL_SUMMARY,
                approved,
                !approved,
                approved ? "api.publishGate.approvalSummary.ready" : "api.publishGate.approvalSummary.missing",
                approved ? "approved=true" : "approved=false"
        );
    }

    private PublishGateItemView coverageThresholdsItem(CoverageSummaryView coverage) {
        boolean blocking = coverage.belowThreshold();
        return new PublishGateItemView(
                PublishGateCheckCode.COVERAGE_THRESHOLDS,
                !blocking,
                blocking,
                blocking ? "api.publishGate.coverageThresholds.blocked" : "api.publishGate.coverageThresholds.ready",
                "aggregatePct=" + coverage.aggregatePercentage()
        );
    }

    private PublishGateItemView apiPolicyItem(UUID templateId) {
        return apiPolicyRepository.findByTemplateId(templateId)
                .map(this::callableReadyApiPolicyItem)
                .orElseGet(() -> new PublishGateItemView(
                        PublishGateCheckCode.API_POLICY,
                        false,
                        true,
                        "api.error.runtime.policyNotConfigured",
                        "skeletonPresent=false"
                ));
    }

    private PublishGateItemView callableReadyApiPolicyItem(ApiPolicyEntity policy) {
        // Skeleton presence is sufficient for publish; empty AD groups do not block (runtime fail-closed).
        return new PublishGateItemView(
                PublishGateCheckCode.API_POLICY,
                true,
                false,
                "api.publishGate.apiPolicy.ready",
                "skeletonPresent=true,adGroupsConfigured=" + hasConfiguredAdGroups(policy)
        );
    }

    private boolean hasConfiguredAdGroups(ApiPolicyEntity policy) {
        String json = policy.getAllowedAdGroupsJson();
        return json != null && !json.isBlank() && !"[]".equals(json.trim());
    }

    private PublishGateItemView contentModuleReferencesItem(UUID versionId) {
        var validation = contentModuleReferenceService.validateReferences(versionId);
        boolean blocking = validation.blocking();
        return new PublishGateItemView(
                PublishGateCheckCode.CONTENT_MODULE_REFERENCES,
                !blocking,
                blocking,
                blocking
                        ? "api.publishGate.contentModuleReferences.blocked"
                        : "api.publishGate.contentModuleReferences.ready",
                "invalidReferences=" + validation.invalidReferences()
                        + ",totalReferences=" + validation.totalReferences()
        );
    }

    private PublishGateItemView blockerStatusItem(
            UUID templateId,
            UUID versionId,
            BindingValidationView bindings,
            CoverageSummaryView coverage
    ) {
        int previewBlockers = previewEvidencePort.countFailedPreviews(templateId, versionId);
        boolean blocking = bindings.summary().blocking()
                || coverage.belowThreshold()
                || previewBlockers > 0;
        return new PublishGateItemView(
                PublishGateCheckCode.BLOCKER_STATUS,
                !blocking,
                blocking,
                blocking ? "api.publishGate.blockerStatus.blocked" : "api.publishGate.blockerStatus.ready",
                "previewFailures=" + previewBlockers
        );
    }

    private TemplateRuleValidationView validateCurrentRules(
            UUID templateId,
            TemplateVersionEntity version,
            ManagementSessionClaims session
    ) {
        List<TemplateRuleValidationItemRequest> rules = templateService.loadRules(version).stream()
                .map(rule -> new TemplateRuleValidationItemRequest(
                        rule.ruleId(),
                        rule.conditionExpression(),
                        rule.targetAnchorId(),
                        rule.trueBranchRuleId(),
                        rule.falseBranchRuleId()
                ))
                .toList();
        return templateRuleValidationService.validateRules(
                templateId,
                new TemplateRuleValidationRequest(rules),
                session
        );
    }
}
