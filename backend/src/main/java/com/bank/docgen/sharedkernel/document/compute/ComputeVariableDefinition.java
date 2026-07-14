package com.bank.docgen.sharedkernel.document.compute;

/**
 * Schema-facing compute variable descriptor for the evaluation engine.
 */
public record ComputeVariableDefinition(
        String variableKey,
        String computeExpression,
        boolean computedType
) {
    public boolean isCompute() {
        if (computedType) {
            return true;
        }
        return computeExpression != null && !computeExpression.isBlank();
    }
}
