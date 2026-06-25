package com.bank.docgen.template.api;

public record CoverageDimensionView(
        String dimensionCode,
        int totalCount,
        int exercisedCount,
        int percentage,
        int thresholdPercentage,
        boolean belowThreshold
) {
}
