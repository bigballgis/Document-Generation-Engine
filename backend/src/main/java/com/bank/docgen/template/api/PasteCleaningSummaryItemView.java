package com.bank.docgen.template.api;

import com.bank.docgen.authoring.structured.PasteCleaningCategory;

public record PasteCleaningSummaryItemView(
        PasteCleaningCategory category,
        String messageKey,
        String detectionSummary
) {
}
