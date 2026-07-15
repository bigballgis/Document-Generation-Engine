package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public record UpsertTestDataSetRequest(
        @NotBlank String name,
        String description,
        @NotNull Map<String, Object> variables,
        Boolean required,
        String scenarioName,
        List<String> coverageTags,
        String piiHandling,
        String piiConfirmReason,
        Boolean secondaryConfirmed
) {
    public UpsertTestDataSetRequest {
        variables = DefensiveCopies.copyMap(variables);
        coverageTags = DefensiveCopies.copyStringList(coverageTags);
    }

    public UpsertTestDataSetRequest(
            String name,
            String description,
            Map<String, Object> variables,
            Boolean required,
            String scenarioName,
            List<String> coverageTags
    ) {
        this(name, description, variables, required, scenarioName, coverageTags, null, null, null);
    }
}
