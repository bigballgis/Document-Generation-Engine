package com.bank.docgen.template.api;

import java.util.List;

public record PasteCleaningSummaryView(
        List<PasteCleaningSummaryItemView> items,
        int transformedCount,
        int removedCount,
        int warningCount,
        int blockedCount
) {
}
