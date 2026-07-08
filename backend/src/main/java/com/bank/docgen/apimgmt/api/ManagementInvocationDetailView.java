package com.bank.docgen.apimgmt.api;

import java.time.Instant;
import java.util.UUID;

public record ManagementInvocationDetailView(
        String invocationId,
        String requestId,
        String routeType,
        String resolvedReleaseVersion,
        String outcome,
        Long durationMs,
        String accessAccountSummary,
        UUID credentialId,
        String batchId,
        String parentInvocationId,
        Instant createdAt,
        boolean documentPresent,
        ManagementInvocationAuditLinkHintView auditLinkHint
) {
}
