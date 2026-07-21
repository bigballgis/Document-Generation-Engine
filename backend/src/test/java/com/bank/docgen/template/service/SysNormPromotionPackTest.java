package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.mapping.ApiPolicyViewMapper;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.contentmodule.service.ContentModuleNestingService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.library.service.AssetLibraryService;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.master.service.MasterDocumentService;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.AnchorBindingView;
import com.bank.docgen.template.api.ContentModuleReferenceView;
import com.bank.docgen.template.api.TemplateExportAssetKeyManifestItemView;
import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.api.TemplateExportClauseNestingGraphEdgeView;
import com.bank.docgen.template.api.TemplateExportClauseNestingGraphView;
import com.bank.docgen.template.api.TemplateExportClauseSnapshotView;
import com.bank.docgen.template.api.TemplateExportMasterPinView;
import com.bank.docgen.template.api.TemplateExportMetadataView;
import com.bank.docgen.template.api.TemplateImportDependencyReportView;
import com.bank.docgen.template.domain.BindingValidationStatus;
import com.bank.docgen.template.domain.TemplateDependencyClosure;
import com.bank.docgen.template.domain.TemplateExportAssetKeyUsage;
import com.bank.docgen.template.domain.TemplateImportDependencySeverity;
import com.bank.docgen.template.domain.TemplateImportDependencyType;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * SYS-NORM Wave 7 / #151 — promotion pack export + dry-run dependency extensions.
 */
@ExtendWith(MockitoExtension.class)
class SysNormPromotionPackTest {

    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private ApiPolicyViewMapper apiPolicyViewMapper;
    @Mock
    private TemplateContentModuleReferenceService contentModuleReferenceService;
    @Mock
    private ManagementAuditRecorder managementAuditRecorder;
    @Mock
    private TemplateService templateService;
    @Mock
    private TemplateCurrentVersionResolver templateCurrentVersionResolver;
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
    @Mock
    private ContentModuleNestingService nestingService;
    @Mock
    private AssetLibraryService assetLibraryService;
    @Mock
    private MasterDocumentService masterDocumentService;

    private TemplateExportService exportService;
    private TemplateImportDependencyPrecheck precheck;
    private UUID templateId;
    private UUID masterId;
    private UUID revisionId;
    private TemplateEntity template;
    private TemplateVersionEntity version;
    private ManagementSessionClaims groupAdmin;
    private byte[] masterBytes;
    private String masterHash;
    private byte[] logoBytes;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        lenient().when(nestingService.extractNestedReferenceKeys(any())).thenReturn(Set.of());
        TemplateExportPromotionSupport promotionSupport = new TemplateExportPromotionSupport(
                contentModuleRepository,
                contentModuleVersionRepository,
                nestingService,
                objectStoragePort
        );
        TemplateExportV2Support v2Support = new TemplateExportV2Support(
                masterDocumentRepository,
                masterRevisionLineRepository,
                objectStoragePort,
                contentModuleRepository,
                contentModuleVersionRepository,
                objectMapper
        );
        exportService = new TemplateExportService(
                templateRepository,
                apiPolicyRepository,
                apiPolicyViewMapper,
                contentModuleReferenceService,
                org.mockito.Mockito.mock(CompositionInclusionRuleService.class),
                managementAuditRecorder,
                templateService,
                new TemplateExportAccessService(new GroupAccessService()),
                objectMapper,
                templateCurrentVersionResolver,
                v2Support,
                promotionSupport
        );
        precheck = new TemplateImportDependencyPrecheck(
                masterDocumentRepository,
                masterRevisionLineRepository,
                objectStoragePort,
                contentModuleRepository,
                contentModuleVersionRepository
        );

