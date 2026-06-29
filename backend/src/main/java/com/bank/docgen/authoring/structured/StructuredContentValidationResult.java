package com.bank.docgen.authoring.structured;

import java.util.List;

public record StructuredContentValidationResult(
        List<StructuredContentFidelityIssue> blockers,
        List<StructuredContentFidelityIssue> warnings
) {

    public static StructuredContentValidationResult of(
            List<StructuredContentFidelityIssue> rawBlockers,
            List<StructuredContentFidelityIssue> rawWarnings
    ) {
        return new StructuredContentValidationResult(
                rawBlockers == null ? List.of() : List.copyOf(rawBlockers),
                rawWarnings == null ? List.of() : List.copyOf(rawWarnings)
        );
    }

    public boolean hasBlockers() {
        return !blockers.isEmpty();
    }
}
