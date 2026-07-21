package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.api.ContentModuleDetailView;
import com.bank.docgen.contentmodule.api.CreateContentModuleRequest;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.contentmodule.service.ContentModuleService;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.ContentModuleReferenceView;
import com.bank.docgen.template.api.ImportTemplateRequest;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.api.TemplateExportClauseSnapshotView;
import com.bank.docgen.template.api.TemplateExportMasterPinView;
import com.bank.docgen.template.api.TemplateExportMetadataView;
import com.bank.docgen.template.api.TemplateImportDependencyItemView;
import com.bank.docgen.template.api.TemplateImportDependencyReportView;
import com.bank.docgen.template.api.TemplateImportDryRunResult;
import com.bank.docgen.template.api.TemplateImportResult;
import com.bank.docgen.template.api.UpsertContentModuleReferenceRequest;
import com.bank.docgen.template.domain.TemplateImportDependencySeverity;
import com.bank.docgen.template.domain.TemplateImportDependencyType;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemplateImportServiceV2Test {

    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private MasterDocumentRepository masterDocumentRepository;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private TemplateService templateService;
    @Mock
    private TemplateContentModuleReferenceService contentModuleReferenceService;
    @Mock
    private ManagementAuditRecorder managementAuditRecorder;
    @Mock
    private TemplateCurrentVersionResolver templateCurrentVersionResolver;
    @Mock
    private TemplateImportDependencyPrecheck dependencyPrecheck;
    @Mock
    private ContentModuleService contentModuleService;
    @Mock
    private ContentModuleRepository contentModuleRepository;
    @Mock
    private ContentModuleVersionRepository contentModuleVersionRepository;

    private TemplateImportService service;
    private UUID templateId;
    private UUID masterId;
    private ManagementSessionClaims groupAdmin;
    private byte[] masterBytes;
    private String masterHash;

    @BeforeEach
    void setUp() {
        service = new TemplateImportService(
                templateRepository,
                templateVersionRepository,
                masterDocumentRepository,
                apiPolicyRepository,
                templateService,
                contentModuleReferenceService,
                org.mockito.Mockito.mock(com.bank.docgen.template.service.CompositionInclusionRuleService.class),
                managementAuditRecorder,
                new TemplateExportAccessService(new GroupAccessService()),
                new TemplateImportBundleValidator(new ObjectMapper().findAndRegisterModules()),
                new ObjectMapper().findAndRegisterModules(),
                templateCurrentVersionResolver,
                dependencyPrecheck,
                contentModuleService,
                contentModuleRepository,
                contentModuleVersionRepository,
                org.mockito.Mockito.mock(com.bank.docgen.library.service.AssetLibraryService.class),
                org.mockito.Mockito.mock(com.bank.docgen.master.service.MasterDocumentService.class)
        );
        templateId = UUID.randomUUID();
        masterId = UUID.randomUUID();
        masterBytes = "MASTER-BYTES".getBytes();
        masterHash = TemplateExportHashSupport.sha256Hex(masterBytes);
        groupAdmin = new ManagementSessionClaims(
                "10000002",
                "10000002",
                "admin@example.com",
                AuthSource.LOCAL,
                List.of("GROUP_ADMIN"),
                List.of("RETAIL"),
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                Instant.now().plusSeconds(3600)
        );
    }

    @Test
    void dryRun_noDbMutation_andAudits() {
        TemplateExportBundleView bundle = v2Bundle(List.of(), List.of(clauseSnapshot(
                "MOD-1", UUID.randomUUID(), "1.0.0", false)));
        TemplateImportDependencyReportView report = greenReport();
        when(dependencyPrecheck.evaluate(any())).thenReturn(report);

        TemplateImportDryRunResult result = service.dryRun(
                new ImportTemplateRequest(masterId.toString(), bundle, null, true),
                groupAdmin,
                masterBytes,
                true
        );

        assertThat(result.imported()).isFalse();
        assertThat(result.dependencyReport().readyToCommit()).isTrue();
        verify(templateRepository, never()).save(any());
        verify(contentModuleService, never()).create(any(), any());
        verify(managementAuditRecorder).recordTemplateImportDryRun(
                eq("RETAIL"),
                eq("TPL-V2-IMPORT"),
                eq(true),
                eq(0),
                eq(TemplateExportV2Support.EXPORT_FORMAT_V2),
                eq("10000002"),
                any()
        );
    }

    @Test
    void commit_blocking_returns422_noPartial() {
        TemplateExportBundleView bundle = v2Bundle(List.of(), List.of(clauseSnapshot(
                "MOD-1", UUID.randomUUID(), "1.0.0", false)));
        TemplateImportDependencyReportView report = new TemplateImportDependencyReportView(
                List.of(new TemplateImportDependencyItemView(
                        TemplateImportDependencyType.MASTER_PIN,
                        TemplateImportDependencySeverity.MISMATCH,
                        "MASTER_FINGERPRINT_MISMATCH",
                        "api.error.template.dep.masterFingerprintMismatch",
                        null
                )),
                1,
                0,
                0,
                false,
                TemplateExportV2Support.EXPORT_FORMAT_V2
        );
        when(dependencyPrecheck.evaluate(any())).thenReturn(report);

        assertThatThrownBy(() -> service.importBundle(
                new ImportTemplateRequest(masterId.toString(), bundle, null, false),
                groupAdmin,
                masterBytes,
                true
        )).isInstanceOf(TemplateImportDependenciesException.class)
                .satisfies(ex -> assertThat(((TemplateImportDependenciesException) ex).dependencyReport().readyToCommit())
                        .isFalse());

        verify(templateRepository, never()).save(any());
        verify(contentModuleService, never()).create(any(), any());
        verify(managementAuditRecorder, never()).recordTemplateImported(
                any(), anyString(), anyString(), anyString(), anyInt(), anyString(), any(), any(), any()
        );
    }

    /**
     * BDD-CE-E01-013 / Critical #2: commit materializes clauses with full semver,
     * remaps contentModuleReferences to target module IDs, and wires via import-time
     * DRAFT-referencable seam.
     */
    @Test
    void commit_materializesClauses_andWiresRemappedRefs() {
        UUID sourceModuleId = UUID.randomUUID();
        UUID targetModuleId = UUID.randomUUID();
        TemplateExportBundleView bundle = v2Bundle(
                List.of(new ContentModuleReferenceView(
                        "clause-a",
                        sourceModuleId.toString(),
                        "1.2.3",
                        false,
                        false,
                        null
                )),
                List.of(clauseSnapshot("MOD-CROSS", sourceModuleId, "1.2.3", false))
        );
        when(dependencyPrecheck.evaluate(any())).thenReturn(greenReport());
        stubFreshImport();
        when(contentModuleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-CROSS"))
                .thenReturn(Optional.empty());
        when(contentModuleService.create(any(CreateContentModuleRequest.class), eq(groupAdmin)))
                .thenReturn(new ContentModuleDetailView(
                        targetModuleId.toString(),
                        "MOD-CROSS",
                        "RETAIL",
                        "MOD-CROSS",
                        "Imported clause snapshot",
                        List.of(),
                        List.of(),
                        List.of()
                ));

        TemplateImportResult result = service.importBundle(
                new ImportTemplateRequest(masterId.toString(), bundle, null, false),
                groupAdmin,
                masterBytes,
                true
        );

        assertThat(result.importSummary().materializedClauseCount()).isEqualTo(1);
        assertThat(result.importSummary().bundleFormat()).isEqualTo(TemplateExportV2Support.EXPORT_FORMAT_V2);
        assertThat(result.template().lifecycleStatus()).isEqualTo(TemplateLifecycleStatus.DRAFT);

        ArgumentCaptor<CreateContentModuleRequest> createCaptor =
                ArgumentCaptor.forClass(CreateContentModuleRequest.class);
        verify(contentModuleService).create(createCaptor.capture(), eq(groupAdmin));
        assertThat(createCaptor.getValue().semanticVersion()).isEqualTo("1.2.3");

        ArgumentCaptor<UpsertContentModuleReferenceRequest> refCaptor =
                ArgumentCaptor.forClass(UpsertContentModuleReferenceRequest.class);
        verify(contentModuleReferenceService).upsertReferenceForImport(
                eq(templateId), refCaptor.capture(), eq(groupAdmin));
        assertThat(refCaptor.getValue().moduleId()).isEqualTo(targetModuleId.toString());
        assertThat(refCaptor.getValue().semanticVersion()).isEqualTo("1.2.3");
        assertThat(refCaptor.getValue().referenceKey()).isEqualToIgnoringCase("clause-a");
        verify(contentModuleReferenceService, never()).upsertReference(any(), any(), any());
        verify(managementAuditRecorder).recordTemplateImported(
                eq(templateId),
                eq("RETAIL"),
                eq("TPL-V2-IMPORT"),
                any(),
                eq(1),
                eq("10000002"),
                any(),
                eq(TemplateExportV2Support.EXPORT_FORMAT_V2),
                eq(1)
        );
    }

    /**
     * BDD-CE-E01-014: when wiring fails mid-commit after materialize, exception propagates
     * and import audit is not recorded (transactional rollback of template/clause writes
     * is owned by {@code @Transactional} on importBundle).
     */
    @Test
    void commit_midTxWiringFailure_propagatesWithoutImportAudit() {
        UUID sourceModuleId = UUID.randomUUID();
        UUID targetModuleId = UUID.randomUUID();
        TemplateExportBundleView bundle = v2Bundle(
                List.of(new ContentModuleReferenceView(
                        "clause-a",
                        sourceModuleId.toString(),
                        "1.2.3",
                        false,
                        false,
                        null
                )),
                List.of(clauseSnapshot("MOD-CROSS", sourceModuleId, "1.2.3", false))
        );
        when(dependencyPrecheck.evaluate(any())).thenReturn(greenReport());
        stubFreshImport();
        when(contentModuleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-CROSS"))
                .thenReturn(Optional.empty());
        when(contentModuleService.create(any(CreateContentModuleRequest.class), eq(groupAdmin)))
                .thenReturn(new ContentModuleDetailView(
                        targetModuleId.toString(),
                        "MOD-CROSS",
                        "RETAIL",
                        "MOD-CROSS",
                        "Imported",
                        List.of(),
                        List.of(),
                        List.of()
                ));
        when(contentModuleReferenceService.upsertReferenceForImport(any(), any(), any()))
                .thenThrow(new TemplateValidationException("api.error.template.contentModuleReferenceInvalid"));

        assertThatThrownBy(() -> service.importBundle(
                new ImportTemplateRequest(masterId.toString(), bundle, null, false),
                groupAdmin,
                masterBytes,
                true
        )).isInstanceOf(TemplateValidationException.class);

        verify(contentModuleService).create(any(), eq(groupAdmin));
        verify(managementAuditRecorder, never()).recordTemplateImported(
                any(), anyString(), anyString(), anyString(), anyInt(), anyString(), any(), any(), any()
        );
    }

    private void stubFreshImport() {
        MasterDocumentEntity master = new MasterDocumentEntity(
                masterId,
                "RETAIL",
                "Retail Master",
                "Retail master document",
                "storage/master.docx",
                "master.docx",
                "10000002"
        );
        master.setStatus(MasterDocumentStatus.APPROVED);
        TemplateEntity importedTemplate = new TemplateEntity(
                templateId,
                "TPL-V2-IMPORT",
                "RETAIL",
                "V2 Import",
                null,
                masterId,
                "10000002"
        );
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(masterId)).thenReturn(Optional.of(master));
        when(templateRepository.findByExternalIdAndDeletedAtIsNull("TPL-V2-IMPORT")).thenReturn(Optional.empty());
        when(templateRepository.findByIdAndDeletedAtIsNull(templateId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(importedTemplate));
        org.mockito.Mockito.lenient()
                .when(templateService.toDetail(importedTemplate))
                .thenReturn(detail(importedTemplate));
    }

    private TemplateDetailView detail(TemplateEntity template) {
        return new TemplateDetailView(
                template.getId().toString(),
                template.getExternalId(),
                template.getGroupCode(),
                template.getName(),
                template.getDescription(),
                masterId.toString(),
                TemplateLifecycleStatus.DRAFT,
                null,
                null,
                UUID.randomUUID().toString(),
                1,
                List.of(),
                List.of(),
                List.of(),
                Instant.now(),
                Instant.now(),
                null,
                null,
                false, null,
                null);
    }

    private TemplateImportDependencyReportView greenReport() {
        return new TemplateImportDependencyReportView(
                List.of(new TemplateImportDependencyItemView(
                        TemplateImportDependencyType.MASTER_PIN,
                        TemplateImportDependencySeverity.OK,
                        "MASTER_FINGERPRINT_OK",
                        "api.error.template.dep.masterFingerprintOk",
                        null
                )),
                0,
                0,
                0,
                true,
                TemplateExportV2Support.EXPORT_FORMAT_V2
        );
    }

    private TemplateExportBundleView v2Bundle(
            List<ContentModuleReferenceView> references,
            List<TemplateExportClauseSnapshotView> clauses
    ) {
        return new TemplateExportBundleView(
                TemplateExportV2Support.EXPORT_FORMAT_V2,
                new TemplateExportMetadataView(
                        templateId.toString(),
                        "TPL-V2-IMPORT",
                        "RETAIL",
                        "V2 Import",
                        null,
                        masterId.toString(),
                        TemplateLifecycleStatus.PUBLISHED,
                        "1.0.0",
                        UUID.randomUUID().toString(),
                        1,
                        Instant.now()
                ),
                List.of(),
                List.of(),
                List.of(),
                references,
                null,
                new TemplateExportMasterPinView(UUID.randomUUID().toString(), masterHash, 1, "PUBLISHED"),
                clauses,
                null,
                List.of()
        );
    }

    private static TemplateExportClauseSnapshotView clauseSnapshot(
            String moduleCode,
            UUID sourceModuleId,
            String semanticVersion,
            boolean locked
    ) {
        int major = Integer.parseInt(semanticVersion.split("\\.")[0].replaceAll("[^0-9]", ""));
        return new TemplateExportClauseSnapshotView(
                moduleCode,
                UUID.randomUUID().toString(),
                Math.max(1, major),
                "{\"nodes\":[]}",
                locked,
                null,
                null,
                null,
                null,
                semanticVersion,
                sourceModuleId.toString()
        );
    }
}
