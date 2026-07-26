package com.bank.docgen.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.bank.docgen.authorization.management.domain.GroupDimension;
import com.bank.docgen.authorization.management.persistence.BusinessGroupEntity;
import com.bank.docgen.library.api.LibraryExportManifestView;
import com.bank.docgen.library.api.LibraryExportRequest;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.service.TemplateAccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Peeled from LibraryExportServiceTest (AI-SCALE #169).
 */
class LibraryExportServiceAccessFilterTest extends LibraryExportServiceTestFixtures {

    @Test
    void libraryExport_skipsDraft() throws Exception {
        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List.of("RETAIL")))
                .thenReturn(List.of(templateA, draft));
        stubV2Build(templateAId, "TPL-A", masterHash, masterBytes, List.of(), List.of());

        try (LibraryExportService.LibraryExportZipArtifact artifact =
                service.exportLibrary(new LibraryExportRequest(null, null, true), groupAdmin)) {
            byte[] zipBytes = artifact.readAllBytes();
            LibraryExportManifestView manifest = readManifest(zipBytes);
            assertThat(manifest.counts().includedCount()).isEqualTo(1);
            assertThat(manifest.counts().skippedCount()).isEqualTo(1);
            assertThat(manifest.templates()).anyMatch(t ->
                    t.templateId().equals(draftId.toString())
                            && "SKIPPED".equals(t.status())
                            && "EXPORT_NOT_ELIGIBLE".equals(t.reasonCode()));
            assertThat(zipEntryNames(zipBytes)).doesNotContain("templates/" + draftId + ".zip");
            verify(templateExportService, never()).buildV2ExportWithoutAudit(eq(draftId), any(), any());
        }
    }
    @Test
    void libraryExport_templateIdsFilter() throws Exception {
        when(templateRepository.findByIdInAndDeletedAtIsNull(List.of(templateAId, templateBId)))
                .thenReturn(List.of(templateA, templateB));
        stubV2Build(templateAId, "TPL-A", masterHash, masterBytes, List.of(), List.of());
        stubV2Build(templateBId, "TPL-B", masterHash, masterBytes, List.of(), List.of());

        try (LibraryExportService.LibraryExportZipArtifact artifact = service.exportLibrary(
                new LibraryExportRequest(null, List.of(templateAId, templateBId), true),
                groupAdmin
        )) {
            LibraryExportManifestView manifest = readManifest(artifact.readAllBytes());
            assertThat(manifest.scope().selection()).isEqualTo("TEMPLATE_IDS");
            assertThat(manifest.counts().includedCount()).isEqualTo(2);
            assertThat(manifest.templates()).extracting(t -> t.templateId())
                    .containsExactlyInAnyOrder(templateAId.toString(), templateBId.toString());
        }
    }
    @Test
    void libraryExport_omitsUnauthorizedIds() throws Exception {
        UUID foreignId = UUID.randomUUID();
        when(templateRepository.findByIdInAndDeletedAtIsNull(List.of(foreignId, templateAId)))
                .thenReturn(List.of(templateA));
        stubV2Build(templateAId, "TPL-A", masterHash, masterBytes, List.of(), List.of());

        try (LibraryExportService.LibraryExportZipArtifact artifact = service.exportLibrary(
                new LibraryExportRequest(null, List.of(foreignId, templateAId), true),
                groupAdmin
        )) {
            LibraryExportManifestView manifest = readManifest(artifact.readAllBytes());
            assertThat(manifest.templates()).extracting(t -> t.templateId())
                    .containsExactly(templateAId.toString())
                    .doesNotContain(foreignId.toString());
            assertThat(manifest.counts().omittedUnauthorizedOrUnknownCount()).isEqualTo(1);
        }
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
    void libraryExport_groupAdminScoped() throws Exception {
        TemplateEntity corporate = published("TPL-CORP", UUID.randomUUID(), "CORPORATE", "10000001");
        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List.of("RETAIL")))
                .thenReturn(List.of(templateA));
        stubV2Build(templateAId, "TPL-A", masterHash, masterBytes, List.of(), List.of());

        try (LibraryExportService.LibraryExportZipArtifact artifact =
                service.exportLibrary(new LibraryExportRequest(null, null, true), groupAdmin)) {
            LibraryExportManifestView manifest = readManifest(artifact.readAllBytes());
            assertThat(manifest.templates()).extracting(t -> t.templateId())
                    .containsExactly(templateAId.toString())
                    .doesNotContain(corporate.getId().toString());
        }
    }
    @Test
    void libraryExport_authorOnlyOwned() throws Exception {
        TemplateEntity otherOwned = published("TPL-OTHER", UUID.randomUUID(), "RETAIL", "10000099");
        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List.of("RETAIL")))
                .thenReturn(List.of(templateA, otherOwned));
        stubV2Build(templateAId, "TPL-A", masterHash, masterBytes, List.of(), List.of());

        try (LibraryExportService.LibraryExportZipArtifact artifact =
                service.exportLibrary(new LibraryExportRequest(null, null, true), author)) {
            LibraryExportManifestView manifest = readManifest(artifact.readAllBytes());
            assertThat(manifest.templates()).extracting(t -> t.templateId())
                    .containsExactly(templateAId.toString())
                    .doesNotContain(otherOwned.getId().toString());
        }
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

        try (LibraryExportService.LibraryExportZipArtifact artifact = service.exportLibrary(
                new LibraryExportRequest(groupId, null, true),
                globalAdmin
        )) {
            LibraryExportManifestView manifest = readManifest(artifact.readAllBytes());
            assertThat(manifest.scope().selection()).isEqualTo("GROUP");
            assertThat(manifest.scope().groupId()).isEqualTo(groupId.toString());
            assertThat(manifest.counts().includedCount()).isEqualTo(2);
        }
    }
}
