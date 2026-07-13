package com.bank.docgen.template.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.template.api.ChangeDiffDimensionView;
import com.bank.docgen.template.api.ChangeDiffModificationView;
import com.bank.docgen.template.api.CompositionRuleView;
import com.bank.docgen.template.domain.ChangeDiffDimension;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Package-private dimension diffs for change-diff computation.
 */
final class ChangeDiffDimensionSupport {

    private final ApiPolicyRepository apiPolicyRepository;
    private final ChangeDiffDimensionHelperSupport helpers;

    ChangeDiffDimensionSupport(
            VariableSchemaRepository variableSchemaRepository,
            AnchorBindingRepository anchorBindingRepository,
            ApiPolicyRepository apiPolicyRepository,
            ObjectMapper objectMapper
    ) {
        this.apiPolicyRepository = apiPolicyRepository;
        this.helpers = new ChangeDiffDimensionHelperSupport(
                variableSchemaRepository, anchorBindingRepository, objectMapper);
    }

    List<ChangeDiffDimensionView> buildDimensions(
            UUID templateId, TemplateVersionEntity candidate, TemplateVersionEntity baseline) {
        List<ChangeDiffDimensionView> dimensions = new ArrayList<>();
        dimensions.add(diffContent(candidate, baseline));
        dimensions.add(diffAnchors(candidate.getId(), baseline == null ? null : baseline.getId()));
        dimensions.add(diffVariables(candidate.getId(), baseline == null ? null : baseline.getId()));
        dimensions.add(diffRules(candidate, baseline));
        dimensions.add(diffContractSummary(templateId, baseline));
        return dimensions;
    }

    private ChangeDiffDimensionView diffContent(TemplateVersionEntity candidate, TemplateVersionEntity baseline) {
        if (baseline == null) {
            return helpers.emptyDimension(ChangeDiffDimension.CONTENT);
        }
        List<ChangeDiffModificationView> modified = new ArrayList<>();
        if (!candidate.getMasterCatalogVersion().equals(baseline.getMasterCatalogVersion())) {
            modified.add(new ChangeDiffModificationView(
                    "masterCatalogVersion", "MODIFIED", "masterCatalogVersion changed"));
        }
        return new ChangeDiffDimensionView(ChangeDiffDimension.CONTENT, List.of(), List.of(), modified);
    }

    private ChangeDiffDimensionView diffAnchors(UUID candidateVersionId, UUID baselineVersionId) {
        if (baselineVersionId == null) {
            List<String> added = helpers.anchorHashes(candidateVersionId).keySet().stream().sorted().toList();
            return new ChangeDiffDimensionView(ChangeDiffDimension.ANCHORS, added, List.of(), List.of());
        }
        Map<String, String> candidateHashes = helpers.anchorHashes(candidateVersionId);
        Map<String, String> baselineHashes = helpers.anchorHashes(baselineVersionId);
        Set<String> candidateIds = candidateHashes.keySet();
        Set<String> baselineIds = baselineHashes.keySet();
        List<String> added = candidateIds.stream().filter(id -> !baselineIds.contains(id)).sorted().toList();
        List<String> removed = baselineIds.stream().filter(id -> !candidateIds.contains(id)).sorted().toList();
        List<ChangeDiffModificationView> modified = new ArrayList<>();
        for (String anchorId : candidateIds) {
            if (baselineIds.contains(anchorId) && !candidateHashes.get(anchorId).equals(baselineHashes.get(anchorId))) {
                modified.add(new ChangeDiffModificationView(
                        anchorId, "MODIFIED", "bindingHash=" + candidateHashes.get(anchorId)));
            }
        }
        return new ChangeDiffDimensionView(ChangeDiffDimension.ANCHORS, added, removed, modified);
    }

    private ChangeDiffDimensionView diffVariables(UUID candidateVersionId, UUID baselineVersionId) {
        Map<String, VariableSchemaEntity> candidateVars = helpers.variableMap(candidateVersionId);
        if (baselineVersionId == null) {
            return new ChangeDiffDimensionView(
                    ChangeDiffDimension.VARIABLES,
                    candidateVars.keySet().stream().sorted().toList(), List.of(), List.of());
        }
        Map<String, VariableSchemaEntity> baselineVars = helpers.variableMap(baselineVersionId);
        Set<String> candidateKeys = candidateVars.keySet();
        Set<String> baselineKeys = baselineVars.keySet();
        List<String> added = candidateKeys.stream().filter(key -> !baselineKeys.contains(key)).sorted().toList();
        List<String> removed = baselineKeys.stream().filter(key -> !candidateKeys.contains(key)).sorted().toList();
        List<ChangeDiffModificationView> modified = new ArrayList<>();
        for (String key : candidateKeys) {
            if (!baselineKeys.contains(key)) {
                continue;
            }
            VariableSchemaEntity candidateVar = candidateVars.get(key);
            VariableSchemaEntity baselineVar = baselineVars.get(key);
            if (candidateVar.isRequired() != baselineVar.isRequired()
                    || candidateVar.getVariableType() != baselineVar.getVariableType()) {
                modified.add(new ChangeDiffModificationView(key, "MODIFIED", "schemaChanged"));
            }
        }
        return new ChangeDiffDimensionView(ChangeDiffDimension.VARIABLES, added, removed, modified);
    }

    private ChangeDiffDimensionView diffRules(TemplateVersionEntity candidate, TemplateVersionEntity baseline) {
        List<CompositionRuleView> candidateRules = helpers.readRules(candidate.getRulesJson());
        if (baseline == null) {
            return new ChangeDiffDimensionView(
                    ChangeDiffDimension.RULES,
                    candidateRules.stream().map(CompositionRuleView::ruleId).sorted().toList(),
                    List.of(), List.of());
        }
        Map<String, CompositionRuleView> candidateMap = helpers.ruleMap(candidateRules);
        Map<String, CompositionRuleView> baselineMap = helpers.ruleMap(helpers.readRules(baseline.getRulesJson()));
        Set<String> candidateIds = candidateMap.keySet();
        Set<String> baselineIds = baselineMap.keySet();
        List<String> added = candidateIds.stream().filter(id -> !baselineIds.contains(id)).sorted().toList();
        List<String> removed = baselineIds.stream().filter(id -> !candidateIds.contains(id)).sorted().toList();
        List<ChangeDiffModificationView> modified = new ArrayList<>();
        for (String ruleId : candidateIds) {
            if (!baselineIds.contains(ruleId)) {
                continue;
            }
            if (!helpers.ruleSignature(candidateMap.get(ruleId)).equals(helpers.ruleSignature(baselineMap.get(ruleId)))) {
                modified.add(new ChangeDiffModificationView(ruleId, "MODIFIED", "ruleExpressionChanged"));
            }
        }
        return new ChangeDiffDimensionView(ChangeDiffDimension.RULES, added, removed, modified);
    }

    private ChangeDiffDimensionView diffContractSummary(UUID templateId, TemplateVersionEntity baseline) {
        ApiPolicyEntity policy = apiPolicyRepository.findByTemplateId(templateId).orElse(null);
        if (policy == null) {
            return helpers.emptyDimension(ChangeDiffDimension.CONTRACT_SUMMARY);
        }
        if (baseline == null) {
            return new ChangeDiffDimensionView(
                    ChangeDiffDimension.CONTRACT_SUMMARY, List.of("apiPolicyConfigured"), List.of(), List.of());
        }
        return helpers.emptyDimension(ChangeDiffDimension.CONTRACT_SUMMARY);
    }
}
