package com.bank.docgen.runtime.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DefaultRouteSummaryView(
        String url,
        String currentTargetReleaseVersion,
        String currentTargetStatus,
        Instant updatedAt,
        String updatedBy,
        String explicitVersionUrl
) {
}
