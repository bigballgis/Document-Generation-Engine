package com.bank.docgen.authoring.structured;

import java.util.List;

public record TableLoopRowDefinition(
        String loopVariable,
        List<TableCellDefinition> cells
) {
}
