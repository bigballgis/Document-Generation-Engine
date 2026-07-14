package com.bank.docgen.sharedkernel.document.compute;

import java.math.BigDecimal;

/**
 * Hard bounds for the whitelist variable-compute DSL (CE-K03 / ADR-0056).
 */
public final class ComputeDslLimits {

    public static final int MAX_EXPRESSION_LENGTH = 2048;
    public static final int MAX_NESTING_DEPTH = 8;
    public static final int MAX_PATH_SEGMENTS = 16;
    public static final int MAX_DEPENDENCY_DEPTH = 8;
    public static final int MAX_COLLECTION_SIZE = 10_000;
    public static final int EXPRESSION_SUMMARY_MAX = 128;
    public static final BigDecimal MAX_SPELL_AMOUNT = new BigDecimal("9999999999999.99");
    public static final String DEFAULT_LOCALE = "zh-CN";

    private ComputeDslLimits() {
    }

    public static String summarizeExpression(String expression) {
        if (expression == null) {
            return "";
        }
        String trimmed = expression.trim();
        if (trimmed.length() <= EXPRESSION_SUMMARY_MAX) {
            return trimmed;
        }
        return trimmed.substring(0, EXPRESSION_SUMMARY_MAX);
    }
}
