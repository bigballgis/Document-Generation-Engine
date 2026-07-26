package com.bank.docgen.rendering;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

final class StructuredContentDocxTableSupport {

    private final StructuredContentDocxStyleSupport styles;

    StructuredContentDocxTableSupport(StructuredContentDocxStyleSupport styles) {
        this.styles = styles;
    }

    void populateTable(JsonNode tableDefinition, XWPFTable table, Map<String, Object> variables) {
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
        boolean repeatHeaderAcrossPages = tableDefinition.path("repeatHeaderAcrossPages").asBoolean(false);
        int rowIndex = 0;
        JsonNode headerRows = tableDefinition.path("headerRows");
        if (headerRows.isArray() && !headerRows.isEmpty()) {
            // FOS-W15-4: PowerShell ConvertTo-Json may flatten @(@(...)) to a single cell array.
            // Accept both [[cells...]] and [cell, cell, ...] shapes.
            JsonNode headerRow = normalizeHeaderRow(headerRows);
            writeTableRow(table, rowIndex, headerRow, columnKeys, variables, true, repeatHeaderAcrossPages);
            rowIndex++;
        }
        JsonNode loopRow = tableDefinition.path("loopRow");
        String loopVariable = loopRow.path("loopVariable").asText("");
        Object rawItems = variables.get(loopVariable);
        if (rawItems instanceof List<?> items) {
            for (Object item : items) {
                writeTableRow(
                        table,
                        rowIndex,
                        loopRow.path("cells"),
                        columnKeys,
                        scopedVariables(variables, item),
                        false,
                        false
                );
                rowIndex++;
            }
        }
        JsonNode footerRows = tableDefinition.path("footerRows");
        if (footerRows.isArray()) {
            for (JsonNode footerRow : footerRows) {
                writeTableRow(table, rowIndex, footerRow, columnKeys, variables, false, false);
                rowIndex++;
            }
        }
    }

    private void writeTableRow(
            XWPFTable table,
            int rowIndex,
            JsonNode cellsNode,
            List<String> columnKeys,
            Map<String, Object> scopedVariables,
            boolean header,
            boolean repeatHeaderAcrossPages
    ) {
        XWPFTableRow row = rowIndex < table.getNumberOfRows() ? table.getRow(rowIndex) : table.createRow();
        while (row.getTableCells().size() < columnKeys.size()) {
            row.addNewTableCell();
        }
        if (header && repeatHeaderAcrossPages) {
            // OOXML <w:tblHeader/> — Word repeats this row on each page of the table.
            row.setRepeatHeader(true);
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
                styles.applyParagraphStyle(paragraph, "TableHeader");
            }
            styles.writeRunText(paragraph, valuesByColumn.getOrDefault(columnKey, ""), header, false, false);
        }
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

    /**
     * Returns the first header row cells array. Canonical shape is {@code [[cell...]]};
     * legacy/flattened shape is {@code [cell...]} (FOS-W15-4 / PowerShell ConvertTo-Json).
     */
    static JsonNode normalizeHeaderRow(JsonNode headerRows) {
        JsonNode first = headerRows.get(0);
        if (first != null && first.isArray()) {
            return first;
        }
        if (first != null && first.isObject() && first.has("columnKey")) {
            return headerRows;
        }
        return first;
    }

    static Map<String, Object> scopedVariables(Map<String, Object> base, Object item) {
        Map<String, Object> scoped = new LinkedHashMap<>(base);
        if (item instanceof Map<?, ?> mapItem) {
            mapItem.forEach((key, value) -> scoped.put(String.valueOf(key), value));
        }
        return scoped;
    }
}
