package com.bank.docgen.rendering;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.springframework.stereotype.Component;

@Component
public class DocxAssembler {

    private static final Pattern ANCHOR_PATTERN = Pattern.compile("\\{\\{anchor:([A-Za-z0-9_.-]+)}}");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([A-Za-z0-9_.-]+)}}");
    private static final Pattern SIMPLE_CONDITION_PATTERN = Pattern.compile(
            "\\$\\{([A-Za-z0-9_.-]+)}\\s*==\\s*(true|false)",
            Pattern.CASE_INSENSITIVE
    );
    private static final String MASTER_FILLER_MARKER =
            "Section-level anchor in the master layout container";

    private final ObjectMapper objectMapper;

    public DocxAssembler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public byte[] assemble(InputStream masterDocx, Map<String, String> anchorContent) {
        try (XWPFDocument document = new XWPFDocument(masterDocx); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            removeMasterLayoutFillerParagraphs(document);
            replaceAnchorsInDocumentBody(document, anchorContent);
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        replaceInParagraphs(cell.getParagraphs(), anchorContent);
                    }
                }
            }
            document.getHeaderList().forEach(header -> replaceInParagraphs(header.getParagraphs(), anchorContent));
            document.getFooterList().forEach(footer -> replaceInParagraphs(footer.getParagraphs(), anchorContent));
            DocxWordCompatibilitySupport.ensureWordCompatiblePackage(document);
            document.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new DocxAssemblyException(ex);
        }
    }

    public String renderStructuredContent(String structuredContentJson, Map<String, Object> variables) {
        return renderStructuredContent(structuredContentJson, variables, Map.of());
    }

    public String renderStructuredContent(
            String structuredContentJson,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures
    ) {
        try {
            JsonNode root = objectMapper.readTree(structuredContentJson);
            JsonNode nodes = resolveRootNodes(root);
            StringBuilder builder = new StringBuilder();
            if (nodes.isArray()) {
                for (int index = 0; index < nodes.size(); index++) {
                    JsonNode node = nodes.get(index);
                    builder.append(renderNode(node, variables, pinnedModuleStructures, true));
                    if (isBlockLevelNode(node) && index < nodes.size() - 1) {
                        builder.append("\n\n");
                    }
                }
            }
            return builder.toString();
        } catch (IOException ex) {
            throw new DocxAssemblyException(ex);
        }
    }

    private JsonNode resolveRootNodes(JsonNode root) {
        JsonNode nodes = root.path("nodes");
        if (nodes.isArray()) {
            return nodes;
        }
        JsonNode blocks = root.path("blocks");
        if (blocks.isArray()) {
            return blocks;
        }
        return nodes;
    }

    private boolean isBlockLevelNode(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return false;
        }
        String type = node.path("type").asText("");
        return switch (type) {
            case "paragraph", "sectionHeading", "conditionBlock", "loopBlock", "tableComponentRef",
                    "tableComponent", "contentModuleRef" -> true;
            default -> "paragraph".equals(type) && node.has("text");
        };
    }

    private String renderNode(
            JsonNode node,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures,
            boolean blockLevel
    ) {
        String type = node.path("type").asText("");
        if ("text".equals(type) || "textRun".equals(type)) {
            return node.path("value").asText("");
        }
        if ("lineBreak".equals(type)) {
            return "\n";
        }
        if ("variable".equals(type)) {
            String key = node.path("key").asText("");
            Object value = variables.get(key);
            return value == null ? "" : String.valueOf(value);
        }
        if ("emphasis".equals(type)) {
            return renderChildren(node, variables, pinnedModuleStructures);
        }
        if ("sectionHeading".equals(type)) {
            String heading = renderChildren(node, variables, pinnedModuleStructures);
            return blockLevel ? heading : heading + "\n";
        }
        if ("contentModuleRef".equals(type)) {
            String referenceKey = node.path("referenceKey").asText("").trim().toUpperCase(Locale.ROOT);
            String pinnedStructure = pinnedModuleStructures.get(referenceKey);
            if (pinnedStructure == null || pinnedStructure.isBlank()) {
                return "";
            }
            return renderStructuredContent(pinnedStructure, variables, pinnedModuleStructures);
        }
        if ("conditionBlock".equals(type)) {
            if (evaluateSimpleCondition(node.path("conditionExpression").asText(""), variables)) {
                return renderChildren(node, variables, pinnedModuleStructures);
            }
            return "";
        }
        if ("loopBlock".equals(type)) {
            return renderLoopBlock(node, variables, pinnedModuleStructures);
        }
        if ("tableComponentRef".equals(type) || "tableComponent".equals(type)) {
            JsonNode tableDefinition = node.has("tableComponent")
                    ? node.get("tableComponent")
                    : node;
            return renderTableComponent(tableDefinition, variables);
        }
        if ("paragraph".equals(type)) {
            JsonNode legacyText = node.get("text");
            if (legacyText != null && legacyText.isTextual()) {
                return legacyText.asText("");
            }
            String inline = renderChildren(node, variables, pinnedModuleStructures);
            return blockLevel ? inline : inline + "\n";
        }
        return "";
    }

    private String renderLoopBlock(
            JsonNode node,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures
    ) {
        String loopVariable = node.path("loopVariable").asText("");
        Object rawItems = variables.get(loopVariable);
        if (!(rawItems instanceof List<?> items) || items.isEmpty()) {
            return renderChildren(node, variables, pinnedModuleStructures);
        }
        StringBuilder rendered = new StringBuilder();
        for (Object item : items) {
            Map<String, Object> scopedVariables = scopedVariables(variables, item);
            rendered.append(renderChildren(node, scopedVariables, pinnedModuleStructures));
            rendered.append("\n");
        }
        return rendered.toString().stripTrailing();
    }

    private Map<String, Object> scopedVariables(Map<String, Object> base, Object item) {
        Map<String, Object> scoped = new LinkedHashMap<>(base);
        if (item instanceof Map<?, ?> mapItem) {
            mapItem.forEach((key, value) -> scoped.put(String.valueOf(key), value));
        }
        return scoped;
    }

    private String renderTableComponent(JsonNode tableDefinition, Map<String, Object> variables) {
        if (tableDefinition == null || tableDefinition.isMissingNode() || !tableDefinition.isObject()) {
            return "";
        }
        List<String> columnKeys = new ArrayList<>();
        List<String> columnHeaders = new ArrayList<>();
        JsonNode columnSchema = tableDefinition.path("columnSchema");
        if (columnSchema.isArray()) {
            for (JsonNode column : columnSchema) {
                String columnKey = column.path("columnKey").asText("");
                columnKeys.add(columnKey);
                columnHeaders.add(resolveHeaderLabel(tableDefinition, columnKey, columnKey));
            }
        }
        StringBuilder rendered = new StringBuilder();
        rendered.append(String.join(" | ", columnHeaders)).append("\n");
        rendered.append("-".repeat(Math.max(12, columnHeaders.size() * 8))).append("\n");

        JsonNode loopRow = tableDefinition.path("loopRow");
        String loopVariable = loopRow.path("loopVariable").asText("");
        Object rawItems = variables.get(loopVariable);
        if (rawItems instanceof List<?> items) {
            for (Object item : items) {
                rendered.append(renderTableRow(loopRow.path("cells"), columnKeys, scopedVariables(variables, item)));
                rendered.append("\n");
            }
        }

        JsonNode footerRows = tableDefinition.path("footerRows");
        if (footerRows.isArray() && !footerRows.isEmpty()) {
            rendered.append("\n");
            for (JsonNode footerRow : footerRows) {
                rendered.append(renderTableRow(footerRow, columnKeys, variables));
                rendered.append("\n");
            }
        }
        return rendered.toString().stripTrailing();
    }

    private String resolveHeaderLabel(JsonNode tableDefinition, String columnKey, String fallback) {
        JsonNode headerRows = tableDefinition.path("headerRows");
        if (headerRows.isArray() && !headerRows.isEmpty()) {
            JsonNode headerRow = headerRows.get(0);
            if (headerRow.isArray()) {
                for (JsonNode cell : headerRow) {
                    if (columnKey.equals(cell.path("columnKey").asText(""))) {
                        String value = cell.path("value").asText("");
                        if (!value.isBlank()) {
                            return value;
                        }
                    }
                }
            }
        }
        return fallback;
    }

    private String renderTableRow(JsonNode cellsNode, List<String> columnKeys, Map<String, Object> variables) {
        Map<String, String> valuesByColumn = new LinkedHashMap<>();
        if (cellsNode.isArray()) {
            for (JsonNode cell : cellsNode) {
                String columnKey = cell.path("columnKey").asText("");
                valuesByColumn.put(columnKey, resolveCellValue(cell, variables));
            }
        }
        List<String> rowValues = new ArrayList<>();
        for (String columnKey : columnKeys) {
            rowValues.add(valuesByColumn.getOrDefault(columnKey, ""));
        }
        return String.join(" | ", rowValues);
    }

    private String resolveCellValue(JsonNode cell, Map<String, Object> variables) {
        if (cell.has("value")) {
            return cell.path("value").asText("");
        }
        String variableKey = cell.path("variableKey").asText("");
        if (variableKey.isBlank()) {
            return "";
        }
        Object value = variables.get(variableKey);
        return value == null ? "" : String.valueOf(value);
    }

    private String renderChildren(
            JsonNode node,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures
    ) {
        StringBuilder rendered = new StringBuilder();
        JsonNode children = node.path("children");
        if (children.isArray()) {
            for (JsonNode child : children) {
                rendered.append(renderNode(child, variables, pinnedModuleStructures, false));
            }
        }
        return rendered.toString();
    }

    private boolean evaluateSimpleCondition(String expression, Map<String, Object> variables) {
        if (expression == null || expression.isBlank()) {
            return false;
        }
        Matcher matcher = SIMPLE_CONDITION_PATTERN.matcher(expression.trim());
        if (!matcher.matches()) {
            return false;
        }
        String variableKey = matcher.group(1);
        boolean expectTrue = "true".equalsIgnoreCase(matcher.group(2));
        return expectTrue == toBoolean(variables.get(variableKey));
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    public Map<String, String> buildAnchorReplacements(
            Map<String, String> bindingJsonByAnchor,
            Map<String, Object> variables
    ) {
        return buildAnchorReplacements(bindingJsonByAnchor, variables, Map.of());
    }

    public Map<String, String> buildAnchorReplacements(
            Map<String, String> bindingJsonByAnchor,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures
    ) {
        return bindingJsonByAnchor.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> renderStructuredContent(entry.getValue(), variables, pinnedModuleStructures)
                ));
    }

    private void replaceStructuredAnchorsInDocumentBody(
            XWPFDocument document,
            Map<String, String> bindingJsonByAnchor,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures
    ) {
        List<AnchorReplacement> replacements = new ArrayList<>();
        List<IBodyElement> bodyElements = document.getBodyElements();
        for (int index = 0; index < bodyElements.size(); index++) {
            IBodyElement element = bodyElements.get(index);
            if (!(element instanceof XWPFParagraph paragraph)) {
                continue;
            }
            String text = paragraph.getText();
            if (text == null || text.isBlank()) {
                continue;
            }
            Matcher matcher = ANCHOR_PATTERN.matcher(text);
            if (!matcher.find()) {
                continue;
            }
            String anchorId = matcher.group(1);
            String structuredJson = bindingJsonByAnchor.get(anchorId);
            if (structuredJson == null || structuredJson.isBlank()) {
                continue;
            }
            replacements.add(new AnchorReplacement(index, anchorId, structuredJson));
        }
        for (int replacementIndex = replacements.size() - 1; replacementIndex >= 0; replacementIndex--) {
            AnchorReplacement replacement = replacements.get(replacementIndex);
            structuredContentDocxWriter.replaceAnchorParagraph(
                    document,
                    replacement.paragraphIndex(),
                    replacement.structuredJson(),
                    variables,
                    pinnedModuleStructures
            );
        }
    }

    private MasterStyleCatalog loadDefaultStyleCatalog(ObjectMapper mapper) {
        try (InputStream inputStream = new ClassPathResource(DEFAULT_STYLE_CATALOG_RESOURCE).getInputStream()) {
            JsonNode root = mapper.readTree(inputStream);
            Map<String, MasterStyleCatalogEntry> styles = new HashMap<>();
            JsonNode stylesNode = root.get("styles");
            if (stylesNode != null && stylesNode.isArray()) {
                for (JsonNode styleNode : stylesNode) {
                    String styleKey = styleNode.path("styleKey").asText("");
                    if (styleKey.isBlank()) {
                        continue;
                    }
                    List<String> applicable = new ArrayList<>();
                    JsonNode applicableNode = styleNode.get("applicableNodeTypes");
                    if (applicableNode != null && applicableNode.isArray()) {
                        applicableNode.forEach(node -> applicable.add(node.asText()));
                    }
                    styles.put(
                            styleKey,
                            new MasterStyleCatalogEntry(
                                    styleKey,
                                    Set.copyOf(applicable),
                                    styleNode.path("renderPurpose").asText("")
                            )
                    );
                }
            }
            return new MasterStyleCatalog(root.path("catalogVersion").asText("1.0"), styles);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load master style catalog: " + DEFAULT_STYLE_CATALOG_RESOURCE, ex);
        }
    }

    private record AnchorReplacement(int paragraphIndex, String anchorId, String structuredJson) {
    }
        List<IBodyElement> bodyElements = document.getBodyElements();
        for (int index = bodyElements.size() - 1; index >= 0; index--) {
            IBodyElement element = bodyElements.get(index);
            if (element instanceof XWPFParagraph paragraph) {
                String text = paragraph.getText();
                if (text != null && text.contains(MASTER_FILLER_MARKER)) {
                    document.removeBodyElement(index);
                }
            }
        }
    }

    private void replaceAnchorsInDocumentBody(XWPFDocument document, Map<String, String> anchorContent) {
        List<Integer> anchorParagraphIndexes = new ArrayList<>();
        List<String> anchorReplacements = new ArrayList<>();
        List<IBodyElement> bodyElements = document.getBodyElements();
        for (int index = 0; index < bodyElements.size(); index++) {
            IBodyElement element = bodyElements.get(index);
            if (!(element instanceof XWPFParagraph paragraph)) {
                continue;
            }
            String text = paragraph.getText();
            if (text == null || text.isBlank() || !ANCHOR_PATTERN.matcher(text).find()) {
                continue;
            }
            String replaced = replaceAnchors(text, anchorContent);
            if (!replaced.equals(text)) {
                anchorParagraphIndexes.add(index);
                anchorReplacements.add(replaced);
            }
        }
        for (int replacementIndex = anchorParagraphIndexes.size() - 1; replacementIndex >= 0; replacementIndex--) {
            expandAnchorParagraph(
                    document,
                    anchorParagraphIndexes.get(replacementIndex),
                    anchorReplacements.get(replacementIndex)
            );
        }
    }

    private void expandAnchorParagraph(XWPFDocument document, int paragraphIndex, String content) {
        IBodyElement element = document.getBodyElements().get(paragraphIndex);
        if (!(element instanceof XWPFParagraph paragraph)) {
            return;
        }
        List<String> blocks = splitParagraphBlocks(content);
        if (blocks.isEmpty()) {
            clearParagraph(paragraph);
            return;
        }
        writeParagraphText(paragraph, blocks.getFirst());
        XWPFParagraph current = paragraph;
        for (int blockIndex = 1; blockIndex < blocks.size(); blockIndex++) {
            try (XmlCursor cursor = current.getCTP().newCursor()) {
                cursor.toEndToken();
                cursor.toNextToken();
                current = document.insertNewParagraph(cursor);
                writeParagraphText(current, blocks.get(blockIndex));
            }
        }
    }

    private List<String> splitParagraphBlocks(String content) {
        String sanitized = sanitizeDocxText(content);
        if (sanitized.isBlank()) {
            return List.of();
        }
        String[] parts = sanitized.split("\\n\\n+");
        List<String> blocks = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.strip();
            if (!trimmed.isEmpty()) {
                blocks.add(trimmed);
            }
        }
        if (blocks.isEmpty()) {
            blocks.add(sanitized.strip());
        }
        return blocks;
    }

    private void clearParagraph(XWPFParagraph paragraph) {
        while (!paragraph.getRuns().isEmpty()) {
            paragraph.removeRun(0);
        }
    }

    private void replaceInParagraphs(Iterable<XWPFParagraph> paragraphs, Map<String, String> anchorContent) {
        for (XWPFParagraph paragraph : paragraphs) {
            String text = paragraph.getText();
            if (text == null || text.isBlank()) {
                continue;
            }
            String replaced = replaceAnchors(text, anchorContent);
            if (!replaced.equals(text)) {
                writeParagraphText(paragraph, replaced);
            }
        }
    }

    private void writeParagraphText(XWPFParagraph paragraph, String text) {
        clearParagraph(paragraph);
        String sanitized = sanitizeDocxText(text);
        if (sanitized.isEmpty()) {
            return;
        }
        String[] lines = sanitized.split("\n", -1);
        XWPFRun run = paragraph.createRun();
        run.setFontFamily("Calibri");
        run.setFontSize(10);
        run.setColor("000000");
        run.setText(lines[0], 0);
        for (int lineIndex = 1; lineIndex < lines.length; lineIndex++) {
            run.addBreak();
            run.setText(lines[lineIndex], lineIndex);
        }
    }

    private String sanitizeDocxText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder sanitized = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == '\n' || character == '\r' || character == '\t' || character >= 0x20) {
                sanitized.append(character);
            }
        }
        return sanitized.toString();
    }

    private String replaceAnchors(String text, Map<String, String> anchorContent) {
        Matcher matcher = ANCHOR_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String anchorId = matcher.group(1);
            String replacement = anchorContent.getOrDefault(anchorId, "");
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        String result = buffer.toString();
        Matcher variableMatcher = VARIABLE_PATTERN.matcher(result);
        StringBuffer variableBuffer = new StringBuffer();
        while (variableMatcher.find()) {
            variableMatcher.appendReplacement(variableBuffer, "");
        }
        variableMatcher.appendTail(variableBuffer);
        return variableBuffer.toString();
    }

    public byte[] assembleFromBytes(byte[] masterBytes, Map<String, String> anchorContent) {
        return assemble(new ByteArrayInputStream(masterBytes), anchorContent);
    }
}
