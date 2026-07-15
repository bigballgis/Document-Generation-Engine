package com.bank.docgen.apimgmt.api;

import java.time.Instant;
import java.util.UUID;

public record ManagementInvocationSummaryView(
        String invocationId,
        String invocationKind,
        String status,
        String requestId,
        String resolvedReleaseVersion,
        String routeType,
        Instant createdAt,
        String accessAccountSummary,
        UUID releaseBundleSnapshotId,
        String releaseBundleHash
) {
}
