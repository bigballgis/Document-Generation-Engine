package com.bank.docgen.rendering.service;

import com.bank.docgen.rendering.api.SubmitTestEligibilityView;
import com.bank.docgen.rendering.api.SubmitTestEligibilityView.BlockingDetails;
import com.bank.docgen.rendering.api.SubmitTestEligibilityView.Conditions;
import com.bank.docgen.rendering.api.SubmitTestEligibilityView.Thresholds;
import com.bank.docgen.rendering.persistence.BatchTestRunEntity;
import com.bank.docgen.rendering.persistence.BatchTestRunRepository;
import com.bank.docgen.rendering.persistence.PreviewRecordRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.domain.BindingValidationStatus;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TestDataSetEntity;
import com.bank.docgen.template.persistence.TestDataSetRepository;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.bank.docgen.template.service.CoverageThresholdResolver;
import com.bank.docgen.template.service.TemplateCurrentVersionResolver;
import com.bank.docgen.template.service.TemplateService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmitTestEligibilityService {

    private static final Logger LOG = LoggerFactory.getLogger(SubmitTestEligibilityService.class);
    private static final int MAX_LIST_SIZE = 5;

    private final TemplateService templateService;
    private final BatchTestRunRepository batchTestRunRepository;
    private final PreviewRecordRepository previewRecordRepository;
    private final VariableSchemaRepository variableSchemaRepository;
    private final AnchorBindingRepository anchorBindingRepository;
    private final TestDataSetRepository testDataSetRepository;
    private final CoverageThresholdResolver coverageThresholdResolver;
    private final TemplateCurrentVersionResolver templateCurrentVersionResolver;
    private final ObjectMapper objectMapper;

    public SubmitTestEligibilityService(
            TemplateService templateService,
            BatchTestRunRepository batchTestRunRepository,
            PreviewRecordRepository previewRecordRepository,
            VariableSchemaRepository variableSchemaRepository,
            AnchorBindingRepository anchorBindingRepository,
            TestDataSetRepository testDataSetRepository,
            CoverageThresholdResolver coverageThresholdResolver,
            TemplateCurrentVersionResolver templateCurrentVersionResolver,
            ObjectMapper objectMapper
    ) {
        this.templateService = templateService;
        this.batchTestRunRepository = batchTestRunRepository;
        this.previewRecordRepository = previewRecordRepository;
        this.variableSchemaRepository = variableSchemaRepository;
        this.anchorBindingRepository = anchorBindingRepository;
        this.testDataSetRepository = testDataSetRepository;
        this.coverageThresholdResolver = coverageThresholdResolver;
        this.templateCurrentVersionResolver = templateCurrentVersionResolver;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public SubmitTestEligibilityView evaluate(UUID templateId, ManagementSessionClaims session) {
        var template = templateService.requireReadableTemplate(templateId, session);
        var version = templateCurrentVersionResolver.requireInFlightDevVersion(templateId);
        var threshold = coverageThresholdResolver.resolveForTemplate(template);

        var latestValid = batchTestRunRepository.findLatestValidByTemplateId(templateId);
        BatchTestRunEntity latestRun = latestValid.orElse(null);

        boolean hasValidTestResult = latestRun != null;
        boolean allSamplesSucceeded = latestRun != null && Boolean.TRUE.equals(latestRun.getAllSamplesSucceeded());
        boolean coverageGatePassed = latestRun != null && Boolean.TRUE.equals(latestRun.getGatePassed());
        boolean eligible = hasValidTestResult && allSamplesSucceeded && coverageGatePassed;

        List<String> uncoveredVariables = new ArrayList<>();
        int uncoveredVariablesTotal = 0;
        List<String> uncoveredAnchors = new ArrayList<>();
        int uncoveredAnchorsTotal = 0;
        List<String> failedDataSetNames = new ArrayList<>();

        if (!eligible) {
            Set<String> exercisedKeys = collectExercisedVariableKeys(templateId);
            uncoveredVariables = computeUncoveredVariables(version.getId(), exercisedKeys);
            uncoveredVariablesTotal = uncoveredVariables.size();
            uncoveredAnchors = computeUncoveredAnchors(version.getId());
            uncoveredAnchorsTotal = uncoveredAnchors.size();
            if (latestRun != null && latestRun.getFailedCount() > 0) {
                failedDataSetNames = collectFailedDataSetNames(latestRun);
            }
        }

        return new SubmitTestEligibilityView(
                eligible,
                new Conditions(hasValidTestResult, allSamplesSucceeded, coverageGatePassed),
                new BlockingDetails(
                        cap(uncoveredAnchors),
                        cap(uncoveredVariables),
                        uncoveredAnchorsTotal,
                        uncoveredVariablesTotal,
                        cap(failedDataSetNames)
                ),
                new Thresholds(
                        threshold.minAnchorBindingPct(),
                        threshold.minRequiredVariablePct(),
                        threshold.minRequiredSamplePct()
                ),
                latestRun != null ? latestRun.getCreatedAt() : null
        );
    }

    private Set<String> collectExercisedVariableKeys(UUID templateId) {
        List<TestDataSetEntity> dataSets = testDataSetRepository.findByTemplateIdOrderByUpdatedAtDesc(templateId);
        Set<String> keys = new HashSet<>();
        for (TestDataSetEntity ds : dataSets) {
            keys.addAll(readVariableKeys(ds.getVariablesJson()));
        }
        return keys;
    }

    private List<String> computeUncoveredVariables(UUID versionId, Set<String> exercisedKeys) {
        return variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(versionId)
                .stream()
                .filter(VariableSchemaEntity::isRequired)
                .filter(v -> !exercisedKeys.contains(v.getVariableKey()))
                .map(VariableSchemaEntity::getVariableKey)
                .toList();
    }

    private List<String> computeUncoveredAnchors(UUID versionId) {
        return anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(versionId)
                .stream()
                .filter(b -> b.getValidationStatus() != BindingValidationStatus.VALID)
                .map(AnchorBindingEntity::getAnchorId)
                .toList();
    }

    private List<String> collectFailedDataSetNames(BatchTestRunEntity run) {
        try {
            List<Map<String, Object>> samples = objectMapper.readValue(
                    run.getSampleResultsJson() != null ? run.getSampleResultsJson() : "[]",
                    new TypeReference<>() {}
            );
            return samples.stream()
                    .filter(s -> !Boolean.TRUE.equals(s.get("success")))
                    .map(s -> (String) s.get("dataSetExternalId"))
                    .filter(id -> id != null)
                    .toList();
        } catch (Exception ex) {
            LOG.debug("Failed to parse sample results for run {}: {}", run.getId(), ex.getMessage());
            return List.of();
        }
    }

    private List<String> readVariableKeys(String variablesJson) {
        if (variablesJson == null || variablesJson.isBlank()) {
            return List.of();
        }
        try {
            Map<String, Object> vars = objectMapper.readValue(variablesJson, new TypeReference<>() {});
            return new ArrayList<>(vars.keySet());
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<String> cap(List<String> list) {
        if (list.size() <= MAX_LIST_SIZE) {
            return list;
        }
        return list.subList(0, MAX_LIST_SIZE);
    }
}
