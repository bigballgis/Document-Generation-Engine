package com.bank.docgen.rendering.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

public record PreviewComparisonView(
        int totalDiffCount,
        int blockerCount,
        int warningCount,
        List<PreviewComparisonItemView> items
) {
    public PreviewComparisonView {
        items = DefensiveCopies.copyList(items);
    }
}
