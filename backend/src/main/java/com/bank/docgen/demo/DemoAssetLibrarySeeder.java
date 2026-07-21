package com.bank.docgen.demo;

import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.library.domain.AssetLibraryAssetClass;
import com.bank.docgen.library.domain.AssetLibraryAssetStatus;
import com.bank.docgen.library.persistence.LibraryAssetEntity;
import com.bank.docgen.library.persistence.LibraryAssetRepository;
import com.bank.docgen.library.service.AssetLibraryService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
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
 * Optional demo/验收 bootstrap of <strong>managed</strong> Asset Library rows (SYS-NORM Wave 8).
 *
 * <p>Inserts real {@code library_asset} + MinIO object bytes for keys aligned with demo bindings
 * ({@code IMG-1}, {@code SEAL-1}). Classpath {@code rendering/demo-images/} is used only as the
 * <em>source bytes</em> for those managed uploads — it is <strong>not</strong> catalog content (N23).
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

    private final DemoAssetLibrarySeedProperties properties;
    private final LibraryAssetRepository libraryAssetRepository;
    private final AssetLibraryService assetLibraryService;
    private final ObjectStoragePort objectStoragePort;

    public DemoAssetLibrarySeeder(
            DemoAssetLibrarySeedProperties properties,
            LibraryAssetRepository libraryAssetRepository,
            AssetLibraryService assetLibraryService,
            ObjectStoragePort objectStoragePort
    ) {
        this.properties = properties;
        this.libraryAssetRepository = libraryAssetRepository;
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
        seedManagedAsset(DEMO_IMAGE_ASSET_KEY, AssetLibraryAssetClass.IMAGE, DEMO_IMAGE_CLASSPATH, "img-1.png");
        seedManagedAsset(DEMO_SEAL_ASSET_KEY, AssetLibraryAssetClass.SEAL, DEMO_SEAL_CLASSPATH, "seal-1.png");
    }

    private void seedManagedAsset(
            String assetKey,
            AssetLibraryAssetClass assetClass,
            String classpathResource,
            String originalFileName
    ) throws IOException {
        Optional<LibraryAssetEntity> existing = libraryAssetRepository.findById(assetKey);
        if (existing.isPresent() && existing.get().getStatus() == AssetLibraryAssetStatus.ACTIVE) {
            log.info("Demo managed asset {} already ACTIVE; skipping", assetKey);
            return;
        }
        // Filesystem test storage does not overwrite; clear orphans when catalog row is absent.
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
        assetLibraryService.upload(seedSession(), file, assetKey, assetClass);
        log.info("Seeded managed library asset {} ({})", assetKey, assetClass);
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
