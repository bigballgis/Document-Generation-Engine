package com.bank.docgen.apimgmt.api;

/**
 * Lightweight Overview readiness counts for ApiPolicyHomeView (SCEN-AOD-06).
 * Not a template catalog — counts only, scoped by GroupAccessService.
 */
public record ApiAccessReadinessSummaryView(
        long publishedInScopeCount,
        long attentionCount,
        long pendingReleaseNeedingSetupCount
) {
}
