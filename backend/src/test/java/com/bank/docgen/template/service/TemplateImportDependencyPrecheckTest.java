package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.template.api.ContentModuleReferenceView;
import com.bank.docgen.template.api.TemplateExportAssetKeyManifestItemView;
import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.api.TemplateExportClauseSnapshotView;
import com.bank.docgen.template.api.TemplateExportMasterPinView;
import com.bank.docgen.template.api.TemplateExportMetadataView;
import com.bank.docgen.template.api.TemplateImportDependencyItemView;
import com.bank.docgen.template.api.TemplateImportDependencyReportView;
import com.bank.docgen.template.domain.TemplateExportAssetKeyUsage;
import com.bank.docgen.template.domain.TemplateImportDependencySeverity;
import com.bank.docgen.template.domain.TemplateImportDependencyType;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import java.io.ByteArrayInputStream;
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
class TemplateImportDependencyPrecheckTest {

    @Mock
    private MasterDocumentRepository masterDocumentRepository;
    @Mock
    private MasterRevisionLineRepository masterRevisionLineRepository;
    @Mock
    private ObjectStoragePort objectStoragePort;
    @Mock
    private ContentModuleRepository contentModuleRepository;
    @Mock
    private ContentModuleVersionRepository contentModuleVersionRepository;

    private TemplateImportDependencyPrecheck precheck;
    private UUID masterId;
    private UUID revisionId;
    private byte[] masterBytes;
    private String masterHash;

    @BeforeEach
    void setUp() {
        precheck = new TemplateImportDependencyPrecheck(
                masterDocumentRepository,
                masterRevisionLineRepository,
                objectStoragePort,
                contentModuleRepository,
                contentModuleVersionRepository
        );
        masterId = UUID.randomUUID();
        revisionId = UUID.randomUUID();
        masterBytes = "TARGET-MASTER".getBytes();
        masterHash = TemplateExportHashSupport.sha256Hex(masterBytes);
    }

    @Test
    void dryRun_masterHashMismatch_blocking() {
        stubTargetMaster(masterHash);
        TemplateExportBundleView bundle = v2Bundle(
                new TemplateExportMasterPinView(revisionId.toString(), "a".repeat(64), 1, "PUBLISHED"),
                List.of(),
                List.of()
        );

        TemplateImportDependencyReportView report = precheck.evaluate(
                new TemplateImportDependencyPrecheck.PrecheckContext(
                        bundle,
                        masterId,
                        masterBytes,
                        true,
                        true
                )
        );

        assertThat(report.readyToCommit()).isFalse();
        assertThat(report.items()).anyMatch(item ->
                item.dependencyType() == TemplateImportDependencyType.MASTER_PIN
                        && item.severity() == TemplateImportDependencySeverity.MISMATCH
                        && "MASTER_FINGERPRINT_MISMATCH".equals(item.code())
        );
    }

