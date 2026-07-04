package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import java.util.List;

public record CoverageSummaryView(
        String templateId,
        int aggregatePercentage,
        boolean belowThreshold,
        List<String> blockerCodes,
        List<CoverageDimensionView> dimensions,
        CoverageThresholdView appliedThreshold
) {
    public CoverageSummaryView {
        blockerCodes = DefensiveCopies.copyList(blockerCodes);
        dimensions = DefensiveCopies.copyList(dimensions);
    }

}
