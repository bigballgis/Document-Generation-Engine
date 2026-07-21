package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.library.api.AssetLibraryAssetView;
import com.bank.docgen.library.domain.AssetLibraryAssetClass;
import com.bank.docgen.library.domain.AssetLibraryAssetStatus;
import com.bank.docgen.library.persistence.LibraryAssetEntity;
import com.bank.docgen.library.persistence.LibraryAssetRepository;
import com.bank.docgen.library.service.AssetLibraryService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.time.Instant;
import java.util.Optional;
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
                assetLibraryService,
                objectStoragePort
        );
    }

    @Test
    void seedIfNeededUploadsManagedImg1AndSeal1() throws Exception {
        // BDD-SYS-NORM-W8-002 — enabled seed inserts managed library assets.
        when(libraryAssetRepository.findById(any())).thenReturn(Optional.empty());
        when(objectStoragePort.exists(any())).thenReturn(false);
        when(assetLibraryService.upload(any(), any(), any(), any()))
                .thenReturn(view("IMG-1", AssetLibraryAssetClass.IMAGE))
                .thenReturn(view("SEAL-1", AssetLibraryAssetClass.SEAL));

        seeder.seedIfNeeded();

        verify(assetLibraryService).upload(
                any(ManagementSessionClaims.class),
                any(MultipartFile.class),
                eq(DemoAssetLibrarySeeder.DEMO_IMAGE_ASSET_KEY),
                eq(AssetLibraryAssetClass.IMAGE)
        );
        verify(assetLibraryService).upload(
                any(ManagementSessionClaims.class),
                any(MultipartFile.class),
                eq(DemoAssetLibrarySeeder.DEMO_SEAL_ASSET_KEY),
                eq(AssetLibraryAssetClass.SEAL)
        );
    }

    @Test
    void seedIfNeededClearsOrphanStorageObjectBeforeUpload() throws Exception {
        when(libraryAssetRepository.findById(DemoAssetLibrarySeeder.DEMO_IMAGE_ASSET_KEY))
                .thenReturn(Optional.empty());
        when(libraryAssetRepository.findById(DemoAssetLibrarySeeder.DEMO_SEAL_ASSET_KEY))
                .thenReturn(Optional.empty());
        when(objectStoragePort.exists(DemoAssetLibrarySeeder.DEMO_IMAGE_ASSET_KEY)).thenReturn(true);
        when(objectStoragePort.exists(DemoAssetLibrarySeeder.DEMO_SEAL_ASSET_KEY)).thenReturn(false);
        when(assetLibraryService.upload(any(), any(), any(), any()))
                .thenReturn(view("IMG-1", AssetLibraryAssetClass.IMAGE))
                .thenReturn(view("SEAL-1", AssetLibraryAssetClass.SEAL));

        seeder.seedIfNeeded();

        verify(objectStoragePort).delete(DemoAssetLibrarySeeder.DEMO_IMAGE_ASSET_KEY);
        verify(objectStoragePort, never()).delete(DemoAssetLibrarySeeder.DEMO_SEAL_ASSET_KEY);
    }

    @Test
    void seedIfNeededSkipsAlreadyActiveManagedKeys() throws Exception {
        LibraryAssetEntity activeImage = activeEntity(
                DemoAssetLibrarySeeder.DEMO_IMAGE_ASSET_KEY,
                AssetLibraryAssetClass.IMAGE
        );
        LibraryAssetEntity activeSeal = activeEntity(
                DemoAssetLibrarySeeder.DEMO_SEAL_ASSET_KEY,
                AssetLibraryAssetClass.SEAL
        );
        when(libraryAssetRepository.findById(DemoAssetLibrarySeeder.DEMO_IMAGE_ASSET_KEY))
                .thenReturn(Optional.of(activeImage));
        when(libraryAssetRepository.findById(DemoAssetLibrarySeeder.DEMO_SEAL_ASSET_KEY))
                .thenReturn(Optional.of(activeSeal));

        seeder.seedIfNeeded();

        verify(assetLibraryService, never()).upload(any(), any(), any(), any());
        verify(objectStoragePort, never()).delete(any());
    }

    @Test
    void runIsNoOpWhenSeedDisabled() {
        properties.setSeedEnabled(false);

        seeder.run(null);

        verify(assetLibraryService, never()).upload(any(), any(), any(), any());
        verify(libraryAssetRepository, never()).findById(any());
    }

    @Test
    void seededKeysAreNotClasspathGhostPaths() {
        // BDD-SYS-NORM-W8-003 — managed keys are catalog keys, not rendering/demo-images/ paths.
        assertThat(DemoAssetLibrarySeeder.DEMO_IMAGE_ASSET_KEY).isEqualTo("IMG-1");
        assertThat(DemoAssetLibrarySeeder.DEMO_SEAL_ASSET_KEY).isEqualTo("SEAL-1");
        assertThat(DemoAssetLibrarySeeder.DEMO_IMAGE_ASSET_KEY).doesNotContain("demo-images");
        assertThat(DemoAssetLibrarySeeder.DEMO_SEAL_ASSET_KEY).doesNotContain("/");
    }

    private static AssetLibraryAssetView view(String key, AssetLibraryAssetClass assetClass) {
        return new AssetLibraryAssetView(
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

    private static LibraryAssetEntity activeEntity(String key, AssetLibraryAssetClass assetClass) {
        return new LibraryAssetEntity(
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
