package com.bank.docgen.authoring.structured;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

public record StructuredContentValidationResult(
        List<StructuredContentFidelityIssue> blockers,
        List<StructuredContentFidelityIssue> warnings
) {

    public StructuredContentValidationResult {
        blockers = DefensiveCopies.copyList(blockers);
        warnings = DefensiveCopies.copyList(warnings);
    }

    public static StructuredContentValidationResult of(
            List<StructuredContentFidelityIssue> rawBlockers,
            List<StructuredContentFidelityIssue> rawWarnings
    ) {
        return new StructuredContentValidationResult(rawBlockers, rawWarnings);
    }

    public boolean hasBlockers() {
        return !blockers.isEmpty();
    }
}
