package com.bank.docgen.rendering;

import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalog;
import com.bank.docgen.sharedkernel.document.expression.ConditionExpressionEvaluator;
import com.bank.docgen.sharedkernel.document.structured.DocxWriterHandledStructuredNodeTypes;
import com.bank.docgen.sharedkernel.document.structured.WriterUnsupportedStructuredNodeTypes;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.IBody;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;

/**
 * Mutable write state for {@link StructuredContentDocxWriter} anchor replacement.
 */
class StructuredContentDocxWriteSession {

    private static final int MAX_NUMBERING_LEVELS = 4;
    private static final ConditionExpressionEvaluator CONDITION_EVALUATOR = ConditionExpressionEvaluator.INSTANCE;

    private final ObjectMapper objectMapper;
    private final MasterStyleCatalog styleCatalog;
    private final StructuredContentImageResolver imageResolver;
    private final XWPFDocument document;
    private final IBody body;
    private final DocxListNumberingSupport listSupport;
    private final Map<String, Object> variables;
    private final Map<String, String> pinnedModuleStructures;
    private final int[] numberingCounters;
    private final StructuredContentDocxStyleSupport styles;
    private final StructuredContentDocxTableSupport tableSupport;
    private final StructuredContentDocxInlineSupport inlineSupport;
    private final StructuredContentDocxCursorSupport cursor;

    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    StructuredContentDocxWriteSession(
            ObjectMapper objectMapper,
            MasterStyleCatalog styleCatalog,
            StructuredContentImageResolver imageResolver,
            XWPFDocument document,
            IBody body,
            DocxListNumberingSupport listSupport,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures,
            int[] numberingCounters
    ) {
        this.objectMapper = objectMapper;
        this.styleCatalog = styleCatalog;
        this.imageResolver = imageResolver;
        this.document = document;
        this.body = body;
        this.listSupport = listSupport;
        this.variables = variables;
        this.pinnedModuleStructures = pinnedModuleStructures;
        this.numberingCounters = numberingCounters;
        this.styles = new StructuredContentDocxStyleSupport(styleCatalog);
        this.tableSupport = new StructuredContentDocxTableSupport(styles);
        this.inlineSupport = new StructuredContentDocxInlineSupport(
                variables,
                styles,
                imageResolver,
                this::rejectIfUnrenderable
        );
        this.cursor = new StructuredContentDocxCursorSupport(document);
    }

    void writeBlockNodes(JsonNode nodes, XWPFParagraph firstParagraph, boolean reuseFirstParagraph) {
        XWPFParagraph currentParagraph = firstParagraph;
        boolean paragraphAvailable = reuseFirstParagraph;
        for (int index = 0; index < nodes.size(); index++) {
            JsonNode node = nodes.get(index);
            String type = node.path("type").asText("");
            if ("tableComponentRef".equals(type) || "tableComponent".equals(type)) {
                JsonNode tableDefinition = node.has("tableComponent") ? node.get("tableComponent") : node;
                if (!paragraphAvailable) {
                    currentParagraph = cursor.insertParagraphAfter(currentParagraph);
                } else {
                    paragraphAvailable = false;
                    StructuredContentDocxWriter.clearParagraph(currentParagraph);
                }
                XWPFTable table = cursor.insertTableAfter(currentParagraph);
                tableSupport.populateTable(tableDefinition, table, variables);
                continue;
            }
            if ("list".equals(type)) {
                writeList(node, paragraphAvailable ? currentParagraph : null);
                paragraphAvailable = false;
                currentParagraph = body.getParagraphs().get(body.getParagraphs().size() - 1);
                continue;
            }
            if ("imageRef".equals(type) || "sealRef".equals(type)) {
                if (!paragraphAvailable) {
                    currentParagraph = cursor.insertParagraphAfter(currentParagraph);
                }
                inlineSupport.writeReferenceNode(node, currentParagraph);
                paragraphAvailable = false;
                continue;
            }
            // LR-A4 (CD-PIT-07): unsupported / writer-missing structured node types must fail-closed.
            // Never silently drop content from published letters.
            rejectIfUnrenderable(type);
            if (!paragraphAvailable) {
                currentParagraph = cursor.insertParagraphAfter(currentParagraph);
            }
            writeBlockNode(node, currentParagraph);
            paragraphAvailable = false;
        }
    }

    /**
     * LR-A4: reject matrix-declared types without a DOCX writer and unknown types.
     * Shared authority: {@link WriterUnsupportedStructuredNodeTypes} /
     * {@link DocxWriterHandledStructuredNodeTypes}.
     */
    private void rejectIfUnrenderable(String type) {
        if (WriterUnsupportedStructuredNodeTypes.containsJsonType(type)
                || !DocxWriterHandledStructuredNodeTypes.containsJsonType(type)) {
            throw new DocxAssemblyException(
                    "api.error.rendering.unsupportedNodeType",
                    "Unsupported structured content node type: " + type
            );
        }
    }

