package com.bank.docgen.runtime.api;

public record RuntimeCredentialSummaryView(
        String credentialExternalId,
        String status,
        String fingerprintSummary
) {
}
