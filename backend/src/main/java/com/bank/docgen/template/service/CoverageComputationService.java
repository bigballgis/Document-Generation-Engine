package com.bank.docgen.template.service;

import com.bank.docgen.template.port.PreviewEvidencePort;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.CoverageDimensionView;
import com.bank.docgen.template.api.CoverageSummaryView;
import com.bank.docgen.template.api.CoverageThresholdView;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.TestDataSetEntity;
import com.bank.docgen.template.persistence.TestDataSetRepository;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
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
    private final TestDataSetRepository testDataSetRepository;
    private final PreviewEvidencePort previewEvidencePort;
    private final CoverageThresholdResolver coverageThresholdResolver;
    private final TemplateCurrentVersionResolver templateVersionSupport;
    private final CoverageDimensionComputeSupport dimensions;

    public CoverageComputationService(
            TemplateService templateService,
            TemplateVersionRepository templateVersionRepository,
            VariableSchemaRepository variableSchemaRepository,
            TestDataSetRepository testDataSetRepository,
            PreviewEvidencePort previewEvidencePort,
            AnchorBindingRepository anchorBindingRepository,
            CoverageThresholdResolver coverageThresholdResolver,
            ObjectMapper objectMapper,
            TemplateCurrentVersionResolver templateVersionSupport
    ) {
        this.templateService = templateService;
        this.templateVersionRepository = templateVersionRepository;
        this.testDataSetRepository = testDataSetRepository;
        this.previewEvidencePort = previewEvidencePort;
        this.coverageThresholdResolver = coverageThresholdResolver;
        this.templateVersionSupport = templateVersionSupport;
        this.dimensions = new CoverageDimensionComputeSupport(
                variableSchemaRepository,
                anchorBindingRepository,
                objectMapper
        );
    }

    @Transactional(readOnly = true)
    public CoverageSummaryView compute(UUID templateId, ManagementSessionClaims session) {
        TemplateEntity template = templateService.requireReadableTemplate(templateId, session);
        TemplateVersionEntity version = templateVersionSupport.requireInFlightDevVersion(templateId);
        return computeForTemplateVersion(template, version);
    }

    @Transactional(readOnly = true)
    public CoverageSummaryView computeForVersion(
            UUID templateId,
            TemplateVersionEntity version,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = templateService.requireReadableTemplate(templateId, session);
        return computeForTemplateVersion(template, version);
    }

    private CoverageSummaryView computeForTemplateVersion(TemplateEntity template, TemplateVersionEntity version) {
        UUID templateId = template.getId();
        CoverageThresholdView threshold = coverageThresholdResolver.resolveForTemplate(template);

        List<TestDataSetEntity> dataSets = testDataSetRepository.findByTemplateIdOrderByUpdatedAtDesc(templateId);
        Set<String> exercisedVariableKeys = dimensions.collectExercisedVariableKeys(dataSets);
        Set<String> testedSampleIds = previewEvidencePort.successfulPreviewTestDataSetExternalIds(
                templateId,
                version.getId()
        );

        CoverageDimensionView requiredVariables = dimensions.computeRequiredVariables(
                version.getId(),
                exercisedVariableKeys,
                threshold.minRequiredVariablePct()
        );
        CoverageDimensionView requiredSamples = dimensions.computeRequiredSamples(
                dataSets,
                testedSampleIds,
                threshold.minRequiredSamplePct()
        );
        CoverageDimensionView anchorBindings = dimensions.computeAnchorBindings(
                version.getId(),
                threshold.minAnchorBindingPct()
        );

        List<CoverageDimensionView> dimensionViews = List.of(requiredVariables, requiredSamples, anchorBindings);
        int aggregatePercentage = dimensionViews.stream()
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
                dimensionViews,
                threshold
        );
    }
}
