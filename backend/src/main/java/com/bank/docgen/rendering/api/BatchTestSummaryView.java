package com.bank.docgen.rendering.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import java.time.Instant;
import java.util.List;

public record BatchTestSummaryView(
        String batchTestRunId,
        String templateId,
        int totalSamples,
        int succeededCount,
        int failedCount,
        int warningCount,
        int blockerCount,
        List<BatchTestSampleResultView> samples,
        Instant createdAt
) {
    public BatchTestSummaryView {
        samples = DefensiveCopies.copyList(samples);
    }

}
