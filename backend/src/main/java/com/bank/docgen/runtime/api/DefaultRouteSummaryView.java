package com.bank.docgen.runtime.api;

import java.time.Instant;

public record DefaultRouteSummaryView(
        String url,
        String currentTargetReleaseVersion,
        String currentTargetStatus,
        Instant updatedAt,
        String updatedBy,
        String explicitVersionUrl
) {
}
