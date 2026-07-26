package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import com.bank.docgen.apimgmt.mapping.ApiPolicyViewMapper;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
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
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.AnchorBindingView;
import com.bank.docgen.template.api.TemplateExportAssetKeyManifestItemView;
import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.api.TemplateExportClauseNestingGraphView;
import com.bank.docgen.template.api.TemplateExportMasterPinView;
import com.bank.docgen.template.api.TemplateExportMetadataView;
import com.bank.docgen.template.domain.BindingValidationStatus;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Shared fixtures (AI-SCALE #169).
 */
@ExtendWith(MockitoExtension.class)
abstract class SysNormPromotionPackTestFixtures {

    @Mock
    protected TemplateRepository templateRepository;
    @Mock
    protected ApiPolicyRepository apiPolicyRepository;
    @Mock
    protected ApiPolicyViewMapper apiPolicyViewMapper;
    @Mock
    protected TemplateContentModuleReferenceService contentModuleReferenceService;
    @Mock
    protected ManagementAuditRecorder managementAuditRecorder;
    @Mock
    protected TemplateService templateService;
    @Mock
    protected TemplateCurrentVersionResolver templateCurrentVersionResolver;
    @Mock
    protected MasterDocumentRepository masterDocumentRepository;
    @Mock
    protected MasterRevisionLineRepository masterRevisionLineRepository;
    @Mock
    protected ObjectStoragePort objectStoragePort;
    @Mock
    protected ContentModuleRepository contentModuleRepository;
    @Mock
    protected ContentModuleVersionRepository contentModuleVersionRepository;
    @Mock
    protected ContentModuleNestingService nestingService;
    @Mock
    protected AssetLibraryService assetLibraryService;
    @Mock
    protected MasterDocumentService masterDocumentService;
    protected TemplateExportService exportService;
    protected TemplateImportDependencyPrecheck precheck;
    protected UUID templateId;
    protected UUID masterId;
    protected UUID revisionId;
    protected TemplateEntity template;
    protected TemplateVersionEntity version;
    protected ManagementSessionClaims groupAdmin;
    protected byte[] masterBytes;
    protected String masterHash;
    protected byte[] logoBytes;
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
    protected void stubExportableTemplateWithLogo() {
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
        // lenient: tests may add further get(...) stubbings for embedded asset keys
        lenient().when(objectStoragePort.get(revision.getStorageKey()))
                .thenReturn(new ByteArrayInputStream(masterBytes));
    }
    protected void stubTargetMaster(String hash) {
        stubTargetMasterBytes(masterBytes);
        assertThat(TemplateExportHashSupport.sha256Hex(masterBytes)).isEqualTo(hash);
    }
    protected void stubTargetMasterBytes(byte[] targetBytes) {
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
    protected TemplateExportBundleView v2Bundle(
            List<TemplateExportAssetKeyManifestItemView> assets,
            TemplateExportClauseNestingGraphView nestingGraph
    ) {
        return v2Bundle(assets, nestingGraph, null);
    }
    protected TemplateExportBundleView v2Bundle(
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
    protected TemplateExportBundleView readBundleFromZip(byte[] zipBytes) throws Exception {
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
    protected com.bank.docgen.template.api.TemplateDetailView detailWithImageBinding() {
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
