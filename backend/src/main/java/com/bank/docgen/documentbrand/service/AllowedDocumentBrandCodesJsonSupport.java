package com.bank.docgen.documentbrand.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

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

    public static String write(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return null;
        }
        List<String> normalized = codes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .toList();
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(normalized);
        } catch (JsonProcessingException ex) {
            throw new DocumentBrandCatalogException(
                    com.bank.docgen.sharedkernel.api.ApiErrorCodes.REQUEST_BODY_INVALID,
                    "api.error.validation.requestBodyInvalid"
            );
        }
    }
}
