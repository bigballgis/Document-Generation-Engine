package com.bank.docgen.rendering.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import java.util.Map;

public record TestGenerateRequest(
        Map<String, Object> variables,
        String testDataSetId
) {
    public TestGenerateRequest {
        variables = DefensiveCopies.copyMap(variables);
    }

}
