package com.bank.docgen.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.library.api.LibraryExportManifestView;
import com.bank.docgen.library.api.LibraryExportRequest;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.template.api.TemplateExportAssetKeyManifestItemView;
import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.api.TemplateExportClauseSnapshotView;
import com.bank.docgen.template.domain.TemplateExportAssetKeyUsage;
import com.bank.docgen.template.service.TemplateExportService;
import com.bank.docgen.template.service.TemplateExportV2Support;
import com.bank.docgen.template.service.TemplateGovernanceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Peeled from LibraryExportServiceTest (AI-SCALE #169).
 */
class LibraryExportServiceZipArtifactTest extends LibraryExportServiceTestFixtures {

    @Test
    void libraryExport_zipContainsManifestAndNestedV2() throws Exception {
        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List.of("RETAIL")))
                .thenReturn(List.of(templateA, templateB));
        stubV2Build(templateAId, "TPL-A", masterHash, masterBytes, List.of(), List.of());
        stubV2Build(templateBId, "TPL-B", masterHash, masterBytes, List.of(), List.of());

        try (LibraryExportService.LibraryExportZipArtifact artifact =
                service.exportLibrary(new LibraryExportRequest(null, null, true), groupAdmin)) {
            byte[] zipBytes = artifact.readAllBytes();
            Set<String> names = zipEntryNames(zipBytes);
            assertThat(names).contains(
                    LibraryExportService.MANIFEST_ENTRY,
                    "templates/" + templateAId + ".zip",
                    "templates/" + templateBId + ".zip",
                    "masters/" + masterHash + ".docx"
            );
            LibraryExportManifestView manifest = readManifest(zipBytes);
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
    }
    @Test
    void libraryExport_dedupesMastersAndClauses() throws Exception {
        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List.of("RETAIL")))
                .thenReturn(List.of(templateA, templateB));
        TemplateExportClauseSnapshotView clause = clauseSnapshot("MOD-1", "1.0.0");
        stubV2Build(templateAId, "TPL-A", masterHash, masterBytes, List.of(clause), List.of());
        stubV2Build(templateBId, "TPL-B", masterHash, masterBytes, List.of(clause), List.of());

        try (LibraryExportService.LibraryExportZipArtifact artifact =
                service.exportLibrary(new LibraryExportRequest(null, null, true), groupAdmin)) {
            byte[] zipBytes = artifact.readAllBytes();
            Set<String> names = zipEntryNames(zipBytes);
            long masterEntries = names.stream().filter(n -> n.startsWith("masters/")).count();
            long clauseEntries = names.stream().filter(n -> n.startsWith("clauses/")).count();
            assertThat(masterEntries).isEqualTo(1);
            assertThat(clauseEntries).isEqualTo(1);
            LibraryExportManifestView manifest = readManifest(zipBytes);
            assertThat(manifest.masterCatalog()).hasSize(1);
            assertThat(manifest.masterCatalog().getFirst().sourceTemplateIds())
                    .containsExactlyInAnyOrder(templateAId.toString(), templateBId.toString());
            assertThat(manifest.clauseCatalog()).hasSize(1);
            assertThat(manifest.clauseCatalog().getFirst().sourceTemplateIds())
                    .containsExactlyInAnyOrder(templateAId.toString(), templateBId.toString());
            assertThat(manifest.counts().uniqueMasterCount()).isEqualTo(1);
            assertThat(manifest.counts().uniqueClauseCount()).isEqualTo(1);
        }
    }
    @Test
    void libraryExport_doesNotRetainAllNestedZipsOnHeap() throws Exception {
        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List.of("RETAIL")))
                .thenReturn(List.of(templateA, templateB));
        stubV2Build(templateAId, "TPL-A", masterHash, masterBytes, List.of(), List.of());
        stubV2Build(templateBId, "TPL-B", masterHash, masterBytes, List.of(), List.of());

        AtomicInteger nestedWrites = new AtomicInteger();
        AtomicInteger peakRetainedNestedMapEntries = new AtomicInteger();
        LibraryExportService.LibraryExportAssemblyProbe probe = retained -> {
            nestedWrites.incrementAndGet();
            peakRetainedNestedMapEntries.updateAndGet(current -> Math.max(current, retained));
        };
        service = new LibraryExportService(
                templateRepository,
                businessGroupRepository,
                templateExportService,
                exportAccessService,
                new GroupAccessService(),
                managementAuditRecorder,
                objectMapper,
                probe,
                LibraryExportService.LibraryExportTempZipFactory.SYSTEM
        );

        try (LibraryExportService.LibraryExportZipArtifact artifact =
                service.exportLibrary(new LibraryExportRequest(null, null, true), groupAdmin)) {
            assertThat(artifact.contentPath()).isNotNull();
            assertThat(Files.isRegularFile(artifact.contentPath())).isTrue();
            assertThat(nestedWrites.get()).isEqualTo(2);
            assertThat(peakRetainedNestedMapEntries.get()).isZero();
            assertThat(LibraryExportService.LibraryExportZipArtifact.class.isRecord()).isFalse();
            assertThat(LibraryExportService.LibraryExportZipArtifact.class.getMethods())
                    .extracting(java.lang.reflect.Method::getName)
                    .doesNotContain("content");
            assertThat(artifact.readAllBytes()).isNotEmpty();
        }
    }
    @Test
    void libraryExport_tempFileDeletedOnSuccess() throws Exception {
        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List.of("RETAIL")))
                .thenReturn(List.of(templateA));
        stubV2Build(templateAId, "TPL-A", masterHash, masterBytes, List.of(), List.of());

        AtomicReference<Path> created = new AtomicReference<>();
        LibraryExportService.LibraryExportTempZipFactory factory = () -> {
            Path path = Files.createTempFile("dge-library-export-test-", ".zip");
            created.set(path);
            return path;
        };
        service = new LibraryExportService(
                templateRepository,
                businessGroupRepository,
                templateExportService,
                exportAccessService,
                new GroupAccessService(),
                managementAuditRecorder,
                objectMapper,
                LibraryExportService.LibraryExportAssemblyProbe.NOOP,
                factory
        );

        LibraryExportService.LibraryExportZipArtifact artifact =
                service.exportLibrary(new LibraryExportRequest(null, null, true), groupAdmin);
        assertThat(Files.isRegularFile(created.get())).isTrue();
        artifact.close();
        assertThat(Files.exists(created.get())).isFalse();
    }
    @Test
    void libraryExport_tempFileDeletedOnEmpty422() {
        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List.of("RETAIL")))
                .thenReturn(List.of(draft));

        AtomicInteger tempCreates = new AtomicInteger();
        LibraryExportService.LibraryExportTempZipFactory factory = () -> {
            tempCreates.incrementAndGet();
            return Files.createTempFile("dge-library-export-test-", ".zip");
        };
        service = new LibraryExportService(
                templateRepository,
                businessGroupRepository,
                templateExportService,
                exportAccessService,
                new GroupAccessService(),
                managementAuditRecorder,
                objectMapper,
                LibraryExportService.LibraryExportAssemblyProbe.NOOP,
                factory
        );

        assertThatThrownBy(() -> service.exportLibrary(new LibraryExportRequest(null, null, true), groupAdmin))
                .isInstanceOf(LibraryExportValidationException.class)
                .satisfies(ex -> {
                    LibraryExportValidationException vex = (LibraryExportValidationException) ex;
                    assertThat(vex.errorCode()).isEqualTo(ApiErrorCodes.LIBRARY_EXPORT_EMPTY);
                });
        assertThat(tempCreates.get()).isZero();
    }
    @Test
    void libraryExport_tempFileDeletedWhenAllEligibleFail() throws Exception {
        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List.of("RETAIL")))
                .thenReturn(List.of(templateA));
        when(templateExportService.buildV2ExportWithoutAudit(eq(templateAId), eq(groupAdmin), isNull()))
                .thenThrow(new TemplateGovernanceException(
                        ApiErrorCodes.PINNED_MASTER_UNAVAILABLE,
                        "api.error.rendering.pinnedMasterUnavailable",
                        HttpStatus.UNPROCESSABLE_ENTITY
                ));

        AtomicReference<Path> created = new AtomicReference<>();
        LibraryExportService.LibraryExportTempZipFactory factory = () -> {
            Path path = Files.createTempFile("dge-library-export-test-", ".zip");
            created.set(path);
            return path;
        };
        service = new LibraryExportService(
                templateRepository,
                businessGroupRepository,
                templateExportService,
                exportAccessService,
                new GroupAccessService(),
                managementAuditRecorder,
                objectMapper,
                LibraryExportService.LibraryExportAssemblyProbe.NOOP,
                factory
        );

        assertThatThrownBy(() -> service.exportLibrary(new LibraryExportRequest(null, null, true), groupAdmin))
                .isInstanceOf(LibraryExportValidationException.class)
                .satisfies(ex -> {
                    LibraryExportValidationException vex = (LibraryExportValidationException) ex;
                    assertThat(vex.errorCode()).isEqualTo(ApiErrorCodes.LIBRARY_EXPORT_EMPTY);
                });
        assertThat(created.get()).isNotNull();
        assertThat(Files.exists(created.get())).isFalse();
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

        try (LibraryExportService.LibraryExportZipArtifact artifact =
                service.exportLibrary(new LibraryExportRequest(null, null, true), groupAdmin)) {
            byte[] zipBytes = artifact.readAllBytes();
            LibraryExportManifestView manifest = readManifest(zipBytes);
            assertThat(manifest.assetKeyManifest()).hasSize(1);
            assertThat(manifest.assetKeyManifest().getFirst().referenceKey()).isEqualTo("LOGO-1");
            assertThat(zipEntryNames(zipBytes).stream().noneMatch(n -> n.startsWith("assets/")))
                    .isTrue();
            assertThat(manifest.counts().uniqueAssetKeyCount()).isEqualTo(1);
        }
    }
    @Test
    void libraryExport_assembleIoFailure_usesExportFailedNotEmpty() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsBytes(any()))
                .thenThrow(new JsonProcessingException("assemble-failed") { });
        AtomicReference<Path> created = new AtomicReference<>();
        LibraryExportService.LibraryExportTempZipFactory factory = () -> {
            Path path = Files.createTempFile("dge-library-export-test-", ".zip");
            created.set(path);
            return path;
        };
        service = new LibraryExportService(
                templateRepository,
                businessGroupRepository,
                templateExportService,
                exportAccessService,
                new GroupAccessService(),
                managementAuditRecorder,
                failingMapper,
                LibraryExportService.LibraryExportAssemblyProbe.NOOP,
                factory
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
        assertThat(created.get()).isNotNull();
        assertThat(Files.exists(created.get())).isFalse();
    }
    @Test
    void libraryExport_onePinnedMasterMissing_othersIncluded() throws Exception {
        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List.of("RETAIL")))
                .thenReturn(List.of(templateA, templateB));
        when(templateExportService.buildV2ExportWithoutAudit(eq(templateAId), eq(groupAdmin), isNull()))
                .thenThrow(new TemplateGovernanceException(
                        ApiErrorCodes.PINNED_MASTER_UNAVAILABLE,
                        "api.error.rendering.pinnedMasterUnavailable",
                        HttpStatus.UNPROCESSABLE_ENTITY
                ));
        stubV2Build(templateBId, "TPL-B", masterHash, masterBytes, List.of(), List.of());

        try (LibraryExportService.LibraryExportZipArtifact artifact =
                service.exportLibrary(new LibraryExportRequest(null, null, true), groupAdmin)) {
            byte[] zipBytes = artifact.readAllBytes();
            LibraryExportManifestView manifest = readManifest(zipBytes);
            assertThat(manifest.counts().includedCount()).isEqualTo(1);
            assertThat(manifest.counts().failedCount()).isEqualTo(1);
            assertThat(manifest.templates()).anyMatch(t ->
                    t.templateId().equals(templateAId.toString())
                            && "FAILED".equals(t.status())
                            && "PINNED_MASTER_UNAVAILABLE".equals(t.reasonCode()));
            assertThat(zipEntryNames(zipBytes))
                    .contains("templates/" + templateBId + ".zip")
                    .doesNotContain("templates/" + templateAId + ".zip");
        }
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
        when(templateExportService.buildV2ExportWithoutAudit(eq(templateAId), eq(groupAdmin), isNull()))
                .thenReturn(new TemplateExportService.BuiltV2Export(bundle, masterBytes, nestedZip));
        when(templateExportService.exportZip(templateAId, groupAdmin, 2))
                .thenReturn(new TemplateExportService.TemplateExportZipArtifact("TPL-A-export.zip", nestedZip));

        try (LibraryExportService.LibraryExportZipArtifact artifact =
                service.exportLibrary(new LibraryExportRequest(null, null, true), groupAdmin)) {
            byte[] fromLibrary = readZipEntry(artifact.readAllBytes(), "templates/" + templateAId + ".zip");
            TemplateExportService.TemplateExportZipArtifact single =
                    templateExportService.exportZip(templateAId, groupAdmin, 2);
            assertThat(sha256Hex(fromLibrary)).isEqualTo(sha256Hex(single.content()));
            assertThat(readZipEntry(fromLibrary, TemplateExportV2Support.ZIP_MASTER_ENTRY)).isEqualTo(masterBytes);
        }
    }
    @Test
    void libraryExport_stripsSecretsFromManifest() throws Exception {
        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List.of("RETAIL")))
                .thenReturn(List.of(templateA));
        stubV2Build(templateAId, "TPL-A", masterHash, masterBytes, List.of(), List.of());

        try (LibraryExportService.LibraryExportZipArtifact artifact =
                service.exportLibrary(new LibraryExportRequest(null, null, true), groupAdmin)) {
            String manifestJson = new String(readZipEntry(artifact.readAllBytes(), LibraryExportService.MANIFEST_ENTRY));
            assertThat(manifestJson.toLowerCase())
                    .doesNotContain("clientsecret")
                    .doesNotContain("credentialid")
                    .doesNotContain("password");
        }
    }
}
