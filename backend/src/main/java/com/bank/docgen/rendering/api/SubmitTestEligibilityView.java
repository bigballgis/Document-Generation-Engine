package com.bank.docgen.rendering.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.time.Instant;
import java.util.List;

public record SubmitTestEligibilityView(
        boolean eligible,
        Conditions conditions,
        BlockingDetails blockingDetails,
        Thresholds thresholds,
        Instant latestRunAt
) {
    public record Conditions(
            boolean hasValidTestResult,
            boolean allSamplesSucceeded,
            boolean coverageGatePassed
    ) {
    }

    public record BlockingDetails(
            List<String> uncoveredAnchors,
            List<String> uncoveredVariables,
            int uncoveredAnchorsTotal,
            int uncoveredVariablesTotal,
            List<String> failedDataSetNames
    ) {
        public BlockingDetails {
            uncoveredAnchors = DefensiveCopies.copyStringList(uncoveredAnchors);
            uncoveredVariables = DefensiveCopies.copyStringList(uncoveredVariables);
            failedDataSetNames = DefensiveCopies.copyStringList(failedDataSetNames);
        }
    }

    public record Thresholds(
            int anchorCoveragePct,
            int variableCoveragePct,
            int sampleCoveragePct
    ) {
    }
}
