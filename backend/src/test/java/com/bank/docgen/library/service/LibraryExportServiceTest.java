package com.bank.docgen.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.domain.GroupDimension;
import com.bank.docgen.authorization.management.persistence.BusinessGroupEntity;
import com.bank.docgen.authorization.management.persistence.BusinessGroupRepository;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.library.api.LibraryExportManifestView;
import com.bank.docgen.library.api.LibraryExportRequest;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.TemplateExportAssetKeyManifestItemView;
import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.api.TemplateExportClauseSnapshotView;
import com.bank.docgen.template.api.TemplateExportMasterPinView;
import com.bank.docgen.template.api.TemplateExportMetadataView;
import com.bank.docgen.template.domain.TemplateExportAssetKeyUsage;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.service.TemplateAccessDeniedException;
import com.bank.docgen.template.service.TemplateExportAccessService;
import com.bank.docgen.template.service.TemplateExportService;
import com.bank.docgen.template.service.TemplateExportV2Support;
import com.bank.docgen.template.service.TemplateGovernanceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class LibraryExportServiceTest {

    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private BusinessGroupRepository businessGroupRepository;
    @Mock
    private TemplateExportService templateExportService;
    @Mock
    private ManagementAuditRecorder managementAuditRecorder;

    private TemplateExportAccessService exportAccessService;
    private LibraryExportService service;
    private ObjectMapper objectMapper;

    private UUID templateAId;
    private UUID templateBId;
    private UUID draftId;
    private UUID masterId;
    private TemplateEntity templateA;
    private TemplateEntity templateB;
    private TemplateEntity draft;
    private ManagementSessionClaims groupAdmin;
    private ManagementSessionClaims globalAdmin;
    private ManagementSessionClaims author;
    private ManagementSessionClaims tester;
    private byte[] masterBytes;
    private String masterHash;

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
        author = session("10000003", List.of("TEMPLATE_AUTHOR"), List.of("RETAIL"));
        tester = session("10000006", List.of("TEMPLATE_TESTER"), List.of("RETAIL"));
    }

    @Test
    void libraryExport_zipContainsManifestAndNestedV2() throws Exception {
        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List.of("RETAIL")))
                .thenReturn(List.of(templateA, templateB));
        stubV2Build(templateAId, "TPL-A", masterHash, masterBytes, List.of(), List.of());
        stubV2Build(templateBId, "TPL-B", masterHash, masterBytes, List.of(), List.of());

        LibraryExportService.LibraryExportZipArtifact artifact =
                service.exportLibrary(new LibraryExportRequest(null, null, true), groupAdmin);

        Set<String> names = zipEntryNames(artifact.content());
        assertThat(names).contains(
                LibraryExportService.MANIFEST_ENTRY,
                "templates/" + templateAId + ".zip",
                "templates/" + templateBId + ".zip",
                "masters/" + masterHash + ".docx"
        );
        LibraryExportManifestView manifest = readManifest(artifact.content());
        assertThat(manifest.format()).isEqualTo(LibraryExportService.LIBRARY_EXPORT_FORMAT);
        assertThat(manifest.bundleVersion()).isEqualTo(2);
        assertThat(manifest.counts().includedCount()).isEqualTo(2);
        assertThat(manifest.counts().uniqueMasterCount()).isEqualTo(1);
        verify(managementAuditRecorder).recordLibraryExport(
                eq(manifest.exportBatchId()),
                eq("ALL_AUTHORIZED"),
                eq(2),
                eq(0),
                eq(0),
                eq(0),
                eq("10000002"),
                any()
        );
    }

    @Test
    void libraryExport_dedupesMastersAndClauses() throws Exception {
        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List.of("RETAIL")))
                .thenReturn(List.of(templateA, templateB));
        TemplateExportClauseSnapshotView clause = clauseSnapshot("MOD-1", "1.0.0");
        stubV2Build(templateAId, "TPL-A", masterHash, masterBytes, List.of(clause), List.of());
        stubV2Build(templateBId, "TPL-B", masterHash, masterBytes, List.of(clause), List.of());

        LibraryExportService.LibraryExportZipArtifact artifact =
                service.exportLibrary(new LibraryExportRequest(null, null, true), groupAdmin);

        Set<String> names = zipEntryNames(artifact.content());
        long masterEntries = names.stream().filter(n -> n.startsWith("masters/")).count();
        long clauseEntries = names.stream().filter(n -> n.startsWith("clauses/")).count();
        assertThat(masterEntries).isEqualTo(1);
        assertThat(clauseEntries).isEqualTo(1);
        LibraryExportManifestView manifest = readManifest(artifact.content());
        assertThat(manifest.masterCatalog()).hasSize(1);
        assertThat(manifest.masterCatalog().getFirst().sourceTemplateIds())
                .containsExactlyInAnyOrder(templateAId.toString(), templateBId.toString());
        assertThat(manifest.clauseCatalog()).hasSize(1);
        assertThat(manifest.clauseCatalog().getFirst().sourceTemplateIds())
                .containsExactlyInAnyOrder(templateAId.toString(), templateBId.toString());
        assertThat(manifest.counts().uniqueMasterCount()).isEqualTo(1);
        assertThat(manifest.counts().uniqueClauseCount()).isEqualTo(1);
    }

    @Test
    void libraryExport_aggregatesAssetKeysWithoutBinaries() throws Exception {
        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List.of("RETAIL")))
                .thenReturn(List.of(templateA));
        stubV2Build(
                templateAId,
                "TPL-A",
                masterHash,
                masterBytes,
                List.of(),
                List.of(new TemplateExportAssetKeyManifestItemView("LOGO-1", TemplateExportAssetKeyUsage.IMAGE))
        );

        LibraryExportService.LibraryExportZipArtifact artifact =
                service.exportLibrary(new LibraryExportRequest(null, null, true), groupAdmin);

        LibraryExportManifestView manifest = readManifest(artifact.content());
        assertThat(manifest.assetKeyManifest()).hasSize(1);
        assertThat(manifest.assetKeyManifest().getFirst().referenceKey()).isEqualTo("LOGO-1");
        assertThat(zipEntryNames(artifact.content()).stream().noneMatch(n -> n.startsWith("assets/")))
                .isTrue();
        assertThat(manifest.counts().uniqueAssetKeyCount()).isEqualTo(1);
    }

    @Test
    void libraryExport_skipsDraft() throws Exception {
        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List.of("RETAIL")))
                .thenReturn(List.of(templateA, draft));
        stubV2Build(templateAId, "TPL-A", masterHash, masterBytes, List.of(), List.of());

        LibraryExportService.LibraryExportZipArtifact artifact =
                service.exportLibrary(new LibraryExportRequest(null, null, true), groupAdmin);

        LibraryExportManifestView manifest = readManifest(artifact.content());
        assertThat(manifest.counts().includedCount()).isEqualTo(1);
        assertThat(manifest.counts().skippedCount()).isEqualTo(1);
        assertThat(manifest.templates()).anyMatch(t ->
                t.templateId().equals(draftId.toString())
                        && "SKIPPED".equals(t.status())
                        && "EXPORT_NOT_ELIGIBLE".equals(t.reasonCode()));
        assertThat(zipEntryNames(artifact.content())).doesNotContain("templates/" + draftId + ".zip");
        verify(templateExportService, never()).buildV2ExportWithoutAudit(eq(draftId), any());
    }

    @Test
    void libraryExport_templateIdsFilter() throws Exception {
        when(templateRepository.findByIdInAndDeletedAtIsNull(List.of(templateAId, templateBId)))
                .thenReturn(List.of(templateA, templateB));
        stubV2Build(templateAId, "TPL-A", masterHash, masterBytes, List.of(), List.of());
        stubV2Build(templateBId, "TPL-B", masterHash, masterBytes, List.of(), List.of());

        LibraryExportService.LibraryExportZipArtifact artifact = service.exportLibrary(
                new LibraryExportRequest(null, List.of(templateAId, templateBId), true),
                groupAdmin
        );

        LibraryExportManifestView manifest = readManifest(artifact.content());
        assertThat(manifest.scope().selection()).isEqualTo("TEMPLATE_IDS");
        assertThat(manifest.counts().includedCount()).isEqualTo(2);
        assertThat(manifest.templates()).extracting(t -> t.templateId())
                .containsExactlyInAnyOrder(templateAId.toString(), templateBId.toString());
    }

    @Test
    void libraryExport_omitsUnauthorizedIds() throws Exception {
        UUID foreignId = UUID.randomUUID();
        when(templateRepository.findByIdInAndDeletedAtIsNull(List.of(foreignId, templateAId)))
                .thenReturn(List.of(templateA));
        stubV2Build(templateAId, "TPL-A", masterHash, masterBytes, List.of(), List.of());

        LibraryExportService.LibraryExportZipArtifact artifact = service.exportLibrary(
                new LibraryExportRequest(null, List.of(foreignId, templateAId), true),
                groupAdmin
        );

        LibraryExportManifestView manifest = readManifest(artifact.content());
        assertThat(manifest.templates()).extracting(t -> t.templateId())
                .containsExactly(templateAId.toString())
                .doesNotContain(foreignId.toString());
        assertThat(manifest.counts().omittedUnauthorizedOrUnknownCount()).isEqualTo(1);
    }

    @Test
    void libraryExport_empty_422() {
        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List.of("RETAIL")))
                .thenReturn(List.of(draft));

        assertThatThrownBy(() -> service.exportLibrary(new LibraryExportRequest(null, null, true), groupAdmin))
                .isInstanceOf(LibraryExportValidationException.class)
                .satisfies(ex -> {
                    LibraryExportValidationException vex = (LibraryExportValidationException) ex;
                    assertThat(vex.errorCode()).isEqualTo(ApiErrorCodes.LIBRARY_EXPORT_EMPTY);
                    assertThat(vex.messageKey()).isEqualTo("api.error.library.exportEmpty");
                });
        verify(managementAuditRecorder, never()).recordLibraryExport(
                any(), any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(), any(), any()
        );
    }

    @Test
    void libraryExport_limitExceeded_422() {
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            ids.add(UUID.randomUUID());
        }

        assertThatThrownBy(() ->
                service.exportLibrary(new LibraryExportRequest(null, ids, true), groupAdmin))
                .isInstanceOf(LibraryExportValidationException.class)
                .satisfies(ex -> {
                    LibraryExportValidationException vex = (LibraryExportValidationException) ex;
                    assertThat(vex.errorCode()).isEqualTo(ApiErrorCodes.LIBRARY_EXPORT_LIMIT_EXCEEDED);
                    assertThat(vex.messageKey()).isEqualTo("api.error.library.exportLimitExceeded");
                });
    }

    @Test
    void libraryExport_assembleIoFailure_usesExportFailedNotEmpty() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsBytes(any()))
                .thenThrow(new JsonProcessingException("assemble-failed") { });
        service = new LibraryExportService(
                templateRepository,
                businessGroupRepository,
                templateExportService,
                exportAccessService,
                new GroupAccessService(),
                managementAuditRecorder,
                failingMapper
        );
        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List.of("RETAIL")))
                .thenReturn(List.of(templateA));
        stubV2Build(templateAId, "TPL-A", masterHash, masterBytes, List.of(), List.of());

        assertThatThrownBy(() -> service.exportLibrary(new LibraryExportRequest(null, null, true), groupAdmin))
                .isInstanceOf(LibraryExportValidationException.class)
                .satisfies(ex -> {
                    LibraryExportValidationException vex = (LibraryExportValidationException) ex;
                    assertThat(vex.errorCode()).isEqualTo(ApiErrorCodes.LIBRARY_EXPORT_FAILED);
                    assertThat(vex.messageKey()).isEqualTo("api.error.library.exportFailed");
                });
        verify(managementAuditRecorder, never()).recordLibraryExport(
                any(), any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(), any(), any()
        );
    }

    @Test
    void libraryExport_onePinnedMasterMissing_othersIncluded() throws Exception {
        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List.of("RETAIL")))
                .thenReturn(List.of(templateA, templateB));
        when(templateExportService.buildV2ExportWithoutAudit(templateAId, groupAdmin))
                .thenThrow(new TemplateGovernanceException(
                        ApiErrorCodes.PINNED_MASTER_UNAVAILABLE,
                        "api.error.rendering.pinnedMasterUnavailable",
                        HttpStatus.UNPROCESSABLE_ENTITY
                ));
        stubV2Build(templateBId, "TPL-B", masterHash, masterBytes, List.of(), List.of());

        LibraryExportService.LibraryExportZipArtifact artifact =
                service.exportLibrary(new LibraryExportRequest(null, null, true), groupAdmin);

        LibraryExportManifestView manifest = readManifest(artifact.content());
        assertThat(manifest.counts().includedCount()).isEqualTo(1);
        assertThat(manifest.counts().failedCount()).isEqualTo(1);
        assertThat(manifest.templates()).anyMatch(t ->
                t.templateId().equals(templateAId.toString())
                        && "FAILED".equals(t.status())
                        && "PINNED_MASTER_UNAVAILABLE".equals(t.reasonCode()));
        assertThat(zipEntryNames(artifact.content()))
                .contains("templates/" + templateBId + ".zip")
                .doesNotContain("templates/" + templateAId + ".zip");
    }

    @Test
    void libraryExport_groupAdminScoped() throws Exception {
        TemplateEntity corporate = published("TPL-CORP", UUID.randomUUID(), "CORPORATE", "10000001");
        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List.of("RETAIL")))
                .thenReturn(List.of(templateA));
        stubV2Build(templateAId, "TPL-A", masterHash, masterBytes, List.of(), List.of());

        LibraryExportService.LibraryExportZipArtifact artifact =
                service.exportLibrary(new LibraryExportRequest(null, null, true), groupAdmin);

        LibraryExportManifestView manifest = readManifest(artifact.content());
        assertThat(manifest.templates()).extracting(t -> t.templateId())
                .containsExactly(templateAId.toString())
                .doesNotContain(corporate.getId().toString());
    }

    @Test
    void libraryExport_authorOnlyOwned() throws Exception {
        TemplateEntity otherOwned = published("TPL-OTHER", UUID.randomUUID(), "RETAIL", "10000099");
        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List.of("RETAIL")))
                .thenReturn(List.of(templateA, otherOwned));
        stubV2Build(templateAId, "TPL-A", masterHash, masterBytes, List.of(), List.of());

        LibraryExportService.LibraryExportZipArtifact artifact =
                service.exportLibrary(new LibraryExportRequest(null, null, true), author);

        LibraryExportManifestView manifest = readManifest(artifact.content());
        assertThat(manifest.templates()).extracting(t -> t.templateId())
                .containsExactly(templateAId.toString())
                .doesNotContain(otherOwned.getId().toString());
    }

    @Test
    void libraryExport_forbiddenForTester() {
        assertThatThrownBy(() -> service.exportLibrary(new LibraryExportRequest(null, null, true), tester))
                .isInstanceOf(TemplateAccessDeniedException.class);
    }

    @Test
    void libraryExport_groupIdFilter() throws Exception {
        UUID groupId = UUID.randomUUID();
        BusinessGroupEntity group = new BusinessGroupEntity(groupId, "RETAIL", "Retail", GroupDimension.BUSINESS_LINE);
        when(businessGroupRepository.findByIdAndDeletedAtIsNull(groupId)).thenReturn(Optional.of(group));
        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List.of("RETAIL")))
                .thenReturn(List.of(templateA, templateB));
        stubV2Build(templateAId, "TPL-A", masterHash, masterBytes, List.of(), List.of());
        stubV2Build(templateBId, "TPL-B", masterHash, masterBytes, List.of(), List.of());

        LibraryExportService.LibraryExportZipArtifact artifact = service.exportLibrary(
                new LibraryExportRequest(groupId, null, true),
                globalAdmin
        );

        LibraryExportManifestView manifest = readManifest(artifact.content());
        assertThat(manifest.scope().selection()).isEqualTo("GROUP");
        assertThat(manifest.scope().groupId()).isEqualTo(groupId.toString());
        assertThat(manifest.counts().includedCount()).isEqualTo(2);
    }

    @Test
    void nestedZip_matchesSingleTemplateE01Export() throws Exception {
        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List.of("RETAIL")))
                .thenReturn(List.of(templateA));
        byte[] nestedZip = minimalV2Zip(masterBytes);
        TemplateExportBundleView bundle = v2Bundle(
                templateAId,
                "TPL-A",
                masterHash,
                List.of(),
                List.of()
        );
        when(templateExportService.buildV2ExportWithoutAudit(templateAId, groupAdmin))
                .thenReturn(new TemplateExportService.BuiltV2Export(bundle, masterBytes, nestedZip));
        when(templateExportService.exportZip(templateAId, groupAdmin, 2))
                .thenReturn(new TemplateExportService.TemplateExportZipArtifact("TPL-A-export.zip", nestedZip));

        LibraryExportService.LibraryExportZipArtifact artifact =
                service.exportLibrary(new LibraryExportRequest(null, null, true), groupAdmin);

        byte[] fromLibrary = readZipEntry(artifact.content(), "templates/" + templateAId + ".zip");
        TemplateExportService.TemplateExportZipArtifact single =
                templateExportService.exportZip(templateAId, groupAdmin, 2);
        assertThat(sha256Hex(fromLibrary)).isEqualTo(sha256Hex(single.content()));
        assertThat(readZipEntry(fromLibrary, TemplateExportV2Support.ZIP_MASTER_ENTRY)).isEqualTo(masterBytes);
    }

    @Test
    void libraryExport_stripsSecretsFromManifest() throws Exception {
        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List.of("RETAIL")))
                .thenReturn(List.of(templateA));
        stubV2Build(templateAId, "TPL-A", masterHash, masterBytes, List.of(), List.of());

        LibraryExportService.LibraryExportZipArtifact artifact =
                service.exportLibrary(new LibraryExportRequest(null, null, true), groupAdmin);

        String manifestJson = new String(readZipEntry(artifact.content(), LibraryExportService.MANIFEST_ENTRY));
        assertThat(manifestJson.toLowerCase())
                .doesNotContain("clientsecret")
                .doesNotContain("credentialid")
                .doesNotContain("password");
    }

    private void stubV2Build(
            UUID templateId,
            String externalId,
            String hash,
            byte[] bytes,
            List<TemplateExportClauseSnapshotView> clauses,
            List<TemplateExportAssetKeyManifestItemView> assets
    ) throws Exception {
        TemplateExportBundleView bundle = v2Bundle(templateId, externalId, hash, clauses, assets);
        when(templateExportService.buildV2ExportWithoutAudit(eq(templateId), any()))
                .thenReturn(new TemplateExportService.BuiltV2Export(bundle, bytes, minimalV2Zip(bytes)));
    }

    private TemplateExportBundleView v2Bundle(
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

    private static TemplateExportClauseSnapshotView clauseSnapshot(String moduleCode, String semanticVersion) {
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

    private TemplateEntity published(String externalId, UUID id, String groupCode, String createdBy) {
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

    private LibraryExportManifestView readManifest(byte[] zipBytes) throws Exception {
        return objectMapper.readValue(
                readZipEntry(zipBytes, LibraryExportService.MANIFEST_ENTRY),
                LibraryExportManifestView.class
        );
    }

    private static Set<String> zipEntryNames(byte[] zipBytes) throws Exception {
        Set<String> names = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        }
        return names;
    }

    private static byte[] readZipEntry(byte[] zipBytes, String name) throws Exception {
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

    private static byte[] minimalV2Zip(byte[] masterDocx) throws Exception {
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

    private static String sha256Hex(byte[] bytes) {
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
