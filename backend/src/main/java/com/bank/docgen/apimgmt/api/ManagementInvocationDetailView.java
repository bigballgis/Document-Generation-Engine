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
        ManagementInvocationAuditLinkHintView auditLinkHint,
        String errorCode,
        String errorCategory,
        String errorMessageKey,
        Boolean errorRetryable,
        String errorMessage,
        UUID releaseBundleSnapshotId,
        String releaseBundleHash
) {
}
