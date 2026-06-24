package com.bank.docgen.master.api;

import java.time.Instant;

public record MasterDocumentSummaryView(
        String id,
        String groupCode,
        String name,
        String status,
        String originalFilename,
        int anchorCount,
        String updatedBy,
        Instant updatedAt
) {
}
