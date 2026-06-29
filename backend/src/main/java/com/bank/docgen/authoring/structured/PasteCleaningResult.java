package com.bank.docgen.authoring.structured;

public record PasteCleaningResult(
        boolean blocked,
        String cleanedStructuredContentJson,
        PasteCleaningSummary summary,
        String prePasteSnapshotJson
) {
}
