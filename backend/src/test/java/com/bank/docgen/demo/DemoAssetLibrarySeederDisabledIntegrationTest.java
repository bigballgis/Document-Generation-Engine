package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.library.persistence.LibraryAssetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:docgen-demo-asset-seed-off;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class DemoAssetLibrarySeederDisabledIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private LibraryAssetRepository libraryAssetRepository;

    @Test
    void defaultProfileDoesNotRegisterSeederOrManagedDemoAssets() {
        // BDD-SYS-NORM-W8-001 / W8-004 — seed off by default; honest empty catalog.
        assertThat(applicationContext.getBeanNamesForType(DemoAssetLibrarySeeder.class)).isEmpty();
        assertThat(libraryAssetRepository.findById(DemoAssetLibrarySeeder.DEMO_IMAGE_ASSET_KEY)).isEmpty();
        assertThat(libraryAssetRepository.findById(DemoAssetLibrarySeeder.DEMO_SEAL_ASSET_KEY)).isEmpty();
    }
}
