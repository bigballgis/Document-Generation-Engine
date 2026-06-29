package com.bank.docgen.authoring.structured;

import java.util.List;

public record PasteCleaningSummary(
        List<PasteCleaningSummaryItem> items,
        int transformedCount,
        int removedCount,
        int warningCount,
        int blockedCount
) {

    public static PasteCleaningSummary of(List<PasteCleaningSummaryItem> rawItems) {
        List<PasteCleaningSummaryItem> items = rawItems == null ? List.of() : List.copyOf(rawItems);
        int transformed = 0;
        int removed = 0;
        int warning = 0;
        int blocked = 0;
        for (PasteCleaningSummaryItem item : items) {
            switch (item.category()) {
                case TRANSFORMED -> transformed++;
                case REMOVED -> removed++;
                case WARNING -> warning++;
                case BLOCKED -> blocked++;
                default -> {
                }
            }
        }
        return new PasteCleaningSummary(items, transformed, removed, warning, blocked);
    }
}
