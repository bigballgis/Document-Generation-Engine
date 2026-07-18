package com.bank.docgen.rendering;

import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

/**
 * Package-private list / content-module expansion helpers for structured DOCX writes.
 */
final class StructuredContentDocxExpandSupport {

    private static final int MAX_NUMBERING_LEVELS = 4;

    private final ObjectMapper objectMapper;
    private final XWPFDocument document;
    private final DocxListNumberingSupport listSupport;
    private final StructuredContentDocxInlineSupport inlineSupport;
    private final StructuredContentDocxStyleSupport styles;
    private final StructuredContentDocxCursorSupport cursor;
    private final Map<String, Object> variables;
    private final Map<String, String> pinnedModuleStructures;
    private final int[] numberingCounters;
    private final BiConsumer<JsonNode, XWPFParagraph> writeBlockNodesReuseFirst;
    private final BiConsumer<JsonNode, XWPFParagraph> writeBlockNode;

    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    StructuredContentDocxExpandSupport(
            ObjectMapper objectMapper,
            XWPFDocument document,
            DocxListNumberingSupport listSupport,
            StructuredContentDocxInlineSupport inlineSupport,
            StructuredContentDocxStyleSupport styles,
            StructuredContentDocxCursorSupport cursor,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures,
            int[] numberingCounters,
            BiConsumer<JsonNode, XWPFParagraph> writeBlockNodesReuseFirst,
            BiConsumer<JsonNode, XWPFParagraph> writeBlockNode
    ) {
        this.objectMapper = objectMapper;
        this.document = document;
        this.listSupport = listSupport;
        this.inlineSupport = inlineSupport;
        this.styles = styles;
        this.cursor = cursor;
        this.variables = variables;
        this.pinnedModuleStructures = pinnedModuleStructures;
        this.numberingCounters = numberingCounters;
        this.writeBlockNodesReuseFirst = writeBlockNodesReuseFirst;
        this.writeBlockNode = writeBlockNode;
    }

    void expandContentModule(JsonNode node, XWPFParagraph paragraph) {
        String referenceKey = node.path("referenceKey").asText("").trim().toUpperCase(Locale.ROOT);
        String pinnedStructure = pinnedModuleStructures.get(referenceKey);
        if (pinnedStructure == null || pinnedStructure.isBlank()) {
            throw new DocxAssemblyException(
                    ApiErrorCodes.CONTENT_MODULE_STRUCTURE_MISSING,
                    ApiErrorCategories.VALIDATION,
                    "api.error.validation.contentModuleStructureMissing",
                    "Content module pinned structure is missing for reference: " + referenceKey
            );
        }
        try {
            JsonNode root = objectMapper.readTree(pinnedStructure);
            writeBlockNodesReuseFirst.accept(StructuredContentDocxWriter.resolveRootNodes(root), paragraph);
        } catch (IOException ex) {
            throw new DocxAssemblyException(ex);
        }
    }

    void writeList(JsonNode listNode, XWPFParagraph firstParagraph) {
        boolean ordered = listNode.path("ordered").asBoolean(false)
                || "ordered".equalsIgnoreCase(listNode.path("listStyle").asText(""));
        JsonNode children = listNode.path("children");
        if (!children.isArray()) {
            return;
        }
        XWPFParagraph current = firstParagraph;
        for (int index = 0; index < children.size(); index++) {
            JsonNode item = children.get(index);
            if (current == null) {
                current = document.createParagraph();
            }
            listSupport.applyListFormatting(current, ordered);
            if ("paragraph".equals(item.path("type").asText(""))) {
                JsonNode itemDirectFormat = item.get("directFormat");
                styles.applyParagraphDirectFormat(current, itemDirectFormat);
                inlineSupport.writeInlineChildren(item, current, itemDirectFormat);
            } else {
                styles.applyParagraphDirectFormat(current, item.get("directFormat"));
                inlineSupport.writeInlineNode(item, current, false, false, false, item.get("directFormat"));
            }
            current = null;
        }
    }

    void writeAttachmentListRef(JsonNode node, XWPFParagraph firstParagraph) {
        AttachmentListRefDocxSupport.writeAttachmentListRef(
                node,
                firstParagraph,
                variables,
                listSupport,
                styles,
                cursor,
                document
        );
    }

    void writeSectionHeading(JsonNode node, XWPFParagraph paragraph) {
        JsonNode directFormat = node.get("directFormat");
        String prefix = StructuredContentDocxCursorSupport.resolveNumberingPrefix(
                node, numberingCounters, MAX_NUMBERING_LEVELS);
        if (!prefix.isBlank()) {
            styles.writeRunText(paragraph, prefix + " ", true, false, false, directFormat);
        }
        inlineSupport.writeInlineChildren(node, paragraph, directFormat);
    }

    void writeInlineOrBlockChildren(JsonNode node, XWPFParagraph paragraph) {
        JsonNode children = node.path("children");
        if (!children.isArray()) {
            return;
        }
        for (int index = 0; index < children.size(); index++) {
            JsonNode child = children.get(index);
            if (StructuredContentDocxCursorSupport.isBlockLevelType(child.path("type").asText(""))) {
                if (index == 0) {
                    writeBlockNode.accept(child, paragraph);
                } else {
                    writeBlockNode.accept(child, cursor.insertParagraphAfter(paragraph));
                }
            } else {
                inlineSupport.writeInlineNode(child, paragraph, false, false, false);
            }
        }
    }
}
