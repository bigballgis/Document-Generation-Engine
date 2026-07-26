package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.bank.docgen.template.api.PublishGateChecklistView;
import com.bank.docgen.template.api.TemplateRuleValidationItemResponse;
import com.bank.docgen.template.api.TemplateRuleValidationSummaryView;
import com.bank.docgen.template.api.TemplateRuleValidationView;
import com.bank.docgen.template.domain.LifecycleAction;
import com.bank.docgen.template.domain.LifecycleDecision;
import com.bank.docgen.template.domain.PublishGateCheckCode;
import com.bank.docgen.template.domain.PublishGatePhase;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
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
    private PreviewEvidencePort previewEvidencePort;
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
    @Mock
    private TemplateCurrentVersionResolver templateCurrentVersionResolver;
    @Mock
    private AnchorBindingRepository anchorBindingRepository;
    @Mock
    private NodeMatrixValidationService nodeMatrixValidationService;

    private PublishGateService service;
    private UUID templateId;
    private UUID versionId;
    private ManagementSessionClaims admin;
    private TemplateEntity template;

    private DocgenRenderingProperties renderingProperties;
    private TemplateVersionEntity inFlightVersion;

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

    @Test
    void publish_withUnresolvedBlocker_isRejected_withChecklist() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(blockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.ready()).isFalse();
        assertThat(checklist.items().stream()
                .anyMatch(item -> item.checkCode() == PublishGateCheckCode.ANCHOR_INTEGRITY && item.blocker()))
                .isTrue();
        assertThatThrownBy(() -> service.assertReady(templateId, admin))
                .isInstanceOf(TemplateValidationException.class)
                .satisfies(ex -> {
                    TemplateValidationException validation = (TemplateValidationException) ex;
                    assertThat(validation.messageKey()).isEqualTo("api.error.template.publishGateBlocked");
                    assertThat(validation.messageArgs()).contains("ANCHOR_INTEGRITY");
                });
    }

    @Test
    void publish_belowCoverageThreshold_isRejected() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
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
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.ready()).isTrue();
        assertThat(checklist.blockerCount()).isZero();
        service.assertReady(templateId, admin);
    }

    @Test
    void bdd003_paginationDeltaOver2xBudget_blocksPublish() {
        inFlightVersion.setAuthorWordPageCount(6);
        when(previewEvidencePort.latestSuccessfulPdfPageCount(templateId, versionId))
                .thenReturn(Optional.of(9));
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.ready()).isFalse();
        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.PAGINATION_DELTA_BUDGET)
                .findFirst())
                .get()
                .satisfies(item -> {
                    assertThat(item.blocker()).isTrue();
                    assertThat(item.ready()).isFalse();
                });
        assertThatThrownBy(() -> service.assertReady(templateId, admin))
                .isInstanceOf(TemplateValidationException.class);
    }

    @Test
    void bdd002_paginationDeltaWarningBand_doesNotBlockPublishGate() {
        inFlightVersion.setAuthorWordPageCount(6);
        when(previewEvidencePort.latestSuccessfulPdfPageCount(templateId, versionId))
                .thenReturn(Optional.of(8));
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.PAGINATION_DELTA_BUDGET)
                .findFirst())
                .get()
                .satisfies(item -> assertThat(item.blocker()).isFalse());
        assertThat(checklist.ready()).isTrue();
    }

    @Test
    void bdd004_missingAuthorWordPageCount_skipsPaginationBudgetBlocker() {
        inFlightVersion.setAuthorWordPageCount(null);
        when(previewEvidencePort.latestSuccessfulPdfPageCount(templateId, versionId))
                .thenReturn(Optional.of(6));
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.PAGINATION_DELTA_BUDGET)
                .findFirst())
                .get()
                .satisfies(item -> assertThat(item.blocker()).isFalse());
        assertThat(checklist.ready()).isTrue();
    }

    @Test
    void publishGate_isEvaluatedLive_notStaticText() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        when(previewEvidencePort.latestBatchTestRun(templateId)).thenReturn(Optional.empty());

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.items().stream()
                .anyMatch(item -> item.checkCode() == PublishGateCheckCode.TEST_RESULTS
                        && item.summary().contains("noBatchRun")
                        && item.blocker()))
                .isTrue();
    }

    @Test
    void submitForApproval_excludesApprovalSummaryAndApiPolicy() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        when(lifecycleRecordRepository.findByTemplateIdOrderByCreatedAtDesc(templateId))
                .thenReturn(List.of());
        when(apiPolicyRepository.findByTemplateId(templateId))
                .thenReturn(Optional.empty());

        PublishGateChecklistView publishChecklist = service.evaluate(templateId, admin);
        PublishGateChecklistView submitChecklist = service.evaluate(
                templateId, admin, PublishGatePhase.SUBMIT_FOR_APPROVAL);

        assertThat(publishChecklist.ready()).isFalse();
        assertThat(submitChecklist.ready()).isTrue();
        assertThat(submitChecklist.items()).noneMatch(item ->
                item.checkCode() == PublishGateCheckCode.APPROVAL_SUMMARY
                        || item.checkCode() == PublishGateCheckCode.API_POLICY);
    }

    @Test
    void submitForApproval_withHardBlocker_rejectedWithSubmitGateMessageKey() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(blockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());

        assertThatThrownBy(() -> service.assertReady(templateId, admin, PublishGatePhase.SUBMIT_FOR_APPROVAL))
                .isInstanceOf(TemplateValidationException.class)
                .hasFieldOrPropertyWithValue("messageKey", "api.error.template.submitForApprovalGateBlocked");
    }

    @Test
    void submitForApproval_allGreen_succeeds() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());

        PublishGateChecklistView checklist = service.evaluate(templateId, admin, PublishGatePhase.SUBMIT_FOR_APPROVAL);

        assertThat(checklist.ready()).isTrue();
        service.assertReadyForSubmitForApproval(templateId, admin);
    }

    @Test
    void publishGate_blocksContentModuleEffectiveExpired_lm008() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        when(contentModuleReferenceService.evaluateEffectiveExpiry(versionId))
                .thenReturn(new com.bank.docgen.template.api.ContentModuleEffectiveExpirySummaryView(
                        true,
                        1,
                        1,
                        List.of("MOD-A@1.0.0 effectiveTo=2026-07-01T00:00:00Z")
                ));

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.ready()).isFalse();
        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.CONTENT_MODULE_EFFECTIVE_EXPIRED)
                .findFirst()
                .orElseThrow())
                .satisfies(item -> {
                    assertThat(item.blocker()).isTrue();
                    assertThat(item.ready()).isFalse();
                    assertThat(item.messageKey()).isEqualTo("api.publishGate.contentModuleEffectiveExpired.blocked");
                    assertThat(item.summary()).contains("MOD-A@1.0.0");
                });
        assertThatThrownBy(() -> service.assertReady(templateId, admin))
                .isInstanceOf(TemplateValidationException.class);
    }

    @Test
    void publishGate_blocksContentModuleEffectiveNotStarted_e5001() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        when(contentModuleReferenceService.evaluateEffectiveNotStarted(versionId))
                .thenReturn(new com.bank.docgen.template.api.ContentModuleEffectiveNotStartedSummaryView(
                        true,
                        1,
                        1,
                        List.of("CLAUSE-A MOD-A@1.0.0 effectiveFrom=2027-01-01T00:00:00Z")
                ));

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.ready()).isFalse();
        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.CONTENT_MODULE_EFFECTIVE_NOT_STARTED)
                .findFirst()
                .orElseThrow())
                .satisfies(item -> {
                    assertThat(item.blocker()).isTrue();
                    assertThat(item.ready()).isFalse();
                    assertThat(item.messageKey())
                            .isEqualTo("api.publishGate.contentModuleEffectiveNotStarted.blocked");
                    assertThat(item.summary()).contains("CLAUSE-A");
                    assertThat(item.summary()).contains("effectiveFrom=");
                    assertThat(item.checkCode())
                            .isNotEqualTo(PublishGateCheckCode.CONTENT_MODULE_EFFECTIVE_EXPIRED);
                });
        assertThatThrownBy(() -> service.assertReady(templateId, admin))
                .isInstanceOf(TemplateValidationException.class);
    }

    @Test
    void publishGate_contentModuleReferencesPass_whenOnlyEffectiveExpired_lm013() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        when(contentModuleReferenceService.validateReferences(versionId))
                .thenReturn(new com.bank.docgen.template.api.ContentModuleReferenceValidationSummaryView(false, 1, 0));
        when(contentModuleReferenceService.evaluateEffectiveExpiry(versionId))
                .thenReturn(new com.bank.docgen.template.api.ContentModuleEffectiveExpirySummaryView(
                        true, 1, 1, List.of("MOD-A@1.0.0 effectiveTo=2026-07-01T00:00:00Z")));

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.CONTENT_MODULE_REFERENCES)
                .findFirst()
                .orElseThrow()
                .ready()).isTrue();
        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.CONTENT_MODULE_EFFECTIVE_EXPIRED)
                .findFirst()
                .orElseThrow()
                .blocker()).isTrue();
    }

    @Test
    void publishGate_blocksContentModuleReferencesWithEmptyPinnedStructure() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        when(contentModuleReferenceService.validateReferences(versionId))
                .thenReturn(new com.bank.docgen.template.api.ContentModuleReferenceValidationSummaryView(true, 1, 1));

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.ready()).isFalse();
        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.CONTENT_MODULE_REFERENCES)
                .findFirst()
                .orElseThrow())
                .satisfies(item -> {
                    assertThat(item.blocker()).isTrue();
                    assertThat(item.ready()).isFalse();
                    assertThat(item.messageKey()).isEqualTo("api.publishGate.contentModuleReferences.blocked");
                });
        assertThatThrownBy(() -> service.assertReady(templateId, admin))
                .isInstanceOf(TemplateValidationException.class);
    }

    @Test
    void publishGate_blocksInvalidContentModuleReferences() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
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

    @Test
    void publishGate_allowsQrBarcodeRef_alone() {
        // BDD-CE-K06b-006 — qrBarcodeRef no longer hard-blocks publish gate
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        String qrJson = "{\"nodes\":[{\"type\":\"qrBarcodeRef\",\"referenceKey\":\"PAYMENT-QR\"}]}";
        AnchorBindingEntity binding = new AnchorBindingEntity(
                UUID.randomUUID(),
                versionId,
                "BODY",
                com.bank.docgen.template.domain.AnchorContentType.RICH_TEXT,
                qrJson,
                com.bank.docgen.template.domain.BindingValidationStatus.VALID
        );
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(versionId))
                .thenReturn(List.of(binding));
        when(nodeMatrixValidationService.countUnsupportedNodeBlockers(qrJson)).thenReturn(0);

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.UNSUPPORTED_STRUCTURED_NODES)
                .findFirst()
                .orElseThrow())
                .satisfies(item -> {
                    assertThat(item.blocker()).isFalse();
                    assertThat(item.ready()).isTrue();
                });
    }

    @Test
    void publishGate_allowsAttachmentListRef_alone() {
        // BDD-CE-K06c-003 — attachmentListRef no longer hard-blocks publish gate
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        String attachmentJson = "{\"nodes\":[{\"type\":\"attachmentListRef\",\"referenceKey\":\"ATTACHMENTS\"}]}";
        AnchorBindingEntity binding = new AnchorBindingEntity(
                UUID.randomUUID(),
                versionId,
                "BODY",
                com.bank.docgen.template.domain.AnchorContentType.RICH_TEXT,
                attachmentJson,
                com.bank.docgen.template.domain.BindingValidationStatus.VALID
        );
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(versionId))
                .thenReturn(List.of(binding));
        when(nodeMatrixValidationService.countUnsupportedNodeBlockers(attachmentJson)).thenReturn(0);

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.UNSUPPORTED_STRUCTURED_NODES)
                .findFirst()
                .orElseThrow())
                .satisfies(item -> {
                    assertThat(item.blocker()).isFalse();
                    assertThat(item.ready()).isTrue();
                });
    }

    @Test
    void publishGate_blocksUnresolvedPasteCleaningResidue() {
        // BDD-OPS-PASTE-BINDING-001 / S4
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        AnchorBindingEntity binding = new AnchorBindingEntity(
                UUID.randomUUID(),
                versionId,
                "BODY",
                com.bank.docgen.template.domain.AnchorContentType.RICH_TEXT,
                "{\"schemaVersion\":\"1.0\",\"nodes\":[]}",
                com.bank.docgen.template.domain.BindingValidationStatus.INCOMPATIBLE_CONTENT_TYPE
        );
        binding.setPasteCleaningEvidenceJson("""
                {"transformedCount":0,"removedCount":0,"warningCount":0,"blockedCount":1,"unresolvedPasteBlockers":true,"items":[{"category":"BLOCKED","messageKey":"paste.summary.blocked","detectionSummary":"Blocked embedded object in pasted HTML."}]}
                """);
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(versionId))
                .thenReturn(List.of(binding));

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.ready()).isFalse();
        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.PASTE_CLEANING_BLOCKERS)
                .findFirst()
                .orElseThrow())
                .satisfies(item -> {
                    assertThat(item.blocker()).isTrue();
                    assertThat(item.ready()).isFalse();
                    assertThat(item.messageKey()).isEqualTo("api.publishGate.pasteCleaningBlockers.blocked");
                    assertThat(item.summary()).contains("unresolvedPasteBindings=1");
                });
        assertThatThrownBy(() -> service.assertReady(templateId, admin))
                .isInstanceOf(TemplateValidationException.class);
    }

    @Test
    void publishGate_pasteCleaningReadyWhenResidueCleared() {
        // BDD-OPS-PASTE-BINDING-001 / S5 (publish dimension)
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        AnchorBindingEntity binding = new AnchorBindingEntity(
                UUID.randomUUID(),
                versionId,
                "BODY",
                com.bank.docgen.template.domain.AnchorContentType.RICH_TEXT,
                "{\"schemaVersion\":\"1.0\",\"nodes\":[]}",
                com.bank.docgen.template.domain.BindingValidationStatus.VALID
        );
        binding.setPasteCleaningEvidenceJson("""
                {"transformedCount":1,"removedCount":0,"warningCount":0,"blockedCount":0,"unresolvedPasteBlockers":false,"items":[]}
                """);
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(versionId))
                .thenReturn(List.of(binding));

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.PASTE_CLEANING_BLOCKERS)
                .findFirst()
                .orElseThrow())
                .satisfies(item -> {
                    assertThat(item.blocker()).isFalse();
                    assertThat(item.ready()).isTrue();
                    assertThat(item.messageKey()).isEqualTo("api.publishGate.pasteCleaningBlockers.ready");
                });
    }

    @Test
    void publishGate_unsupportedStructuredNodes_readyWhenAbsent() {
        // A8 — happy path: no writer-unsupported nodes → dedicated check ready
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.UNSUPPORTED_STRUCTURED_NODES)
                .findFirst()
                .orElseThrow())
                .satisfies(item -> {
                    assertThat(item.blocker()).isFalse();
                    assertThat(item.ready()).isTrue();
                    assertThat(item.messageKey()).isEqualTo("api.publishGate.unsupportedStructuredNodes.ready");
                });
    }

    @Test
    void publish_withSkeletonPolicyAndEmptyAdGroups_isBlocked_fosW7_2() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        ApiPolicyEntity skeleton = ApiPolicyEntity.createSkeleton(templateId, "10000002");
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(skeleton));

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.API_POLICY)
                .findFirst()
                .orElseThrow())
                .satisfies(item -> {
                    assertThat(item.blocker()).isTrue();
                    assertThat(item.ready()).isFalse();
                    assertThat(item.messageKey()).isEqualTo("api.publishGate.apiPolicy.blocked");
                    assertThat(item.summary()).contains("adGroupsConfigured=false");
                    assertThat(item.summary()).contains("defaultRouteConfigured=false");
                });
    }

    @Test
    void publish_withSkeletonPolicyAndDefaultRoute_isReady_fosW7_2() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        ApiPolicyEntity skeleton = ApiPolicyEntity.createSkeleton(templateId, "10000002");
        skeleton.updateDefaultRouteDomain("1.0.0", "10000002");
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(skeleton));

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.API_POLICY)
                .findFirst()
                .orElseThrow())
                .satisfies(item -> {
                    assertThat(item.blocker()).isFalse();
                    assertThat(item.ready()).isTrue();
                    assertThat(item.summary()).contains("defaultRouteConfigured=true");
                });
    }

    @Test
    void publish_withMalformedRuleExpression_isBlocked_ruleBounds() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        when(templateRuleValidationService.validateRules(
                org.mockito.ArgumentMatchers.eq(templateId),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(admin)))
                .thenReturn(malformedRules());

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.ready()).isFalse();
        assertThat(checklist.items().stream()
                .anyMatch(item -> item.checkCode() == PublishGateCheckCode.RULE_BOUNDS && item.blocker()))
                .isTrue();
    }

    @Test
    void publish_withoutApiPolicySkeleton_isBlocked() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.empty());

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.items().stream()
                .anyMatch(item -> item.checkCode() == PublishGateCheckCode.API_POLICY && item.blocker()))
                .isTrue();
    }

    @Test
    void evaluateForRelease_usesPublishedVersion_withoutInFlightDev() {
        String releaseVersion = "1.0.0";
        UUID publishedVersionId = UUID.randomUUID();
        TemplateVersionEntity published = new TemplateVersionEntity(publishedVersionId, templateId, "10000002");
        published.setReleaseVersion(releaseVersion);
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(templateId, releaseVersion))
                .thenReturn(Optional.of(published));
        when(templateService.validateBindingsForVersion(templateId, published, admin))
                .thenReturn(nonBlockingBindings());
        when(coverageComputationService.computeForVersion(templateId, published, admin))
                .thenReturn(greenCoverage());
        when(changeDiffService.computeForVersion(templateId, published, admin))
                .thenReturn(new com.bank.docgen.template.api.ChangeDiffView(
                        templateId.toString(), null, publishedVersionId.toString(), null, false, 0, List.of(), List.of()));
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(publishedVersionId))
                .thenReturn(List.of(new VariableSchemaEntity(
                        UUID.randomUUID(), publishedVersionId, "field", VariableType.TEXT, true, null, null, "desc", null)));
        when(previewEvidencePort.countSuccessfulPreviews(templateId, publishedVersionId)).thenReturn(1);
        when(previewEvidencePort.countFailedPreviews(templateId, publishedVersionId)).thenReturn(0);
        when(contentModuleReferenceService.validateReferences(publishedVersionId))
                .thenReturn(new com.bank.docgen.template.api.ContentModuleReferenceValidationSummaryView(false, 0, 0));
        when(contentModuleReferenceService.evaluateEffectiveExpiry(publishedVersionId))
                .thenReturn(new com.bank.docgen.template.api.ContentModuleEffectiveExpirySummaryView(
                        false, 0, 0, List.of()));
        when(contentModuleReferenceService.evaluateEffectiveNotStarted(publishedVersionId))
                .thenReturn(new com.bank.docgen.template.api.ContentModuleEffectiveNotStartedSummaryView(
                        false, 0, 0, List.of()));
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(publishedVersionId))
                .thenReturn(List.of());
        when(templateRuleValidationService.validateRulesForVersion(
                org.mockito.ArgumentMatchers.eq(templateId),
                org.mockito.ArgumentMatchers.eq(published),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(admin)))
                .thenReturn(validRules());

        PublishGateChecklistView checklist = service.evaluateForRelease(templateId, releaseVersion, admin);

        assertThat(checklist.templateId()).isEqualTo(templateId.toString());
        assertThat(checklist.ready()).isTrue();
        assertThat(checklist.items()).isNotEmpty();
        assertThat(checklist.items().stream().map(item -> item.checkCode()).toList())
                .contains(
                        PublishGateCheckCode.ANCHOR_INTEGRITY,
                        PublishGateCheckCode.VARIABLE_SCHEMA,
                        PublishGateCheckCode.APPROVAL_SUMMARY,
                        PublishGateCheckCode.API_POLICY,
                        PublishGateCheckCode.PAGINATION_DELTA_BUDGET
                );
    }

    @Test
    void evaluateForRelease_unknownRelease_throwsTemplateNotFound() {
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(templateId, "9.9.9"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.evaluateForRelease(templateId, "9.9.9", admin))
                .isInstanceOf(TemplateNotFoundException.class);
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

    private TemplateRuleValidationView malformedRules() {
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

    @Test
    void publishGate_blocksWhenLatestPreviewHasUnviewedFidelityWarnings() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        when(previewEvidencePort.countUnviewedFidelityWarnings(templateId, versionId)).thenReturn(2);

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.ready()).isFalse();
        assertThat(checklist.items().stream()
                .anyMatch(item -> item.checkCode() == PublishGateCheckCode.FIDELITY_WARNINGS_VIEWED
                        && item.blocker()))
                .isTrue();
    }

    @Test
    void publishGate_readyWhenAllFidelityWarningsViewed() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        when(previewEvidencePort.countUnviewedFidelityWarnings(templateId, versionId)).thenReturn(0);

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.FIDELITY_WARNINGS_VIEWED)
                .findFirst()
                .orElseThrow()
                .ready())
                .isTrue();
    }
}
