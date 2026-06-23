package com.bank.docgen.master.api;

import java.time.Instant;

public record MasterReviewRecordView(
        String action,
        String decision,
        String changeSummary,
        String commentSummary,
        String actorUsername,
        Instant createdAt
) {
}
