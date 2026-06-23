package com.bank.docgen.runtime.api;

import java.time.Instant;

public record TaskSummaryView(
        String taskId,
        String status,
        String queryPath,
        Instant acceptedAt,
        Instant updatedAt,
        Instant expiresAt
) {
}
