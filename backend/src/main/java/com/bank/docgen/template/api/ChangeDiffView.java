package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

public record ChangeDiffView(
        String templateId,
        String baselineReleaseVersion,
        String candidateVersionId,
        boolean hasChanges,
        int totalChangeCount,
        List<ChangeDiffDimensionView> dimensions
) {
    public ChangeDiffView {
        dimensions = DefensiveCopies.copyList(dimensions);
    }
}
