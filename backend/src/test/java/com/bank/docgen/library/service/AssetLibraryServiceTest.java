package com.bank.docgen.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.infrastructure.storage.ObjectStorageException;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.library.api.AssetLibraryAssetView;
import com.bank.docgen.library.domain.AssetLibraryAssetClass;
import com.bank.docgen.library.domain.AssetLibraryAssetStatus;
import com.bank.docgen.library.persistence.LibraryAssetEntity;
import com.bank.docgen.library.persistence.LibraryAssetRepository;
import com.bank.docgen.rendering.StructuredContentImageResolver;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class AssetLibraryServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-16T01:00:00Z");
    private static final byte[] PNG_BYTES = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D
    };
    private static final byte[] JPEG_BYTES = new byte[] {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46
    };

    @Mock
    private LibraryAssetRepository repository;
    @Mock
    private ObjectStoragePort objectStoragePort;
    @Mock
    private ManagementAuditRecorder auditRecorder;

    private AssetLibraryService service;
    private ManagementSessionClaims author;
    private ManagementSessionClaims approver;
    private ManagementSessionClaims admin;
    private ManagementSessionClaims tester;
    private ManagementSessionClaims auditAdmin;

    @BeforeEach
    void setUp() {
        service = new AssetLibraryService(
                repository,
                objectStoragePort,
                new GroupAccessService(),
                auditRecorder,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        author = session("10000003", List.of("DOCUMENT_AUTHOR"));
        approver = session("10000004", List.of("GROUP_ADMIN"));
        admin = session("10000001", List.of("GLOBAL_ADMIN"));
        tester = session("10000006", List.of("TEMPLATE_TESTER"));
        auditAdmin = session("10000007", List.of("AUDIT_ADMIN"));
    }

    @Test
    void uploadImage_storesObjectAndCatalog_active() {
        when(repository.findById("IMG-E02-001")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetLibraryAssetView view = service.upload(
                author,
                pngFile("logo.png"),
                "IMG-E02-001",
                AssetLibraryAssetClass.IMAGE
        );

        assertThat(view.assetKey()).isEqualTo("IMG-E02-001");
        assertThat(view.status()).isEqualTo(AssetLibraryAssetStatus.ACTIVE);
        assertThat(view.contentType()).isEqualTo("image/png");
        assertThat(view.sizeBytes()).isEqualTo(PNG_BYTES.length);
        assertThat(view.contentSha256()).matches("^[a-f0-9]{64}$");
        assertThat(view.uploadedBy()).isEqualTo("10000003");
        assertThat(view.uploadedAt()).isEqualTo(NOW);

        verify(objectStoragePort).put(eq("IMG-E02-001"), any(), eq((long) PNG_BYTES.length), eq("image/png"));
        verify(auditRecorder).recordAssetLibraryUpload(
                eq("IMG-E02-001"),
                eq("IMAGE"),
                eq("10000003"),
                anyString(),
                anyString()
        );

        when(objectStoragePort.exists("IMG-E02-001")).thenReturn(true);
        when(objectStoragePort.get("IMG-E02-001")).thenReturn(new ByteArrayInputStream(PNG_BYTES));
        StructuredContentImageResolver resolver =
                new StructuredContentImageResolver(objectStoragePort, demoDisabledProperties());
        assertThat(resolver.resolveImageRef("IMG-E02-001").bytes()).isEqualTo(PNG_BYTES);
    }

    @Test
    void upload_invalidKey_422() {
        assertThatThrownBy(() -> service.upload(author, pngFile("a.png"), "bad/key", AssetLibraryAssetClass.IMAGE))
                .isInstanceOf(AssetLibraryValidationException.class)
                .satisfies(ex -> {
                    AssetLibraryValidationException vex = (AssetLibraryValidationException) ex;
                    assertThat(vex.errorCode()).isEqualTo(ApiErrorCodes.ASSET_LIBRARY_ASSET_KEY_INVALID);
                    assertThat(vex.messageKey()).isEqualTo("api.error.assetLibrary.assetKeyInvalid");
                });
        verify(objectStoragePort, never()).put(anyString(), any(), anyLong(), anyString());
        verify(repository, never()).save(any());
    }

    @Test
    void upload_unsupportedType_422() {
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "x.pdf", "application/pdf", new byte[] {0x25, 0x50, 0x44, 0x46}
        );
        assertThatThrownBy(() -> service.upload(author, pdf, "IMG-E02-003", AssetLibraryAssetClass.IMAGE))
                .isInstanceOf(AssetLibraryValidationException.class)
                .satisfies(ex -> assertThat(((AssetLibraryValidationException) ex).errorCode())
                        .isEqualTo(ApiErrorCodes.ASSET_LIBRARY_CONTENT_TYPE_UNSUPPORTED));
        verify(objectStoragePort, never()).put(anyString(), any(), anyLong(), anyString());
    }

    @Test
    void upload_tooLarge_422() {
        byte[] oversized = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(PNG_BYTES, 0, oversized, 0, PNG_BYTES.length);
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", oversized);
        assertThatThrownBy(() -> service.upload(author, file, "IMG-E02-004", AssetLibraryAssetClass.IMAGE))
                .isInstanceOf(AssetLibraryValidationException.class)
                .satisfies(ex -> assertThat(((AssetLibraryValidationException) ex).errorCode())
                        .isEqualTo(ApiErrorCodes.ASSET_LIBRARY_PAYLOAD_TOO_LARGE));
        verify(objectStoragePort, never()).put(anyString(), any(), anyLong(), anyString());
    }

    @Test
    void upload_activeConflict_409() {
        when(repository.findById("IMG-E02-005")).thenReturn(Optional.of(activeEntity("IMG-E02-005")));
        assertThatThrownBy(() -> service.upload(author, pngFile("a.png"), "IMG-E02-005", AssetLibraryAssetClass.IMAGE))
                .isInstanceOf(AssetLibraryConflictException.class)
                .satisfies(ex -> assertThat(((AssetLibraryConflictException) ex).messageKey())
                        .isEqualTo("api.error.assetLibrary.assetKeyConflict"));
        verify(objectStoragePort, never()).put(anyString(), any(), anyLong(), anyString());
    }

    @Test
    void upload_disabledKey_reactivates() {
        LibraryAssetEntity disabled = activeEntity("IMG-E02-006");
        disabled.markDisabled(NOW.minusSeconds(60));
        when(repository.findById("IMG-E02-006")).thenReturn(Optional.of(disabled));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetLibraryAssetView view = service.upload(
                author,
                pngFile("logo.png"),
                "IMG-E02-006",
                AssetLibraryAssetClass.IMAGE
        );

        assertThat(view.status()).isEqualTo(AssetLibraryAssetStatus.ACTIVE);
        verify(objectStoragePort).put(eq("IMG-E02-006"), any(), anyLong(), eq("image/png"));
        verify(auditRecorder).recordAssetLibraryReupload(
                eq("IMG-E02-006"),
                eq("IMAGE"),
                eq("10000003"),
                anyString(),
                anyString()
        );
    }

    @Test
    void upload_magicMismatch_422() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.png", "image/png", new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07}
        );
        assertThatThrownBy(() -> service.upload(author, file, "IMG-E02-022", AssetLibraryAssetClass.IMAGE))
                .isInstanceOf(AssetLibraryValidationException.class)
                .satisfies(ex -> {
                    AssetLibraryValidationException vex = (AssetLibraryValidationException) ex;
                    assertThat(vex.errorCode()).isEqualTo(ApiErrorCodes.ASSET_LIBRARY_CONTENT_TYPE_MISMATCH);
                    assertThat(vex.messageKey()).isEqualTo("api.error.assetLibrary.contentTypeMismatch");
                });
    }

    @Test
    void uploadSeal_forbiddenForAuthor() {
        assertThatThrownBy(() -> service.upload(author, pngFile("seal.png"), "SEAL-E02-007", AssetLibraryAssetClass.SEAL))
                .isInstanceOf(AssetLibraryAccessDeniedException.class);
        verify(objectStoragePort, never()).put(anyString(), any(), anyLong(), anyString());
    }

    @Test
    void uploadSeal_allowedForApprover() {
        when(repository.findById("SEAL-E02-008")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetLibraryAssetView view = service.upload(
                approver,
                pngFile("seal.png"),
                "SEAL-E02-008",
                AssetLibraryAssetClass.SEAL
        );

        assertThat(view.assetClass()).isEqualTo(AssetLibraryAssetClass.SEAL);
        when(objectStoragePort.exists("SEAL-E02-008")).thenReturn(true);
        when(objectStoragePort.get("SEAL-E02-008")).thenReturn(new ByteArrayInputStream(PNG_BYTES));
        StructuredContentImageResolver resolver =
                new StructuredContentImageResolver(objectStoragePort, demoDisabledProperties());
        assertThat(resolver.resolveSealRef("SEAL-E02-008").bytes()).isEqualTo(PNG_BYTES);
    }

    @Test
    void uploadSeal_allowedForAdmin() {
        when(repository.findById("SEAL-E02-009")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetLibraryAssetView view = service.upload(
                admin,
                jpegFile("seal.jpg"),
                "SEAL-E02-009",
                AssetLibraryAssetClass.SEAL
        );

        assertThat(view.contentType()).isEqualTo("image/jpeg");
        assertThat(view.status()).isEqualTo(AssetLibraryAssetStatus.ACTIVE);
    }

    @Test
    void list_defaultsActive() {
        when(repository.search(eq(AssetLibraryAssetStatus.ACTIVE), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(activeEntity("IMG-A")), Pageable.ofSize(20), 1));

        PageView<AssetLibraryAssetView> page = service.list(author, null, null, null, null, null);

        assertThat(page.content()).extracting(AssetLibraryAssetView::assetKey).containsExactly("IMG-A");
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void list_filterClassAndQuery() {
        when(repository.searchByQuery(
                eq(AssetLibraryAssetStatus.ACTIVE),
                eq(AssetLibraryAssetClass.SEAL),
                eq("SEAL-E02"),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(activeEntity("SEAL-E02-011")), Pageable.ofSize(20), 1));

        PageView<AssetLibraryAssetView> page = service.list(
                author, 0, 20, AssetLibraryAssetClass.SEAL, null, "SEAL-E02"
        );

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().assetKey()).isEqualTo("SEAL-E02-011");
    }

    @Test
    void tester_listActiveOnly_uploadForbidden() {
        when(repository.search(eq(AssetLibraryAssetStatus.ACTIVE), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(20), 0));

        PageView<AssetLibraryAssetView> page = service.list(
                tester, 0, 20, null, AssetLibraryListStatusFilter.DISABLED, null
        );
        assertThat(page.content()).isEmpty();
        verify(repository).search(eq(AssetLibraryAssetStatus.ACTIVE), eq(null), any(Pageable.class));

        assertThatThrownBy(() -> service.upload(tester, pngFile("a.png"), "IMG-T", AssetLibraryAssetClass.OTHER))
                .isInstanceOf(AssetLibraryAccessDeniedException.class);
        assertThatThrownBy(() -> service.disable(tester, "IMG-T"))
                .isInstanceOf(AssetLibraryAccessDeniedException.class);
    }

    @Test
    void disable_removesResolvableObject_idempotentWhenAlreadyDisabled() {
        LibraryAssetEntity entity = activeEntity("IMG-E02-013");
        when(repository.findById("IMG-E02-013")).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // Post-delete verify: object must be gone (or never present).
        when(objectStoragePort.exists(anyString())).thenReturn(false);

        AssetLibraryAssetView view = service.disable(admin, "IMG-E02-013");

        assertThat(view.status()).isEqualTo(AssetLibraryAssetStatus.DISABLED);
        ArgumentCaptor<String> deletedKeys = ArgumentCaptor.forClass(String.class);
        verify(objectStoragePort, org.mockito.Mockito.atLeastOnce()).delete(deletedKeys.capture());
        assertThat(deletedKeys.getAllValues()).contains("IMG-E02-013");
        verify(auditRecorder).recordAssetLibraryDisable(
                eq("IMG-E02-013"),
                eq("IMAGE"),
                eq("10000001"),
                anyString(),
                anyString()
        );

        LibraryAssetEntity alreadyDisabled = activeEntity("IMG-E02-013");
        alreadyDisabled.markDisabled(NOW.minusSeconds(10));
        when(repository.findById("IMG-E02-013")).thenReturn(Optional.of(alreadyDisabled));
        AssetLibraryAssetView again = service.disable(admin, "IMG-E02-013");
        assertThat(again.status()).isEqualTo(AssetLibraryAssetStatus.DISABLED);
    }

    @Test
    void disable_whenDeleteThrows_doesNotMarkDisabled() {
        LibraryAssetEntity entity = activeEntity("IMG-E02-C6-DEL");
        when(repository.findById("IMG-E02-C6-DEL")).thenReturn(Optional.of(entity));
        org.mockito.Mockito.doThrow(new ObjectStorageException("Failed to delete object", new RuntimeException("io")))
                .when(objectStoragePort).delete("IMG-E02-C6-DEL");

        assertThatThrownBy(() -> service.disable(admin, "IMG-E02-C6-DEL"))
                .isInstanceOf(ObjectStorageException.class);

        verify(repository, never()).save(any());
        verify(auditRecorder, never()).recordAssetLibraryDisable(
                anyString(), anyString(), anyString(), anyString(), anyString()
        );
        assertThat(entity.getStatus()).isEqualTo(AssetLibraryAssetStatus.ACTIVE);
    }

    @Test
    void disable_whenObjectStillExistsAfterDelete_doesNotMarkDisabled() {
        LibraryAssetEntity entity = activeEntity("IMG-E02-C6-STILL");
        when(repository.findById("IMG-E02-C6-STILL")).thenReturn(Optional.of(entity));
        when(objectStoragePort.exists("IMG-E02-C6-STILL")).thenReturn(true);

        assertThatThrownBy(() -> service.disable(admin, "IMG-E02-C6-STILL"))
                .isInstanceOf(ObjectStorageException.class);

        verify(objectStoragePort).delete("IMG-E02-C6-STILL");
        verify(repository, never()).save(any());
        verify(auditRecorder, never()).recordAssetLibraryDisable(
                anyString(), anyString(), anyString(), anyString(), anyString()
        );
        assertThat(entity.getStatus()).isEqualTo(AssetLibraryAssetStatus.ACTIVE);
    }

    @Test
    void disable_whenExistsCheckAmbiguous_doesNotMarkDisabled() {
        LibraryAssetEntity entity = activeEntity("IMG-E02-C6-AMB");
        when(repository.findById("IMG-E02-C6-AMB")).thenReturn(Optional.of(entity));
        when(objectStoragePort.exists("IMG-E02-C6-AMB"))
                .thenThrow(new ObjectStorageException("Failed to stat object", new RuntimeException("io")));

        assertThatThrownBy(() -> service.disable(admin, "IMG-E02-C6-AMB"))
                .isInstanceOf(ObjectStorageException.class);

        verify(objectStoragePort).delete("IMG-E02-C6-AMB");
        verify(repository, never()).save(any());
        verify(auditRecorder, never()).recordAssetLibraryDisable(
                anyString(), anyString(), anyString(), anyString(), anyString()
        );
        assertThat(entity.getStatus()).isEqualTo(AssetLibraryAssetStatus.ACTIVE);
    }

    @Test
    void disable_forbiddenForAuthor() {
        assertThatThrownBy(() -> service.disable(author, "IMG-E02-014"))
                .isInstanceOf(AssetLibraryAccessDeniedException.class);
        verify(repository, never()).findById(anyString());
    }

    @Test
    void list_forbiddenForAuditAdmin() {
        assertThatThrownBy(() -> service.list(auditAdmin, null, null, null, null, null))
                .isInstanceOf(AssetLibraryAccessDeniedException.class);
    }

    @Test
    void disable_notFound_404() {
        when(repository.findById("MISSING-KEY")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.disable(admin, "MISSING-KEY"))
                .isInstanceOf(AssetLibraryNotFoundException.class)
                .satisfies(ex -> assertThat(((AssetLibraryNotFoundException) ex).messageKey())
                        .isEqualTo("api.error.assetLibrary.assetNotFound"));
    }

    @Test
    void resolverProtocol_signaturesUnchanged() throws Exception {
        assertThat(StructuredContentImageResolver.class.getMethod("resolveImageRef", String.class)).isNotNull();
        assertThat(StructuredContentImageResolver.class.getMethod("resolveSealRef", String.class)).isNotNull();
        assertThat(StructuredContentImageResolver.ResolvedImage.class.getRecordComponents()).hasSize(2);
    }

    private static DocgenRenderingProperties demoDisabledProperties() {
        DocgenRenderingProperties properties = new DocgenRenderingProperties();
        properties.setDemoClasspathImageTierEnabled(false);
        return properties;
    }

    private static LibraryAssetEntity activeEntity(String key) {
        return new LibraryAssetEntity(
                key,
                key.startsWith("SEAL") ? AssetLibraryAssetClass.SEAL : AssetLibraryAssetClass.IMAGE,
                AssetLibraryAssetStatus.ACTIVE,
                "image/png",
                PNG_BYTES.length,
                "a".repeat(64),
                "file.png",
                "10000003",
                NOW
        );
    }

    private static MockMultipartFile pngFile(String name) {
        return new MockMultipartFile("file", name, "image/png", PNG_BYTES);
    }

    private static MockMultipartFile jpegFile(String name) {
        return new MockMultipartFile("file", name, "image/jpeg", JPEG_BYTES);
    }

    private static ManagementSessionClaims session(String username, List<String> roles) {
        return new ManagementSessionClaims(
                username,
                "User",
                username + "@example.com",
                AuthSource.LOCAL,
                roles,
                List.of("RETAIL"),
                "route.dashboard-home",
                List.of(),
                NOW.plusSeconds(3600)
        );
    }
}
