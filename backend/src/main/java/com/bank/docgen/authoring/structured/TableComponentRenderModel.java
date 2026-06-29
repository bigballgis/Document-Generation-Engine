package com.bank.docgen.authoring.structured;

import java.util.List;

public record TableComponentRenderModel(
        String componentKey,
        List<TableColumnDefinition> columns,
        List<List<TableCellDefinition>> headerRows,
        boolean repeatHeaderAcrossPages,
        TableLoopRowDefinition loopRow,
        List<List<TableCellDefinition>> footerRows
) {
}
