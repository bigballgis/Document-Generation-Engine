package com.bank.docgen.rendering;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.library.service.AssetLibraryStorageKeys;
import com.bank.docgen.library.service.LibraryAssetActiveLookup;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Resolves image and seal references to embeddable bytes.
 *
 * <p>ALGI-C5: when a template owning {@code groupCode} is present in
 * {@link AssetResolveGroupContext}, resolve succeeds only for ACTIVE catalog
 * membership under that group and namespaced MinIO keys {@code {groupCode}/{assetKey}}.
 * Bare / foreign-group MinIO keys never satisfy managed resolve. Demo classpath tier
 * remains orthogonal (N23 / TPC; prod off).
 */
@Component
public class StructuredContentImageResolver {

    private static final Logger LOG = LoggerFactory.getLogger(StructuredContentImageResolver.class);
    private static final String DEMO_IMAGE_PREFIX = "rendering/demo-images/";

    private final ObjectStoragePort objectStoragePort;
    private final boolean demoClasspathTierEnabled;
    private final LibraryAssetActiveLookup catalogLookup;

    @Autowired
    public StructuredContentImageResolver(
            ObjectStoragePort objectStoragePort,
            DocgenRenderingProperties renderingProperties,
            LibraryAssetActiveLookup catalogLookup
    ) {
        this.objectStoragePort = objectStoragePort;
        this.demoClasspathTierEnabled = renderingProperties.isDemoClasspathImageTierEnabled();
        this.catalogLookup = catalogLookup;
    }

    /** Test-only constructor; production wiring uses {@link DocgenRenderingProperties}. */
    public StructuredContentImageResolver(ObjectStoragePort objectStoragePort, boolean demoClasspathTierEnabled) {
        this(objectStoragePort, demoClasspathTierEnabled, null);
    }

    /** Test-only constructor with optional catalog gate. */
    public StructuredContentImageResolver(
            ObjectStoragePort objectStoragePort,
            boolean demoClasspathTierEnabled,
            LibraryAssetActiveLookup catalogLookup
    ) {
        this.objectStoragePort = objectStoragePort;
        this.demoClasspathTierEnabled = demoClasspathTierEnabled;
        this.catalogLookup = catalogLookup;
    }

    public ResolvedImage resolveImageRef(String imageRef) {
        return resolveReference(imageRef, ReferenceKind.IMAGE);
    }

    public ResolvedImage resolveSealRef(String referenceKey) {
        return resolveReference(referenceKey, ReferenceKind.SEAL);
    }

    private ResolvedImage resolveReference(String reference, ReferenceKind kind) {
        if (reference == null || reference.isBlank()) {
            throw notFound(kind, reference);
        }
        String normalized = reference.trim();
        String groupCode = AssetResolveGroupContext.currentGroupCode();

        if (groupCode != null && !groupCode.isBlank()) {
            return resolveForGroup(normalized, groupCode.trim(), kind);
        }

        // No owning-group context: managed MinIO bare keys are not authoritative; demo classpath only.
        if (demoClasspathTierEnabled) {
            ResolvedImage fromClasspath = loadFromDemoClasspath(normalized, kind);
            if (fromClasspath != null) {
                LOG.info(
                        "Resolved {} reference '{}' from demo classpath tier ({})",
                        kind.logLabel(),
                        normalized,
                        DEMO_IMAGE_PREFIX
                );
                return fromClasspath;
            }
        }
        throw notFound(kind, normalized);
    }

