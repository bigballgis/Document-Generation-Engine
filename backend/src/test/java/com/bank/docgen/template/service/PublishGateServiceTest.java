package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.rendering.domain.PreviewStatus;
import com.bank.docgen.rendering.persistence.BatchTestRunEntity;
import com.bank.docgen.rendering.persistence.BatchTestRunRepository;
import com.bank.docgen.rendering.persistence.PreviewRecordEntity;
import com.bank.docgen.rendering.persistence.PreviewRecordRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.BindingValidationSummaryView;
import com.bank.docgen.template.api.BindingValidationView;
import com.bank.docgen.template.api.CoverageDimensionView;
import com.bank.docgen.template.api.CoverageSummaryView;
import com.bank.docgen.template.api.CoverageThresholdView;
import com.bank.docgen.template.api.PublishGateChecklistView;
import com.bank.docgen.template.api.TemplateRuleValidationItemResponse;
import com.bank.docgen.template.api.TemplateRuleValidationSummaryView;
import com.bank.docgen.template.api.TemplateRuleValidationView;
import com.bank.docgen.template.domain.LifecycleAction;
import com.bank.docgen.template.domain.LifecycleDecision;
import com.bank.docgen.template.domain.PublishGateCheckCode;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.bank.docgen.template.domain.VariableType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublishGateServiceTest {

    @Mock
    private TemplateService templateService;
    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private TemplateLifecycleRecordRepository lifecycleRecordRepository;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private PreviewRecordRepository previewRecordRepository;
    @Mock
    private BatchTestRunRepository batchTestRunRepository;
    @Mock
    private CoverageComputationService coverageComputationService;
    @Mock
    private ChangeDiffService changeDiffService;
    @Mock
    private TemplateRuleValidationService templateRuleValidationService;
    @Mock
    private VariableSchemaRepository variableSchemaRepository;
    @Mock
    private TemplateContentModuleReferenceService contentModuleReferenceService;

    private PublishGateService service;
    private UUID templateId;
    private UUID versionId;
    private ManagementSessionClaims admin;
    private TemplateEntity template;

    @BeforeEach
    void setUp() {
        service = new PublishGateService(
                templateService,
                templateVersionRepository,
                lifecycleRecordRepository,
                apiPolicyRepository,
                previewRecordRepository,
                batchTestRunRepository,
                coverageComputationService,
                changeDiffService,
                templateRuleValidationService,
                variableSchemaRepository,
                contentModuleReferenceService
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
                "10000002"
        );
        admin = new ManagementSessionClaims(
                "10000002",
                "Admin",
                "admin@example.com",
                AuthSource.LOCAL,
                List.of("GROUP_ADMIN"),
                List.of("RETAIL"),
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                Instant.now().plusSeconds(3600)
        );
        when(templateService.requireReadableTemplate(templateId, admin)).thenReturn(template);
        when(templateVersionRepository.findByTemplateIdAndDevVersionNumber(templateId, 1))
                .thenReturn(Optional.of(new TemplateVersionEntity(versionId, templateId, "10000002")));
        when(templateService.loadRules(any(TemplateVersionEntity.class))).thenReturn(List.of());
        when(templateRuleValidationService.validateRules(
                org.mockito.ArgumentMatchers.eq(templateId),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(admin)))
                .thenReturn(validRules());
        when(changeDiffService.compute(templateId, admin))
                .thenReturn(new com.bank.docgen.template.api.ChangeDiffView(
                        templateId.toString(), "1.0.0", versionId.toString(), false, 0, List.of()));
        when(lifecycleRecordRepository.findByTemplateIdOrderByCreatedAtDesc(templateId))
                .thenReturn(List.of(approvalRecord()));
        when(apiPolicyRepository.findByTemplateId(templateId))
                .thenReturn(Optional.of(new ApiPolicyEntity(UUID.randomUUID(), templateId, "[]", "10000002")));
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(versionId))
                .thenReturn(List.of(new VariableSchemaEntity(
                        UUID.randomUUID(), versionId, "field", VariableType.TEXT, true, null, null, "desc")));
        when(batchTestRunRepository.findByTemplateIdOrderByCreatedAtDesc(templateId))
                .thenReturn(List.of(new BatchTestRunEntity(
                        UUID.randomUUID(), templateId, "10000002", 1, 1, 0, 0, 0, "[]")));
        when(previewRecordRepository.findByTemplateIdAndTemplateVersionIdAndStatus(
                templateId, versionId, PreviewStatus.SUCCEEDED))
                .thenReturn(List.of(successfulPreview()));
        when(previewRecordRepository.findByTemplateIdAndTemplateVersionIdAndStatus(
                templateId, versionId, PreviewStatus.FAILED))
                .thenReturn(List.of());
        when(contentModuleReferenceService.validateReferences(versionId))
                .thenReturn(new com.bank.docgen.template.api.ContentModuleReferenceValidationSummaryView(false, 0, 0));
    }

    @Test
    void publish_withUnresolvedBlocker_isRejected_withChecklist() {
        when(templateService.validateBindings(templateId, admin)).thenReturn(blockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.ready()).isFalse();
        assertThat(checklist.items().stream()
                .anyMatch(item -> item.checkCode() == PublishGateCheckCode.ANCHOR_INTEGRITY && item.blocker()))
                .isTrue();
        assertThatThrownBy(() -> service.assertReady(templateId, admin))
                .isInstanceOf(TemplateValidationException.class);
    }

    @Test
    void publish_belowCoverageThreshold_isRejected() {
        when(templateService.validateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(blockedCoverage());

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.ready()).isFalse();
        assertThat(checklist.items().stream()
                .anyMatch(item -> item.checkCode() == PublishGateCheckCode.COVERAGE_THRESHOLDS && item.blocker()))
                .isTrue();
        assertThatThrownBy(() -> service.assertReady(templateId, admin))
                .isInstanceOf(TemplateValidationException.class);
    }

    @Test
    void publish_allGreen_succeeds_andRecordsChecklist() {
        when(templateService.validateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.ready()).isTrue();
        assertThat(checklist.blockerCount()).isZero();
        service.assertReady(templateId, admin);
    }

    @Test
    void publishGate_isEvaluatedLive_notStaticText() {
        when(templateService.validateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        when(batchTestRunRepository.findByTemplateIdOrderByCreatedAtDesc(templateId))
                .thenReturn(List.of());

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.items().stream()
                .anyMatch(item -> item.checkCode() == PublishGateCheckCode.TEST_RESULTS
                        && item.summary().contains("noBatchRun")
                        && item.blocker()))
                .isTrue();
    }

    @Test
    void publishGate_blocksInvalidContentModuleReferences() {
        when(templateService.validateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        when(contentModuleReferenceService.validateReferences(versionId))
                .thenReturn(new com.bank.docgen.template.api.ContentModuleReferenceValidationSummaryView(true, 1, 1));

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.ready()).isFalse();
        assertThat(checklist.items().stream()
                .anyMatch(item -> item.checkCode() == PublishGateCheckCode.CONTENT_MODULE_REFERENCES
                        && item.blocker()))
                .isTrue();
    }

    private BindingValidationView nonBlockingBindings() {
        return new BindingValidationView(
                List.of(),
                new BindingValidationSummaryView(false, 1, 1, 0, 0, 0)
        );
    }

    private BindingValidationView blockingBindings() {
        return new BindingValidationView(
                List.of(),
                new BindingValidationSummaryView(true, 1, 0, 1, 0, 0)
        );
    }

    private CoverageSummaryView greenCoverage() {
        CoverageDimensionView dimension = new CoverageDimensionView(
                CoverageComputationService.DIMENSION_REQUIRED_VARIABLES, 1, 1, 100, 80, false);
        return new CoverageSummaryView(
                templateId.toString(), 100, false, List.of(), List.of(dimension),
                new CoverageThresholdView("GLOBAL", null, 80, 100, 80));
    }

    private CoverageSummaryView blockedCoverage() {
        CoverageDimensionView dimension = new CoverageDimensionView(
                CoverageComputationService.DIMENSION_REQUIRED_VARIABLES, 2, 1, 50, 80, true);
        return new CoverageSummaryView(
                templateId.toString(), 50, true,
                List.of(CoverageComputationService.BLOCKER_REQUIRED_VARIABLES),
                List.of(dimension),
                new CoverageThresholdView("GLOBAL", null, 80, 100, 80));
    }

    private TemplateRuleValidationView validRules() {
        return new TemplateRuleValidationView(
                true,
                List.of(),
                new TemplateRuleValidationSummaryView(false, 0, 0, 0, 0, 0, 0)
        );
    }

    private TemplateLifecycleRecordEntity approvalRecord() {
        return new TemplateLifecycleRecordEntity(
                UUID.randomUUID(),
                templateId,
                LifecycleAction.RECORD_APPROVAL_DECISION,
                null,
                null,
                LifecycleDecision.APPROVED,
                "Approved",
                null,
                "10000007"
        );
    }

    private PreviewRecordEntity successfulPreview() {
        PreviewRecordEntity preview = new PreviewRecordEntity(
                UUID.randomUUID(),
                templateId,
                versionId,
                "DOCX",
                "hash",
                "10000002",
                "TDS-1",
                null
        );
        preview.markSucceeded("previews/test/output.docx", "[]");
        return preview;
    }
}
