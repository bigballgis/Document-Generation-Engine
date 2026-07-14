package com.bank.docgen.contentmodule.api;

import java.time.Instant;

public record ContentModuleWorkflowTaskView(
        String moduleId,
        String moduleCode,
        String name,
        String groupCode,
        String kind,
        String semanticVersion,
        String rejectionReason,
        Instant updatedAt
) {
}
