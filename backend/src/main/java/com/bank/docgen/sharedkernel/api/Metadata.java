package com.bank.docgen.sharedkernel.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Metadata(
        String auditId,
        String traceId,
        String requestId,
        String idempotencyKey,
        String idempotencyStatus,
        String originalRequestAt,
        String templateId,
        String routeType,
        String resolvedReleaseVersion
) {
    public static Metadata minimal(String auditId, String traceId) {
        return new Metadata(auditId, traceId, null, null, null, null, null, null, null);
    }
}
