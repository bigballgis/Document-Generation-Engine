package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.GroupDimension;
import com.bank.docgen.authorization.management.persistence.BusinessGroupEntity;
import com.bank.docgen.authorization.management.persistence.BusinessGroupRepository;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.library.api.AssetLibraryAssetView;
import com.bank.docgen.library.domain.AssetLibraryAssetClass;
import com.bank.docgen.library.domain.AssetLibraryAssetStatus;
import com.bank.docgen.library.persistence.LibraryAssetEntity;
import com.bank.docgen.library.persistence.LibraryAssetRepository;
import com.bank.docgen.library.service.AssetLibraryService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class DemoAssetLibrarySeederTest {

    @Mock
    private LibraryAssetRepository libraryAssetRepository;
    @Mock
    private BusinessGroupRepository businessGroupRepository;
    @Mock
    private AssetLibraryService assetLibraryService;
    @Mock
    private ObjectStoragePort objectStoragePort;

    private DemoAssetLibrarySeedProperties properties;
    private DemoAssetLibrarySeeder seeder;

    @BeforeEach
    void setUp() {
        properties = new DemoAssetLibrarySeedProperties();
        properties.setSeedEnabled(true);
        seeder = new DemoAssetLibrarySeeder(
                properties,
                libraryAssetRepository,
                businessGroupRepository,
                assetLibraryService,
                objectStoragePort
        );
    }

    @Test
    void seedIfNeededUploadsManagedImg1AndSeal1PerGroup() throws Exception {
        // BDD-ALGI-017 / BDD-SYS-NORM-W8-002
        when(businessGroupRepository.findByDeletedAtIsNullOrderByGroupCodeAsc())
                .thenReturn(List.of(group("CORP"), group("RETAIL")));
        when(libraryAssetRepository.findByGroupCodeAndAssetKeyAndDeletedAtIsNull(any(), any()))
                .thenReturn(Optional.empty());
        when(objectStoragePort.exists(any())).thenReturn(false);
        when(assetLibraryService.upload(any(), any(), any(), any(), any()))
                .thenReturn(view("CORP", "IMG-1", AssetLibraryAssetClass.IMAGE));

        seeder.seedIfNeeded();

        verify(assetLibraryService, times(2)).upload(
                any(ManagementSessionClaims.class),
                any(MultipartFile.class),
                eq(DemoAssetLibrarySeeder.DEMO_IMAGE_ASSET_KEY),
                eq(AssetLibraryAssetClass.IMAGE),
                any()
        );
        verify(assetLibraryService, times(2)).upload(
                any(ManagementSessionClaims.class),
                any(MultipartFile.class),
                eq(DemoAssetLibrarySeeder.DEMO_SEAL_ASSET_KEY),
                eq(AssetLibraryAssetClass.SEAL),
                any()
        );
        verify(assetLibraryService).upload(any(), any(), eq("IMG-1"), eq(AssetLibraryAssetClass.IMAGE), eq("CORP"));
        verify(assetLibraryService).upload(any(), any(), eq("IMG-1"), eq(AssetLibraryAssetClass.IMAGE), eq("RETAIL"));
    }

    @Test
    void seedIfNeededClearsOrphanStorageObjectBeforeUpload() throws Exception {
        when(businessGroupRepository.findByDeletedAtIsNullOrderByGroupCodeAsc())
                .thenReturn(List.of(group("CORP")));
        when(libraryAssetRepository.findByGroupCodeAndAssetKeyAndDeletedAtIsNull(any(), any()))
                .thenReturn(Optional.empty());
        when(objectStoragePort.exists("CORP/IMG-1")).thenReturn(true);
        when(objectStoragePort.exists("IMG-1")).thenReturn(true);
        when(objectStoragePort.exists("CORP/SEAL-1")).thenReturn(false);
        when(objectStoragePort.exists("SEAL-1")).thenReturn(false);
        when(assetLibraryService.upload(any(), any(), any(), any(), any()))
                .thenReturn(view("CORP", "IMG-1", AssetLibraryAssetClass.IMAGE));

        seeder.seedIfNeeded();

        verify(objectStoragePort).delete("CORP/IMG-1");
        verify(objectStoragePort).delete("IMG-1");
    }

    @Test
    void seedIfNeededSkipsAlreadyActiveManagedKeys() throws Exception {
        when(businessGroupRepository.findByDeletedAtIsNullOrderByGroupCodeAsc())
                .thenReturn(List.of(group("CORP")));
        LibraryAssetEntity activeImage = activeEntity("CORP", DemoAssetLibrarySeeder.DEMO_IMAGE_ASSET_KEY,
                AssetLibraryAssetClass.IMAGE);
        LibraryAssetEntity activeSeal = activeEntity("CORP", DemoAssetLibrarySeeder.DEMO_SEAL_ASSET_KEY,
                AssetLibraryAssetClass.SEAL);
        when(libraryAssetRepository.findByGroupCodeAndAssetKeyAndDeletedAtIsNull(
                "CORP", DemoAssetLibrarySeeder.DEMO_IMAGE_ASSET_KEY))
                .thenReturn(Optional.of(activeImage));
        when(libraryAssetRepository.findByGroupCodeAndAssetKeyAndDeletedAtIsNull(
                "CORP", DemoAssetLibrarySeeder.DEMO_SEAL_ASSET_KEY))
                .thenReturn(Optional.of(activeSeal));

        seeder.seedIfNeeded();

        verify(assetLibraryService, never()).upload(any(), any(), any(), any(), any());
        verify(objectStoragePort, never()).delete(any());
    }

    @Test
    void runIsNoOpWhenSeedDisabled() {
        properties.setSeedEnabled(false);

        seeder.run(null);

        verify(assetLibraryService, never()).upload(any(), any(), any(), any(), any());
        verify(libraryAssetRepository, never()).findByGroupCodeAndAssetKeyAndDeletedAtIsNull(any(), any());
    }

    @Test
    void seededKeysAreNotClasspathGhostPaths() {
        assertThat(DemoAssetLibrarySeeder.DEMO_IMAGE_ASSET_KEY).isEqualTo("IMG-1");
        assertThat(DemoAssetLibrarySeeder.DEMO_SEAL_ASSET_KEY).isEqualTo("SEAL-1");
        assertThat(DemoAssetLibrarySeeder.DEMO_IMAGE_ASSET_KEY).doesNotContain("demo-images");
        assertThat(DemoAssetLibrarySeeder.DEMO_SEAL_ASSET_KEY).doesNotContain("/");
    }

    private static BusinessGroupEntity group(String code) {
        return new BusinessGroupEntity(UUID.randomUUID(), code, code, GroupDimension.BUSINESS_LINE);
    }

    private static AssetLibraryAssetView view(String group, String key, AssetLibraryAssetClass assetClass) {
        return new AssetLibraryAssetView(
                group,
                key,
                assetClass,
                AssetLibraryAssetStatus.ACTIVE,
                "image/png",
                70L,
                "abc",
                key + ".png",
                "10000001",
                Instant.parse("2026-07-21T00:00:00Z")
        );
    }

    private static LibraryAssetEntity activeEntity(
            String groupCode,
            String key,
            AssetLibraryAssetClass assetClass
    ) {
        return new LibraryAssetEntity(
                groupCode,
                key,
                assetClass,
                AssetLibraryAssetStatus.ACTIVE,
                "image/png",
                70L,
                "abc",
                key + ".png",
                "10000001",
                Instant.parse("2026-07-21T00:00:00Z")
        );
    }
}
