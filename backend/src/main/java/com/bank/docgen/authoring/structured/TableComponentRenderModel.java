package com.bank.docgen.authoring.structured;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

public record TableComponentRenderModel(
        String componentKey,
        List<TableColumnDefinition> columns,
        List<List<TableCellDefinition>> headerRows,
        boolean repeatHeaderAcrossPages,
        TableLoopRowDefinition loopRow,
        List<List<TableCellDefinition>> footerRows
) {
    public TableComponentRenderModel {
        columns = DefensiveCopies.copyList(columns);
        headerRows = DefensiveCopies.copyNestedList(headerRows);
        footerRows = DefensiveCopies.copyNestedList(footerRows);
    }
}
