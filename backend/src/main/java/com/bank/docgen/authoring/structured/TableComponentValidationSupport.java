package com.bank.docgen.authoring.structured;

import com.bank.docgen.sharedkernel.document.fidelity.FidelityWarningCode;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Package-private table-component layout / schema / cell parsing helpers.
 */
final class TableComponentValidationSupport {

    TableComponentValidationResult validateAndBuildRenderModel(JsonNode root) {
        List<StructuredContentFidelityIssue> blockers = new ArrayList<>();
        if (!root.isObject()) {
            blockers.add(tableIssue(
                    FidelityWarningCode.INVALID_TABLE_COMPONENT,
                    TableComponentService.MESSAGE_KEY_INVALID_TABLE_COMPONENT,
                    "definition",
                    "Table component definition is not a JSON object.",
                    "Provide a valid table component definition."
            ));
            return result(blockers, Optional.empty());
        }
        validateLayout(root.get("layout"), blockers);
        if (!blockers.isEmpty()) {
            return result(blockers, Optional.empty());
        }
        String componentKey = root.path("componentKey").asText("").trim();
        if (componentKey.isBlank()) {
            blockers.add(tableIssue(
                    FidelityWarningCode.INVALID_TABLE_COMPONENT,
                    TableComponentService.MESSAGE_KEY_INVALID_TABLE_COMPONENT,
                    "componentKey",
                    "Table component key is required.",
                    "Set componentKey on the table component definition."
            ));
            return result(blockers, Optional.empty());
        }
        List<TableColumnDefinition> columns = parseColumnSchema(root.get("columnSchema"), blockers);
        if (blockers.stream().anyMatch(issue -> issue.code() == FidelityWarningCode.INVALID_TABLE_COMPONENT)) {
            return result(blockers, Optional.empty());
        }
        Set<String> columnKeys = new HashSet<>();
        columns.forEach(column -> columnKeys.add(column.columnKey()));
        List<List<TableCellDefinition>> headerRows = parseRows(root.get("headerRows"), columnKeys, blockers, "headerRows");
        List<List<TableCellDefinition>> footerRows = parseRows(root.get("footerRows"), columnKeys, blockers, "footerRows");
        TableLoopRowDefinition loopRow = parseLoopRow(root.get("loopRow"), columnKeys, blockers);
        boolean repeatHeaderAcrossPages = root.path("repeatHeaderAcrossPages").asBoolean(false);
        if (blockers.isEmpty()) {
            return result(
                    blockers,
                    Optional.of(new TableComponentRenderModel(
                            componentKey,
                            List.copyOf(columns),
                            List.copyOf(headerRows),
                            repeatHeaderAcrossPages,
                            loopRow,
                            List.copyOf(footerRows)
                    ))
            );
        }
        return result(blockers, Optional.empty());
    }

    void walkContentNodes(
            JsonNode nodes,
            String location,
            Set<String> declaredVariableKeys,
            List<StructuredContentFidelityIssue> blockers,
            java.util.function.Function<String, TableComponentValidationResult> validateJson
    ) {
        for (int index = 0; index < nodes.size(); index++) {
            JsonNode node = nodes.get(index);
            String nodeLocation = location + "[" + index + "]";
            if (node.isObject()
                    && "tableComponentRef".equals(node.path("type").asText())
                    && node.has("tableComponent")
                    && node.get("tableComponent").isObject()) {
                TableComponentValidationResult validation =
                        validateJson.apply(node.get("tableComponent").toString());
                blockers.addAll(validation.fidelity().blockers());
                validateLoopRowVariable(
                        node.get("tableComponent").get("loopRow"),
                        nodeLocation + ".tableComponent.loopRow",
                        declaredVariableKeys,
                        blockers
                );
            }
            JsonNode children = node.get("children");
            if (children != null && children.isArray()) {
                walkContentNodes(children, nodeLocation + ".children", declaredVariableKeys, blockers, validateJson);
            }
        }
    }

    void validateLoopRowVariable(
            JsonNode loopRowNode,
            String location,
            Set<String> declaredVariableKeys,
            List<StructuredContentFidelityIssue> blockers
    ) {
        if (loopRowNode == null || loopRowNode.isNull()) {
            return;
        }
        String loopVariable = loopRowNode.path("loopVariable").asText("").trim();
        if (loopVariable.isBlank() || !declaredVariableKeys.contains(loopVariable)) {
            blockers.add(tableIssue(
                    FidelityWarningCode.UNRESOLVED_VARIABLE,
                    NodeMatrixValidationService.MESSAGE_KEY_UNRESOLVED_VARIABLE,
                    location,
                    "Loop variable '" + loopVariable + "' is not declared in the template schema.",
                    "Declare the loop variable in the template schema or remove the loop row."
            ));
        }
    }

    void validateLayout(JsonNode layout, List<StructuredContentFidelityIssue> blockers) {
        if (layout == null || layout.isNull() || !layout.isObject()) {
            return;
        }
        if (layout.path("nestedTable").asBoolean(false)) {
            blockers.add(tableIssue(
                    FidelityWarningCode.NESTED_TABLE,
                    TableComponentService.MESSAGE_KEY_NESTED_TABLE,
                    "layout.nestedTable",
                    "Nested tables are not supported in v1 table components.",
                    "Flatten the table structure or split into separate table components."
            ));
        }
        if (layout.path("floating").asBoolean(false) || layout.path("absolutePosition").asBoolean(false)) {
            blockers.add(tableIssue(
                    FidelityWarningCode.UNRELIABLE_TABLE_LAYOUT,
                    TableComponentService.MESSAGE_KEY_UNRELIABLE_TABLE_LAYOUT,
                    "layout",
                    "Floating or absolute-positioned tables cannot render reliably.",
                    "Use a controlled table layout without floating or absolute positioning."
            ));
        }
    }

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

    TableComponentValidationResult result(
            List<StructuredContentFidelityIssue> blockers,
            Optional<TableComponentRenderModel> renderModel
    ) {
        return new TableComponentValidationResult(
                StructuredContentValidationResult.of(blockers, List.of()),
                renderModel
        );
    }

    StructuredContentFidelityIssue tableIssue(
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

    TableComponentValidationResult invalidDefinitionResult() {
        List<StructuredContentFidelityIssue> blockers = new ArrayList<>();
        blockers.add(tableIssue(
                FidelityWarningCode.INVALID_TABLE_COMPONENT,
                TableComponentService.MESSAGE_KEY_INVALID_TABLE_COMPONENT,
                "definition",
                "Table component definition is invalid or unreadable.",
                "Provide valid JSON for the table component."
        ));
        return result(blockers, Optional.empty());
    }
}
