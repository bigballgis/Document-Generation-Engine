package com.bank.docgen.library.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.persistence.BusinessGroupRepository;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.library.api.LibraryExportManifestView;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.TemplateExportAssetKeyManifestItemView;
import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.api.TemplateExportClauseSnapshotView;
import com.bank.docgen.template.api.TemplateExportMasterPinView;
import com.bank.docgen.template.api.TemplateExportMetadataView;
import com.bank.docgen.template.domain.TemplateDependencyClosure;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.service.TemplateExportAccessService;
import com.bank.docgen.template.service.TemplateExportService;
import com.bank.docgen.template.service.TemplateExportV2Support;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
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
abstract class LibraryExportServiceTestFixtures {

    @Mock
    protected TemplateRepository templateRepository;
    @Mock
    protected BusinessGroupRepository businessGroupRepository;
    @Mock
    protected TemplateExportService templateExportService;
    @Mock
    protected ManagementAuditRecorder managementAuditRecorder;
    protected TemplateExportAccessService exportAccessService;
    protected LibraryExportService service;
    protected ObjectMapper objectMapper;
    protected UUID templateAId;
    protected UUID templateBId;
    protected UUID draftId;
    protected UUID masterId;
    protected TemplateEntity templateA;
    protected TemplateEntity templateB;
    protected TemplateEntity draft;
    protected ManagementSessionClaims groupAdmin;
    protected ManagementSessionClaims globalAdmin;
    protected ManagementSessionClaims author;
    protected ManagementSessionClaims tester;
    protected byte[] masterBytes;
    protected String masterHash;
    protected static Set<String> zipEntryNames(byte[] zipBytes) throws Exception {
        Set<String> names = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        }
        return names;
    }
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        exportAccessService = new TemplateExportAccessService(new GroupAccessService());
        service = new LibraryExportService(
                templateRepository,
                businessGroupRepository,
                templateExportService,
                exportAccessService,
                new GroupAccessService(),
                managementAuditRecorder,
                objectMapper
        );
        templateAId = UUID.randomUUID();
        templateBId = UUID.randomUUID();
        draftId = UUID.randomUUID();
        masterId = UUID.randomUUID();
        masterBytes = "SHARED-MASTER-DOCX".getBytes();
        masterHash = sha256Hex(masterBytes);
        templateA = published("TPL-A", templateAId, "RETAIL", "10000003");
        templateB = published("TPL-B", templateBId, "RETAIL", "10000003");
        draft = published("TPL-DRAFT", draftId, "RETAIL", "10000003");
        draft.setLifecycleStatus(TemplateLifecycleStatus.DRAFT);
        groupAdmin = session("10000002", List.of("GROUP_ADMIN"), List.of("RETAIL"));
        globalAdmin = session("10000001", List.of("GLOBAL_ADMIN"), List.of("*"));
        author = session("10000003", List.of("DOCUMENT_AUTHOR"), List.of("RETAIL"));
        tester = session("10000006", List.of("TEMPLATE_TESTER"), List.of("RETAIL"));
    }
    protected void stubV2Build(
            UUID templateId,
            String externalId,
            String hash,
            byte[] bytes,
            List<TemplateExportClauseSnapshotView> clauses,
            List<TemplateExportAssetKeyManifestItemView> assets
    ) throws Exception {
        TemplateExportBundleView bundle = v2Bundle(templateId, externalId, hash, clauses, assets);
        // doReturn avoids PotentialStubbingProblem when tests already stubbed other templateIds
        org.mockito.Mockito.doReturn(new TemplateExportService.BuiltV2Export(bundle, bytes, minimalV2Zip(bytes)))
                .when(templateExportService)
                .buildV2ExportWithoutAudit(eq(templateId), any(), nullable(TemplateDependencyClosure.class));
    }
    protected TemplateExportBundleView v2Bundle(
            UUID templateId,
            String externalId,
            String hash,
            List<TemplateExportClauseSnapshotView> clauses,
            List<TemplateExportAssetKeyManifestItemView> assets
    ) {
        return new TemplateExportBundleView(
                TemplateExportV2Support.EXPORT_FORMAT_V2,
                new TemplateExportMetadataView(
                        templateId.toString(),
                        externalId,
                        "RETAIL",
                        externalId,
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
                new TemplateExportMasterPinView(UUID.randomUUID().toString(), hash, 1, "PUBLISHED"),
                clauses,
                null,
                assets
        );
    }
    protected static TemplateExportClauseSnapshotView clauseSnapshot(String moduleCode, String semanticVersion) {
        return new TemplateExportClauseSnapshotView(
                moduleCode,
                UUID.randomUUID().toString(),
                1,
                "{\"nodes\":[]}",
                true,
                null,
                null,
                null,
                null,
                semanticVersion,
                UUID.randomUUID().toString()
        );
    }
    protected TemplateEntity published(String externalId, UUID id, String groupCode, String createdBy) {
        TemplateEntity entity = new TemplateEntity(
                id,
                externalId,
                groupCode,
                externalId,
                null,
                masterId,
                createdBy
        );
        entity.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        entity.setReleaseVersion("1.0.0");
        return entity;
    }
    protected LibraryExportManifestView readManifest(byte[] zipBytes) throws Exception {
        return objectMapper.readValue(
                readZipEntry(zipBytes, LibraryExportService.MANIFEST_ENTRY),
                LibraryExportManifestView.class
        );
    }
    protected static byte[] readZipEntry(byte[] zipBytes, String name) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (name.equals(entry.getName())) {
                    return zip.readAllBytes();
                }
            }
        }
        throw new AssertionError("Missing zip entry: " + name);
    }
    protected static byte[] minimalV2Zip(byte[] masterDocx) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("template-export-bundle.json"));
            zip.write("{\"format\":\"template-export-bundle-v2-json\"}".getBytes());
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(TemplateExportV2Support.ZIP_MASTER_ENTRY));
            zip.write(masterDocx);
            zip.closeEntry();
        }
        return out.toByteArray();
    }
    protected static ManagementSessionClaims session(
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
    protected static String sha256Hex(byte[] bytes) {
        try {
            byte[] hashed = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
