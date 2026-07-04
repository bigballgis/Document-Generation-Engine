package com.bank.docgen.authoring.structured;

@SuppressWarnings("PMD.UnusedAssignment")
public record PasteCleaningResult(
        boolean blocked,
        String cleanedStructuredContentJson,
        PasteCleaningSummary summary,
        String prePasteSnapshotJson
) {
    public PasteCleaningResult {
        summary = summary == null ? null : PasteCleaningSummary.of(summary.items());
    }
}
