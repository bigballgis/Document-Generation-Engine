package com.bank.docgen.runtime.api;

public record AdGroupAuthorizationSummaryView(
        boolean authorized,
        int cacheTtlSeconds,
        String authorizationScopeSummary,
        String effectivePolicyDescription
) {
}
