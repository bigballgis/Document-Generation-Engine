package com.bank.docgen.authoring.structured;

import com.bank.docgen.sharedkernel.document.fidelity.FidelityWarningCode;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Package-private column / row / cell parse helpers for table components.
 */
final class TableComponentParseSupport {

    List<TableColumnDefinition> parseColumnSchema(JsonNode columnSchema, List<StructuredContentFidelityIssue> blockers) {
        List<TableColumnDefinition> columns = new ArrayList<>();
        if (columnSchema == null || !columnSchema.isArray() || columnSchema.isEmpty()) {
            blockers.add(tableIssue(
                    FidelityWarningCode.INVALID_TABLE_COMPONENT,
                    TableComponentService.MESSAGE_KEY_INVALID_TABLE_COMPONENT,
                    "columnSchema",
                    "Table component columnSchema must contain at least one column.",
                    "Define one or more columns with columnKey and optional widthPct."
            ));
            return columns;
        }
        Set<String> seenKeys = new HashSet<>();
        for (int index = 0; index < columnSchema.size(); index++) {
            JsonNode columnNode = columnSchema.get(index);
            String columnKey = columnNode.path("columnKey").asText("").trim();
            if (columnKey.isBlank() || !seenKeys.add(columnKey)) {
                blockers.add(tableIssue(
                        FidelityWarningCode.INVALID_TABLE_COMPONENT,
                        TableComponentService.MESSAGE_KEY_INVALID_TABLE_COMPONENT,
                        "columnSchema[" + index + "]",
                        "Each columnSchema entry requires a unique columnKey.",
                        "Provide unique columnKey values in columnSchema."
                ));
                continue;
            }
            Integer widthPct = columnNode.has("widthPct") && columnNode.get("widthPct").isNumber()
                    ? columnNode.get("widthPct").intValue()
                    : null;
            columns.add(new TableColumnDefinition(columnKey, widthPct));
        }
        return columns;
    }

    List<List<TableCellDefinition>> parseRows(
            JsonNode rowsNode,
            Set<String> columnKeys,
            List<StructuredContentFidelityIssue> blockers,
            String fieldName
    ) {
        List<List<TableCellDefinition>> rows = new ArrayList<>();
        if (rowsNode == null || rowsNode.isNull()) {
            return rows;
        }
        if (!rowsNode.isArray()) {
            blockers.add(tableIssue(
                    FidelityWarningCode.INVALID_TABLE_COMPONENT,
                    TableComponentService.MESSAGE_KEY_INVALID_TABLE_COMPONENT,
                    fieldName,
                    fieldName + " must be an array of row cell arrays.",
                    "Provide header/footer rows as arrays of cell objects."
            ));
            return rows;
        }
        for (int rowIndex = 0; rowIndex < rowsNode.size(); rowIndex++) {
            JsonNode rowNode = rowsNode.get(rowIndex);
            if (!rowNode.isArray()) {
                continue;
            }
            List<TableCellDefinition> cells = new ArrayList<>();
            for (int cellIndex = 0; cellIndex < rowNode.size(); cellIndex++) {
                cells.add(parseCell(rowNode.get(cellIndex), columnKeys, blockers, fieldName + "[" + rowIndex + "][" + cellIndex + "]"));
            }
            rows.add(cells);
        }
        return rows;
    }

    TableLoopRowDefinition parseLoopRow(
            JsonNode loopRowNode,
            Set<String> columnKeys,
            List<StructuredContentFidelityIssue> blockers
    ) {
        if (loopRowNode == null || loopRowNode.isNull()) {
            return null;
        }
        String loopVariable = loopRowNode.path("loopVariable").asText("").trim();
        List<TableCellDefinition> cells = new ArrayList<>();
        JsonNode cellsNode = loopRowNode.get("cells");
        if (cellsNode != null && cellsNode.isArray()) {
            for (int index = 0; index < cellsNode.size(); index++) {
                cells.add(parseCell(cellsNode.get(index), columnKeys, blockers, "loopRow.cells[" + index + "]"));
            }
        }
        return new TableLoopRowDefinition(loopVariable.isBlank() ? null : loopVariable, List.copyOf(cells));
    }

    TableCellDefinition parseCell(
            JsonNode cellNode,
            Set<String> columnKeys,
            List<StructuredContentFidelityIssue> blockers,
            String location
    ) {
        if (cellNode == null || !cellNode.isObject()) {
            return new TableCellDefinition(null, null, null);
        }
        String columnKey = cellNode.path("columnKey").asText("").trim();
        if (!columnKey.isBlank() && !columnKeys.contains(columnKey)) {
            blockers.add(tableIssue(
                    FidelityWarningCode.INVALID_TABLE_COMPONENT,
                    TableComponentService.MESSAGE_KEY_INVALID_TABLE_COMPONENT,
                    location,
                    "Cell columnKey '" + columnKey + "' is not declared in columnSchema.",
                    "Reference a columnKey from columnSchema."
            ));
        }
        String value = cellNode.has("value") ? cellNode.get("value").asText(null) : null;
        String variableKey = cellNode.has("variableKey") ? cellNode.get("variableKey").asText(null) : null;
        return new TableCellDefinition(columnKey.isBlank() ? null : columnKey, value, variableKey);
    }

    private StructuredContentFidelityIssue tableIssue(
            FidelityWarningCode code,
            String messageKey,
            String location,
            String detectionSummary,
            String suggestion
    ) {
        return new StructuredContentFidelityIssue(
                StructuredContentFidelitySeverity.BLOCKER,
                code,
                messageKey,
                location,
                detectionSummary,
                suggestion
        );
    }
}