    @Test
    void dryRun_missingClause_willMaterialize() {
        stubTargetMaster(masterHash);
        TemplateExportBundleView bundle = v2Bundle(
                new TemplateExportMasterPinView(revisionId.toString(), masterHash, 1, "PUBLISHED"),
                List.of(clauseSnapshot("MOD-NEW", UUID.randomUUID(), "1.0.0", false)),
                List.of()
        );
        when(contentModuleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-NEW")).thenReturn(Optional.empty());

        TemplateImportDependencyReportView report = precheck.evaluate(
                new TemplateImportDependencyPrecheck.PrecheckContext(
                        bundle, masterId, masterBytes, true, true
                )
        );

        assertThat(report.items()).anyMatch(item ->
                item.severity() == TemplateImportDependencySeverity.WILL_MATERIALIZE
                        && "CLAUSE_WILL_MATERIALIZE".equals(item.code())
        );
        assertThat(report.items().stream()
                .filter(i -> i.code().equals("CLAUSE_WILL_MATERIALIZE"))
                .noneMatch(i -> i.severity() == TemplateImportDependencySeverity.MISSING
                        || i.severity() == TemplateImportDependencySeverity.MISMATCH)).isTrue();
    }

    @Test
    void dryRun_missingAsset_blocking() {
        stubTargetMaster(masterHash);
        TemplateExportBundleView bundle = v2Bundle(
                new TemplateExportMasterPinView(revisionId.toString(), masterHash, 1, "PUBLISHED"),
                List.of(),
                List.of(new TemplateExportAssetKeyManifestItemView("MISSING-LOGO", TemplateExportAssetKeyUsage.IMAGE))
        );
        when(objectStoragePort.exists("MISSING-LOGO")).thenReturn(false);
        when(objectStoragePort.exists("MISSING-LOGO.png")).thenReturn(false);
        when(objectStoragePort.exists("MISSING-LOGO.jpg")).thenReturn(false);
        when(objectStoragePort.exists("MISSING-LOGO.jpeg")).thenReturn(false);

        TemplateImportDependencyReportView report = precheck.evaluate(
                new TemplateImportDependencyPrecheck.PrecheckContext(
                        bundle, masterId, masterBytes, true, true
                )
        );

        assertThat(report.readyToCommit()).isFalse();
        assertThat(report.items()).anyMatch(item ->
                item.dependencyType() == TemplateImportDependencyType.ASSET_KEY
                        && item.severity() == TemplateImportDependencySeverity.MISSING
        );
    }

    @Test
    void dryRun_allGreen_readyToCommit() {
        stubTargetMaster(masterHash);
        TemplateExportBundleView bundle = v2Bundle(
                new TemplateExportMasterPinView(revisionId.toString(), masterHash, 1, "PUBLISHED"),
                List.of(clauseSnapshot("MOD-1", UUID.randomUUID(), "1.0.0", false)),
                List.of(new TemplateExportAssetKeyManifestItemView("LOGO-1", TemplateExportAssetKeyUsage.IMAGE))
        );
        when(contentModuleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-1")).thenReturn(Optional.empty());
        when(objectStoragePort.exists("LOGO-1")).thenReturn(true);

        TemplateImportDependencyReportView report = precheck.evaluate(
                new TemplateImportDependencyPrecheck.PrecheckContext(
                        bundle, masterId, masterBytes, true, true
                )
        );

        assertThat(report.blockingCount()).isZero();
        assertThat(report.readyToCommit()).isTrue();
        assertThat(report.items().stream().map(TemplateImportDependencyItemView::code))
                .contains("MASTER_FINGERPRINT_OK", "CLAUSE_WILL_MATERIALIZE", "ASSET_KEY_PRESENT");
    }

    /**
     * BDD-CE-E01-011 / Critical #1: real v2 exports carry source-env moduleId UUIDs in
     * contentModuleReferences. On an empty target those UUIDs must not produce false
     * CLAUSE_MISSING when clauseSnapshots cover the same clause by moduleCode.
     */
    @Test
    void dryRun_sourceModuleIdsUnknownOnTarget_butSnapshotsCover_readyToCommit() {
        stubTargetMaster(masterHash);
        UUID sourceModuleId = UUID.randomUUID();
        TemplateExportBundleView bundle = v2BundleWithReferences(
                new TemplateExportMasterPinView(revisionId.toString(), masterHash, 1, "PUBLISHED"),
                List.of(clauseSnapshot("MOD-CROSS", sourceModuleId, "1.2.3", false)),
                List.of(new TemplateExportAssetKeyManifestItemView("LOGO-1", TemplateExportAssetKeyUsage.IMAGE)),
                List.of(new ContentModuleReferenceView(
                        "clause-cross",
                        sourceModuleId.toString(),
                        "1.2.3",
                        false,
                        false,
                        null
                ))
        );
        when(contentModuleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-CROSS"))
                .thenReturn(Optional.empty());
        // Source UUID must not be required on target — do not stub findById.
        when(objectStoragePort.exists("LOGO-1")).thenReturn(true);

        TemplateImportDependencyReportView report = precheck.evaluate(
                new TemplateImportDependencyPrecheck.PrecheckContext(
                        bundle, masterId, masterBytes, true, true
                )
        );

        assertThat(report.items()).noneMatch(item -> "CLAUSE_MISSING".equals(item.code()));
        assertThat(report.items()).anyMatch(item ->
                item.severity() == TemplateImportDependencySeverity.WILL_MATERIALIZE
                        && "CLAUSE_WILL_MATERIALIZE".equals(item.code())
                        && "MOD-CROSS".equals(item.detail())
        );
        assertThat(report.blockingCount()).isZero();
        assertThat(report.readyToCommit()).isTrue();
    }

    private void stubTargetMaster(String hash) {
        MasterDocumentEntity master = org.mockito.Mockito.mock(MasterDocumentEntity.class);
        org.mockito.Mockito.lenient().when(master.getCurrentRevisionLineId()).thenReturn(revisionId);
        org.mockito.Mockito.lenient().when(master.getStatus()).thenReturn(MasterDocumentStatus.APPROVED);
        org.mockito.Mockito.lenient().when(master.getGroupCode()).thenReturn("RETAIL");
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(masterId)).thenReturn(Optional.of(master));
        MasterRevisionLineEntity revision = new MasterRevisionLineEntity(
                revisionId,
                masterId,
                "masters/current.docx",
                "master.docx",
                1,
                MasterDocumentStatus.APPROVED,
                1,
                true,
                null,
                "10000001"
        );
        when(masterRevisionLineRepository.findByIdAndMasterIdAndDeletedAtIsNull(revisionId, masterId))
                .thenReturn(Optional.of(revision));
        when(objectStoragePort.get(revision.getStorageKey()))
                .thenReturn(new ByteArrayInputStream(masterBytes));
        assertThat(TemplateExportHashSupport.sha256Hex(masterBytes)).isEqualTo(hash);
    }

    private TemplateExportBundleView v2Bundle(
            TemplateExportMasterPinView pin,
            List<TemplateExportClauseSnapshotView> clauses,
            List<TemplateExportAssetKeyManifestItemView> assets
    ) {
        return v2BundleWithReferences(pin, clauses, assets, List.of());
    }

    private TemplateExportBundleView v2BundleWithReferences(
            TemplateExportMasterPinView pin,
            List<TemplateExportClauseSnapshotView> clauses,
            List<TemplateExportAssetKeyManifestItemView> assets,
            List<ContentModuleReferenceView> references
    ) {
        return new TemplateExportBundleView(
                TemplateExportV2Support.EXPORT_FORMAT_V2,
                new TemplateExportMetadataView(
                        UUID.randomUUID().toString(),
                        "TPL-V2",
                        "RETAIL",
                        "V2",
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
                pin,
                clauses,
                null,
                assets
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
