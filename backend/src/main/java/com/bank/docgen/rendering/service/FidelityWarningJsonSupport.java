package com.bank.docgen.rendering.service;

import com.bank.docgen.rendering.api.FidelityWarningView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Serializes and deserializes fidelity warnings stored on {@code preview_record.fidelity_warnings_json}.
 */
@Component
public class FidelityWarningJsonSupport {

    private final ObjectMapper objectMapper;

    public FidelityWarningJsonSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String writeWarnings(List<FidelityWarningView> warnings) {
        try {
            return objectMapper.writeValueAsString(warnings);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    public List<FidelityWarningView> readWarnings(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) {
                return List.of();
            }
            List<FidelityWarningView> warnings = new ArrayList<>();
            for (JsonNode node : root) {
                warnings.add(new FidelityWarningView(
                        node.path("code").asText(""),
                        node.path("messageKey").asText(""),
                        textOrNull(node, "location"),
                        textOrNull(node, "artifact"),
                        Boolean.valueOf(node.path("viewed").asBoolean(false))
                ));
            }
            return List.copyOf(warnings);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    public int countUnviewed(List<FidelityWarningView> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return 0;
        }
        return (int) warnings.stream().filter(warning -> !Boolean.TRUE.equals(warning.viewed())).count();
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text.isBlank() ? null : text;
    }
}
