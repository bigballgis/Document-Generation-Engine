package com.bank.docgen.apimgmt.api;

import java.time.Instant;

public record ApiCredentialSummaryView(
        String credentialId,
        String externalId,
        String status,
        Instant createdAt,
        Instant revokedAt
) {
}
