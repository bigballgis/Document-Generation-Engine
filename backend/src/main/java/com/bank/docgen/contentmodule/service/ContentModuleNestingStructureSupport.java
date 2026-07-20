package com.bank.docgen.contentmodule.service;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;

/**
 * Package-private helpers to extract {@code contentModuleRef.referenceKey} values from structure JSON.
 */
final class ContentModuleNestingStructureSupport {

    private ContentModuleNestingStructureSupport() {
    }

    /**
     * Extract distinct nest reference keys. Blank/null → empty set.
     * Malformed JSON → fail-closed {@link ContentModuleGovernanceException} (422).
     */
    static Set<String> extractReferenceKeys(ObjectMapper objectMapper, String contentStructureJson) {
        if (contentStructureJson == null || contentStructureJson.isBlank()) {
            return Set.of();
        }
        try {
            JsonNode root = objectMapper.readTree(contentStructureJson);
            Set<String> keys = new LinkedHashSet<>();
            collectReferenceKeys(root, keys);
            return keys;
        } catch (IOException ex) {
            throw new ContentModuleGovernanceException(
                    ApiErrorCodes.CONTENT_MODULE_NESTING_STRUCTURE_INVALID,
                    "api.error.contentModule.nestingStructureInvalid",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }
    }

    private static void collectReferenceKeys(JsonNode node, Set<String> keys) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            if ("contentModuleRef".equals(node.path("type").asText(""))) {
                String key = node.path("referenceKey").asText("").trim().toUpperCase(Locale.ROOT);
                if (!key.isEmpty()) {
                    keys.add(key);
                }
            }
            node.properties().forEach(entry -> collectReferenceKeys(entry.getValue(), keys));
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectReferenceKeys(child, keys);
            }
        }
    }
}
