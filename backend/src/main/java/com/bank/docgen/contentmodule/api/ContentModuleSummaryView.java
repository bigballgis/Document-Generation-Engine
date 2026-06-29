package com.bank.docgen.contentmodule.api;

import java.time.Instant;
import java.util.List;

public record ContentModuleSummaryView(
        String moduleId,
        String moduleCode,
        String groupCode,
        String name,
        String description,
        List<String> sharedGroupCodes,
        Instant createdAt,
        Instant updatedAt
) {
}
