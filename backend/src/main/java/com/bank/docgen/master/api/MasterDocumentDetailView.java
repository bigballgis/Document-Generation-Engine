package com.bank.docgen.master.api;

import java.time.Instant;
import java.util.List;

public record MasterDocumentDetailView(
        String id,
        String groupCode,
        String name,
        String description,
        String status,
        String originalFilename,
        String changeSummary,
        List<MasterAnchorView> anchors,
        List<MasterReviewRecordView> reviewHistory,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
}
