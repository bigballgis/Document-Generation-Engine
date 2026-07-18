package com.bank.docgen.rendering.goldencorpus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

/**
 * Loads and validates DOCX / PDF assertion configs. Rejects pixel / screenshot types (K07-C6).
 */
public final class GoldenCorpusAssertionLoader {

    private static final Set<String> FORBIDDEN_TYPES = Set.of(
            "PIXEL",
            "PIXEL_COMPARE",
            "SCREENSHOT",
            "IMAGE_HASH",
            "SSIM",
            "BITMAP",
            "VISUAL_GOLDEN"
    );

    private static final Set<String> DOCX_TYPES = Set.of("XML_CONTAINS", "XML_NOT_CONTAINS", "XPATH_EXISTS");
    /** PDF text + layout-metric kinds (IBL-C1). Pixel/visual kinds remain forbidden. */
    private static final Set<String> PDF_TYPES = Set.of(
            "TEXT_CONTAINS",
            "TEXT_NOT_CONTAINS",
            "PAGE_COUNT",
            "TEXT_POSITION"
    );

    private final ObjectMapper objectMapper;

    public GoldenCorpusAssertionLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode loadDocxAssertions(Path packageDir) {
        return loadAndValidate(packageDir.resolve(GoldenCorpusPackageLayout.EXPECTED_DOCX), DOCX_TYPES);
    }

    public JsonNode loadPdfAssertions(Path packageDir) {
        return loadAndValidate(packageDir.resolve(GoldenCorpusPackageLayout.EXPECTED_PDF), PDF_TYPES);
    }

    public JsonNode loadAndValidate(Path assertionFile, Set<String> allowedTypes) {
        try (InputStream input = Files.newInputStream(assertionFile)) {
            JsonNode root = objectMapper.readTree(input);
            JsonNode assertions = root.path("assertions");
            if (assertions.isMissingNode() || assertions.isNull()) {
                return root;
            }
            if (!assertions.isArray()) {
                throw new GoldenCorpusException("assertions must be an array in " + assertionFile);
            }
            for (JsonNode assertion : assertions) {
                String type = assertion.path("type").asText("").trim().toUpperCase(Locale.ROOT);
                if (type.isEmpty()) {
                    throw new GoldenCorpusException("assertion.type is required in " + assertionFile);
                }
                if (FORBIDDEN_TYPES.contains(type) || type.contains("PIXEL") || type.contains("SCREENSHOT")) {
                    throw new GoldenCorpusException(
                            "Forbidden pixel/visual assertion type '" + type + "' in " + assertionFile
                                    + " — PIXEL_* rejected unless PD-2 ADR Accepted; use DOCX XML/XPath,"
                                    + " PDF text, PAGE_COUNT, or TEXT_POSITION"
                    );
                }
                if (!allowedTypes.contains(type)) {
                    throw new GoldenCorpusException(
                            "Unsupported assertion type '" + type + "' in " + assertionFile
                                    + "; allowed=" + allowedTypes
                    );
                }
            }
            return root;
        } catch (IOException ex) {
            throw new GoldenCorpusException("Failed to read assertions " + assertionFile, ex);
        }
    }
}
