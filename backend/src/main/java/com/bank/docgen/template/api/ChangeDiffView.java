package com.bank.docgen.template.api;

import java.util.List;

public record ChangeDiffView(
        String templateId,
        String baselineReleaseVersion,
        String candidateVersionId,
        boolean hasChanges,
        int totalChangeCount,
        List<ChangeDiffDimensionView> dimensions
) {
}
