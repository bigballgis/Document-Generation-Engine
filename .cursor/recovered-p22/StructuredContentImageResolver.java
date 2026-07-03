package com.bank.docgen.rendering;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Optional;
import org.springframework.core.io.ClassPathResource;

/**
 * Resolves demo image and seal references to embeddable PNG bytes from classpath assets.
 */
public class StructuredContentImageResolver {

    private static final String DEMO_IMAGE_PREFIX = "rendering/demo-images/";

    public Optional<ResolvedImage> resolveImageRef(String imageRef) {
        return resolveReference(imageRef, "image");
    }

    public Optional<ResolvedImage> resolveSealRef(String referenceKey) {
        return resolveReference(referenceKey, "seal");
    }

    private Optional<ResolvedImage> resolveReference(String reference, String fallbackName) {
        if (reference == null || reference.isBlank()) {
            return Optional.empty();
        }
        String normalized = reference.trim();
        Optional<ResolvedImage> direct = loadClasspath(normalized + ".png", normalized + ".png");
        if (direct.isPresent()) {
            return direct;
        }
        String sanitized = normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        return loadClasspath(sanitized + ".png", fallbackName + "-" + sanitized + ".png");
    }

    private Optional<ResolvedImage> loadClasspath(String resourceName, String fileName) {
        String path = DEMO_IMAGE_PREFIX + resourceName;
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            return Optional.empty();
        }
        try (InputStream inputStream = resource.getInputStream()) {
            return Optional.of(new ResolvedImage(inputStream.readAllBytes(), fileName));
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    public record ResolvedImage(byte[] bytes, String fileName) {
    }
}
