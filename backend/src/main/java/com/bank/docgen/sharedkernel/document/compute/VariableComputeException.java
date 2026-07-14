package com.bank.docgen.sharedkernel.document.compute;

/**
 * Fail-closed compute evaluation / validation failure (CE-K03).
 */
public final class VariableComputeException extends RuntimeException {

    private final String variableKey;
    private final String expressionSummary;

    public VariableComputeException(String variableKey, String expression, String reason) {
        super(reason == null ? "variable compute failed" : reason);
        this.variableKey = variableKey == null ? "" : variableKey;
        this.expressionSummary = ComputeDslLimits.summarizeExpression(expression);
    }

    public String variableKey() {
        return variableKey;
    }

    public String expressionSummary() {
        return expressionSummary;
    }

    public String messageKey() {
        return "api.error.variable.computeFailed";
    }
}
