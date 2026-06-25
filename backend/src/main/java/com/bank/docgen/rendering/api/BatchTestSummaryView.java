package com.bank.docgen.rendering.api;

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
}
