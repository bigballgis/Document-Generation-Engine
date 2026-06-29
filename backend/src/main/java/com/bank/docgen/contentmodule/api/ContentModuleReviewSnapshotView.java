package com.bank.docgen.contentmodule.api;

import java.time.Instant;

public record ContentModuleReviewSnapshotView(
        String moduleId,
        String state,
        Instant updatedAt,
        String updatedBy,
        String rejectionReason
) {
}
