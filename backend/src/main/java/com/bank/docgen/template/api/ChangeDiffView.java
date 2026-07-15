package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

public record ChangeDiffView(
        String templateId,
        String baselineReleaseVersion,
        String candidateVersionId,
        String candidateReleaseVersion,
        boolean hasChanges,
        int totalChangeCount,
        List<ChangeDiffDimensionView> dimensions,
        List<ChangeDiffHumanReadableEntry> humanReadableEntries
) {
    public ChangeDiffView {
        dimensions = DefensiveCopies.copyList(dimensions);
        humanReadableEntries = DefensiveCopies.copyList(humanReadableEntries);
    }
}
