package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.Map;

/**
 * CE-K03 sample evaluation request for author compute preview (no document produced).
 */
public record ComputeExpressionEvaluateRequest(
        String variableKey,
        String expression,
        Map<String, Object> sampleVariables,
        String locale
) {
    public ComputeExpressionEvaluateRequest {
        sampleVariables = DefensiveCopies.copyMap(sampleVariables);
    }
}
