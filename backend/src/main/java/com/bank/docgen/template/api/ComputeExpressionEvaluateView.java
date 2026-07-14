package com.bank.docgen.template.api;

/**
 * CE-K03 sample evaluation result (author compute preview).
 */
public record ComputeExpressionEvaluateView(
        boolean success,
        Object result,
        String variableKey,
        String expressionSummary
) {
}
