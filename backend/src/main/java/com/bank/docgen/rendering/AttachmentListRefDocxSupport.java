package com.bank.docgen.rendering;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

/**
 * Emits {@code attachmentListRef} as ordered numbered-list paragraphs (CE-K06c).
 */
final class AttachmentListRefDocxSupport {

    private AttachmentListRefDocxSupport() {
    }

    static void writeAttachmentListRef(
            JsonNode node,
            XWPFParagraph firstParagraph,
            Map<String, Object> variables,
            DocxListNumberingSupport listSupport,
            StructuredContentDocxStyleSupport styles,
            StructuredContentDocxCursorSupport cursor,
            XWPFDocument document
    ) {
        List<String> items = resolveItems(node, variables);
        if (items.isEmpty()) {
            return;
        }
        XWPFParagraph current = firstParagraph;
        for (int index = 0; index < items.size(); index++) {
            if (index == 0) {
                if (current == null) {
                    current = document.createParagraph();
                }
            } else {
                current = cursor.insertParagraphAfter(current);
            }
            listSupport.applyListFormatting(current, true);
            styles.writeRunText(current, items.get(index), false, false, false);
        }
    }

    static List<String> resolveItems(JsonNode node, Map<String, Object> variables) {
        String referenceKey = node.path("referenceKey").asText("").trim();
        if (referenceKey.isEmpty()) {
            throw payloadMissing();
        }
        if (variables == null || !variables.containsKey(referenceKey)) {
            throw payloadMissing();
        }
        Object value = variables.get(referenceKey);
        if (value == null) {
            throw payloadMissing();
        }
        return toStringList(value);
    }

    private static List<String> toStringList(Object value) {
        if (value instanceof String[] array) {
            return List.of(array);
        }
        if (value instanceof JsonNode jsonNode) {
            if (!jsonNode.isArray()) {
                throw payloadInvalid();
            }
            List<String> items = new ArrayList<>(jsonNode.size());
            for (JsonNode element : jsonNode) {
                if (element == null || !element.isTextual()) {
                    throw payloadInvalid();
                }
                items.add(element.asText());
            }
            return items;
        }
        if (value instanceof List<?> list) {
            List<String> items = new ArrayList<>(list.size());
            for (Object element : list) {
                if (!(element instanceof String text)) {
                    throw payloadInvalid();
                }
                items.add(text);
            }
            return items;
        }
        throw payloadInvalid();
    }

    private static DocxAssemblyException payloadMissing() {
        return new DocxAssemblyException(
                "api.error.rendering.attachmentListPayloadMissing",
                "attachmentListRef payload is missing or null for the referenced variable key"
        );
    }

    private static DocxAssemblyException payloadInvalid() {
        return new DocxAssemblyException(
                "api.error.rendering.attachmentListPayloadInvalid",
                "attachmentListRef payload must be a string array"
        );
    }
}
