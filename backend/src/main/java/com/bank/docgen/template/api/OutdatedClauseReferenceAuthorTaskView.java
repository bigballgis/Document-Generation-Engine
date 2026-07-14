package com.bank.docgen.template.api;

import java.time.Instant;

public record OutdatedClauseReferenceAuthorTaskView(
        String templateId,
        String externalId,
        String groupCode,
        String name,
        String inFlightDevVersionId,
        int outdatedReferenceCount,
        Instant updatedAt
) {
}
