package com.bank.docgen.template.api;

import java.util.List;

/**
 * CE-K03 compute expression validation request (syntax + reference existence).
 */
public record ComputeExpressionValidateRequest(
        String variableKey,
        String expression,
        List<String> knownVariableKeys
) {
}
