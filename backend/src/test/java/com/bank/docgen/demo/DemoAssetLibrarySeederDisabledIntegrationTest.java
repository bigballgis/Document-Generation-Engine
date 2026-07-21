package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.library.persistence.LibraryAssetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "docgen.demo-asset-library.seed-enabled=false",
        "spring.datasource.url=jdbc:h2:mem:docgen-demo-asset-seed-off;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "docgen.storage.bucket=docgen-demo-asset-seed-off"
})
class DemoAssetLibrarySeederDisabledIntegrationTest {

    @Autowired
    private LibraryAssetRepository libraryAssetRepository;

    @Test
    void doesNotSeedManagedAssetsWhenDisabled() {
        assertThat(libraryAssetRepository.findByGroupCodeAndAssetKeyAndDeletedAtIsNull(
                "CORP", DemoAssetLibrarySeeder.DEMO_IMAGE_ASSET_KEY)).isEmpty();
        assertThat(libraryAssetRepository.findByGroupCodeAndAssetKeyAndDeletedAtIsNull(
                "RETAIL", DemoAssetLibrarySeeder.DEMO_IMAGE_ASSET_KEY)).isEmpty();
        assertThat(libraryAssetRepository.findByGroupCodeAndAssetKeyAndDeletedAtIsNull(
                "CORP", DemoAssetLibrarySeeder.DEMO_SEAL_ASSET_KEY)).isEmpty();
    }
}
