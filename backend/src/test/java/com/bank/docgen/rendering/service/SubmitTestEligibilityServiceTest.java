package com.bank.docgen.rendering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.rendering.api.SubmitTestEligibilityView;
import com.bank.docgen.rendering.persistence.BatchTestRunEntity;
import com.bank.docgen.rendering.persistence.BatchTestRunRepository;
import com.bank.docgen.rendering.persistence.PreviewRecordRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.CoverageThresholdView;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TestDataSetEntity;
import com.bank.docgen.template.persistence.TestDataSetRepository;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.bank.docgen.template.service.CoverageThresholdResolver;
import com.bank.docgen.template.service.TemplateCurrentVersionResolver;
import com.bank.docgen.template.service.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubmitTestEligibilityServiceTest {

    @Mock
    private TemplateService templateService;
    @Mock
    private BatchTestRunRepository batchTestRunRepository;
    @Mock
    private PreviewRecordRepository previewRecordRepository;
    @Mock
    private VariableSchemaRepository variableSchemaRepository;
    @Mock
    private AnchorBindingRepository anchorBindingRepository;
    @Mock
    private TestDataSetRepository testDataSetRepository;
    @Mock
    private CoverageThresholdResolver coverageThresholdResolver;
    @Mock
    private TemplateCurrentVersionResolver templateCurrentVersionResolver;

    private SubmitTestEligibilityService service;
    private UUID templateId;
    private UUID versionId;
    private ManagementSessionClaims session;
    private TemplateEntity template;
    private TemplateVersionEntity version; // NOSONAR - concrete instance for test

    @BeforeEach
    void setUp() {
        service = new SubmitTestEligibilityService(
                templateService,
                batchTestRunRepository,
                previewRecordRepository,
                variableSchemaRepository,
                anchorBindingRepository,
                testDataSetRepository,
                coverageThresholdResolver,
                templateCurrentVersionResolver,
                new ObjectMapper()
        );
        templateId = UUID.randomUUID();
        versionId = UUID.randomUUID();
        session = new ManagementSessionClaims(
                "10000001", "Author", "author@test.com",
                AuthSource.LOCAL, List.of("TEMPLATE_AUTHOR"),
                List.of("RETAIL"), "route.home", List.of("route.home"),
                Instant.now().plusSeconds(3600)
        );
        template = new TemplateEntity(templateId, "TPL-1", "RETAIL", "Demo", null, UUID.randomUUID(), "author");
        version = new TemplateVersionEntity(versionId, templateId, "author");
        when(templateService.requireReadableTemplate(templateId, session)).thenReturn(template);
        when(templateCurrentVersionResolver.requireInFlightDevVersion(templateId)).thenReturn(version);
        when(coverageThresholdResolver.resolveForTemplate(template))
                .thenReturn(new CoverageThresholdView("GLOBAL", null, 80, 100, 80));
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(versionId)).thenReturn(List.of());
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(versionId)).thenReturn(List.of());
        when(testDataSetRepository.findByTemplateIdOrderByUpdatedAtDesc(templateId)).thenReturn(List.of());
    }

    @Test
    void evaluate_noValidRun_notEligible() {
        when(batchTestRunRepository.findLatestValidByTemplateId(templateId)).thenReturn(Optional.empty());

        SubmitTestEligibilityView result = service.evaluate(templateId, session);

        assertThat(result.eligible()).isFalse();
        assertThat(result.conditions().hasValidTestResult()).isFalse();
        assertThat(result.conditions().allSamplesSucceeded()).isFalse();
        assertThat(result.conditions().coverageGatePassed()).isFalse();
        assertThat(result.latestRunAt()).isNull();
    }

    @Test
    void evaluate_validRunWithAllGatesPassed_isEligible() {
        when(batchTestRunRepository.findLatestValidByTemplateId(templateId))
                .thenReturn(Optional.of(successfulRun()));

        SubmitTestEligibilityView result = service.evaluate(templateId, session);

        assertThat(result.eligible()).isTrue();
        assertThat(result.conditions().hasValidTestResult()).isTrue();
        assertThat(result.conditions().allSamplesSucceeded()).isTrue();
        assertThat(result.conditions().coverageGatePassed()).isTrue();
        assertThat(result.latestRunAt()).isNotNull();
    }

    @Test
    void evaluate_runWithFailedSamples_notEligible() {
        BatchTestRunEntity run = runWithFailedSamples();
        when(batchTestRunRepository.findLatestValidByTemplateId(templateId)).thenReturn(Optional.of(run));

        SubmitTestEligibilityView result = service.evaluate(templateId, session);

        assertThat(result.eligible()).isFalse();
        assertThat(result.conditions().allSamplesSucceeded()).isFalse();
    }

    @Test
    void evaluate_runWithCoverageGateFailed_notEligible() {
        BatchTestRunEntity run = runWithGateFailed();
        when(batchTestRunRepository.findLatestValidByTemplateId(templateId)).thenReturn(Optional.of(run));

        SubmitTestEligibilityView result = service.evaluate(templateId, session);

        assertThat(result.eligible()).isFalse();
        assertThat(result.conditions().coverageGatePassed()).isFalse();
    }

    @Test
    void evaluate_thresholds_matchConfig() {
        when(batchTestRunRepository.findLatestValidByTemplateId(templateId)).thenReturn(Optional.empty());

        SubmitTestEligibilityView result = service.evaluate(templateId, session);

        assertThat(result.thresholds().anchorCoveragePct()).isEqualTo(80);
        assertThat(result.thresholds().variableCoveragePct()).isEqualTo(80);
        assertThat(result.thresholds().sampleCoveragePct()).isEqualTo(100);
    }

    private BatchTestRunEntity successfulRun() {
        BatchTestRunEntity run = BatchTestRunEntity.startNew(
                UUID.randomUUID(), templateId, versionId, "author", 3
        );
        run.completeRun(3, 0, 0, 0, "[]",
                BigDecimal.valueOf(90), BigDecimal.valueOf(85), BigDecimal.valueOf(100),
                true, true
        );
        return run;
    }

    private BatchTestRunEntity runWithFailedSamples() {
        BatchTestRunEntity run = BatchTestRunEntity.startNew(
                UUID.randomUUID(), templateId, versionId, "author", 3
        );
        run.completeRun(2, 1, 0, 0, "[{\"dataSetExternalId\":\"DS-FAIL\",\"success\":false}]",
                BigDecimal.valueOf(90), BigDecimal.valueOf(85), BigDecimal.valueOf(66),
                false, false
        );
        return run;
    }

    private BatchTestRunEntity runWithGateFailed() {
        BatchTestRunEntity run = BatchTestRunEntity.startNew(
                UUID.randomUUID(), templateId, versionId, "author", 3
        );
        run.completeRun(3, 0, 0, 0, "[]",
                BigDecimal.valueOf(60), BigDecimal.valueOf(50), BigDecimal.valueOf(100),
                true, false
        );
        return run;
    }
}
