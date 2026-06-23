package com.bank.docgen.apimgmt.api;

import java.time.Instant;

public record ApiCredentialCreatedView(
        String credentialId,
        String externalId,
        String secret,
        String status,
        Instant createdAt
) {
}
