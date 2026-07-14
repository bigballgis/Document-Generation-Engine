package com.bank.docgen.template.api;

/**
 * CE-K03 compute expression validation result.
 */
public record ComputeExpressionValidateView(
        boolean valid,
        String message
) {
}
