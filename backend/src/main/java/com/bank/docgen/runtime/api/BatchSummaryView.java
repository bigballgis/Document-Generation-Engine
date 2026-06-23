package com.bank.docgen.runtime.api;

public record BatchSummaryView(
        int totalCount,
        int processedCount,
        int successCount,
        int failureCount,
        int skippedCount
) {
}
