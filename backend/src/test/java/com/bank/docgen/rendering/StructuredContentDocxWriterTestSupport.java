package com.bank.docgen.rendering;

import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalog;
import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalogEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.core.io.ClassPathResource;

/**
 * Shared fixtures for {@link StructuredContentDocxWriterTest} and assembler fidelity extensions.
 */
public final class StructuredContentDocxWriterTestSupport {

    private static final String DEFAULT_STYLE_CATALOG_RESOURCE = "authoring/default-master-style-catalog-v1.json";

    private StructuredContentDocxWriterTestSupport() {
    }

    public static StructuredContentDocxWriter createWriter(ObjectMapper objectMapper) {
        return createWriter(objectMapper, demoTierResolver());
    }

    public static StructuredContentDocxWriter createWriter(
            ObjectMapper objectMapper,
            StructuredContentImageResolver imageResolver
    ) {
        return new StructuredContentDocxWriter(
                objectMapper,
                loadDefaultStyleCatalog(objectMapper),
                imageResolver
        );
    }

    private static StructuredContentImageResolver demoTierResolver() {
        return new StructuredContentImageResolver(EMPTY_OBJECT_STORAGE, true);
    }

    static StructuredContentImageResolver demoTierImageResolver() {
        return demoTierResolver();
    }

    public static DocxAssembler createAssembler(ObjectMapper objectMapper) {
        return new DocxAssembler(objectMapper, demoTierResolver());
    }

    private static final com.bank.docgen.infrastructure.storage.ObjectStoragePort EMPTY_OBJECT_STORAGE =
            new com.bank.docgen.infrastructure.storage.ObjectStoragePort() {
                @Override
                public void put(String objectKey, InputStream content, long contentLength, String contentType) {
                    // no-op for fidelity tests
                }

                @Override
                public InputStream get(String objectKey) {
                    throw new UnsupportedOperationException("No object storage in demo-tier writer tests");
                }

                @Override
                public void delete(String objectKey) {
                    // no-op for fidelity tests
                }

                @Override
                public boolean exists(String objectKey) {
                    return false;
                }
            };

    static byte[] renderAnchorParagraph(
            StructuredContentDocxWriter writer,
            String structuredJson,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures
    ) throws IOException {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("{{anchor:BODY}}");
            writer.replaceAnchorParagraph(
                    document,
                    0,
                    structuredJson,
                    variables == null ? Map.of() : variables,
                    pinnedModuleStructures == null ? Map.of() : pinnedModuleStructures
            );
            DocxWordCompatibilitySupport.ensureWordCompatiblePackage(document);
            document.write(output);
            return output.toByteArray();
        }
    }

    static XWPFDocument openDocument(byte[] docxBytes) throws IOException {
        return new XWPFDocument(new ByteArrayInputStream(docxBytes));
    }

    static XWPFParagraph firstParagraph(byte[] docxBytes) throws IOException {
        try (XWPFDocument document = openDocument(docxBytes)) {
            return document.getParagraphs().getFirst();
        }
    }

    static MasterStyleCatalog loadDefaultStyleCatalog(ObjectMapper mapper) {
        try (InputStream inputStream = new ClassPathResource(DEFAULT_STYLE_CATALOG_RESOURCE).getInputStream()) {
            JsonNode root = mapper.readTree(inputStream);
            Map<String, MasterStyleCatalogEntry> styles = new HashMap<>();
            JsonNode stylesNode = root.get("styles");
            if (stylesNode != null && stylesNode.isArray()) {
                for (JsonNode styleNode : stylesNode) {
                    String styleKey = styleNode.path("styleKey").asText("");
                    if (styleKey.isBlank()) {
                        continue;
                    }
                    List<String> applicable = new ArrayList<>();
                    JsonNode applicableNode = styleNode.get("applicableNodeTypes");
                    if (applicableNode != null && applicableNode.isArray()) {
                        applicableNode.forEach(node -> applicable.add(node.asText()));
                    }
                    styles.put(
                            styleKey,
                            new MasterStyleCatalogEntry(
                                    styleKey,
                                    Set.copyOf(applicable),
                                    styleNode.path("renderPurpose").asText("")
                            )
                    );
                }
            }
            return new MasterStyleCatalog(root.path("catalogVersion").asText("1.0"), styles);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load master style catalog: " + DEFAULT_STYLE_CATALOG_RESOURCE, ex);
        }
    }
}
