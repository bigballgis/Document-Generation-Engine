package com.bank.docgen.runtime.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RuntimeCredentialSummaryView(
        /** Serialized as OpenAPI {@code credentialId} (FOS-W9-6). */
        @JsonProperty("credentialId") String credentialExternalId,
        String status,
        String fingerprintSummary,
        Instant expiresAt
) {
}
