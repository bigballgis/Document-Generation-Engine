package com.bank.docgen.runtime.api;

import java.util.List;

public record BatchSummaryView(
        int totalCount,
        int processedCount,
        int successCount,
        int failureCount,
        int skippedCount
) {
}
