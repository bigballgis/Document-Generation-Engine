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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Package-private dimension diffs for change-diff computation.
 */
final class ChangeDiffDimensionSupport {

    private final VariableSchemaRepository variableSchemaRepository;
    private final AnchorBindingRepository anchorBindingRepository;
    private final ApiPolicyRepository apiPolicyRepository;
    private final ObjectMapper objectMapper;

    ChangeDiffDimensionSupport(
            VariableSchemaRepository variableSchemaRepository,
            AnchorBindingRepository anchorBindingRepository,
            ApiPolicyRepository apiPolicyRepository,
            ObjectMapper objectMapper
    ) {
        this.variableSchemaRepository = variableSchemaRepository;
        this.anchorBindingRepository = anchorBindingRepository;
        this.apiPolicyRepository = apiPolicyRepository;
        this.objectMapper = objectMapper;
    }

    List<ChangeDiffDimensionView> buildDimensions(UUID templateId, TemplateVersionEntity candidate, TemplateVersionEntity baseline) {
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
            return emptyDimension(ChangeDiffDimension.CONTENT);
        }
        List<ChangeDiffModificationView> modified = new ArrayList<>();
        if (!candidate.getMasterCatalogVersion().equals(baseline.getMasterCatalogVersion())) {
            modified.add(new ChangeDiffModificationView(
                    "masterCatalogVersion",
                    "MODIFIED",
                    "masterCatalogVersion changed"
            ));
        }
        return new ChangeDiffDimensionView(ChangeDiffDimension.CONTENT, List.of(), List.of(), modified);
    }

    private ChangeDiffDimensionView diffAnchors(UUID candidateVersionId, UUID baselineVersionId) {
        if (baselineVersionId == null) {
            List<String> added = anchorBindingRepository
                    .findByTemplateVersionIdOrderByAnchorIdAsc(candidateVersionId)
                    .stream()
                    .map(binding -> binding.getAnchorId())
                    .toList();
            return new ChangeDiffDimensionView(ChangeDiffDimension.ANCHORS, added, List.of(), List.of());
        }

        Map<String, String> candidateHashes = anchorHashes(candidateVersionId);
        Map<String, String> baselineHashes = anchorHashes(baselineVersionId);
        Set<String> candidateIds = candidateHashes.keySet();
        Set<String> baselineIds = baselineHashes.keySet();

        List<String> added = candidateIds.stream().filter(id -> !baselineIds.contains(id)).sorted().toList();
        List<String> removed = baselineIds.stream().filter(id -> !candidateIds.contains(id)).sorted().toList();
        List<ChangeDiffModificationView> modified = new ArrayList<>();
        for (String anchorId : candidateIds) {
            if (baselineIds.contains(anchorId) && !candidateHashes.get(anchorId).equals(baselineHashes.get(anchorId))) {
                modified.add(new ChangeDiffModificationView(
                        anchorId,
                        "MODIFIED",
                        "bindingHash=" + candidateHashes.get(anchorId)
                ));
            }
        }
        return new ChangeDiffDimensionView(ChangeDiffDimension.ANCHORS, added, removed, modified);
    }

    private ChangeDiffDimensionView diffVariables(UUID candidateVersionId, UUID baselineVersionId) {
        Map<String, VariableSchemaEntity> candidateVars = variableMap(candidateVersionId);
        if (baselineVersionId == null) {
            List<String> added = candidateVars.keySet().stream().sorted().toList();
            return new ChangeDiffDimensionView(ChangeDiffDimension.VARIABLES, added, List.of(), List.of());
        }

        Map<String, VariableSchemaEntity> baselineVars = variableMap(baselineVersionId);
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
        List<CompositionRuleView> candidateRules = readRules(candidate.getRulesJson());
        if (baseline == null) {
            List<String> added = candidateRules.stream().map(CompositionRuleView::ruleId).sorted().toList();
            return new ChangeDiffDimensionView(ChangeDiffDimension.RULES, added, List.of(), List.of());
        }

        List<CompositionRuleView> baselineRules = readRules(baseline.getRulesJson());
        Map<String, CompositionRuleView> candidateMap = ruleMap(candidateRules);
        Map<String, CompositionRuleView> baselineMap = ruleMap(baselineRules);
        Set<String> candidateIds = candidateMap.keySet();
        Set<String> baselineIds = baselineMap.keySet();

        List<String> added = candidateIds.stream().filter(id -> !baselineIds.contains(id)).sorted().toList();
        List<String> removed = baselineIds.stream().filter(id -> !candidateIds.contains(id)).sorted().toList();
        List<ChangeDiffModificationView> modified = new ArrayList<>();
        for (String ruleId : candidateIds) {
            if (!baselineIds.contains(ruleId)) {
                continue;
            }
            CompositionRuleView candidateRule = candidateMap.get(ruleId);
            CompositionRuleView baselineRule = baselineMap.get(ruleId);
            if (!ruleSignature(candidateRule).equals(ruleSignature(baselineRule))) {
                modified.add(new ChangeDiffModificationView(ruleId, "MODIFIED", "ruleExpressionChanged"));
            }
        }
        return new ChangeDiffDimensionView(ChangeDiffDimension.RULES, added, removed, modified);
    }

    private ChangeDiffDimensionView diffContractSummary(UUID templateId, TemplateVersionEntity baseline) {
        ApiPolicyEntity policy = apiPolicyRepository.findByTemplateId(templateId).orElse(null);
        if (policy == null) {
            return emptyDimension(ChangeDiffDimension.CONTRACT_SUMMARY);
        }
        if (baseline == null) {
            return new ChangeDiffDimensionView(
                    ChangeDiffDimension.CONTRACT_SUMMARY,
                    List.of("apiPolicyConfigured"),
                    List.of(),
                    List.of()
            );
        }
        return emptyDimension(ChangeDiffDimension.CONTRACT_SUMMARY);
    }

    private Map<String, String> anchorHashes(UUID versionId) {
        Map<String, String> hashes = new HashMap<>();
        anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(versionId)
                .forEach(binding -> hashes.put(binding.getAnchorId(), fingerprint(binding.getStructuredContentJson())));
        return hashes;
    }

    private Map<String, VariableSchemaEntity> variableMap(UUID versionId) {
        Map<String, VariableSchemaEntity> map = new HashMap<>();
        variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(versionId)
                .forEach(variable -> map.put(variable.getVariableKey(), variable));
        return map;
    }

    private Map<String, CompositionRuleView> ruleMap(List<CompositionRuleView> rules) {
        Map<String, CompositionRuleView> map = new HashMap<>();
        rules.forEach(rule -> map.put(rule.ruleId(), rule));
        return map;
    }

    private String ruleSignature(CompositionRuleView rule) {
        return rule.conditionExpression()
                + "|" + rule.targetAnchorId()
                + "|" + rule.trueBranchRuleId()
                + "|" + rule.falseBranchRuleId();
    }

    private List<CompositionRuleView> readRules(String rulesJson) {
        if (rulesJson == null || rulesJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rulesJson, new TypeReference<List<CompositionRuleView>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private ChangeDiffDimensionView emptyDimension(ChangeDiffDimension dimension) {
        return new ChangeDiffDimensionView(dimension, List.of(), List.of(), List.of());
    }

    private String fingerprint(String value) {
        if (value == null) {
            return "none";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (Exception ex) {
            return "unknown";
        }
    }
}
