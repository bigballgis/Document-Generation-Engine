package com.bank.docgen.runtime.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdGroupAuthorizationSummaryView(
        boolean authorized,
        int cacheTtlSeconds,
        String authorizationScopeSummary,
        String effectivePolicyDescription
) {
}
