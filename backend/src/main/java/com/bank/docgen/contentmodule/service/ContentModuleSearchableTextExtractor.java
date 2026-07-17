package com.bank.docgen.contentmodule.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * CE-G05 — extract human-readable text from {@code content_structure_json} for tsvector.
 * Covers paragraph / text / list item leaves; JSON key names and structural enums are skipped.
 */
@Component
public class ContentModuleSearchableTextExtractor {

    private static final Set<String> TEXT_FIELD_NAMES = Set.of(
            "text", "content", "value", "label", "title", "heading", "caption"
    );

    private final ObjectMapper objectMapper;

    public ContentModuleSearchableTextExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String extract(String contentStructureJson) {
        if (contentStructureJson == null || contentStructureJson.isBlank()) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(contentStructureJson);
            List<String> parts = new ArrayList<>();
            collect(root, parts);
            return String.join(" ", parts).trim();
        } catch (IOException | RuntimeException ignored) {
            return "";
        }
    }

    private void collect(JsonNode node, List<String> parts) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collect(child, parts);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isTextual()) {
                if (isTextField(key)) {
                    String text = value.asText();
                    if (text != null && !text.isBlank()) {
                        parts.add(text.trim());
                    }
                }
                continue;
            }
            collect(value, parts);
        }
    }

    private static boolean isTextField(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        return TEXT_FIELD_NAMES.contains(normalized)
                || normalized.endsWith("text")
                || "textrun".equals(normalized);
    }
}
