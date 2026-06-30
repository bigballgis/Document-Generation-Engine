package com.bank.docgen.master.api;

import java.time.Instant;
import java.util.List;

public record MasterRevisionLineDetailView(
        String id,
        String masterId,
        String lineLabel,
        String status,
        String originalFilename,
        String changeSummary,
        boolean current,
        int revisionSequence,
        List<MasterAnchorView> anchors,
        List<MasterReviewRecordView> reviewHistory,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
}
