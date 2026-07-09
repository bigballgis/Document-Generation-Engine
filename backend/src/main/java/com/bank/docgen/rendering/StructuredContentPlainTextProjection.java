package com.bank.docgen.rendering;

import java.util.ArrayList;
import java.util.List;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

/**
 * Derives plain-text projection from an authoritative {@link XWPFDocument} produced by
 * {@link StructuredContentDocxWriter} (F1-C2 — not a second renderer).
 */
final class StructuredContentPlainTextProjection {

    private StructuredContentPlainTextProjection() {
    }

    static String fromDocument(XWPFDocument document) {
        StringBuilder builder = new StringBuilder();
        List<IBodyElement> bodyElements = document.getBodyElements();
        for (int index = 0; index < bodyElements.size(); index++) {
            IBodyElement element = bodyElements.get(index);
            if (element instanceof XWPFParagraph paragraph) {
                if (!builder.isEmpty() && index > 0) {
                    builder.append("\n\n");
                }
                appendParagraph(builder, paragraph);
                continue;
            }
            if (element instanceof XWPFTable table) {
                if (!builder.isEmpty()) {
                    builder.append("\n\n");
                }
                appendTable(builder, table);
            }
        }
        return builder.toString();
    }

    private static void appendParagraph(StringBuilder builder, XWPFParagraph paragraph) {
        for (XWPFRun run : paragraph.getRuns()) {
            String text = run.getText(0);
            if (text != null) {
                builder.append(text);
            }
            if (run.getCTR() != null) {
                for (int breakIndex = 0; breakIndex < run.getCTR().getBrList().size(); breakIndex++) {
                    builder.append('\n');
                }
            }
        }
    }

    private static void appendTable(StringBuilder builder, XWPFTable table) {
        List<XWPFTableRow> rows = table.getRows();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            if (rowIndex > 0) {
                builder.append('\n');
            }
            List<String> cellValues = new ArrayList<>();
            for (XWPFTableCell cell : rows.get(rowIndex).getTableCells()) {
                cellValues.add(cell.getText().trim());
            }
            builder.append(String.join(" | ", cellValues));
        }
    }
}
