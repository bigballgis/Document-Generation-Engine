package com.bank.docgen.apimgmt.service;

import java.time.Instant;
import java.util.UUID;

public record ManagementInvocationFilters(
        String status,
        String invocationKind,
        String requestId,
        Instant createdAfter,
        Instant createdBefore,
        UUID credentialId
) {

    public static ManagementInvocationFilters empty() {
        return new ManagementInvocationFilters(null, null, null, null, null, null);
    }
}
