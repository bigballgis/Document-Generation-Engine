package com.bank.docgen.runtime.api;

public record ErrorCodeSummaryView(
        String category,
        String code,
        String messageKey,
        boolean retryable,
        String message
) {
}
