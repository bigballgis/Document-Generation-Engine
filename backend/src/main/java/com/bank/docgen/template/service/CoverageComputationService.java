package com.bank.docgen.template.service;

import com.bank.docgen.rendering.domain.PreviewStatus;
import com.bank.docgen.rendering.persistence.PreviewRecordEntity;
import com.bank.docgen.rendering.persistence.PreviewRecordRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.CoverageDimensionView;
import com.bank.docgen.template.api.CoverageSummaryView;
import com.bank.docgen.template.api.CoverageThresholdView;
import com.bank.docgen.template.domain.BindingValidationStatus;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.TestDataSetEntity;
import com.bank.docgen.template.persistence.TestDataSetRepository;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CoverageComputationService {

    public static final String DIMENSION_REQUIRED_VARIABLES = "REQUIRED_VARIABLES";
    public static final String DIMENSION_REQUIRED_SAMPLES = "REQUIRED_SAMPLES";
    public static final String DIMENSION_ANCHOR_BINDINGS = "ANCHOR_BINDINGS";

    public static final String BLOCKER_REQUIRED_VARIABLES = "REQUIRED_VARIABLE_COVERAGE_BELOW_THRESHOLD";
    public static final String BLOCKER_REQUIRED_SAMPLES = "REQUIRED_SAMPLE_COVERAGE_BELOW_THRESHOLD";
    public static final String BLOCKER_ANCHOR_BINDINGS = "ANCHOR_BINDING_COVERAGE_BELOW_THRESHOLD";

    private final TemplateService templateService;
    private final TemplateVersionRepository templateVersionRepository;
    private final VariableSchemaRepository variableSchemaRepository;
    private final TestDataSetRepository testDataSetRepository;
    private final PreviewRecordRepository previewRecordRepository;
    private final AnchorBindingRepository anchorBindingRepository;
    private final CoverageThresholdResolver coverageThresholdResolver;
    private final ObjectMapper objectMapper;

    public CoverageComputationService(
            TemplateService templateService,
            TemplateVersionRepository templateVersionRepository,
            VariableSchemaRepository variableSchemaRepository,
            TestDataSetRepository testDataSetRepository,
            PreviewRecordRepository previewRecordRepository,
            AnchorBindingRepository anchorBindingRepository,
            CoverageThresholdResolver coverageThresholdResolver,
            ObjectMapper objectMapper
    ) {
        this.templateService = templateService;
        this.templateVersionRepository = templateVersionRepository;
        this.variableSchemaRepository = variableSchemaRepository;
        this.testDataSetRepository = testDataSetRepository;
        this.previewRecordRepository = previewRecordRepository;
        this.anchorBindingRepository = anchorBindingRepository;
        this.coverageThresholdResolver = coverageThresholdResolver;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public CoverageSummaryView compute(UUID templateId, ManagementSessionClaims session) {
        TemplateEntity template = templateService.requireReadableTemplate(templateId, session);
        TemplateVersionEntity version = currentDevVersion(templateId);
        CoverageThresholdView threshold = coverageThresholdResolver.resolveForTemplate(template);

        List<TestDataSetEntity> dataSets = testDataSetRepository.findByTemplateIdOrderByUpdatedAtDesc(templateId);
        Set<String> exercisedVariableKeys = collectExercisedVariableKeys(dataSets);
        List<PreviewRecordEntity> successfulPreviews = previewRecordRepository
                .findByTemplateIdAndTemplateVersionIdAndStatus(templateId, version.getId(), PreviewStatus.SUCCEEDED);
        Set<String> testedSampleIds = new HashSet<>();
        successfulPreviews.forEach(preview -> {
            if (preview.getTestDataSetExternalId() != null) {
                testedSampleIds.add(preview.getTestDataSetExternalId());
            }
        });

        CoverageDimensionView requiredVariables = computeRequiredVariables(
                version.getId(),
                exercisedVariableKeys,
                threshold.minRequiredVariablePct()
        );
        CoverageDimensionView requiredSamples = computeRequiredSamples(
                dataSets,
                testedSampleIds,
                threshold.minRequiredSamplePct()
        );
        CoverageDimensionView anchorBindings = computeAnchorBindings(
                version.getId(),
                threshold.minAnchorBindingPct()
        );

        List<CoverageDimensionView> dimensions = List.of(requiredVariables, requiredSamples, anchorBindings);
        int aggregatePercentage = dimensions.stream()
                .mapToInt(CoverageDimensionView::percentage)
                .min()
                .orElse(100);
        List<String> blockerCodes = new ArrayList<>();
        if (requiredVariables.belowThreshold()) {
            blockerCodes.add(BLOCKER_REQUIRED_VARIABLES);
        }
        if (requiredSamples.belowThreshold()) {
            blockerCodes.add(BLOCKER_REQUIRED_SAMPLES);
        }
        if (anchorBindings.belowThreshold()) {
            blockerCodes.add(BLOCKER_ANCHOR_BINDINGS);
        }

        return new CoverageSummaryView(
                templateId.toString(),
                aggregatePercentage,
                !blockerCodes.isEmpty(),
                blockerCodes,
                dimensions,
                threshold
        );
    }

    private CoverageDimensionView computeRequiredVariables(
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
        int percentage = percentage(exercised, total);
        return new CoverageDimensionView(
                DIMENSION_REQUIRED_VARIABLES,
                total,
                exercised,
                percentage,
                thresholdPercentage,
                total > 0 && percentage < thresholdPercentage
        );
    }

    private CoverageDimensionView computeRequiredSamples(
            List<TestDataSetEntity> dataSets,
            Set<String> testedSampleIds,
            int thresholdPercentage
    ) {
        List<TestDataSetEntity> requiredSamples = dataSets.stream().filter(TestDataSetEntity::isRequired).toList();
        int total = requiredSamples.size();
        int exercised = (int) requiredSamples.stream()
                .filter(sample -> testedSampleIds.contains(sample.getExternalId()))
                .count();
        int percentage = percentage(exercised, total);
        return new CoverageDimensionView(
                DIMENSION_REQUIRED_SAMPLES,
                total,
                exercised,
                percentage,
                thresholdPercentage,
                total > 0 && percentage < thresholdPercentage
        );
    }

    private CoverageDimensionView computeAnchorBindings(UUID versionId, int thresholdPercentage) {
        List<AnchorBindingEntity> bindings = anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(versionId);
        int total = bindings.size();
        int exercised = (int) bindings.stream()
                .filter(binding -> binding.getValidationStatus() == BindingValidationStatus.VALID)
                .count();
        int percentage = percentage(exercised, total);
        return new CoverageDimensionView(
                DIMENSION_ANCHOR_BINDINGS,
                total,
                exercised,
                percentage,
                thresholdPercentage,
                total > 0 && percentage < thresholdPercentage
        );
    }

    private Set<String> collectExercisedVariableKeys(List<TestDataSetEntity> dataSets) {
        Set<String> keys = new HashSet<>();
        for (TestDataSetEntity dataSet : dataSets) {
            keys.addAll(readVariableKeys(dataSet.getVariablesJson()));
        }
        return keys;
    }

    private List<String> readVariableKeys(String variablesJson) {
        if (variablesJson == null || variablesJson.isBlank()) {
            return List.of();
        }
        try {
            java.util.Map<String, Object> variables = objectMapper.readValue(variablesJson, new TypeReference<>() {
            });
            return new ArrayList<>(variables.keySet());
        } catch (Exception ex) {
            return List.of();
        }
    }

    private int percentage(int exercised, int total) {
        if (total == 0) {
            return 100;
        }
        return (int) Math.floor((exercised * 100.0) / total);
    }

    private TemplateVersionEntity currentDevVersion(UUID templateId) {
        return templateVersionRepository.findByTemplateIdAndDevVersionNumber(templateId, 1)
                .orElseThrow(TemplateNotFoundException::new);
    }
}
