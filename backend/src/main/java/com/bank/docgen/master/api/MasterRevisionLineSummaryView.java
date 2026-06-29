package com.bank.docgen.master.api;

import java.time.Instant;

public record MasterRevisionLineSummaryView(
        String id,
        String lineLabel,
        String status,
        String originalFilename,
        int anchorCount,
        Instant updatedAt,
        String updatedBy,
        boolean current
) {
}
