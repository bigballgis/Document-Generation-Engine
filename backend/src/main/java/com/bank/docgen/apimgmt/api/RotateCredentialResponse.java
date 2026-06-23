package com.bank.docgen.apimgmt.api;

import java.time.Instant;

public record RotateCredentialResponse(
        String credentialId,
        String externalId,
        String secret,
        Instant rotatedAt
) {
}
