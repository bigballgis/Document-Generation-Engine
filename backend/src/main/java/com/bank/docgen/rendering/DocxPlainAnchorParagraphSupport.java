package com.bank.docgen.rendering;

import java.util.ArrayList;
import java.util.List;
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

final class DocxPlainAnchorParagraphSupport {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([A-Za-z0-9_.-]+)}}");

    private DocxPlainAnchorParagraphSupport() {
    }

    static void replaceAnchorsInDocumentBody(
            XWPFDocument document,
            Map<String, String> anchorContent,
            Pattern anchorPattern
    ) {
        List<Integer> anchorParagraphIndexes = new ArrayList<>();
        List<String> anchorReplacements = new ArrayList<>();
        List<IBodyElement> bodyElements = document.getBodyElements();
        for (int index = 0; index < bodyElements.size(); index++) {
            IBodyElement element = bodyElements.get(index);
            if (!(element instanceof XWPFParagraph paragraph)) {
                continue;
            }
            String text = paragraph.getText();
            if (text == null || text.isBlank() || !anchorPattern.matcher(text).find()) {
                continue;
            }
            String replaced = replaceAnchors(text, anchorContent, anchorPattern);
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

    static void replaceInParagraphs(
            Iterable<XWPFParagraph> paragraphs,
            Map<String, String> anchorContent,
            Pattern anchorPattern
    ) {
        for (XWPFParagraph paragraph : paragraphs) {
            String text = paragraph.getText();
            if (text == null || text.isBlank()) {
                continue;
            }
            String replaced = replaceAnchors(text, anchorContent, anchorPattern);
            if (!replaced.equals(text)) {
                writeParagraphText(paragraph, replaced);
            }
        }
    }

    static void replaceInTablesHeadersAndFooters(
            XWPFDocument document,
            Map<String, String> anchorContent,
            Pattern anchorPattern
    ) {
        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    replaceInParagraphs(cell.getParagraphs(), anchorContent, anchorPattern);
                }
            }
        }
        document.getHeaderList().forEach(header ->
                replaceInParagraphs(header.getParagraphs(), anchorContent, anchorPattern));
        document.getFooterList().forEach(footer ->
                replaceInParagraphs(footer.getParagraphs(), anchorContent, anchorPattern));
    }

    static void expandAnchorParagraph(XWPFDocument document, int paragraphIndex, String content) {
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

    static String replaceAnchors(String text, Map<String, String> anchorContent, Pattern anchorPattern) {
        Matcher matcher = anchorPattern.matcher(text);
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

    static List<String> splitParagraphBlocks(String content) {
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

    static void clearParagraph(XWPFParagraph paragraph) {
        while (!paragraph.getRuns().isEmpty()) {
            paragraph.removeRun(0);
        }
    }

    static void writeParagraphText(XWPFParagraph paragraph, String text) {
        clearParagraph(paragraph);
        String sanitized = sanitizeDocxText(text);
        if (sanitized.isEmpty()) {
            return;
        }
        String[] lines = sanitized.split("\n", -1);
        // CE-K02: do not hard-code Calibri/10pt/black — inherit master docDefaults / paragraph style.
        XWPFRun run = paragraph.createRun();
        run.setText(lines[0], 0);
        for (int lineIndex = 1; lineIndex < lines.length; lineIndex++) {
            run.addBreak();
            run.setText(lines[lineIndex], lineIndex);
        }
    }

    static String sanitizeDocxText(String text) {
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
}
