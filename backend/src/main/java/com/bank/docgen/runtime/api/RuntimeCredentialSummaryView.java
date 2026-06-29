package com.bank.docgen.runtime.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RuntimeCredentialSummaryView(
        String credentialExternalId,
        String status,
        String fingerprintSummary
) {
}
