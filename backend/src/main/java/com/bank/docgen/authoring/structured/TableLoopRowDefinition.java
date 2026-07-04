package com.bank.docgen.authoring.structured;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

public record TableLoopRowDefinition(
        String loopVariable,
        List<TableCellDefinition> cells
) {
    public TableLoopRowDefinition {
        cells = DefensiveCopies.copyList(cells);
    }
}