    private void writeBlockNode(JsonNode node, XWPFParagraph paragraph) {
        String type = node.path("type").asText("");
        rejectIfUnrenderable(type);
        if ("conditionBlock".equals(type)) {
            if (CONDITION_EVALUATOR.evaluate(node.path("conditionExpression").asText(""), variables)) {
                writeInlineOrBlockChildren(node, paragraph);
            }
            return;
        }
        if ("loopBlock".equals(type)) {
            writeLoopBlock(node, paragraph);
            return;
        }
        if ("contentModuleRef".equals(type)) {
            expandContentModule(node, paragraph);
            return;
        }
        if ("sectionHeading".equals(type)) {
            styles.applyParagraphStyle(paragraph, styles.resolveStyleRef(node, "Heading1"));
            writeSectionHeading(node, paragraph);
            return;
        }
        if ("paragraph".equals(type)) {
            JsonNode legacyText = node.get("text");
            if (legacyText != null && legacyText.isTextual()) {
                styles.applyParagraphStyle(paragraph, styles.resolveStyleRef(node, "BodyText"));
                styles.writeRunText(paragraph, legacyText.asText(""), false, false, false);
                return;
            }
            styles.applyParagraphStyle(paragraph, styles.resolveStyleRef(node, "BodyText"));
            inlineSupport.writeInlineChildren(node, paragraph);
            return;
        }
        if ("text".equals(type) || "textRun".equals(type)) {
            styles.applyParagraphStyle(paragraph, styles.resolveStyleRef(node, "BodyText"));
            styles.writeRunText(paragraph, node.path("value").asText(""), false, false, false);
            return;
        }
        inlineSupport.writeInlineChildren(node, paragraph);
    }

    private void writeLoopBlock(JsonNode node, XWPFParagraph paragraph) {
        String loopVariable = node.path("loopVariable").asText("");
        Object rawItems = variables.get(loopVariable);
        if (!(rawItems instanceof List<?> items) || items.isEmpty()) {
            writeInlineOrBlockChildren(node, paragraph);
            return;
        }
        XWPFParagraph current = paragraph;
        for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
            Map<String, Object> scoped = StructuredContentDocxTableSupport.scopedVariables(variables, items.get(itemIndex));
            StructuredContentDocxWriteSession scopedSession = new StructuredContentDocxWriteSession(
                    objectMapper,
                    styleCatalog,
                    imageResolver,
                    document,
                    body,
                    listSupport,
                    scoped,
                    pinnedModuleStructures,
                    numberingCounters
            );
            if (itemIndex == 0) {
                scopedSession.writeInlineOrBlockChildren(node, current);
            } else {
                XWPFParagraph next = cursor.insertParagraphAfter(current);
                scopedSession.writeInlineOrBlockChildren(node, next);
                current = next;
            }
        }
    }

    private void expandContentModule(JsonNode node, XWPFParagraph paragraph) {
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
            writeBlockNodes(StructuredContentDocxWriter.resolveRootNodes(root), paragraph, true);
        } catch (IOException ex) {
            throw new DocxAssemblyException(ex);
        }
    }

    private void writeList(JsonNode listNode, XWPFParagraph firstParagraph) {
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
                inlineSupport.writeInlineChildren(item, current);
            } else {
                inlineSupport.writeInlineNode(item, current, false, false, false);
            }
            current = null;
        }
    }

    private void writeSectionHeading(JsonNode node, XWPFParagraph paragraph) {
        String prefix = StructuredContentDocxCursorSupport.resolveNumberingPrefix(
                node, numberingCounters, MAX_NUMBERING_LEVELS);
        if (!prefix.isBlank()) {
            styles.writeRunText(paragraph, prefix + " ", true, false, false);
        }
        inlineSupport.writeInlineChildren(node, paragraph);
    }

    private void writeInlineOrBlockChildren(JsonNode node, XWPFParagraph paragraph) {
        JsonNode children = node.path("children");
        if (!children.isArray()) {
            return;
        }
        for (int index = 0; index < children.size(); index++) {
            JsonNode child = children.get(index);
            if (StructuredContentDocxCursorSupport.isBlockLevelType(child.path("type").asText(""))) {
                if (index == 0) {
                    writeBlockNode(child, paragraph);
                } else {
                    writeBlockNode(child, cursor.insertParagraphAfter(paragraph));
                }
            } else {
                inlineSupport.writeInlineNode(child, paragraph, false, false, false);
            }
        }
    }
}
