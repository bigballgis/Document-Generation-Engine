package com.bank.docgen.rendering;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

@Component
public class DocxAssembler {

    private static final Pattern ANCHOR_PATTERN = Pattern.compile("\\{\\{anchor:([A-Za-z0-9_.-]+)}}");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([A-Za-z0-9_.-]+)}}");

    private final ObjectMapper objectMapper;

    public DocxAssembler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public byte[] assemble(InputStream masterDocx, Map<String, String> anchorContent) {
        try (XWPFDocument document = new XWPFDocument(masterDocx); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            replaceInParagraphs(document.getParagraphs(), anchorContent);
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        replaceInParagraphs(cell.getParagraphs(), anchorContent);
                    }
                }
            }
            document.getHeaderList().forEach(header -> replaceInParagraphs(header.getParagraphs(), anchorContent));
            document.getFooterList().forEach(footer -> replaceInParagraphs(footer.getParagraphs(), anchorContent));
            document.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new DocxAssemblyException(ex);
        }
    }

    public String renderStructuredContent(String structuredContentJson, Map<String, Object> variables) {
        try {
            JsonNode root = objectMapper.readTree(structuredContentJson);
            JsonNode nodes = root.path("nodes");
            StringBuilder builder = new StringBuilder();
            if (nodes.isArray()) {
                for (JsonNode node : nodes) {
                    builder.append(renderNode(node, variables));
                }
            }
            return builder.toString();
        } catch (IOException ex) {
            throw new DocxAssemblyException(ex);
        }
    }

    private String renderNode(JsonNode node, Map<String, Object> variables) {
        String type = node.path("type").asText("");
        if ("text".equals(type)) {
            return node.path("value").asText("");
        }
        if ("variable".equals(type)) {
            String key = node.path("key").asText("");
            Object value = variables.get(key);
            return value == null ? "" : String.valueOf(value);
        }
        if ("paragraph".equals(type)) {
            StringBuilder paragraph = new StringBuilder();
            JsonNode children = node.path("children");
            if (children.isArray()) {
                for (JsonNode child : children) {
                    paragraph.append(renderNode(child, variables));
                }
            }
            return paragraph.toString();
        }
        return "";
    }

    public Map<String, String> buildAnchorReplacements(
            Map<String, String> bindingJsonByAnchor,
            Map<String, Object> variables
    ) {
        return bindingJsonByAnchor.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> renderStructuredContent(entry.getValue(), variables)
                ));
    }

    private void replaceInParagraphs(Iterable<XWPFParagraph> paragraphs, Map<String, String> anchorContent) {
        for (XWPFParagraph paragraph : paragraphs) {
            String text = paragraph.getText();
            if (text == null || text.isBlank()) {
                continue;
            }
            String replaced = replaceAnchors(text, anchorContent);
            if (!replaced.equals(text)) {
                while (!paragraph.getRuns().isEmpty()) {
                    paragraph.removeRun(0);
                }
                XWPFRun run = paragraph.createRun();
                run.setText(replaced);
            }
        }
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
