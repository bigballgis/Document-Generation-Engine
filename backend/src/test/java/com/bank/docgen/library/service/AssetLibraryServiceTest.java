package com.bank.docgen.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.persistence.BusinessGroupRepository;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.infrastructure.storage.ObjectStorageException;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.library.api.AssetLibraryAssetView;
import com.bank.docgen.library.domain.AssetLibraryAssetClass;
import com.bank.docgen.library.domain.AssetLibraryAssetStatus;
import com.bank.docgen.library.persistence.LibraryAssetEntity;
import com.bank.docgen.library.persistence.LibraryAssetRepository;
import com.bank.docgen.rendering.AssetResolveGroupContext;
import com.bank.docgen.rendering.StructuredContentImageResolver;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
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
    private BusinessGroupRepository businessGroupRepository;
    @Mock
    private ManagementAuditRecorder auditRecorder;

    private AssetLibraryService service;
    private ManagementSessionClaims author;
    private ManagementSessionClaims corpOnlyAuthor;
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
                businessGroupRepository,
                auditRecorder,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        author = session("10000003", List.of("DOCUMENT_AUTHOR"), List.of("RETAIL"));
        corpOnlyAuthor = session("10000013", List.of("DOCUMENT_AUTHOR"), List.of("CORP"));
        approver = session("10000004", List.of("GROUP_ADMIN"), List.of("RETAIL", "CORP"));
        admin = session("10000001", List.of("GLOBAL_ADMIN"), List.of("*"));
        tester = session("10000006", List.of("TEMPLATE_TESTER"), List.of("RETAIL"));
        auditAdmin = session("10000007", List.of("AUDIT_ADMIN"), List.of("RETAIL"));
    }

    @Test
    void uploadImage_storesNamespacedObjectAndCatalog_active() {
        when(businessGroupRepository.existsByGroupCodeAndDeletedAtIsNull("RETAIL")).thenReturn(true);
        when(repository.findByGroupCodeAndAssetKeyAndDeletedAtIsNull("RETAIL", "IMG-E02-001"))
                .thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetLibraryAssetView view = service.upload(
                author,
                pngFile("logo.png"),
                "IMG-E02-001",
                AssetLibraryAssetClass.IMAGE,
                "RETAIL"
        );

        assertThat(view.groupCode()).isEqualTo("RETAIL");
        assertThat(view.assetKey()).isEqualTo("IMG-E02-001");
        assertThat(view.status()).isEqualTo(AssetLibraryAssetStatus.ACTIVE);
        verify(objectStoragePort).put(
                eq("RETAIL/IMG-E02-001"), any(), eq((long) PNG_BYTES.length), eq("image/png")
        );
        verify(auditRecorder).recordAssetLibraryUpload(
                eq("RETAIL"),
                eq("IMG-E02-001"),
                eq("IMAGE"),
                eq("10000003"),
                anyString(),
                anyString()
        );

        when(objectStoragePort.exists("RETAIL/IMG-E02-001")).thenReturn(true);
        when(objectStoragePort.get("RETAIL/IMG-E02-001")).thenReturn(new ByteArrayInputStream(PNG_BYTES));
        LibraryAssetActiveLookup lookup = (group, key) -> "RETAIL".equals(group) && "IMG-E02-001".equals(key);
        StructuredContentImageResolver resolver =
                new StructuredContentImageResolver(objectStoragePort, false, lookup);
        AssetResolveGroupContext.runWithGroup("RETAIL", () ->
                assertThat(resolver.resolveImageRef("IMG-E02-001").bytes()).isEqualTo(PNG_BYTES)
        );
    }

    @Test
    void upload_missingGroupCode_422() {
        assertThatThrownBy(() -> service.upload(author, pngFile("a.png"), "IMG-X", AssetLibraryAssetClass.IMAGE, null))
                .isInstanceOf(AssetLibraryValidationException.class)
                .satisfies(ex -> assertThat(((AssetLibraryValidationException) ex).errorCode())
                        .isEqualTo(ApiErrorCodes.ASSET_LIBRARY_GROUP_CODE_REQUIRED));
        verify(objectStoragePort, never()).put(anyString(), any(), anyLong(), anyString());
    }

    @Test
    void upload_unauthorizedGroup_403() {
        when(businessGroupRepository.existsByGroupCodeAndDeletedAtIsNull("CORP")).thenReturn(true);
        assertThatThrownBy(() ->
                service.upload(author, pngFile("a.png"), "IMG-X", AssetLibraryAssetClass.IMAGE, "CORP"))
                .isInstanceOf(AssetLibraryAccessDeniedException.class);
        verify(objectStoragePort, never()).put(anyString(), any(), anyLong(), anyString());
    }

    @Test
    void upload_sameKeyDifferentGroups_allowed() {
        when(businessGroupRepository.existsByGroupCodeAndDeletedAtIsNull("RETAIL")).thenReturn(true);
        when(repository.findByGroupCodeAndAssetKeyAndDeletedAtIsNull("RETAIL", "IMG-ALGI-004"))
                .thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetLibraryAssetView view = service.upload(
                author, pngFile("a.png"), "IMG-ALGI-004", AssetLibraryAssetClass.IMAGE, "RETAIL"
        );
        assertThat(view.groupCode()).isEqualTo("RETAIL");
        assertThat(view.assetKey()).isEqualTo("IMG-ALGI-004");
    }

    @Test
    void upload_activeConflictWithinGroup_409() {
        when(businessGroupRepository.existsByGroupCodeAndDeletedAtIsNull("RETAIL")).thenReturn(true);
        when(repository.findByGroupCodeAndAssetKeyAndDeletedAtIsNull("RETAIL", "IMG-E02-005"))
                .thenReturn(Optional.of(activeEntity("RETAIL", "IMG-E02-005")));
        assertThatThrownBy(() ->
                service.upload(author, pngFile("a.png"), "IMG-E02-005", AssetLibraryAssetClass.IMAGE, "RETAIL"))
                .isInstanceOf(AssetLibraryConflictException.class)
                .satisfies(ex -> assertThat(((AssetLibraryConflictException) ex).messageKey())
                        .isEqualTo("api.error.assetLibrary.assetKeyConflict"));
        verify(objectStoragePort, never()).put(anyString(), any(), anyLong(), anyString());
    }

    @Test
    void upload_invalidKey_422() {
        when(businessGroupRepository.existsByGroupCodeAndDeletedAtIsNull("RETAIL")).thenReturn(true);
        assertThatThrownBy(() ->
                service.upload(author, pngFile("a.png"), "bad/key", AssetLibraryAssetClass.IMAGE, "RETAIL"))
                .isInstanceOf(AssetLibraryValidationException.class)
                .satisfies(ex -> {
                    AssetLibraryValidationException vex = (AssetLibraryValidationException) ex;
                    assertThat(vex.errorCode()).isEqualTo(ApiErrorCodes.ASSET_LIBRARY_ASSET_KEY_INVALID);
                });
        verify(objectStoragePort, never()).put(anyString(), any(), anyLong(), anyString());
    }

    @Test
    void upload_unsupportedType_422() {
        when(businessGroupRepository.existsByGroupCodeAndDeletedAtIsNull("RETAIL")).thenReturn(true);
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "x.pdf", "application/pdf", new byte[] {0x25, 0x50, 0x44, 0x46}
        );
        assertThatThrownBy(() ->
                service.upload(author, pdf, "IMG-E02-003", AssetLibraryAssetClass.IMAGE, "RETAIL"))
                .isInstanceOf(AssetLibraryValidationException.class)
                .satisfies(ex -> assertThat(((AssetLibraryValidationException) ex).errorCode())
                        .isEqualTo(ApiErrorCodes.ASSET_LIBRARY_CONTENT_TYPE_UNSUPPORTED));
    }

    @Test
    void upload_tooLarge_422() {
        when(businessGroupRepository.existsByGroupCodeAndDeletedAtIsNull("RETAIL")).thenReturn(true);
        byte[] oversized = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(PNG_BYTES, 0, oversized, 0, PNG_BYTES.length);
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", oversized);
        assertThatThrownBy(() ->
                service.upload(author, file, "IMG-E02-004", AssetLibraryAssetClass.IMAGE, "RETAIL"))
                .isInstanceOf(AssetLibraryValidationException.class)
                .satisfies(ex -> assertThat(((AssetLibraryValidationException) ex).errorCode())
                        .isEqualTo(ApiErrorCodes.ASSET_LIBRARY_PAYLOAD_TOO_LARGE));
    }

    @Test
    void upload_disabledKey_reactivates() {
        when(businessGroupRepository.existsByGroupCodeAndDeletedAtIsNull("RETAIL")).thenReturn(true);
        LibraryAssetEntity disabled = activeEntity("RETAIL", "IMG-E02-006");
        disabled.markDisabled(NOW.minusSeconds(60));
        when(repository.findByGroupCodeAndAssetKeyAndDeletedAtIsNull("RETAIL", "IMG-E02-006"))
                .thenReturn(Optional.of(disabled));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetLibraryAssetView view = service.upload(
                author, pngFile("logo.png"), "IMG-E02-006", AssetLibraryAssetClass.IMAGE, "RETAIL"
        );

        assertThat(view.status()).isEqualTo(AssetLibraryAssetStatus.ACTIVE);
        verify(objectStoragePort).put(eq("RETAIL/IMG-E02-006"), any(), anyLong(), eq("image/png"));
        verify(auditRecorder).recordAssetLibraryReupload(
                eq("RETAIL"), eq("IMG-E02-006"), eq("IMAGE"), eq("10000003"), anyString(), anyString()
        );
    }

    @Test
    void uploadSeal_forbiddenForAuthor() {
        assertThatThrownBy(() ->
                service.upload(author, pngFile("seal.png"), "SEAL-E02-007", AssetLibraryAssetClass.SEAL, "RETAIL"))
                .isInstanceOf(AssetLibraryAccessDeniedException.class);
    }

    @Test
    void uploadSeal_allowedForApprover() {
        when(businessGroupRepository.existsByGroupCodeAndDeletedAtIsNull("CORP")).thenReturn(true);
        when(repository.findByGroupCodeAndAssetKeyAndDeletedAtIsNull("CORP", "SEAL-E02-008"))
                .thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetLibraryAssetView view = service.upload(
                approver, pngFile("seal.png"), "SEAL-E02-008", AssetLibraryAssetClass.SEAL, "CORP"
        );

        assertThat(view.assetClass()).isEqualTo(AssetLibraryAssetClass.SEAL);
        when(objectStoragePort.exists("CORP/SEAL-E02-008")).thenReturn(true);
        when(objectStoragePort.get("CORP/SEAL-E02-008")).thenReturn(new ByteArrayInputStream(PNG_BYTES));
        LibraryAssetActiveLookup lookup = (g, k) -> "CORP".equals(g) && "SEAL-E02-008".equals(k);
        StructuredContentImageResolver resolver =
                new StructuredContentImageResolver(objectStoragePort, false, lookup);
        AssetResolveGroupContext.runWithGroup("CORP", () ->
                assertThat(resolver.resolveSealRef("SEAL-E02-008").bytes()).isEqualTo(PNG_BYTES)
        );
    }

    @Test
    void uploadSeal_allowedForAdmin() {
        when(businessGroupRepository.existsByGroupCodeAndDeletedAtIsNull("RETAIL")).thenReturn(true);
        when(repository.findByGroupCodeAndAssetKeyAndDeletedAtIsNull("RETAIL", "SEAL-E02-009"))
                .thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssetLibraryAssetView view = service.upload(
                admin, jpegFile("seal.jpg"), "SEAL-E02-009", AssetLibraryAssetClass.SEAL, "RETAIL"
        );

        assertThat(view.contentType()).isEqualTo("image/jpeg");
        assertThat(view.groupCode()).isEqualTo("RETAIL");
    }

    @Test
    void list_defaultsActive_scopedToAuthorizedGroups() {
        when(repository.search(
                eq(AssetLibraryAssetStatus.ACTIVE),
                isNull(),
                isNull(),
                eq(true),
                eq(List.of("RETAIL")),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(activeEntity("RETAIL", "IMG-A")), Pageable.ofSize(20), 1));

        PageView<AssetLibraryAssetView> page = service.list(author, null, null, null, null, null, null);

        assertThat(page.content()).extracting(AssetLibraryAssetView::assetKey).containsExactly("IMG-A");
        assertThat(page.content().getFirst().groupCode()).isEqualTo("RETAIL");
    }

    @Test
    void list_unauthorizedGroupFilter_emptyNoLeak() {
        PageView<AssetLibraryAssetView> page =
                service.list(corpOnlyAuthor, null, null, null, null, null, "RETAIL");

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        verify(repository, never()).search(any(), any(), any(), anyBoolean(), any(), any());
    }

    @Test
    void list_globalAdmin_allGroupsAndFilter() {
        when(repository.search(
                eq(AssetLibraryAssetStatus.ACTIVE),
                isNull(),
                isNull(),
                eq(false),
                any(Collection.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                List.of(activeEntity("CORP", "IMG-C"), activeEntity("RETAIL", "IMG-R")),
                Pageable.ofSize(20),
                2
        ));
        when(repository.search(
                eq(AssetLibraryAssetStatus.ACTIVE),
                isNull(),
                eq("CORP"),
                eq(false),
                any(Collection.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(activeEntity("CORP", "IMG-C")), Pageable.ofSize(20), 1));

        PageView<AssetLibraryAssetView> all = service.list(admin, null, null, null, null, null, null);
        assertThat(all.content()).extracting(AssetLibraryAssetView::groupCode).containsExactly("CORP", "RETAIL");

        PageView<AssetLibraryAssetView> filtered = service.list(admin, null, null, null, null, null, "CORP");
        assertThat(filtered.content()).extracting(AssetLibraryAssetView::groupCode).containsExactly("CORP");
    }

    @Test
    void list_doesNotFabricateClasspathDemoImageGhostsWhenRepositoryEmpty() {
        when(repository.search(
                eq(AssetLibraryAssetStatus.ACTIVE),
                isNull(),
                isNull(),
                eq(true),
                eq(List.of("RETAIL")),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(20), 0));

        PageView<AssetLibraryAssetView> page = service.list(author, null, null, null, null, null, null);

        assertThat(page.content()).isEmpty();
    }

    @Test
    void list_filterClassAndQuery() {
        when(repository.searchByQuery(
                eq(AssetLibraryAssetStatus.ACTIVE),
                eq(AssetLibraryAssetClass.SEAL),
                isNull(),
                eq(true),
                eq(List.of("RETAIL")),
                eq("SEAL-E02"),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(activeEntity("RETAIL", "SEAL-E02-011")), Pageable.ofSize(20), 1));

        PageView<AssetLibraryAssetView> page = service.list(
                author, 0, 20, AssetLibraryAssetClass.SEAL, null, "SEAL-E02", null
        );

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().assetKey()).isEqualTo("SEAL-E02-011");
    }

    @Test
    void tester_listActiveOnly_uploadForbidden() {
        when(repository.search(
                eq(AssetLibraryAssetStatus.ACTIVE),
                isNull(),
                isNull(),
                eq(true),
                eq(List.of("RETAIL")),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(20), 0));

        PageView<AssetLibraryAssetView> page = service.list(
                tester, 0, 20, null, AssetLibraryListStatusFilter.DISABLED, null, null
        );
        assertThat(page.content()).isEmpty();

        assertThatThrownBy(() ->
                service.upload(tester, pngFile("a.png"), "IMG-T", AssetLibraryAssetClass.OTHER, "RETAIL"))
                .isInstanceOf(AssetLibraryAccessDeniedException.class);
        assertThatThrownBy(() -> service.disable(tester, "IMG-T", "RETAIL"))
                .isInstanceOf(AssetLibraryAccessDeniedException.class);
    }

    @Test
    void disable_removesNamespacedObject_idempotentWhenAlreadyDisabled() {
        when(businessGroupRepository.existsByGroupCodeAndDeletedAtIsNull("RETAIL")).thenReturn(true);
        LibraryAssetEntity entity = activeEntity("RETAIL", "IMG-E02-013");
        when(repository.findByGroupCodeAndAssetKeyAndDeletedAtIsNull("RETAIL", "IMG-E02-013"))
                .thenReturn(Optional.of(entity));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(objectStoragePort.exists(anyString())).thenReturn(false);

        AssetLibraryAssetView view = service.disable(admin, "IMG-E02-013", "RETAIL");

        assertThat(view.status()).isEqualTo(AssetLibraryAssetStatus.DISABLED);
        ArgumentCaptor<String> deletedKeys = ArgumentCaptor.forClass(String.class);
        verify(objectStoragePort, org.mockito.Mockito.atLeastOnce()).delete(deletedKeys.capture());
        assertThat(deletedKeys.getAllValues()).contains("RETAIL/IMG-E02-013");
        verify(auditRecorder).recordAssetLibraryDisable(
                eq("RETAIL"), eq("IMG-E02-013"), eq("IMAGE"), eq("10000001"), anyString(), anyString()
        );

        LibraryAssetEntity alreadyDisabled = activeEntity("RETAIL", "IMG-E02-013");
        alreadyDisabled.markDisabled(NOW.minusSeconds(10));
        when(repository.findByGroupCodeAndAssetKeyAndDeletedAtIsNull("RETAIL", "IMG-E02-013"))
                .thenReturn(Optional.of(alreadyDisabled));
        AssetLibraryAssetView again = service.disable(admin, "IMG-E02-013", "RETAIL");
        assertThat(again.status()).isEqualTo(AssetLibraryAssetStatus.DISABLED);
    }

    @Test
    void disable_outsideAuthorizedGroup_403() {
        when(businessGroupRepository.existsByGroupCodeAndDeletedAtIsNull("RETAIL")).thenReturn(true);
        ManagementSessionClaims corpAdmin = session("10000020", List.of("GROUP_ADMIN"), List.of("CORP"));
        assertThatThrownBy(() -> service.disable(corpAdmin, "IMG-X", "RETAIL"))
                .isInstanceOf(AssetLibraryAccessDeniedException.class);
        verify(repository, never()).findByGroupCodeAndAssetKeyAndDeletedAtIsNull(anyString(), anyString());
    }

    @Test
    void disable_whenDeleteThrows_doesNotMarkDisabled() {
        when(businessGroupRepository.existsByGroupCodeAndDeletedAtIsNull("RETAIL")).thenReturn(true);
        LibraryAssetEntity entity = activeEntity("RETAIL", "IMG-E02-C6-DEL");
        when(repository.findByGroupCodeAndAssetKeyAndDeletedAtIsNull("RETAIL", "IMG-E02-C6-DEL"))
                .thenReturn(Optional.of(entity));
        org.mockito.Mockito.doThrow(new ObjectStorageException("Failed to delete object", new RuntimeException("io")))
                .when(objectStoragePort).delete("RETAIL/IMG-E02-C6-DEL");

        assertThatThrownBy(() -> service.disable(admin, "IMG-E02-C6-DEL", "RETAIL"))
                .isInstanceOf(ObjectStorageException.class);

        verify(repository, never()).save(any());
        assertThat(entity.getStatus()).isEqualTo(AssetLibraryAssetStatus.ACTIVE);
    }

    @Test
    void disable_whenObjectStillExistsAfterDelete_doesNotMarkDisabled() {
        when(businessGroupRepository.existsByGroupCodeAndDeletedAtIsNull("RETAIL")).thenReturn(true);
        LibraryAssetEntity entity = activeEntity("RETAIL", "IMG-E02-C6-STILL");
        when(repository.findByGroupCodeAndAssetKeyAndDeletedAtIsNull("RETAIL", "IMG-E02-C6-STILL"))
                .thenReturn(Optional.of(entity));
        when(objectStoragePort.exists("RETAIL/IMG-E02-C6-STILL")).thenReturn(true);

        assertThatThrownBy(() -> service.disable(admin, "IMG-E02-C6-STILL", "RETAIL"))
                .isInstanceOf(ObjectStorageException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void disable_forbiddenForAuthor() {
        assertThatThrownBy(() -> service.disable(author, "IMG-E02-014", "RETAIL"))
                .isInstanceOf(AssetLibraryAccessDeniedException.class);
        verify(repository, never()).findByGroupCodeAndAssetKeyAndDeletedAtIsNull(anyString(), anyString());
    }

    @Test
    void list_forbiddenForAuditAdmin() {
        assertThatThrownBy(() -> service.list(auditAdmin, null, null, null, null, null, null))
                .isInstanceOf(AssetLibraryAccessDeniedException.class);
    }

    @Test
    void disable_notFound_404() {
        when(businessGroupRepository.existsByGroupCodeAndDeletedAtIsNull("RETAIL")).thenReturn(true);
        when(repository.findByGroupCodeAndAssetKeyAndDeletedAtIsNull("RETAIL", "MISSING-KEY"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.disable(admin, "MISSING-KEY", "RETAIL"))
                .isInstanceOf(AssetLibraryNotFoundException.class);
    }

    @Test
    void disable_missingGroupCode_422() {
        assertThatThrownBy(() -> service.disable(admin, "IMG-X", "  "))
                .isInstanceOf(AssetLibraryValidationException.class)
                .satisfies(ex -> assertThat(((AssetLibraryValidationException) ex).errorCode())
                        .isEqualTo(ApiErrorCodes.ASSET_LIBRARY_GROUP_CODE_REQUIRED));
    }

    @Test
    void resolverProtocol_signaturesUnchanged() throws Exception {
        assertThat(StructuredContentImageResolver.class.getMethod("resolveImageRef", String.class)).isNotNull();
        assertThat(StructuredContentImageResolver.class.getMethod("resolveSealRef", String.class)).isNotNull();
        assertThat(StructuredContentImageResolver.ResolvedImage.class.getRecordComponents()).hasSize(2);
    }

    private static LibraryAssetEntity activeEntity(String groupCode, String key) {
        return new LibraryAssetEntity(
                groupCode,
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

    private static ManagementSessionClaims session(String username, List<String> roles, List<String> groups) {
        return new ManagementSessionClaims(
                username,
                "User",
                username + "@example.com",
                AuthSource.LOCAL,
                roles,
                groups,
                "route.dashboard-home",
                List.of(),
                NOW.plusSeconds(3600)
        );
    }
}
