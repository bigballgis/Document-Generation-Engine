package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.authorization.management.domain.GroupDimension;
import com.bank.docgen.authorization.management.persistence.BusinessGroupEntity;
import com.bank.docgen.authorization.management.persistence.BusinessGroupRepository;
import com.bank.docgen.library.domain.AssetLibraryAssetClass;
import com.bank.docgen.library.domain.AssetLibraryAssetStatus;
import com.bank.docgen.library.persistence.LibraryAssetEntity;
import com.bank.docgen.library.persistence.LibraryAssetRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "docgen.demo-asset-library.seed-enabled=true",
        "spring.datasource.url=jdbc:h2:mem:docgen-demo-asset-seed;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "docgen.storage.bucket=docgen-demo-asset-seed"
})
class DemoAssetLibrarySeederIntegrationTest {

    @Autowired
    private LibraryAssetRepository libraryAssetRepository;

    @Autowired
    private BusinessGroupRepository businessGroupRepository;

    @Autowired
    private DemoAssetLibrarySeeder seeder;

    @BeforeEach
    void ensureSeedGroups() {
        ensureGroup("CORP");
        ensureGroup("RETAIL");
    }

    @Test
    void seedsManagedImg1AndSeal1LibraryAssetsPerGroup() throws Exception {
        // BDD-ALGI-017 / BDD-SYS-NORM-W8-002
        seeder.seedIfNeeded();

        LibraryAssetEntity corpImage = libraryAssetRepository
                .findByGroupCodeAndAssetKeyAndDeletedAtIsNull("CORP", DemoAssetLibrarySeeder.DEMO_IMAGE_ASSET_KEY)
                .orElseThrow();
        LibraryAssetEntity retailImage = libraryAssetRepository
                .findByGroupCodeAndAssetKeyAndDeletedAtIsNull("RETAIL", DemoAssetLibrarySeeder.DEMO_IMAGE_ASSET_KEY)
                .orElseThrow();
        LibraryAssetEntity corpSeal = libraryAssetRepository
                .findByGroupCodeAndAssetKeyAndDeletedAtIsNull("CORP", DemoAssetLibrarySeeder.DEMO_SEAL_ASSET_KEY)
                .orElseThrow();

        assertThat(corpImage.getAssetClass()).isEqualTo(AssetLibraryAssetClass.IMAGE);
        assertThat(corpImage.getStatus()).isEqualTo(AssetLibraryAssetStatus.ACTIVE);
        assertThat(corpImage.getContentType()).isEqualTo("image/png");
        assertThat(corpImage.getSizeBytes()).isPositive();
        assertThat(corpImage.getGroupCode()).isEqualTo("CORP");

        assertThat(retailImage.getGroupCode()).isEqualTo("RETAIL");
        assertThat(retailImage.getAssetKey()).isEqualTo("IMG-1");

        assertThat(corpSeal.getAssetClass()).isEqualTo(AssetLibraryAssetClass.SEAL);
        assertThat(corpSeal.getStatus()).isEqualTo(AssetLibraryAssetStatus.ACTIVE);
        assertThat(corpSeal.getAssetKey()).isEqualTo("SEAL-1");
    }

    private void ensureGroup(String groupCode) {
        if (businessGroupRepository.findByGroupCodeAndDeletedAtIsNull(groupCode).isEmpty()) {
            businessGroupRepository.save(
                    new BusinessGroupEntity(UUID.randomUUID(), groupCode, groupCode, GroupDimension.BUSINESS_LINE)
            );
        }
    }
}
