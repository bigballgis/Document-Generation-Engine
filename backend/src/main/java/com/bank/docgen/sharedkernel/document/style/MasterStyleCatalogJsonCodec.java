package com.bank.docgen.sharedkernel.document.style;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Serializes / deserializes durable master style catalogs for revision persistence.
 */
public final class MasterStyleCatalogJsonCodec {

    private MasterStyleCatalogJsonCodec() {
    }

    public static String write(ObjectMapper objectMapper, MasterStyleCatalog catalog) {
        try {
            return objectMapper.writeValueAsString(catalog);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize master style catalog", ex);
        }
    }

    public static MasterStyleCatalog read(ObjectMapper objectMapper, String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, MasterStyleCatalog.class);
        } catch (JsonProcessingException ex) {
            throw new MasterDocxStyleCatalogParseException("Unable to deserialize persisted style catalog", ex);
        }
    }
}
