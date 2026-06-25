package com.bank.docgen.template.api;

public record CoverageThresholdView(
        String scopeType,
        String groupCode,
        int minRequiredVariablePct,
        int minRequiredSamplePct,
        int minAnchorBindingPct
) {
}
