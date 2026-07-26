package com.bank.docgen.template.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authoring.structured.NodeMatrixValidationService;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.template.port.BatchTestRunGateSnapshot;
import com.bank.docgen.template.port.PreviewEvidencePort;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.BindingValidationSummaryView;
import com.bank.docgen.template.api.BindingValidationView;
import com.bank.docgen.template.api.CoverageDimensionView;
import com.bank.docgen.template.api.CoverageSummaryView;
import com.bank.docgen.template.api.CoverageThresholdView;
import com.bank.docgen.template.api.TemplateRuleValidationItemResponse;
import com.bank.docgen.template.api.TemplateRuleValidationSummaryView;
import com.bank.docgen.template.api.TemplateRuleValidationView;
import com.bank.docgen.template.domain.LifecycleAction;
import com.bank.docgen.template.domain.LifecycleDecision;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Shared Mockito fixtures for PublishGateService* tests (AI-SCALE #169).
 */
@ExtendWith(MockitoExtension.class)
abstract class PublishGateServiceTestFixtures {

    @Mock
    protected TemplateService templateService;
    @Mock
    protected TemplateVersionRepository templateVersionRepository;
    @Mock
    protected TemplateLifecycleRecordRepository lifecycleRecordRepository;
    @Mock
    protected ApiPolicyRepository apiPolicyRepository;
    @Mock
    protected PreviewEvidencePort previewEvidencePort;
    @Mock
    protected CoverageComputationService coverageComputationService;
    @Mock
    protected ChangeDiffService changeDiffService;
    @Mock
    protected TemplateRuleValidationService templateRuleValidationService;
    @Mock
    protected VariableSchemaRepository variableSchemaRepository;
    @Mock
    protected TemplateContentModuleReferenceService contentModuleReferenceService;
    @Mock
    protected TemplateCurrentVersionResolver templateCurrentVersionResolver;
    @Mock
    protected AnchorBindingRepository anchorBindingRepository;
    @Mock
    protected NodeMatrixValidationService nodeMatrixValidationService;
    protected PublishGateService service;
    protected UUID templateId;
    protected UUID versionId;
    protected ManagementSessionClaims admin;
    protected TemplateEntity template;
    protected DocgenRenderingProperties renderingProperties;
    protected TemplateVersionEntity inFlightVersion;
    @BeforeEach
    void setUp() {
        renderingProperties = new DocgenRenderingProperties();
        renderingProperties.setPaginationDeltaBudgetPages(1);
        service = new PublishGateService(
                templateService,
                templateVersionRepository,
                lifecycleRecordRepository,
                apiPolicyRepository,
                previewEvidencePort,
                coverageComputationService,
                changeDiffService,
                templateRuleValidationService,
                variableSchemaRepository,
                contentModuleReferenceService,
                templateCurrentVersionResolver,
                anchorBindingRepository,
                nodeMatrixValidationService,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                renderingProperties
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
        inFlightVersion = new TemplateVersionEntity(versionId, templateId, "10000002");
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
        lenient().when(templateCurrentVersionResolver.requireInFlightDevVersion(templateId))
                .thenReturn(inFlightVersion);
        lenient().when(templateService.loadRules(any(TemplateVersionEntity.class))).thenReturn(List.of());
        lenient().when(templateRuleValidationService.validateRules(
                org.mockito.ArgumentMatchers.eq(templateId),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(admin)))
                .thenReturn(validRules());
        lenient().when(changeDiffService.compute(templateId, admin))
                .thenReturn(new com.bank.docgen.template.api.ChangeDiffView(
                        templateId.toString(), "1.0.0", versionId.toString(), null, false, 0, List.of(), List.of()));
        lenient().when(lifecycleRecordRepository.findByTemplateIdOrderByCreatedAtDesc(templateId))
                .thenReturn(List.of(approvalRecord()));
        ApiPolicyEntity callablePolicy = new ApiPolicyEntity(UUID.randomUUID(), templateId, "[]", "10000002");
        callablePolicy.updateDefaultRouteDomain("1.0.0", "10000002");
        lenient().when(apiPolicyRepository.findByTemplateId(templateId))
                .thenReturn(Optional.of(callablePolicy));
        lenient().when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(versionId))
                .thenReturn(List.of(new VariableSchemaEntity(
                        UUID.randomUUID(), versionId, "field", VariableType.TEXT, true, null, null, "desc", null)));
        lenient().when(previewEvidencePort.latestBatchTestRun(templateId))
                .thenReturn(Optional.of(new BatchTestRunGateSnapshot(0)));
        lenient().when(previewEvidencePort.countSuccessfulPreviews(templateId, versionId)).thenReturn(1);
        lenient().when(previewEvidencePort.countFailedPreviews(templateId, versionId)).thenReturn(0);
        lenient().when(previewEvidencePort.countUnviewedFidelityWarnings(templateId, versionId)).thenReturn(0);
        lenient().when(previewEvidencePort.latestSuccessfulPdfPageCount(any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(contentModuleReferenceService.validateReferences(versionId))
                .thenReturn(new com.bank.docgen.template.api.ContentModuleReferenceValidationSummaryView(false, 0, 0));
        lenient().when(contentModuleReferenceService.evaluateEffectiveExpiry(versionId))
                .thenReturn(new com.bank.docgen.template.api.ContentModuleEffectiveExpirySummaryView(
                        false, 0, 0, List.of()));
        lenient().when(contentModuleReferenceService.evaluateEffectiveNotStarted(versionId))
                .thenReturn(new com.bank.docgen.template.api.ContentModuleEffectiveNotStartedSummaryView(
                        false, 0, 0, List.of()));
        lenient().when(contentModuleReferenceService.evaluateLocaleMismatch(any()))
                .thenReturn(new com.bank.docgen.template.api.ContentModuleLocaleMismatchSummaryView(
                        false, 0, 0, List.of()));
        lenient().when(contentModuleReferenceService.evaluateNestingClosure(any()))
                .thenReturn(com.bank.docgen.contentmodule.api.ContentModuleNestingPublishSummaryView.clear());
        lenient().when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(versionId))
                .thenReturn(List.of());
        lenient().when(nodeMatrixValidationService.countUnsupportedNodeBlockers(any()))
                .thenReturn(0);
    }
    protected BindingValidationView nonBlockingBindings() {
        return new BindingValidationView(
                List.of(),
                new BindingValidationSummaryView(false, 1, 1, 0, 0, 0)
        );
    }
    protected BindingValidationView blockingBindings() {
        return new BindingValidationView(
                List.of(),
                new BindingValidationSummaryView(true, 1, 0, 1, 0, 0)
        );
    }
    protected CoverageSummaryView greenCoverage() {
        CoverageDimensionView dimension = new CoverageDimensionView(
                CoverageComputationService.DIMENSION_REQUIRED_VARIABLES, 1, 1, 100, 80, false);
        return new CoverageSummaryView(
                templateId.toString(), 100, false, List.of(), List.of(dimension),
                new CoverageThresholdView("GLOBAL", null, 80, 100, 80));
    }
    protected CoverageSummaryView blockedCoverage() {
        CoverageDimensionView dimension = new CoverageDimensionView(
                CoverageComputationService.DIMENSION_REQUIRED_VARIABLES, 2, 1, 50, 80, true);
        return new CoverageSummaryView(
                templateId.toString(), 50, true,
                List.of(CoverageComputationService.BLOCKER_REQUIRED_VARIABLES),
                List.of(dimension),
                new CoverageThresholdView("GLOBAL", null, 80, 100, 80));
    }
    protected TemplateRuleValidationView malformedRules() {
        return new TemplateRuleValidationView(
                false,
                List.of(new TemplateRuleValidationItemResponse(
                        "rule-1",
                        "${customerName} === null",
                        "ANCHOR-1",
                        null,
                        null,
                        com.bank.docgen.template.domain.RuleValidationStatus.MALFORMED_RULE
                )),
                new TemplateRuleValidationSummaryView(true, 1, 0, 0, 0, 0, 1)
        );
    }
    protected TemplateRuleValidationView validRules() {
        return new TemplateRuleValidationView(
                true,
                List.of(),
                new TemplateRuleValidationSummaryView(false, 0, 0, 0, 0, 0, 0)
        );
    }
    protected TemplateLifecycleRecordEntity approvalRecord() {
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
}
