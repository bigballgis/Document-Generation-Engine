package com.bank.docgen.rendering.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import java.util.Map;

public record TestGenerateRequest(
        Map<String, Object> variables,
        String testDataSetId,
        PreviewCompositionContext context
) {
    public TestGenerateRequest {
        variables = DefensiveCopies.copyMap(variables);
    }

    /** Compatibility constructor for callers that omit composition context. */
    public TestGenerateRequest(Map<String, Object> variables, String testDataSetId) {
        this(variables, testDataSetId, null);
    }
}
