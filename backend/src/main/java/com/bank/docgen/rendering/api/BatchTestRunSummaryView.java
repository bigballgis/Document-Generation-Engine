package com.bank.docgen.rendering.api;

import java.math.BigDecimal;
import java.time.Instant;

public record BatchTestRunSummaryView(
        String runId,
        Instant createdAt,
        String createdBy,
        String createdByDisplayName,
        String status,
        int successCount,
        int failedCount,
        int totalCount,
        BigDecimal anchorCoveragePct,
        BigDecimal variableCoveragePct,
        BigDecimal sampleCoveragePct,
        Boolean gatePassed,
        Instant invalidatedAt
) {
}