    private ResolvedImage resolveForGroup(String normalized, String groupCode, ReferenceKind kind) {
        boolean active = catalogLookup != null && catalogLookup.isActive(groupCode, normalized);
        if (active) {
            for (String storageKey : AssetLibraryStorageKeys.namespacedResolvableKeys(groupCode, normalized)) {
                ResolvedImage fromStorage = loadFromObjectStorage(storageKey, fileNameForKey(normalized, storageKey));
                if (fromStorage != null) {
                    return fromStorage;
                }
            }
            throw notFound(kind, normalized);
        }

        // Catalog miss / DISABLED / cross-group: ignore bare or foreign MinIO; optional demo classpath.
        if (demoClasspathTierEnabled) {
            ResolvedImage fromClasspath = loadFromDemoClasspath(normalized, kind);
            if (fromClasspath != null) {
                LOG.info(
                        "Resolved {} reference '{}' from demo classpath tier ({}) after catalog miss for group {}",
                        kind.logLabel(),
                        normalized,
                        DEMO_IMAGE_PREFIX,
                        groupCode
                );
                return fromClasspath;
            }
        }
        throw notFound(kind, normalized);
    }

    private ResolvedImage loadFromObjectStorage(String storageKey, String fileName) {
        if (!objectStoragePort.exists(storageKey)) {
            return null;
        }
        try (InputStream inputStream = objectStoragePort.get(storageKey)) {
            return new ResolvedImage(inputStream.readAllBytes(), fileName);
        } catch (IOException ex) {
            throw new DocxAssemblyException(ex);
        }
    }

    private ResolvedImage loadFromDemoClasspath(String reference, ReferenceKind kind) {
        String directResource = reference.toLowerCase(Locale.ROOT) + ".png";
        ResolvedImage direct = loadClasspath(directResource, reference + ".png");
        if (direct != null) {
            return direct;
        }
        String sanitized = reference.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        return loadClasspath(sanitized + ".png", kind.fallbackFilePrefix() + "-" + sanitized + ".png");
    }

    private ResolvedImage loadClasspath(String resourceName, String fileName) {
        String path = DEMO_IMAGE_PREFIX + resourceName;
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            return null;
        }
        try (InputStream inputStream = resource.getInputStream()) {
            return new ResolvedImage(inputStream.readAllBytes(), fileName);
        } catch (IOException ex) {
            throw new DocxAssemblyException(ex);
        }
    }

    private static String fileNameForKey(String reference, String storageKey) {
        if (storageKey.contains(".")) {
            int slash = storageKey.lastIndexOf('/');
            String name = slash >= 0 ? storageKey.substring(slash + 1) : storageKey;
            return name;
        }
        return reference + ".png";
    }

    private static DocxAssemblyException notFound(ReferenceKind kind, String reference) {
        String safeReference = reference == null || reference.isBlank() ? "<empty>" : reference.trim();
        return new DocxAssemblyException(
                kind.errorCode(),
                ApiErrorCategories.RENDERING,
                kind.messageKey(),
                kind.errorMessage(safeReference)
        );
    }

    private enum ReferenceKind {
        IMAGE(ApiErrorCodes.IMAGE_ASSET_NOT_FOUND, "api.error.rendering.imageAssetNotFound", "image", "image"),
        SEAL(ApiErrorCodes.SEAL_ASSET_NOT_FOUND, "api.error.rendering.sealAssetNotFound", "seal", "seal");

        private final String errorCode;
        private final String messageKey;
        private final String fallbackFilePrefix;
        private final String logLabel;

        ReferenceKind(String errorCode, String messageKey, String fallbackFilePrefix, String logLabel) {
            this.errorCode = errorCode;
            this.messageKey = messageKey;
            this.fallbackFilePrefix = fallbackFilePrefix;
            this.logLabel = logLabel;
        }

        String errorCode() {
            return errorCode;
        }

        String messageKey() {
            return messageKey;
        }

        String fallbackFilePrefix() {
            return fallbackFilePrefix;
        }

        String logLabel() {
            return logLabel;
        }

        String errorMessage(String reference) {
            return switch (this) {
                case IMAGE -> "Image asset not found for reference: " + reference;
                case SEAL -> "Seal asset not found for reference: " + reference;
            };
        }
    }

    public record ResolvedImage(byte[] bytes, String fileName) {

        public ResolvedImage {
            bytes = DefensiveCopies.copyBytes(bytes);
        }
    }
}
