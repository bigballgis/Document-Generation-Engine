package com.bank.docgen.template.api;

public record PasteCleanResultView(
        boolean blocked,
        String cleanedStructuredContentJson,
        PasteCleaningSummaryView summary,
        String prePasteSnapshotJson
) {
}
