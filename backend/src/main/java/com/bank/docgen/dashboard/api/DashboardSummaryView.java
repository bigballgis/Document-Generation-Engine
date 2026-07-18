package com.bank.docgen.dashboard.api;

/**
 * Authorized-group Dashboard Overview bucket counts (PRR-D01c / BDD-PRR-D01C-002).
 * Counts only — not a catalog page payload.
 */
public record DashboardSummaryView(
        long masterPendingReview,
        long masterVersionsInProgress,
        long templateVersionsInWorkflow,
        long publishedVersions,
        long stoppedVersions,
        long catalogMasters,
        long catalogTemplates
) {

    public static DashboardSummaryView zeros() {
        return new DashboardSummaryView(0L, 0L, 0L, 0L, 0L, 0L, 0L);
    }
}
