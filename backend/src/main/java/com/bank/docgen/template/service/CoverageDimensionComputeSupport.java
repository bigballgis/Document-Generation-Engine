package com.bank.docgen.template.service;

import com.bank.docgen.template.api.CoverageDimensionView;
import com.bank.docgen.template.domain.BindingValidationStatus;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TestDataSetEntity;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Package-private per-dimension coverage calculations.
 */
final class CoverageDimensionComputeSupport {

    private static final Logger LOG = LoggerFactory.getLogger(CoverageDimensionComputeSupport.class);

    private final VariableSchemaRepository variableSchemaRepository;
    private final AnchorBindingRepository anchorBindingRepository;
    private final ObjectMapper objectMapper;

    CoverageDimensionComputeSupport(
            VariableSchemaRepository variableSchemaRepository,
            AnchorBindingRepository anchorBindingRepository,
            ObjectMapper objectMapper
    ) {
        this.variableSchemaRepository = variableSchemaRepository;
        this.anchorBindingRepository = anchorBindingRepository;
        this.objectMapper = objectMapper;
    }

    CoverageDimensionView computeRequiredVariables(
            UUID versionId,
            Set<String> exercisedVariableKeys,
            int thresholdPercentage
    ) {
        List<VariableSchemaEntity> requiredVariables = variableSchemaRepository
                .findByTemplateVersionIdOrderByVariableKeyAsc(versionId)
                .stream()
                .filter(VariableSchemaEntity::isRequired)
                .toList();
        int total = requiredVariables.size();
        int exercised = (int) requiredVariables.stream()
                .filter(variable -> exercisedVariableKeys.contains(variable.getVariableKey()))
                .count();
        return dimension(
                CoverageComputationService.DIMENSION_REQUIRED_VARIABLES,
                total,
                exercised,
                thresholdPercentage
        );
    }

    CoverageDimensionView computeRequiredSamples(
            List<TestDataSetEntity> dataSets,
            Set<String> testedSampleIds,
            int thresholdPercentage
    ) {
        List<TestDataSetEntity> requiredSamples = dataSets.stream().filter(TestDataSetEntity::isRequired).toList();
        int total = requiredSamples.size();
        int exercised = (int) requiredSamples.stream()
                .filter(sample -> testedSampleIds.contains(sample.getExternalId()))
                .count();
        return dimension(
                CoverageComputationService.DIMENSION_REQUIRED_SAMPLES,
                total,
                exercised,
                thresholdPercentage
        );
    }

    CoverageDimensionView computeAnchorBindings(UUID versionId, int thresholdPercentage) {
        List<AnchorBindingEntity> bindings = anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(versionId);
        int total = bindings.size();
        int exercised = (int) bindings.stream()
                .filter(binding -> binding.getValidationStatus() == BindingValidationStatus.VALID)
                .count();
        return dimension(
                CoverageComputationService.DIMENSION_ANCHOR_BINDINGS,
                total,
                exercised,
                thresholdPercentage
        );
    }

    Set<String> collectExercisedVariableKeys(List<TestDataSetEntity> dataSets) {
        Set<String> keys = new HashSet<>();
        for (TestDataSetEntity dataSet : dataSets) {
            keys.addAll(readVariableKeys(dataSet.getVariablesJson()));
        }
        return keys;
    }

    private CoverageDimensionView dimension(
            String dimension,
            int total,
            int exercised,
            int thresholdPercentage
    ) {
        int percentage = percentage(exercised, total);
        return new CoverageDimensionView(
                dimension,
                total,
                exercised,
                percentage,
                thresholdPercentage,
                total > 0 && percentage < thresholdPercentage
        );
    }

    private List<String> readVariableKeys(String variablesJson) {
        if (variablesJson == null || variablesJson.isBlank()) {
            return List.of();
        }
        try {
            java.util.Map<String, Object> variables = objectMapper.readValue(variablesJson, new TypeReference<>() {
            });
            return new ArrayList<>(variables.keySet());
        } catch (JsonProcessingException ex) {
            LOG.debug("Failed to parse test data set variables JSON: {}", ex.getMessage());
            return List.of();
        }
    }

    private int percentage(int exercised, int total) {
        if (total == 0) {
            return 100;
        }
        return (int) Math.floor((exercised * 100.0) / total);
    }
}
