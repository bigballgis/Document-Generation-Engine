package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

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
    public TestDataSetView {
        variables = DefensiveCopies.copyMap(variables);
        coverageTags = DefensiveCopies.copyList(coverageTags);
    }

}
