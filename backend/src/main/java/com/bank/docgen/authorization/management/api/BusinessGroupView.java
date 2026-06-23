package com.bank.docgen.authorization.management.api;

import java.time.Instant;

public record BusinessGroupView(
        String id,
        String groupCode,
        String displayName,
        String dimension,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