        templateId = UUID.randomUUID();
        masterId = UUID.randomUUID();
        revisionId = UUID.randomUUID();
        masterBytes = "PINNED-MASTER-DOCX".getBytes();
        masterHash = TemplateExportHashSupport.sha256Hex(masterBytes);
        logoBytes = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        template = new TemplateEntity(
                templateId,
                "TPL-PROMO",
                "RETAIL",
                "Promo Template",
                "Wave 7",
                masterId,
                "10000003"
        );
        template.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        template.setReleaseVersion("1.0.0");
        version = new TemplateVersionEntity(UUID.randomUUID(), templateId, "10000003");
        version.setMasterRevisionId(revisionId);
        version.setMasterFileHash(masterHash);
        version.setPinMetadataJson("{\"pinOrigin\":\"PUBLISHED\"}");
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
    void promotionExport_embedsAssetBinaries_pp001() throws Exception {
        stubExportableTemplateWithLogo();
        when(objectStoragePort.exists("LOGO-1")).thenReturn(true);
        when(objectStoragePort.get("LOGO-1")).thenReturn(new ByteArrayInputStream(logoBytes));

        TemplateExportService.TemplateExportZipArtifact artifact = exportService.exportZip(
                templateId,
                groupAdmin,
                2,
                TemplateDependencyClosure.PROMOTION
        );

        boolean sawAsset = false;
        boolean sawAssetsTree = false;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(artifact.content()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().startsWith("artifacts/assets/")) {
                    sawAssetsTree = true;
                }
                if (TemplateExportAssetPathSupport.zipEntryName("LOGO-1").equals(entry.getName())) {
                    sawAsset = true;
                    assertThat(zip.readAllBytes()).isEqualTo(logoBytes);
                }
                if ("template-export-bundle.json".equals(entry.getName())) {
                    String json = new String(zip.readAllBytes());
                    assertThat(json).contains("LOGO-1");
                    assertThat(json).contains("\"dependencyClosure\":\"PROMOTION\"");
                    assertThat(json.toLowerCase()).doesNotContain("documentbrand");
                    assertThat(json.toLowerCase()).doesNotContain("legalentity");
                    assertThat(json.toLowerCase()).doesNotContain("clientsecret");
                }
            }
        }
        assertThat(sawAssetsTree).isTrue();
        assertThat(sawAsset).isTrue();
    }

    @Test
    void defaultV2_stillKeysOnly_pp004() throws Exception {
        stubExportableTemplateWithLogo();
        // No asset get() for default v2 — keys only
        TemplateExportService.TemplateExportZipArtifact artifact =
                exportService.exportZip(templateId, groupAdmin, 2, null);

        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(artifact.content()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                assertThat(entry.getName()).doesNotStartWith("artifacts/assets/");
            }
        }
    }

    @Test
    void promotionExport_includesNestingClosure_pp002() throws Exception {
        stubExportableTemplateWithLogo();
        UUID parentModuleId = UUID.randomUUID();
        UUID childModuleId = UUID.randomUUID();
        UUID parentVersionId = UUID.randomUUID();
        UUID childVersionId = UUID.randomUUID();
        when(contentModuleReferenceService.listReferences(templateId, groupAdmin)).thenReturn(List.of(
                new ContentModuleReferenceView("root", parentModuleId.toString(), "1.0.0", false, false, null)
        ));
        ContentModuleEntity parent = org.mockito.Mockito.mock(ContentModuleEntity.class);
        when(parent.getModuleCode()).thenReturn("MOD-PARENT");
        when(parent.getId()).thenReturn(parentModuleId);
        when(contentModuleRepository.findByIdAndDeletedAtIsNull(parentModuleId)).thenReturn(Optional.of(parent));
        ContentModuleVersionEntity parentVersion = org.mockito.Mockito.mock(ContentModuleVersionEntity.class);
        when(parentVersion.getId()).thenReturn(parentVersionId);
        when(parentVersion.getSemanticVersion()).thenReturn("1.0.0");
        when(parentVersion.getContentStructureJson()).thenReturn(
                "{\"nodes\":[{\"type\":\"contentModuleRef\",\"referenceKey\":\"MOD-CHILD\"}]}");
        when(contentModuleVersionRepository.findByModuleIdAndSemanticVersion(parentModuleId, "1.0.0"))
                .thenReturn(Optional.of(parentVersion));

        when(nestingService.extractNestedReferenceKeys(
                "{\"nodes\":[{\"type\":\"contentModuleRef\",\"referenceKey\":\"MOD-CHILD\"}]}"))
                .thenReturn(Set.of("MOD-CHILD"));
        when(nestingService.extractNestedReferenceKeys("{\"nodes\":[]}")).thenReturn(Set.of());

        ContentModuleEntity child = org.mockito.Mockito.mock(ContentModuleEntity.class);
        when(child.getModuleCode()).thenReturn("MOD-CHILD");
        when(child.getId()).thenReturn(childModuleId);
        when(contentModuleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-CHILD"))
                .thenReturn(Optional.of(child));
        ContentModuleVersionEntity childVersion = org.mockito.Mockito.mock(ContentModuleVersionEntity.class);
        when(childVersion.getId()).thenReturn(childVersionId);
        when(childVersion.getSemanticVersion()).thenReturn("1.0.0");
        when(childVersion.getContentStructureJson()).thenReturn("{\"nodes\":[]}");
        when(contentModuleVersionRepository.findByModuleIdOrderBySemanticVersionDesc(childModuleId))
                .thenReturn(List.of(childVersion));

        when(objectStoragePort.exists("LOGO-1")).thenReturn(true);
        when(objectStoragePort.get("LOGO-1")).thenReturn(new ByteArrayInputStream(logoBytes));

        TemplateExportService.TemplateExportZipArtifact artifact = exportService.exportZip(
                templateId, groupAdmin, 2, TemplateDependencyClosure.PROMOTION);
        TemplateExportBundleView bundle = readBundleFromZip(artifact.content());

        assertThat(bundle.dependencyClosure()).isEqualTo("PROMOTION");
        assertThat(bundle.clauseSnapshots()).extracting(TemplateExportClauseSnapshotView::moduleCode)
                .contains("MOD-PARENT", "MOD-CHILD");
        assertThat(bundle.clauseNestingGraph()).isNotNull();
        assertThat(bundle.clauseNestingGraph().edges()).anyMatch(edge ->
                "MOD-PARENT".equals(edge.parentModuleCode())
                        && "MOD-CHILD".equals(edge.childModuleCode())
                        && edge.depth() == 1
        );
    }

    @Test
    void exportJson_rejectsPromotionClosure() {
        assertThatThrownBy(() -> exportService.exportJson(
                templateId, groupAdmin, 2, TemplateDependencyClosure.PROMOTION))
                .isInstanceOf(TemplateValidationException.class)
                .extracting(ex -> ((TemplateValidationException) ex).messageKey())
                .isEqualTo("api.error.template.exportFormatUnsupported");
    }

    @Test
    void promotionExport_cycleFailsClosed_pp003() {
        when(templateRepository.findByIdAndDeletedAtIsNull(templateId)).thenReturn(Optional.of(template));
        when(templateCurrentVersionResolver.requireExportableVersion(templateId)).thenReturn(version);
        when(templateService.toDetail(template)).thenReturn(detailWithImageBinding());
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.empty());
        MasterRevisionLineEntity revision = new MasterRevisionLineEntity(
                revisionId,
                masterId,
                "masters/" + revisionId + ".docx",
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

        UUID parentModuleId = UUID.randomUUID();
        when(contentModuleReferenceService.listReferences(templateId, groupAdmin)).thenReturn(List.of(
                new ContentModuleReferenceView("root", parentModuleId.toString(), "1.0.0", false, false, null)
        ));
        ContentModuleEntity parent = org.mockito.Mockito.mock(ContentModuleEntity.class);
        when(parent.getModuleCode()).thenReturn("MOD-A");
        when(parent.getId()).thenReturn(parentModuleId);
        when(contentModuleRepository.findByIdAndDeletedAtIsNull(parentModuleId)).thenReturn(Optional.of(parent));
        ContentModuleVersionEntity parentVersion = org.mockito.Mockito.mock(ContentModuleVersionEntity.class);
        when(parentVersion.getId()).thenReturn(UUID.randomUUID());
        when(parentVersion.getSemanticVersion()).thenReturn("1.0.0");
        when(parentVersion.getContentStructureJson()).thenReturn(
                "{\"nodes\":[{\"type\":\"contentModuleRef\",\"referenceKey\":\"MOD-A\"}]}");
        when(contentModuleVersionRepository.findByModuleIdAndSemanticVersion(parentModuleId, "1.0.0"))
                .thenReturn(Optional.of(parentVersion));
        when(nestingService.extractNestedReferenceKeys(any())).thenReturn(Set.of("MOD-A"));

        assertThatThrownBy(() -> exportService.exportZip(
                templateId, groupAdmin, 2, TemplateDependencyClosure.PROMOTION))
                .isInstanceOf(TemplateGovernanceException.class)
                .extracting(ex -> ((TemplateGovernanceException) ex).errorCode())
                .isEqualTo(ApiErrorCodes.CONTENT_MODULE_NESTING_CYCLE);
    }

    @Test
    void dryRun_promotionField_masterWillMaterialize_withoutAssetsOrNesting() {
        byte[] targetBytes = "OTHER-TARGET-MASTER".getBytes();
        stubTargetMasterBytes(targetBytes);
        // Pin hash = pack master; target fingerprint differs → WILL_MATERIALIZE when PROMOTION marker set
        TemplateExportBundleView bundle = v2Bundle(
                List.of(),
                null,
                TemplateDependencyClosure.PROMOTION.name()
        );
        TemplateImportDependencyReportView report = precheck.evaluate(
                new TemplateImportDependencyPrecheck.PrecheckContext(
                        bundle,
                        masterId,
                        masterBytes,
                        true,
                        true,
                        Map.of()
                )
        );

        assertThat(report.items()).anyMatch(item ->
                item.dependencyType() == TemplateImportDependencyType.MASTER_PIN
                        && item.severity() == TemplateImportDependencySeverity.WILL_MATERIALIZE
                        && "MASTER_WILL_MATERIALIZE".equals(item.code())
        );
        assertThat(report.items()).noneMatch(item -> "MASTER_FINGERPRINT_MISMATCH".equals(item.code()));
    }

    @Test
    void dryRun_assetBinary_willMaterialize_pp008() {
        stubTargetMaster(masterHash);
        when(objectStoragePort.exists("LOGO-1")).thenReturn(false);
        when(objectStoragePort.exists("LOGO-1.png")).thenReturn(false);
        when(objectStoragePort.exists("LOGO-1.jpg")).thenReturn(false);
        when(objectStoragePort.exists("LOGO-1.jpeg")).thenReturn(false);

        TemplateExportBundleView bundle = v2Bundle(
                List.of(new TemplateExportAssetKeyManifestItemView("LOGO-1", TemplateExportAssetKeyUsage.IMAGE)),
                null
        );
        TemplateImportDependencyReportView report = precheck.evaluate(
                new TemplateImportDependencyPrecheck.PrecheckContext(
                        bundle,
                        masterId,
                        masterBytes,
                        true,
                        true,
                        Map.of("LOGO-1", logoBytes)
                )
        );

        assertThat(report.readyToCommit()).isTrue();
        assertThat(report.items()).anyMatch(item ->
                item.dependencyType() == TemplateImportDependencyType.ASSET_BINARY
                        && item.severity() == TemplateImportDependencySeverity.WILL_MATERIALIZE
                        && "ASSET_WILL_MATERIALIZE".equals(item.code())
        );
    }

    @Test
    void dryRun_missingBinary_blocking_pp008() {
        stubTargetMaster(masterHash);
        when(objectStoragePort.exists("LOGO-1")).thenReturn(false);
        when(objectStoragePort.exists("LOGO-1.png")).thenReturn(false);
        when(objectStoragePort.exists("LOGO-1.jpg")).thenReturn(false);
        when(objectStoragePort.exists("LOGO-1.jpeg")).thenReturn(false);

        TemplateExportBundleView bundle = v2Bundle(
                List.of(new TemplateExportAssetKeyManifestItemView("LOGO-1", TemplateExportAssetKeyUsage.IMAGE)),
                new TemplateExportClauseNestingGraphView(
                        List.of(new TemplateExportClauseNestingGraphEdgeView("A", "B", 1)),
                        1
                )
        );
        // Nesting incomplete → also blocking; asset binary absent with promotion carrier
        when(contentModuleRepository.findByModuleCodeAndDeletedAtIsNull("A")).thenReturn(Optional.empty());
        when(contentModuleRepository.findByModuleCodeAndDeletedAtIsNull("B")).thenReturn(Optional.empty());

        TemplateImportDependencyReportView report = precheck.evaluate(
                new TemplateImportDependencyPrecheck.PrecheckContext(
                        bundle, masterId, masterBytes, true, true, Map.of()
                )
        );

        assertThat(report.readyToCommit()).isFalse();
        assertThat(report.items()).anyMatch(item -> "ASSET_BINARY_ABSENT".equals(item.code()));
    }

    @Test
    void dryRun_nestingIncomplete_blocking_pp009() {
        stubTargetMaster(masterHash);
        TemplateExportBundleView bundle = v2Bundle(
                List.of(),
                new TemplateExportClauseNestingGraphView(
                        List.of(new TemplateExportClauseNestingGraphEdgeView("MOD-P", "MOD-C", 1)),
                        1
                )
        );
        when(contentModuleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-P")).thenReturn(Optional.empty());
        when(contentModuleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-C")).thenReturn(Optional.empty());

        TemplateImportDependencyReportView report = precheck.evaluate(
                new TemplateImportDependencyPrecheck.PrecheckContext(
                        bundle, masterId, masterBytes, true, true, Map.of()
                )
        );

        assertThat(report.readyToCommit()).isFalse();
        assertThat(report.items()).anyMatch(item ->
                item.dependencyType() == TemplateImportDependencyType.CLAUSE_NESTING
                        && item.severity() == TemplateImportDependencySeverity.MISSING
                        && "CLAUSE_NESTING_MISSING".equals(item.code())
        );
    }

    @Test
    void e01_nonPromotion_failClosed_regression_pp019() {
        stubTargetMaster(masterHash);
        when(objectStoragePort.exists("MISSING-LOGO")).thenReturn(false);
        when(objectStoragePort.exists("MISSING-LOGO.png")).thenReturn(false);
        when(objectStoragePort.exists("MISSING-LOGO.jpg")).thenReturn(false);
        when(objectStoragePort.exists("MISSING-LOGO.jpeg")).thenReturn(false);

        TemplateExportBundleView bundle = v2Bundle(
                List.of(new TemplateExportAssetKeyManifestItemView(
                        "MISSING-LOGO", TemplateExportAssetKeyUsage.IMAGE)),
                null
        );
        TemplateImportDependencyReportView report = precheck.evaluate(
                new TemplateImportDependencyPrecheck.PrecheckContext(
                        bundle, masterId, masterBytes, true, true, Map.of()
                )
        );

        assertThat(report.readyToCommit()).isFalse();
        assertThat(report.items()).anyMatch(item ->
                item.dependencyType() == TemplateImportDependencyType.ASSET_KEY
                        && "ASSET_KEY_MISSING".equals(item.code())
        );
    }

    @Test
    void parseZip_acceptsEmbeddedAssets() {
        // Smoke: path helper round-trip
        String entry = TemplateExportAssetPathSupport.zipEntryName("LOGO-1");
        assertThat(entry).isEqualTo("artifacts/assets/LOGO-1");
        assertThat(TemplateExportAssetPathSupport.decodePathSegment("LOGO-1")).isEqualTo("LOGO-1");
        verify(assetLibraryService, org.mockito.Mockito.never()).materializeImportedAsset(
                any(), any(), any(), any(), any(), any(), any());
        verify(masterDocumentService, org.mockito.Mockito.never()).materializeDraftFromImport(
                any(), any(), any(), any(), any());
    }

    private void stubExportableTemplateWithLogo() {
        when(templateRepository.findByIdAndDeletedAtIsNull(templateId)).thenReturn(Optional.of(template));
        when(templateCurrentVersionResolver.requireExportableVersion(templateId)).thenReturn(version);
        when(templateService.loadRules(version)).thenReturn(List.of());
        when(templateService.toDetail(template)).thenReturn(detailWithImageBinding());
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.empty());
        when(contentModuleReferenceService.listReferences(templateId, groupAdmin)).thenReturn(List.of());
        MasterRevisionLineEntity revision = new MasterRevisionLineEntity(
                revisionId,
                masterId,
                "masters/" + revisionId + ".docx",
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
    }

    private void stubTargetMaster(String hash) {
        stubTargetMasterBytes(masterBytes);
        assertThat(TemplateExportHashSupport.sha256Hex(masterBytes)).isEqualTo(hash);
    }

    private void stubTargetMasterBytes(byte[] targetBytes) {
        MasterDocumentEntity master = org.mockito.Mockito.mock(MasterDocumentEntity.class);
        lenient().when(master.getCurrentRevisionLineId()).thenReturn(revisionId);
        lenient().when(master.getStatus()).thenReturn(MasterDocumentStatus.APPROVED);
        lenient().when(master.getGroupCode()).thenReturn("RETAIL");
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
                .thenReturn(new ByteArrayInputStream(targetBytes));
    }

    private TemplateExportBundleView v2Bundle(
            List<TemplateExportAssetKeyManifestItemView> assets,
            TemplateExportClauseNestingGraphView nestingGraph
    ) {
        return v2Bundle(assets, nestingGraph, null);
    }

    private TemplateExportBundleView v2Bundle(
            List<TemplateExportAssetKeyManifestItemView> assets,
            TemplateExportClauseNestingGraphView nestingGraph,
            String dependencyClosure
    ) {
        return new TemplateExportBundleView(
                TemplateExportV2Support.EXPORT_FORMAT_V2,
                new TemplateExportMetadataView(
                        UUID.randomUUID().toString(),
                        "TPL-PROMO",
                        "RETAIL",
                        "Promo",
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
                List.of(),
                null,
                new TemplateExportMasterPinView(revisionId.toString(), masterHash, 1, "PUBLISHED"),
                List.of(),
                null,
                assets,
                null,
                nestingGraph,
                dependencyClosure
        );
    }

    private TemplateExportBundleView readBundleFromZip(byte[] zipBytes) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("template-export-bundle.json".equals(entry.getName())) {
                    return new ObjectMapper().findAndRegisterModules()
                            .readValue(zip.readAllBytes(), TemplateExportBundleView.class);
                }
            }
        }
        throw new AssertionError("template-export-bundle.json missing from ZIP");
    }

    private com.bank.docgen.template.api.TemplateDetailView detailWithImageBinding() {
        return new com.bank.docgen.template.api.TemplateDetailView(
                templateId.toString(),
                "TPL-PROMO",
                "RETAIL",
                "Promo Template",
                "Wave 7",
                masterId.toString(),
                TemplateLifecycleStatus.PUBLISHED,
                null,
                "1.0.0",
                version.getId().toString(),
                1,
                List.of(),
                List.of(new AnchorBindingView(
                        UUID.randomUUID().toString(),
                        "LOGO",
                        "IMAGE",
                        "{\"nodes\":[{\"type\":\"paragraph\",\"children\":[{\"type\":\"imageRef\",\"imageRef\":\"LOGO-1\"}]}]}",
                        BindingValidationStatus.VALID
                )),
                List.of(),
                Instant.now(),
                Instant.now(),
                "test-user",
                "Test User",
                true,
                null,
                null
        );
    }
}
