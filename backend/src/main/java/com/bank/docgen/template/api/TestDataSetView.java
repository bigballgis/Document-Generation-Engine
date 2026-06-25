package com.bank.docgen.template.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record TestDataSetView(
        String testDataSetId,
        String templateId,
        String name,
        String description,
        Map<String, Object> variables,
        boolean required,
        String scenarioName,
        List<String> coverageTags,
        int datasetVersion,
        boolean locked,
        String derivedFromId,
        Instant createdAt,
        Instant updatedAt
) {
}
