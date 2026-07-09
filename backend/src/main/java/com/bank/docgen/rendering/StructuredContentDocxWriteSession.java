package com.bank.docgen.rendering;

import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalog;
import com.bank.docgen.sharedkernel.document.expression.ConditionExpressionEvaluator;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.IBody;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;

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
                    currentParagraph = insertParagraphAfter(currentParagraph);
                } else {
                    paragraphAvailable = false;
                    StructuredContentDocxWriter.clearParagraph(currentParagraph);
                }
                XWPFTable table = insertTableAfter(currentParagraph);
                populateTable(tableDefinition, table);
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
                    currentParagraph = insertParagraphAfter(currentParagraph);
                }
                writeReferenceNode(node, currentParagraph);
                paragraphAvailable = false;
                continue;
            }
            // LR-A4 (CD-PIT-07): unsupported structured node types must fail-closed.
            // qrBarcodeRef / attachmentListRef are declared in the v1 node matrix but have
            // no writer branch — silently dropping them loses content in published letters.
            // Throw a fidelity blocker instead so the author is forced to fix the template.
            if (isUnsupportedRenderableType(type)) {
                throw new DocxAssemblyException(
                        "api.error.rendering.unsupportedNodeType",
                        "Unsupported structured content node type: " + type
                );
            }
            if (!paragraphAvailable) {
                currentParagraph = insertParagraphAfter(currentParagraph);
            }
            writeBlockNode(node, currentParagraph);
            paragraphAvailable = false;
        }
    }

    /**
     * LR-A4: types declared in {@link com.bank.docgen.authoring.structured.StructuredContentNodeType}
     * but NOT handled by this writer. They are renderable in principle (the matrix admits them)
     * but no DOCX emission branch exists yet — failing closed prevents silent content loss.
     */
    private boolean isUnsupportedRenderableType(String type) {
        return "qrBarcodeRef".equals(type) || "attachmentListRef".equals(type);
    }

    private void writeBlockNode(JsonNode node, XWPFParagraph paragraph) {
        String type = node.path("type").asText("");
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
            applyParagraphStyle(paragraph, resolveStyleRef(node, "Heading1"));
            writeSectionHeading(node, paragraph);
            return;
        }
        if ("paragraph".equals(type)) {
            JsonNode legacyText = node.get("text");
            if (legacyText != null && legacyText.isTextual()) {
                applyParagraphStyle(paragraph, resolveStyleRef(node, "BodyText"));
                writeRunText(paragraph, legacyText.asText(""), false, false, false);
                return;
            }
            applyParagraphStyle(paragraph, resolveStyleRef(node, "BodyText"));
            writeInlineChildren(node, paragraph);
            return;
        }
        if ("text".equals(type) || "textRun".equals(type)) {
            applyParagraphStyle(paragraph, resolveStyleRef(node, "BodyText"));
            writeRunText(paragraph, node.path("value").asText(""), false, false, false);
            return;
        }
        writeInlineChildren(node, paragraph);
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
            Map<String, Object> scoped = scopedVariables(variables, items.get(itemIndex));
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
                XWPFParagraph next = insertParagraphAfter(current);
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
                writeInlineChildren(item, current);
            } else {
                writeInlineNode(item, current, false, false, false);
            }
            current = null;
        }
    }

    private void writeSectionHeading(JsonNode node, XWPFParagraph paragraph) {
        String prefix = resolveNumberingPrefix(node);
        if (!prefix.isBlank()) {
            writeRunText(paragraph, prefix + " ", true, false, false);
        }
        writeInlineChildren(node, paragraph);
    }

    private String resolveNumberingPrefix(JsonNode node) {
        JsonNode numbering = node.get("numbering");
        if (numbering == null || !numbering.isObject()) {
            return "";
        }
        String explicit = numbering.path("displayNumber").asText("").trim();
        if (!explicit.isBlank()) {
            return explicit;
        }
        int level = numbering.path("level").asInt(1);
        if (level < 1 || level > MAX_NUMBERING_LEVELS) {
            return "";
        }
        numberingCounters[level - 1]++;
        for (int deeper = level; deeper < MAX_NUMBERING_LEVELS; deeper++) {
            numberingCounters[deeper] = 0;
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < level; index++) {
            if (index > 0) {
                builder.append('.');
            }
            builder.append(numberingCounters[index]);
        }
        return builder.toString();
    }

    private void writeInlineOrBlockChildren(JsonNode node, XWPFParagraph paragraph) {
        JsonNode children = node.path("children");
        if (!children.isArray()) {
            return;
        }
        for (int index = 0; index < children.size(); index++) {
            JsonNode child = children.get(index);
            if (isBlockLevelType(child.path("type").asText(""))) {
                if (index == 0) {
                    writeBlockNode(child, paragraph);
                } else {
                    writeBlockNode(child, insertParagraphAfter(paragraph));
                }
            } else {
                writeInlineNode(child, paragraph, false, false, false);
            }
        }
    }

    private void writeInlineChildren(JsonNode node, XWPFParagraph paragraph) {
        JsonNode children = node.path("children");
        if (!children.isArray()) {
            return;
        }
        for (JsonNode child : children) {
            writeInlineNode(child, paragraph, false, false, false);
        }
    }

    private void writeInlineNode(
            JsonNode node,
            XWPFParagraph paragraph,
            boolean bold,
            boolean italic,
            boolean underline
    ) {
        String type = node.path("type").asText("");
        if ("text".equals(type) || "textRun".equals(type)) {
            writeRunText(paragraph, node.path("value").asText(""), bold, italic, underline);
            return;
        }
        if ("variable".equals(type)) {
            String key = node.path("key").asText("");
            Object value = variables.get(key);
            writeRunText(paragraph, value == null ? "" : String.valueOf(value), bold, italic, underline);
            return;
        }
        if ("lineBreak".equals(type)) {
            XWPFRun run = paragraph.createRun();
            applyDefaultRunStyle(run);
            run.addBreak();
            return;
        }
        if ("emphasis".equals(type)) {
            EmphasisStyle emphasisStyle = resolveEmphasis(node);
            writeInlineChildrenWithStyle(node, paragraph, emphasisStyle.bold(), emphasisStyle.italic(), underline);
            return;
        }
        if ("underline".equals(type)) {
            writeInlineChildrenWithStyle(node, paragraph, bold, italic, true);
            return;
        }
        if ("styleRef".equals(type)) {
            String styleKey = node.path("styleRef").asText("");
            if (styleCatalog.find(styleKey) != null) {
                applyParagraphStyle(paragraph, styleKey);
            }
            return;
        }
        if ("imageRef".equals(type) || "sealRef".equals(type)) {
            writeReferenceNode(node, paragraph);
        }
    }

    private void writeInlineChildrenWithStyle(
            JsonNode node,
            XWPFParagraph paragraph,
            boolean bold,
            boolean italic,
            boolean underline
    ) {
        JsonNode children = node.path("children");
        if (!children.isArray()) {
            return;
        }
        for (JsonNode child : children) {
            writeInlineNode(child, paragraph, bold, italic, underline);
        }
    }

    private void writeReferenceNode(JsonNode node, XWPFParagraph paragraph) {
        StructuredContentImageResolver.ResolvedImage image;
        if ("sealRef".equals(node.path("type").asText(""))) {
            image = imageResolver.resolveSealRef(node.path("referenceKey").asText(""));
        } else {
            image = imageResolver.resolveImageRef(node.path("imageRef").asText(""));
        }
        try {
            XWPFRun run = paragraph.createRun();
            applyDefaultRunStyle(run);
            run.addPicture(
                    new java.io.ByteArrayInputStream(image.bytes()),
                    XWPFDocument.PICTURE_TYPE_PNG,
                    image.fileName(),
                    Units.toEMU(48),
                    Units.toEMU(48)
            );
        } catch (Exception ex) {
            throw new DocxAssemblyException(ex);
        }
    }

    private void writeRunText(
            XWPFParagraph paragraph,
            String text,
            boolean bold,
            boolean italic,
            boolean underline
    ) {
        if (text == null || text.isEmpty()) {
            return;
        }
        XWPFRun run = paragraph.createRun();
        applyDefaultRunStyle(run);
        run.setBold(bold);
        run.setItalic(italic);
        if (underline) {
            run.setUnderline(UnderlinePatterns.SINGLE);
        }
        run.setText(text);
    }

    private void populateTable(JsonNode tableDefinition, XWPFTable table) {
        List<String> columnKeys = new ArrayList<>();
        JsonNode columnSchema = tableDefinition.path("columnSchema");
        if (columnSchema.isArray()) {
            for (JsonNode column : columnSchema) {
                columnKeys.add(column.path("columnKey").asText(""));
            }
        }
        if (columnKeys.isEmpty()) {
            return;
        }
        int rowIndex = 0;
        JsonNode headerRows = tableDefinition.path("headerRows");
        if (headerRows.isArray() && !headerRows.isEmpty()) {
            JsonNode headerRow = headerRows.get(0);
            writeTableRow(table, rowIndex++, headerRow, columnKeys, variables, true);
        }
        JsonNode loopRow = tableDefinition.path("loopRow");
        String loopVariable = loopRow.path("loopVariable").asText("");
        Object rawItems = variables.get(loopVariable);
        if (rawItems instanceof List<?> items) {
            for (Object item : items) {
                writeTableRow(
                        table,
                        rowIndex++,
                        loopRow.path("cells"),
                        columnKeys,
                        scopedVariables(variables, item),
                        false
                );
            }
        }
        JsonNode footerRows = tableDefinition.path("footerRows");
        if (footerRows.isArray()) {
            for (JsonNode footerRow : footerRows) {
                writeTableRow(table, rowIndex++, footerRow, columnKeys, variables, false);
            }
        }
    }

    private void writeTableRow(
            XWPFTable table,
            int rowIndex,
            JsonNode cellsNode,
            List<String> columnKeys,
            Map<String, Object> scopedVariables,
            boolean header
    ) {
        XWPFTableRow row = rowIndex < table.getNumberOfRows() ? table.getRow(rowIndex) : table.createRow();
        while (row.getTableCells().size() < columnKeys.size()) {
            row.addNewTableCell();
        }
        Map<String, String> valuesByColumn = new LinkedHashMap<>();
        if (cellsNode.isArray()) {
            for (JsonNode cell : cellsNode) {
                valuesByColumn.put(cell.path("columnKey").asText(""), resolveCellValue(cell, scopedVariables));
            }
        }
        for (int columnIndex = 0; columnIndex < columnKeys.size(); columnIndex++) {
            String columnKey = columnKeys.get(columnIndex);
            XWPFTableCell cell = row.getCell(columnIndex);
            XWPFParagraph paragraph = cell.getParagraphs().isEmpty()
                    ? cell.addParagraph()
                    : cell.getParagraphs().getFirst();
            StructuredContentDocxWriter.clearParagraph(paragraph);
            if (header) {
                applyParagraphStyle(paragraph, "TableHeader");
            }
            writeRunText(paragraph, valuesByColumn.getOrDefault(columnKey, ""), header, false, false);
        }
    }

    private XWPFParagraph insertParagraphAfter(XWPFParagraph paragraph) {
        try (XmlCursor cursor = paragraph.getCTP().newCursor()) {
            cursor.toEndToken();
            cursor.toNextToken();
            return document.insertNewParagraph(cursor);
        }
    }

    private XWPFTable insertTableAfter(XWPFParagraph paragraph) {
        try (XmlCursor cursor = paragraph.getCTP().newCursor()) {
            cursor.toEndToken();
            cursor.toNextToken();
            return document.insertNewTbl(cursor);
        }
    }

    private String resolveStyleRef(JsonNode node, String fallback) {
        if (node.has("styleRef") && !node.get("styleRef").isNull()) {
            String styleRef = node.get("styleRef").asText("").trim();
            if (!styleRef.isBlank() && styleCatalog.find(styleRef) != null) {
                return styleRef;
            }
        }
        return fallback;
    }

    private void applyParagraphStyle(XWPFParagraph paragraph, String styleKey) {
        paragraph.setStyle(DocxMasterStyleRegistry.resolveWordStyleId(styleKey));
    }

    private EmphasisStyle resolveEmphasis(JsonNode node) {
        String variant = node.path("variant").asText("bold").trim().toLowerCase(Locale.ROOT);
        return switch (variant) {
            case "italic" -> new EmphasisStyle(false, true);
            case "bolditalic", "bold_italic" -> new EmphasisStyle(true, true);
            default -> new EmphasisStyle(true, false);
        };
    }

    private boolean isBlockLevelType(String type) {
        return switch (type) {
            case "paragraph", "sectionHeading", "conditionBlock", "loopBlock", "tableComponentRef",
                    "tableComponent", "contentModuleRef", "list" -> true;
            default -> false;
        };
    }

    private static String resolveCellValue(JsonNode cell, Map<String, Object> scopedVariables) {
        if (cell.has("value")) {
            return cell.path("value").asText("");
        }
        String variableKey = cell.path("variableKey").asText("");
        if (variableKey.isBlank()) {
            return "";
        }
        Object value = scopedVariables.get(variableKey);
        return value == null ? "" : String.valueOf(value);
    }

    private static Map<String, Object> scopedVariables(Map<String, Object> base, Object item) {
        Map<String, Object> scoped = new LinkedHashMap<>(base);
        if (item instanceof Map<?, ?> mapItem) {
            mapItem.forEach((key, value) -> scoped.put(String.valueOf(key), value));
        }
        return scoped;
    }

    private static void applyDefaultRunStyle(XWPFRun run) {
        run.setFontFamily("Calibri");
        run.setFontSize(10);
        run.setColor("000000");
    }

    private record EmphasisStyle(boolean bold, boolean italic) {
    }
}
