package com.bank.docgen.template.api;

import java.util.List;

public record CoverageSummaryView(
        String templateId,
        int aggregatePercentage,
        boolean belowThreshold,
        List<String> blockerCodes,
        List<CoverageDimensionView> dimensions,
        CoverageThresholdView appliedThreshold
) {
}
