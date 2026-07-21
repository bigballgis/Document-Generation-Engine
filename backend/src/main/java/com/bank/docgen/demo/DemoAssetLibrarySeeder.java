package com.bank.docgen.demo;

import com.bank.docgen.authorization.management.persistence.BusinessGroupEntity;
import com.bank.docgen.authorization.management.persistence.BusinessGroupRepository;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.library.domain.AssetLibraryAssetClass;
import com.bank.docgen.library.domain.AssetLibraryAssetStatus;
import com.bank.docgen.library.persistence.LibraryAssetEntity;
import com.bank.docgen.library.persistence.LibraryAssetRepository;
import com.bank.docgen.library.service.AssetLibraryService;
import com.bank.docgen.library.service.AssetLibraryStorageKeys;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Optional demo/验收 bootstrap of <strong>managed</strong> Asset Library rows (SYS-NORM Wave 8 + ALGI-C13).
 *
 * <p>When enabled, seeds group-scoped ACTIVE {@code IMG-1} / {@code SEAL-1} for each existing seeded
 * business group among {@code CORP,RETAIL,TRADE,WEALTH} — not a platform-shared unscoped row.
 *
 * <p>Off by default. Enable: {@code DOCGEN_SEED_DEMO_ASSET_LIBRARY=true} /
 * {@code docgen.demo-asset-library.seed-enabled=true}.
 */
@Component
@Order(48)
@ConditionalOnProperty(prefix = "docgen.demo-asset-library", name = "seed-enabled", havingValue = "true")
@EnableConfigurationProperties(DemoAssetLibrarySeedProperties.class)
public class DemoAssetLibrarySeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoAssetLibrarySeeder.class);

    static final String DEMO_IMAGE_ASSET_KEY = "IMG-1";
    static final String DEMO_SEAL_ASSET_KEY = "SEAL-1";
    private static final String DEMO_IMAGE_CLASSPATH = "rendering/demo-images/img-1.png";
    private static final String DEMO_SEAL_CLASSPATH = "rendering/demo-images/seal-1.png";
    private static final Set<String> DEMO_SEED_GROUPS = Set.of("CORP", "RETAIL", "TRADE", "WEALTH");

    private final DemoAssetLibrarySeedProperties properties;
    private final LibraryAssetRepository libraryAssetRepository;
    private final BusinessGroupRepository businessGroupRepository;
    private final AssetLibraryService assetLibraryService;
    private final ObjectStoragePort objectStoragePort;

    public DemoAssetLibrarySeeder(
            DemoAssetLibrarySeedProperties properties,
            LibraryAssetRepository libraryAssetRepository,
            BusinessGroupRepository businessGroupRepository,
            AssetLibraryService assetLibraryService,
            ObjectStoragePort objectStoragePort
    ) {
        this.properties = properties;
        this.libraryAssetRepository = libraryAssetRepository;
        this.businessGroupRepository = businessGroupRepository;
        this.assetLibraryService = assetLibraryService;
        this.objectStoragePort = objectStoragePort;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isSeedEnabled()) {
            return;
        }
        try {
            seedIfNeeded();
        } catch (Exception ex) {
            log.error("Demo asset library seed failed: {}", ex.getMessage(), ex);
        }
    }

    void seedIfNeeded() throws IOException {
        List<String> groupCodes = businessGroupRepository.findByDeletedAtIsNullOrderByGroupCodeAsc().stream()
                .map(BusinessGroupEntity::getGroupCode)
                .filter(DEMO_SEED_GROUPS::contains)
                .toList();
        if (groupCodes.isEmpty()) {
            log.warn("Demo asset library seed skipped: no CORP/RETAIL/TRADE/WEALTH groups present");
            return;
        }
        for (String groupCode : groupCodes) {
            seedManagedAsset(
                    groupCode,
                    DEMO_IMAGE_ASSET_KEY,
                    AssetLibraryAssetClass.IMAGE,
                    DEMO_IMAGE_CLASSPATH,
                    "img-1.png"
            );
            seedManagedAsset(
                    groupCode,
                    DEMO_SEAL_ASSET_KEY,
                    AssetLibraryAssetClass.SEAL,
                    DEMO_SEAL_CLASSPATH,
                    "seal-1.png"
            );
        }
    }

    private void seedManagedAsset(
            String groupCode,
            String assetKey,
            AssetLibraryAssetClass assetClass,
            String classpathResource,
            String originalFileName
    ) throws IOException {
        Optional<LibraryAssetEntity> existing =
                libraryAssetRepository.findByGroupCodeAndAssetKeyAndDeletedAtIsNull(groupCode, assetKey);
        if (existing.isPresent() && existing.get().getStatus() == AssetLibraryAssetStatus.ACTIVE) {
            log.info("Demo managed asset {}/{} already ACTIVE; skipping", groupCode, assetKey);
            return;
        }
        String objectKey = AssetLibraryStorageKeys.namespacedKey(groupCode, assetKey);
        if (objectStoragePort.exists(objectKey)) {
            objectStoragePort.delete(objectKey);
        }
        // Clear legacy bare key orphan if present.
        if (objectStoragePort.exists(assetKey)) {
            objectStoragePort.delete(assetKey);
        }
        byte[] bytes = readClasspathBytes(classpathResource);
        MultipartFile file = new ByteArrayMultipartFile(
                "file",
                originalFileName,
                "image/png",
                bytes
        );
        assetLibraryService.upload(seedSession(), file, assetKey, assetClass, groupCode);
        log.info("Seeded managed library asset {}/{} ({})", groupCode, assetKey, assetClass);
    }

    private static byte[] readClasspathBytes(String classpathResource) throws IOException {
        ClassPathResource resource = new ClassPathResource(classpathResource);
        if (!resource.exists()) {
            throw new IllegalStateException("Demo asset classpath resource missing: " + classpathResource);
        }
        try (InputStream inputStream = resource.getInputStream()) {
            return inputStream.readAllBytes();
        }
    }

    private static ManagementSessionClaims seedSession() {
        return DemoCatalogSessions.globalAdminSession();
    }
}
