package com.bank.docgen.rendering;

import com.bank.docgen.authoring.structured.MasterStyleCatalog;
import com.bank.docgen.authoring.structured.MasterStyleCatalogEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.IBody;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class DocxAssembler {

    private static final Pattern ANCHOR_PATTERN = Pattern.compile("\\{\\{anchor:([A-Za-z0-9_.-]+)}}");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([A-Za-z0-9_.-]+)}}");
    private static final String MASTER_FILLER_MARKER =
            "Section-level anchor in the master layout container";
    private static final String DEFAULT_STYLE_CATALOG_RESOURCE = "authoring/default-master-style-catalog-v1.json";

    private final MasterStyleCatalog styleCatalog;
    private final StructuredContentDocxWriter structuredContentDocxWriter;

    public DocxAssembler(ObjectMapper objectMapper, StructuredContentImageResolver imageResolver) {
        this.styleCatalog = loadDefaultStyleCatalog(objectMapper);
        this.structuredContentDocxWriter = new StructuredContentDocxWriter(
                objectMapper,
                styleCatalog,
                imageResolver
        );
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
        return structuredContentDocxWriter.renderPlainTextProjection(
                structuredContentJson,
                variables,
                pinnedModuleStructures
        );
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

    private void removeMasterLayoutFillerParagraphs(XWPFDocument document) {
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

    public byte[] assembleStructured(
            InputStream masterDocx,
            Map<String, String> bindingJsonByAnchor,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures
    ) {
        try (XWPFDocument document = new XWPFDocument(masterDocx); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            removeMasterLayoutFillerParagraphs(document);
            replaceStructuredAnchorsInDocumentBody(
                    document,
                    bindingJsonByAnchor,
                    variables,
                    pinnedModuleStructures
            );
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        replaceStructuredAnchorsInParagraphs(
                                document,
                                cell,
                                cell.getParagraphs(),
                                bindingJsonByAnchor,
                                variables,
                                pinnedModuleStructures
                        );
                    }
                }
            }
            for (XWPFHeader header : document.getHeaderList()) {
                replaceStructuredAnchorsInParagraphs(
                        document,
                        header,
                        header.getParagraphs(),
                        bindingJsonByAnchor,
                        variables,
                        pinnedModuleStructures
                );
            }
            for (var footer : document.getFooterList()) {
                replaceStructuredAnchorsInParagraphs(
                        document,
                        footer,
                        footer.getParagraphs(),
                        bindingJsonByAnchor,
                        variables,
                        pinnedModuleStructures
                );
            }
            DocxWordCompatibilitySupport.ensureWordCompatiblePackage(document);
            document.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new DocxAssemblyException(ex);
        }
    }

    public byte[] assembleStructuredFromBytes(
            byte[] masterBytes,
            Map<String, String> bindingJsonByAnchor,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures
    ) {
        return assembleStructured(
                new ByteArrayInputStream(masterBytes),
                bindingJsonByAnchor,
                variables,
                pinnedModuleStructures
        );
    }

    private void replaceStructuredAnchorsInDocumentBody(
            XWPFDocument document,
            Map<String, String> bindingJsonByAnchor,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures
    ) {
        List<AnchorReplacement> replacements = collectStructuredAnchorReplacements(
                document.getBodyElements(),
                bindingJsonByAnchor
        );
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

    private void replaceStructuredAnchorsInParagraphs(
            XWPFDocument document,
            IBody body,
            List<XWPFParagraph> paragraphs,
            Map<String, String> bindingJsonByAnchor,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures
    ) {
        List<AnchorReplacement> replacements = collectStructuredAnchorReplacements(paragraphs, bindingJsonByAnchor);
        for (int replacementIndex = replacements.size() - 1; replacementIndex >= 0; replacementIndex--) {
            AnchorReplacement replacement = replacements.get(replacementIndex);
            structuredContentDocxWriter.replaceStructuredAnchorInParagraph(
                    document,
                    body,
                    paragraphs.get(replacement.paragraphIndex()),
                    replacement.structuredJson(),
                    variables,
                    pinnedModuleStructures
            );
        }
    }

    private List<AnchorReplacement> collectStructuredAnchorReplacements(
            List<?> paragraphContainers,
            Map<String, String> bindingJsonByAnchor
    ) {
        List<AnchorReplacement> replacements = new ArrayList<>();
        for (int index = 0; index < paragraphContainers.size(); index++) {
            Object container = paragraphContainers.get(index);
            XWPFParagraph paragraph;
            if (container instanceof IBodyElement bodyElement && bodyElement instanceof XWPFParagraph bodyParagraph) {
                paragraph = bodyParagraph;
            } else if (container instanceof XWPFParagraph directParagraph) {
                paragraph = directParagraph;
            } else {
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
        return replacements;
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
}
