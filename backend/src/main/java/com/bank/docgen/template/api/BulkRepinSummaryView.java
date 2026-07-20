package com.bank.docgen.template.api;

public record BulkRepinSummaryView(
        boolean dryRun,
        int wouldApplyCount,
        int appliedCount,
        int skippedLockedCount,
        int skippedAlreadyAtTargetCount,
        int skippedNoMatchCount,
        int failedCount
) {
}
