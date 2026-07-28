package com.bank.docgen.rendering;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;

/**
 * POI fidelity regression safety net for {@link StructuredContentDocxWriter}.
 * BDD: BDD-F1-A1-004 — must stay green before F1-T03 dual-track removal.
 */
/**
 * Shared fixtures for StructuredContentDocxWriter* tests (AI-SCALE #169).
 */
abstract class StructuredContentDocxWriterTestFixtures {

    protected final ObjectMapper objectMapper = new ObjectMapper();
    protected StructuredContentDocxWriter writer;
    @BeforeEach
    void setUp() {
        writer = StructuredContentDocxWriterTestSupport.createWriter(objectMapper);
    }
    protected static byte[] pngBytes(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
    protected static StructuredContentImageResolver keyAgnosticResolver(byte[] bytes) {
        return new StructuredContentImageResolver(
                new com.bank.docgen.infrastructure.storage.ObjectStoragePort() {
                    @Override
                    public void put(String objectKey, java.io.InputStream content, long contentLength, String contentType) {
                    }

                    @Override
                    public java.io.InputStream get(String objectKey) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void delete(String objectKey) {
                    }

                    @Override
                    public boolean exists(String objectKey) {
                        return false;
                    }
                },
                false
        ) {
            @Override
            public ResolvedImage resolveImageRef(String imageRef) {
                return new StructuredContentImageResolver.ResolvedImage(bytes, "custom.png");
            }

            @Override
            public ResolvedImage resolveSealRef(String referenceKey) {
                return new StructuredContentImageResolver.ResolvedImage(bytes, "seal.png");
            }
        };
    }
    protected byte[] render(String structuredJson, Map<String, Object> variables) throws Exception {
        return render(structuredJson, variables, Map.of());
    }
    protected byte[] render(
            String structuredJson,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures
    ) throws Exception {
        return StructuredContentDocxWriterTestSupport.renderAnchorParagraph(
                writer,
                structuredJson,
                variables,
                pinnedModuleStructures
        );
    }
    protected static String readZipPart(byte[] docxBytes, String partName) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docxBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (partName.equals(entry.getName())) {
                    return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new IllegalStateException("DOCX part not found: " + partName);
    }
    protected static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int index = 0;
        while ((index = haystack.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
