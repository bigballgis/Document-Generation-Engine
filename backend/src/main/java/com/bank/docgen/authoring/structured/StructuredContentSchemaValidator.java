package com.bank.docgen.authoring.structured;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Validates {@code structured_content_json} against the confirmed v1 node matrix (P18-T01).
 */
@Component
public class StructuredContentSchemaValidator {

    public static final String MESSAGE_KEY_INVALID = "api.error.template.structuredContentInvalid";
    public static final String MESSAGE_KEY_UNKNOWN_NODE = "api.error.template.structuredContentUnknownNodeType";
    public static final String MESSAGE_KEY_FORBIDDEN = "api.error.template.structuredContentForbiddenConstruct";

    private static final List<Pattern> FORBIDDEN_VALUE_PATTERNS = List.of(
            Pattern.compile("<\\s*script", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<\\s*iframe", Pattern.CASE_INSENSITIVE),
            Pattern.compile("javascript\\s*:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("position\\s*:\\s*absolute", Pattern.CASE_INSENSITIVE)
    );

    private static final List<String> FORBIDDEN_FIELD_NAMES = List.of(
            "html",
            "rawHtml",
            "rawHtmlContent",
            "css",
            "styleSheet",
            "absolutePosition"
    );

    private final ObjectMapper objectMapper;

    public StructuredContentSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void validate(String structuredContentJson) {
        try {
            JsonNode root = objectMapper.readTree(structuredContentJson);
            if (!root.isObject() || !root.has("nodes") || !root.get("nodes").isArray()) {
                throw new StructuredContentSchemaException(MESSAGE_KEY_INVALID);
            }
            scanForbiddenConstructs(root);
            for (JsonNode node : root.get("nodes")) {
                validateNode(node);
            }
        } catch (StructuredContentSchemaException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new StructuredContentSchemaException(MESSAGE_KEY_INVALID, ex);
        }
    }

    private void validateNode(JsonNode node) {
        if (!node.isObject()) {
            throw new StructuredContentSchemaException(MESSAGE_KEY_INVALID);
        }
        String rawType = node.path("type").asText(null);
        StructuredContentNodeType nodeType = StructuredContentNodeType.fromJsonType(rawType)
                .orElseThrow(() -> new StructuredContentSchemaException(MESSAGE_KEY_UNKNOWN_NODE));
        scanForbiddenConstructs(node);
        if (nodeType.category() == StructuredContentNodeType.NodeCategory.INLINE) {
            return;
        }
        JsonNode children = node.get("children");
        if (children != null) {
            if (!children.isArray()) {
                throw new StructuredContentSchemaException(MESSAGE_KEY_INVALID);
            }
            for (JsonNode child : children) {
                validateNode(child);
            }
        }
    }

    private void scanForbiddenConstructs(JsonNode node) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String name = field.getKey();
                if (FORBIDDEN_FIELD_NAMES.contains(name.toLowerCase(Locale.ROOT))) {
                    throw new StructuredContentSchemaException(MESSAGE_KEY_FORBIDDEN);
                }
                scanForbiddenConstructs(field.getValue());
            }
            return;
        }
        if (node.isArray()) {
            for (JsonNode element : node) {
                scanForbiddenConstructs(element);
            }
            return;
        }
        if (node.isTextual()) {
            String value = node.asText();
            for (Pattern pattern : FORBIDDEN_VALUE_PATTERNS) {
                if (pattern.matcher(value).find()) {
                    throw new StructuredContentSchemaException(MESSAGE_KEY_FORBIDDEN);
                }
            }
        }
    }
}
