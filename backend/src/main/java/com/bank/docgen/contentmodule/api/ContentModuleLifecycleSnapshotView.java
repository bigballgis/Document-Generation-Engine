package com.bank.docgen.contentmodule.api;

import java.time.Instant;

public record ContentModuleLifecycleSnapshotView(
        String moduleId,
        String state,
        Instant updatedAt,
        String updatedBy
) {
}
