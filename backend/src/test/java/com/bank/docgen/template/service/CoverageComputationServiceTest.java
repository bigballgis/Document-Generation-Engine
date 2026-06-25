package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.rendering.domain.PreviewStatus;
import com.bank.docgen.rendering.persistence.PreviewRecordEntity;
import com.bank.docgen.rendering.persistence.PreviewRecordRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
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
import com.bank.docgen.template.domain.AnchorContentType;
import com.bank.docgen.template.domain.VariableType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CoverageComputationServiceTest {

    @Mock
    private TemplateService templateService;
    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private VariableSchemaRepository variableSchemaRepository;
    @Mock
    private TestDataSetRepository testDataSetRepository;
    @Mock
    private PreviewRecordRepository previewRecordRepository;
    @Mock
    private AnchorBindingRepository anchorBindingRepository;
    @Mock
    private CoverageThresholdResolver coverageThresholdResolver;

    private CoverageComputationService service;
    private UUID templateId;
    private UUID versionId;
    private ManagementSessionClaims author;
    private TemplateEntity template;

    @BeforeEach
    void setUp() {
        service = new CoverageComputationService(
                templateService,
                templateVersionRepository,
                variableSchemaRepository,
                testDataSetRepository,
                previewRecordRepository,
                anchorBindingRepository,
                coverageThresholdResolver,
                new ObjectMapper()
        );
        templateId = UUID.randomUUID();
        versionId = UUID.randomUUID();
        template = new TemplateEntity(
                templateId,
                "TPL-1",
                "RETAIL",
                "Demo",
                null,
                UUID.randomUUID(),
                "10000003"
        );
        author = new ManagementSessionClaims(
                "10000003",
                "Author",
                "author@example.com",
                AuthSource.LOCAL,
                List.of("TEMPLATE_AUTHOR"),
                List.of("RETAIL"),
                "route.template-authoring-home",
                List.of("route.template-authoring-home"),
                Instant.now().plusSeconds(3600)
        );
        when(templateService.requireReadableTemplate(templateId, author)).thenReturn(template);
        when(templateVersionRepository.findByTemplateIdAndDevVersionNumber(templateId, 1))
                .thenReturn(java.util.Optional.of(new TemplateVersionEntity(versionId, templateId, "10000003")));
        when(coverageThresholdResolver.resolveForTemplate(template))
                .thenReturn(new CoverageThresholdView("GLOBAL", null, 80, 100, 80));
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(versionId))
                .thenReturn(List.of(validBinding("ANCHOR-1")));
        when(previewRecordRepository.findByTemplateIdAndTemplateVersionIdAndStatus(
                templateId, versionId, PreviewStatus.SUCCEEDED))
                .thenReturn(List.of());
    }

    @Test
    void coverage_countsRequiredVariablesExercised() {
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(versionId))
                .thenReturn(List.of(
                        requiredVariable("customerName"),
                        requiredVariable("accountId")
                ));
        when(testDataSetRepository.findByTemplateIdOrderByUpdatedAtDesc(templateId))
                .thenReturn(List.of(dataSet("TDS-1", "{\"customerName\":\"SecretCustomer\"}", false)));

        CoverageSummaryView summary = service.compute(templateId, author);

        CoverageSummaryView requiredVariables = summary;
        assertThat(requiredVariables.dimensions().get(0).totalCount()).isEqualTo(2);
        assertThat(requiredVariables.dimensions().get(0).exercisedCount()).isEqualTo(1);
        assertThat(requiredVariables.dimensions().get(0).percentage()).isEqualTo(50);
    }

    @Test
    void coverage_belowThreshold_isFlaggedAsBlocker() {
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(versionId))
                .thenReturn(List.of(
                        requiredVariable("customerName"),
                        requiredVariable("accountId")
                ));
        when(testDataSetRepository.findByTemplateIdOrderByUpdatedAtDesc(templateId))
                .thenReturn(List.of(dataSet("TDS-1", "{\"customerName\":\"SecretCustomer\"}", false)));

        CoverageSummaryView summary = service.compute(templateId, author);

        assertThat(summary.belowThreshold()).isTrue();
        assertThat(summary.blockerCodes())
                .contains(CoverageComputationService.BLOCKER_REQUIRED_VARIABLES);
        assertThat(summary.aggregatePercentage()).isEqualTo(50);
    }

    @Test
    void coverage_summary_excludesVariablePlaintext() throws Exception {
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(versionId))
                .thenReturn(List.of(requiredVariable("customerName")));
        when(testDataSetRepository.findByTemplateIdOrderByUpdatedAtDesc(templateId))
                .thenReturn(List.of(dataSet("TDS-1", "{\"customerName\":\"SecretCustomer\"}", false)));

        CoverageSummaryView summary = service.compute(templateId, author);
        String serialized = new ObjectMapper().writeValueAsString(summary);

        assertThat(serialized).doesNotContain("SecretCustomer");
    }

    private VariableSchemaEntity requiredVariable(String key) {
        return new VariableSchemaEntity(
                UUID.randomUUID(),
                versionId,
                key,
                VariableType.TEXT,
                true,
                null,
                null,
                "Required"
        );
    }

    private TestDataSetEntity dataSet(String externalId, String variablesJson, boolean required) {
        return new TestDataSetEntity(
                UUID.randomUUID(),
                templateId,
                externalId,
                "Sample",
                null,
                variablesJson,
                required,
                null,
                "[]",
                1,
                false,
                null
        );
    }

    private AnchorBindingEntity validBinding(String anchorId) {
        return new AnchorBindingEntity(
                UUID.randomUUID(),
                versionId,
                anchorId,
                AnchorContentType.TEXT,
                "{}",
                BindingValidationStatus.VALID
        );
    }
}
