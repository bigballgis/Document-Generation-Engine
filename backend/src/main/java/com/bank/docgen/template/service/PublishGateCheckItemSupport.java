package com.bank.docgen.template.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authoring.structured.NodeMatrixValidationService;
import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.sharedkernel.document.fidelity.PaginationDeltaEvaluator;
import com.bank.docgen.template.port.PreviewEvidencePort;
import com.bank.docgen.template.api.BindingValidationView;
import com.bank.docgen.template.api.ChangeDiffView;
import com.bank.docgen.template.api.CoverageSummaryView;
import com.bank.docgen.template.api.PublishGateItemView;
import com.bank.docgen.template.api.TemplateRuleValidationView;
import com.bank.docgen.template.domain.LifecycleAction;
import com.bank.docgen.template.domain.LifecycleDecision;
import com.bank.docgen.template.domain.PublishGateCheckCode;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;

/**
 * Package-private builders for individual publish-gate checklist items.
 */
final class PublishGateCheckItemSupport {

    private final TemplateLifecycleRecordRepository lifecycleRecordRepository;
    private final ApiPolicyRepository apiPolicyRepository;
    private final PreviewEvidencePort previewEvidencePort;
    private final VariableSchemaRepository variableSchemaRepository;
    private final DocgenRenderingProperties renderingProperties;
    private final PublishGateCheckItemContentSupport contentItems;

    PublishGateCheckItemSupport(
            TemplateLifecycleRecordRepository lifecycleRecordRepository,
            ApiPolicyRepository apiPolicyRepository,
            PreviewEvidencePort previewEvidencePort,
            VariableSchemaRepository variableSchemaRepository,
            TemplateContentModuleReferenceService contentModuleReferenceService,
            AnchorBindingRepository anchorBindingRepository,
            NodeMatrixValidationService nodeMatrixValidationService,
            ObjectMapper objectMapper,
            DocgenRenderingProperties renderingProperties
    ) {
        this.lifecycleRecordRepository = lifecycleRecordRepository;
        this.apiPolicyRepository = apiPolicyRepository;
        this.previewEvidencePort = previewEvidencePort;
        this.variableSchemaRepository = variableSchemaRepository;
        this.renderingProperties = renderingProperties;
        this.contentItems = new PublishGateCheckItemContentSupport(
                previewEvidencePort,
                contentModuleReferenceService,
                anchorBindingRepository,
                nodeMatrixValidationService,
                objectMapper
        );
    }

    PublishGateItemView anchorIntegrityItem(BindingValidationView bindings) {
        boolean blocking = bindings.summary().blocking();
        return new PublishGateItemView(
                PublishGateCheckCode.ANCHOR_INTEGRITY,
                !blocking,
                blocking,
                blocking ? "api.publishGate.anchorIntegrity.blocked" : "api.publishGate.anchorIntegrity.ready",
                "blocking=" + bindings.summary().blocking()
        );
    }

    PublishGateItemView variableSchemaItem(UUID versionId) {
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

    PublishGateItemView ruleBoundsItem(TemplateRuleValidationView ruleValidation) {
        boolean blocking = ruleValidation.summary().blocking();
        return new PublishGateItemView(
                PublishGateCheckCode.RULE_BOUNDS,
                !blocking,
                blocking,
                blocking ? "api.publishGate.ruleBounds.blocked" : "api.publishGate.ruleBounds.ready",
                "invalidRules=" + (ruleValidation.summary().totalRules() - ruleValidation.summary().validCount())
        );
    }

    PublishGateItemView testResultsItem(UUID templateId) {
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

    PublishGateItemView previewPresentItem(UUID templateId, UUID versionId) {
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

    PublishGateItemView fidelityWarningsViewedItem(UUID templateId, UUID versionId) {
        int unviewedCount = previewEvidencePort.countUnviewedFidelityWarnings(templateId, versionId);
        boolean ready = unviewedCount == 0;
        return new PublishGateItemView(
                PublishGateCheckCode.FIDELITY_WARNINGS_VIEWED,
                ready,
                !ready,
                ready
                        ? "api.publishGate.fidelityWarningsViewed.ready"
                        : "api.publishGate.fidelityWarningsViewed.blocked",
                "unviewedCount=" + unviewedCount
        );
    }

    /**
     * ADR-0042: blocks publish when |pdfPages − authorWordPageCount| &gt; 2×B.
     * Skips (ready) when Word page count or measured PDF pages are unavailable.
     */
    PublishGateItemView paginationDeltaBudgetItem(UUID templateId, TemplateVersionEntity version) {
        Integer authorWordPageCount = version.getAuthorWordPageCount();
        Integer pdfPageCount = previewEvidencePort
                .latestSuccessfulPdfPageCount(templateId, version.getId())
                .orElse(null);
        PaginationDeltaEvaluator.Evaluation evaluation = PaginationDeltaEvaluator.evaluate(
                authorWordPageCount,
                pdfPageCount,
                renderingProperties.getPaginationDeltaBudgetPages()
        );
        boolean blocking = evaluation.isPublishBlocker();
        String summary = "authorWordPageCount=" + authorWordPageCount
                + ",pdfPages=" + pdfPageCount
                + ",delta=" + evaluation.delta()
                + ",budget=" + renderingProperties.getPaginationDeltaBudgetPages()
                + ",outcome=" + evaluation.outcome();
        return new PublishGateItemView(
                PublishGateCheckCode.PAGINATION_DELTA_BUDGET,
                !blocking,
                blocking,
                blocking
                        ? "api.publishGate.paginationDeltaBudget.blocked"
                        : "api.publishGate.paginationDeltaBudget.ready",
                summary
        );
    }

    PublishGateItemView changeDiffItem(ChangeDiffView changeDiff) {
        return new PublishGateItemView(
                PublishGateCheckCode.CHANGE_DIFF,
                true,
                false,
                "api.publishGate.changeDiff.ready",
                "changeCount=" + changeDiff.totalChangeCount()
        );
    }

    PublishGateItemView approvalSummaryItem(UUID templateId) {
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

    PublishGateItemView coverageThresholdsItem(CoverageSummaryView coverage) {
        boolean blocking = coverage.belowThreshold();
        return new PublishGateItemView(
                PublishGateCheckCode.COVERAGE_THRESHOLDS,
                !blocking,
                blocking,
                blocking ? "api.publishGate.coverageThresholds.blocked" : "api.publishGate.coverageThresholds.ready",
                "aggregatePct=" + coverage.aggregatePercentage()
        );
    }

    PublishGateItemView apiPolicyItem(UUID templateId) {
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

    PublishGateItemView contentModuleReferencesItem(UUID versionId) {
        return contentItems.contentModuleReferencesItem(versionId);
    }

    PublishGateItemView contentModuleEffectiveExpiredItem(UUID versionId) {
        return contentItems.contentModuleEffectiveExpiredItem(versionId);
    }

    PublishGateItemView contentModuleLocaleMismatchItem(UUID versionId) {
        return contentItems.contentModuleLocaleMismatchItem(versionId);
    }

    PublishGateItemView unsupportedStructuredNodesItem(UUID versionId) {
        return contentItems.unsupportedStructuredNodesItem(versionId);
    }

    PublishGateItemView pasteCleaningBlockersItem(UUID versionId) {
        return contentItems.pasteCleaningBlockersItem(versionId);
    }

    PublishGateItemView blockerStatusItem(
            UUID templateId,
            UUID versionId,
            BindingValidationView bindings,
            CoverageSummaryView coverage
    ) {
        return contentItems.blockerStatusItem(templateId, versionId, bindings, coverage);
    }
}
