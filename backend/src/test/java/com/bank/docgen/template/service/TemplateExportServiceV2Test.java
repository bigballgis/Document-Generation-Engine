package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.api.ApiPolicyView;
import com.bank.docgen.apimgmt.mapping.ApiPolicyViewMapper;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.AnchorBindingView;
import com.bank.docgen.template.api.CompositionRuleView;
import com.bank.docgen.template.api.ContentModuleReferenceView;
import com.bank.docgen.template.api.TemplateExportResult;
import com.bank.docgen.template.api.VariableSchemaView;
import com.bank.docgen.template.domain.BindingValidationStatus;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.domain.VariableType;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemplateExportServiceV2Test {

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

    private TemplateExportService service;
    private UUID templateId;
    private UUID masterId;
    private UUID revisionId;
    private TemplateEntity template;
    private TemplateVersionEntity version;
    private ManagementSessionClaims groupAdmin;
    private byte[] masterBytes;
    private String masterHash;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        TemplateExportV2Support v2Support = new TemplateExportV2Support(
                masterDocumentRepository,
                masterRevisionLineRepository,
                objectStoragePort,
                contentModuleRepository,
                contentModuleVersionRepository,
                objectMapper
        );
        service = new TemplateExportService(
                templateRepository,
                apiPolicyRepository,
                apiPolicyViewMapper,
                contentModuleReferenceService,
                org.mockito.Mockito.mock(com.bank.docgen.template.service.CompositionInclusionRuleService.class),
                managementAuditRecorder,
                templateService,
                new TemplateExportAccessService(new GroupAccessService()),
                objectMapper,
                templateCurrentVersionResolver,
                v2Support
        );
        templateId = UUID.randomUUID();
        masterId = UUID.randomUUID();
        revisionId = UUID.randomUUID();
        masterBytes = "PINNED-MASTER-DOCX".getBytes();
        masterHash = TemplateExportHashSupport.sha256Hex(masterBytes);
        template = new TemplateEntity(
                templateId,
                "TPL-RETAIL-LETTER",
                "RETAIL",
                "Retail Letter",
                "Slice template",
                masterId,
                "10000003"
        );
        template.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        template.setReleaseVersion("1.0.0");
        version = new TemplateVersionEntity(UUID.randomUUID(), templateId, "10000003");
        version.setMasterRevisionId(revisionId);
        version.setMasterFileHash(masterHash);
        version.setPinMetadataJson("{\"pinOrigin\":\"PUBLISHED\"}");
        version.setRenderProfileVersion("rp-v1");
        version.setRenderProfileJson("{\"renderProfileVersion\":\"rp-v1\"}");
        groupAdmin = session("10000002", List.of("GROUP_ADMIN"), List.of("RETAIL"));
    }

    @Test
    void exportDefault_stillV1() {
        stubReadableTemplate();
        when(templateCurrentVersionResolver.requireExportableVersion(templateId)).thenReturn(version);
        when(templateService.loadRules(version)).thenReturn(List.of());
        when(templateService.toDetail(template)).thenReturn(detailWithArtifacts());
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.empty());
        when(contentModuleReferenceService.listReferences(templateId, groupAdmin)).thenReturn(List.of());

        TemplateExportResult result = service.exportJson(templateId, groupAdmin);

        assertThat(result.format()).isEqualTo(TemplateExportService.EXPORT_FORMAT);
        assertThat(result.bundle().masterPin()).isNull();
    }

    @Test
    void exportV2Zip_embedsMasterDocxAndMatchingHash() throws Exception {
        stubReadableTemplate();
        when(templateCurrentVersionResolver.requireExportableVersion(templateId)).thenReturn(version);
        when(templateService.loadRules(version)).thenReturn(List.of());
        when(templateService.toDetail(template)).thenReturn(detailWithArtifacts());
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

        TemplateExportService.TemplateExportZipArtifact artifact =
                service.exportZip(templateId, groupAdmin, 2);

        assertThat(artifact.content()).isNotEmpty();
        boolean sawJson = false;
        boolean sawMaster = false;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(artifact.content()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("template-export-bundle.json".equals(entry.getName())) {
                    sawJson = true;
                    String json = new String(zip.readAllBytes());
                    assertThat(json).contains("template-export-bundle-v2-json");
                    assertThat(json).contains(masterHash);
                } else if ("artifacts/master.docx".equals(entry.getName())) {
                    sawMaster = true;
                    assertThat(TemplateExportHashSupport.sha256Hex(zip.readAllBytes())).isEqualTo(masterHash);
                }
            }
        }
        assertThat(sawJson).isTrue();
        assertThat(sawMaster).isTrue();
    }

    @Test
    void exportV2_includesClauseSnapshotsAndAssetKeys() {
        stubReadableTemplate();
        when(templateCurrentVersionResolver.requireExportableVersion(templateId)).thenReturn(version);
        when(templateService.loadRules(version)).thenReturn(List.of());
        when(templateService.toDetail(template)).thenReturn(detailWithImageBinding());
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.empty());
        UUID moduleId = UUID.randomUUID();
        UUID moduleVersionId = UUID.randomUUID();
        when(contentModuleReferenceService.listReferences(templateId, groupAdmin)).thenReturn(List.of(
                new ContentModuleReferenceView("clause-a", moduleId.toString(), "1.2.3", true, false, null)
        ));
        var module = org.mockito.Mockito.mock(com.bank.docgen.contentmodule.persistence.ContentModuleEntity.class);
        when(module.getModuleCode()).thenReturn("MOD-1");
        when(module.getId()).thenReturn(moduleId);
        when(contentModuleRepository.findByIdAndDeletedAtIsNull(moduleId)).thenReturn(Optional.of(module));
        var moduleVersion = org.mockito.Mockito.mock(
                com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity.class);
        when(moduleVersion.getId()).thenReturn(moduleVersionId);
        when(moduleVersion.getSemanticVersion()).thenReturn("1.2.3");
        when(moduleVersion.getContentStructureJson()).thenReturn("{\"nodes\":[]}");
        when(contentModuleVersionRepository.findByModuleIdAndSemanticVersion(moduleId, "1.2.3"))
                .thenReturn(Optional.of(moduleVersion));
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

        TemplateExportResult result = service.exportJson(templateId, groupAdmin, 2);

        assertThat(result.format()).isEqualTo(TemplateExportService.EXPORT_FORMAT_V2);
        assertThat(result.bundle().clauseSnapshots()).hasSize(1);
        assertThat(result.bundle().clauseSnapshots().get(0).moduleCode()).isEqualTo("MOD-1");
        assertThat(result.bundle().clauseSnapshots().get(0).semanticVersion()).isEqualTo("1.2.3");
        assertThat(result.bundle().clauseSnapshots().get(0).versionNumber()).isEqualTo(1);
        assertThat(result.bundle().clauseSnapshots().get(0).sourceModuleId()).isEqualTo(moduleId.toString());
        assertThat(result.bundle().assetKeyManifest()).isNotEmpty();
        assertThat(result.bundle().renderProfile()).isNotNull();
        assertThat(result.bundle().masterPin().masterFileHash()).isEqualTo(masterHash);
    }

    @Test
    void exportV2_stripsSecrets() {
        stubReadableTemplate();
        when(templateCurrentVersionResolver.requireExportableVersion(templateId)).thenReturn(version);
        when(templateService.loadRules(version)).thenReturn(List.of(
                new CompositionRuleView("rule-1", "true", "HEADER", "", "")
        ));
        when(templateService.toDetail(template)).thenReturn(detailWithArtifacts());
        ApiPolicyEntity policyEntity = org.mockito.Mockito.mock(ApiPolicyEntity.class);
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(policyEntity));
        when(apiPolicyViewMapper.toPolicyView(policyEntity)).thenReturn(policyView());
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

        TemplateExportResult result = service.exportJson(templateId, groupAdmin, 2);

        assertThat(result.bundle().policySnapshot()).isNotNull();
        assertThat(new ObjectMapper().findAndRegisterModules().valueToTree(result.bundle()).toString().toLowerCase())
                .doesNotContain("credentialid")
                .doesNotContain("clientsecret");
        verify(managementAuditRecorder).recordTemplateExported(
                eq(templateId),
                eq("RETAIL"),
                eq("TPL-RETAIL-LETTER"),
                eq("10000002"),
                any()
        );
    }

    @Test
    void exportV2_rejectsDraft() {
        template.setLifecycleStatus(TemplateLifecycleStatus.DRAFT);
        stubReadableTemplate();

        assertThatThrownBy(() -> service.exportJson(templateId, groupAdmin, 2))
                .isInstanceOf(TemplateValidationException.class);
    }

    private void stubReadableTemplate() {
        when(templateRepository.findByIdAndDeletedAtIsNull(templateId)).thenReturn(Optional.of(template));
    }

    private com.bank.docgen.template.api.TemplateDetailView detailWithArtifacts() {
        return new com.bank.docgen.template.api.TemplateDetailView(
                templateId.toString(),
                "TPL-RETAIL-LETTER",
                "RETAIL",
                "Retail Letter",
                "Slice template",
                masterId.toString(),
                TemplateLifecycleStatus.PUBLISHED,
                null,
                "1.0.0",
                version.getId().toString(),
                1,
                List.of(new VariableSchemaView(
                        UUID.randomUUID().toString(),
                        "customerName",
                        VariableType.TEXT,
                        true,
                        "Customer",
                        null,
                        "Customer name",
                        null
                )),
                List.of(new AnchorBindingView(
                        UUID.randomUUID().toString(),
                        "HEADER",
                        "TEXT",
                        "{\"nodes\":[]}",
                        BindingValidationStatus.VALID
                )),
                List.of(),
                Instant.now(),
                Instant.now(),
                "test-user",
                "Test User",
                true,
                null,
                null);
    }

    private com.bank.docgen.template.api.TemplateDetailView detailWithImageBinding() {
        return new com.bank.docgen.template.api.TemplateDetailView(
                templateId.toString(),
                "TPL-RETAIL-LETTER",
                "RETAIL",
                "Retail Letter",
                "Slice template",
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
                null);
    }

    private ApiPolicyView policyView() {
        return new ApiPolicyView(
                templateId.toString(),
                1,
                List.of("RETAIL_API"),
                "1.0.0",
                List.of("DOCX"),
                List.of("SYNC_STREAM"),
                false,
                10,
                100,
                10000,
                false,
                false,
                true,
                90,
                30,
                Instant.now()
        );
    }

    private static ManagementSessionClaims session(
            String username,
            List<String> roles,
            List<String> authorizedGroupCodes
    ) {
        return new ManagementSessionClaims(
                username,
                username,
                username + "@example.com",
                AuthSource.LOCAL,
                roles,
                authorizedGroupCodes,
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                Instant.now().plusSeconds(3600)
        );
    }
}
