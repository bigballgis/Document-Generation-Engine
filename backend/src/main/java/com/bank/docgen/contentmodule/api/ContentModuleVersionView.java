package com.bank.docgen.contentmodule.api;

import com.bank.docgen.contentmodule.domain.ContentModuleLifecycleState;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewState;
import java.time.Instant;

public record ContentModuleVersionView(
        String versionId,
        String semanticVersion,
        ContentModuleReviewState reviewState,
        ContentModuleLifecycleState lifecycleState,
        String changeDescription,
        String contentStructureJson,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt
) {
}
