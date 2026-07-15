package com.bank.docgen.rendering;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.xmlbeans.XmlCursor;

/**
 * Package-private cursor helpers for structured DOCX write sessions.
 */
final class StructuredContentDocxCursorSupport {

    private final XWPFDocument document;

    StructuredContentDocxCursorSupport(XWPFDocument document) {
        this.document = document;
    }

    XWPFParagraph insertParagraphAfter(XWPFParagraph paragraph) {
        try (XmlCursor cursor = paragraph.getCTP().newCursor()) {
            cursor.toEndToken();
            cursor.toNextToken();
            return document.insertNewParagraph(cursor);
        }
    }

    XWPFTable insertTableAfter(XWPFParagraph paragraph) {
        try (XmlCursor cursor = paragraph.getCTP().newCursor()) {
            cursor.toEndToken();
            cursor.toNextToken();
            return document.insertNewTbl(cursor);
        }
    }

    static boolean isBlockLevelType(String type) {
        return switch (type) {
            case "paragraph", "sectionHeading", "conditionBlock", "loopBlock", "tableComponentRef",
                    "tableComponent", "contentModuleRef", "list", "attachmentListRef" -> true;
            default -> false;
        };
    }

    static String resolveNumberingPrefix(com.fasterxml.jackson.databind.JsonNode node, int[] numberingCounters, int maxLevels) {
        com.fasterxml.jackson.databind.JsonNode numbering = node.get("numbering");
        if (numbering == null || !numbering.isObject()) {
            return "";
        }
        String explicit = numbering.path("displayNumber").asText("").trim();
        if (!explicit.isBlank()) {
            return explicit;
        }
        int level = numbering.path("level").asInt(1);
        if (level < 1 || level > maxLevels) {
            return "";
        }
        numberingCounters[level - 1]++;
        for (int deeper = level; deeper < maxLevels; deeper++) {
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
}
