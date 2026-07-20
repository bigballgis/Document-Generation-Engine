package com.bank.docgen.template.service;

import com.bank.docgen.template.api.BindingValidationView;
import com.bank.docgen.template.api.ChangeDiffView;
import com.bank.docgen.template.api.CoverageSummaryView;
import com.bank.docgen.template.api.PublishGateChecklistView;
import com.bank.docgen.template.api.PublishGateItemView;
import com.bank.docgen.template.api.TemplateRuleValidationView;
import com.bank.docgen.template.domain.PublishGateCheckCode;
import com.bank.docgen.template.domain.PublishGatePhase;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Package-private checklist aggregation and phase filtering for publish-gate evaluation.
 */
final class PublishGateChecklistSupport {

    private final PublishGateCheckItemSupport items;

    PublishGateChecklistSupport(PublishGateCheckItemSupport items) {
        this.items = items;
    }

    PublishGateChecklistView buildChecklist(
            UUID templateId,
            TemplateVersionEntity version,
            PublishGatePhase phase,
            BindingValidationView bindings,
            CoverageSummaryView coverage,
            ChangeDiffView changeDiff,
            TemplateRuleValidationView ruleValidation
    ) {
        List<PublishGateItemView> checklistItems = new ArrayList<>();
        checklistItems.add(items.anchorIntegrityItem(bindings));
        checklistItems.add(items.variableSchemaItem(version.getId()));
        checklistItems.add(items.ruleBoundsItem(ruleValidation));
        checklistItems.add(items.testResultsItem(templateId));
        checklistItems.add(items.previewPresentItem(templateId, version.getId()));
        checklistItems.add(items.fidelityWarningsViewedItem(templateId, version.getId()));
        checklistItems.add(items.changeDiffItem(changeDiff));
        checklistItems.add(items.approvalSummaryItem(templateId));
        checklistItems.add(items.coverageThresholdsItem(coverage));
        checklistItems.add(items.apiPolicyItem(templateId));
        checklistItems.add(items.contentModuleReferencesItem(version.getId()));
        checklistItems.add(items.contentModuleEffectiveExpiredItem(version.getId()));
        checklistItems.add(items.contentModuleEffectiveNotStartedItem(version.getId()));
        checklistItems.add(items.contentModuleLocaleMismatchItem(version.getId()));
        checklistItems.addAll(items.contentModuleNestingItems(version.getId()));
        checklistItems.add(items.compositionInclusionReferenceItem(version));
        checklistItems.add(items.unsupportedStructuredNodesItem(version.getId()));
        checklistItems.add(items.pasteCleaningBlockersItem(version.getId()));
        checklistItems.add(items.paginationDeltaBudgetItem(templateId, version));
        checklistItems.add(items.blockerStatusItem(templateId, version.getId(), bindings, coverage));

        List<PublishGateItemView> phaseItems = filterForPhase(checklistItems, phase);
        int blockerCount = (int) phaseItems.stream().filter(PublishGateItemView::blocker).count();
        boolean ready = blockerCount == 0;
        return new PublishGateChecklistView(templateId.toString(), ready, blockerCount, phaseItems);
    }

    private List<PublishGateItemView> filterForPhase(List<PublishGateItemView> checklistItems, PublishGatePhase phase) {
        if (phase == PublishGatePhase.PUBLISH) {
            return checklistItems;
        }
        return checklistItems.stream()
                .filter(item -> item.checkCode() != PublishGateCheckCode.APPROVAL_SUMMARY
                        && item.checkCode() != PublishGateCheckCode.API_POLICY)
                .toList();
    }
}
