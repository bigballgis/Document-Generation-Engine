package com.bank.docgen.documentbrand.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

/**
 * Template {@code allowedDocumentBrandCodes} JSON helper.
 *
 * <p>ADR-0071 / Wave 6: writes always strip to empty/null (no catalog resurrection).
 * Parse remains for historical rows until Flyway clears them; generate ignores allow-list.
 */
public final class AllowedDocumentBrandCodesJsonSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {
    };

    private AllowedDocumentBrandCodesJsonSupport() {
    }

    public static List<String> parse(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> values = MAPPER.readValue(json, LIST_TYPE);
            if (values == null || values.isEmpty()) {
                return List.of();
            }
            return values.stream()
                    .filter(code -> code != null && !code.isBlank())
                    .map(String::trim)
                    .toList();
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    /**
     * Wave 6 lock: any write (including non-empty historical allow-lists) persists as empty.
     */
    public static String write(List<String> codes) {
        return null;
    }
}
