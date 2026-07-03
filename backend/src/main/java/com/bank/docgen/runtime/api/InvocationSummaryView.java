package com.bank.docgen.runtime.api;

import java.time.Instant;

public record InvocationSummaryView(
        String invocationId,
        String invocationKind,
        String templateId,
        String resolvedReleaseVersion,
        String routeType,
        String status,
        String requestId,
        String idempotencyKey,
        String batchId,
        String taskId,
        String parentInvocationId,
        String itemId,
        Boolean artifactSaved,
        String documentId,
        Instant documentExpiresAt,
        Instant recordExpiresAt,
        Instant createdAt,
        Integer childItemCount
) {
}
