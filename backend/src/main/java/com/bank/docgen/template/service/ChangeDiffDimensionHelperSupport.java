package com.bank.docgen.template.service;

import com.bank.docgen.template.api.ChangeDiffDimensionView;
import com.bank.docgen.template.api.CompositionRuleView;
import com.bank.docgen.template.domain.ChangeDiffDimension;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Package-private maps / fingerprints / empty-dimension helpers for change-diff.
 */
final class ChangeDiffDimensionHelperSupport {

    private final VariableSchemaRepository variableSchemaRepository;
    private final AnchorBindingRepository anchorBindingRepository;
    private final ObjectMapper objectMapper;

    ChangeDiffDimensionHelperSupport(
            VariableSchemaRepository variableSchemaRepository,
            AnchorBindingRepository anchorBindingRepository,
            ObjectMapper objectMapper
    ) {
        this.variableSchemaRepository = variableSchemaRepository;
        this.anchorBindingRepository = anchorBindingRepository;
        this.objectMapper = objectMapper;
    }

    Map<String, String> anchorHashes(UUID versionId) {
        Map<String, String> hashes = new HashMap<>();
        anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(versionId)
                .forEach(binding -> hashes.put(binding.getAnchorId(), fingerprint(binding.getStructuredContentJson())));
        return hashes;
    }

    Map<String, VariableSchemaEntity> variableMap(UUID versionId) {
        Map<String, VariableSchemaEntity> map = new HashMap<>();
        variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(versionId)
                .forEach(variable -> map.put(variable.getVariableKey(), variable));
        return map;
    }

    Map<String, CompositionRuleView> ruleMap(List<CompositionRuleView> rules) {
        Map<String, CompositionRuleView> map = new HashMap<>();
        rules.forEach(rule -> map.put(rule.ruleId(), rule));
        return map;
    }

    String ruleSignature(CompositionRuleView rule) {
        return rule.conditionExpression()
                + "|" + rule.targetAnchorId()
                + "|" + rule.trueBranchRuleId()
                + "|" + rule.falseBranchRuleId();
    }

    List<CompositionRuleView> readRules(String rulesJson) {
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

    ChangeDiffDimensionView emptyDimension(ChangeDiffDimension dimension) {
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
