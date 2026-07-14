package com.bank.docgen.contentmodule.api;

import java.time.Instant;

public record ContentModuleReviewRecordView(
        String action,
        String decision,
        String changeSummary,
        String commentSummary,
        String actorUsername,
        Instant createdAt,
        String semanticVersion,
        Boolean selfApprovalException,
        String exceptionReason
) {
}
