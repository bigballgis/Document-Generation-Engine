package com.bank.docgen.template.api;

import java.time.Instant;
import java.util.Map;

public record TestDataSetView(
        String testDataSetId,
        String templateId,
        String name,
        String description,
        Map<String, Object> variables,
        Instant createdAt,
        Instant updatedAt
) {
}
