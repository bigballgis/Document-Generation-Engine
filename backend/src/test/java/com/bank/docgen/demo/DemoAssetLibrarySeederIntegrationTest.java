package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.library.domain.AssetLibraryAssetClass;
import com.bank.docgen.library.domain.AssetLibraryAssetStatus;
import com.bank.docgen.library.persistence.LibraryAssetEntity;
import com.bank.docgen.library.persistence.LibraryAssetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "docgen.demo-asset-library.seed-enabled=true",
        // Isolate from suite-wide shared H2 + filesystem bucket pollution.
        "spring.datasource.url=jdbc:h2:mem:docgen-demo-asset-seed;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "docgen.storage.bucket=docgen-demo-asset-seed"
})
class DemoAssetLibrarySeederIntegrationTest {

    @Autowired
    private LibraryAssetRepository libraryAssetRepository;

    @Autowired
    private DemoAssetLibrarySeeder seeder;

    @Test
    void seedsManagedImg1AndSeal1LibraryAssets() throws Exception {
        // BDD-SYS-NORM-W8-002 — demo/验收 seed path inserts real managed rows.
        // Explicit re-seed: ApplicationRunner may have run before a shared-context wipe.
        seeder.seedIfNeeded();

        LibraryAssetEntity image = libraryAssetRepository.findById(DemoAssetLibrarySeeder.DEMO_IMAGE_ASSET_KEY)
                .orElseThrow();
        LibraryAssetEntity seal = libraryAssetRepository.findById(DemoAssetLibrarySeeder.DEMO_SEAL_ASSET_KEY)
                .orElseThrow();

        assertThat(image.getAssetClass()).isEqualTo(AssetLibraryAssetClass.IMAGE);
        assertThat(image.getStatus()).isEqualTo(AssetLibraryAssetStatus.ACTIVE);
        assertThat(image.getContentType()).isEqualTo("image/png");
        assertThat(image.getSizeBytes()).isPositive();
        assertThat(image.getAssetKey()).doesNotContain("demo-images");

        assertThat(seal.getAssetClass()).isEqualTo(AssetLibraryAssetClass.SEAL);
        assertThat(seal.getStatus()).isEqualTo(AssetLibraryAssetStatus.ACTIVE);
        assertThat(seal.getAssetKey()).isEqualTo("SEAL-1");
    }
}
