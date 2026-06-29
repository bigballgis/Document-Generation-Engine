package com.bank.docgen.authoring.structured;

public record PasteCleaningSummaryItem(
        PasteCleaningCategory category,
        String messageKey,
        String detectionSummary
) {
}
