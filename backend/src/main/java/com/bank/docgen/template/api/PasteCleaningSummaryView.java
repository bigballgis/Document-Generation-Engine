package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import java.util.List;

public record PasteCleaningSummaryView(
        List<PasteCleaningSummaryItemView> items,
        int transformedCount,
        int removedCount,
        int warningCount,
        int blockedCount
) {
    public PasteCleaningSummaryView {
        items = DefensiveCopies.copyList(items);
    }

}
