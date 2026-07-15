package com.bank.docgen.sharedkernel.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorDetail(
        String code,
        String category,
        String message,
        String messageKey,
        boolean retryable,
        List<FieldError> fieldErrors,
        java.util.Map<String, Object> idempotencyConflict,
        List<BatchErrorItemView> items,
        Object dependencyReport
) {
    public ErrorDetail {
        fieldErrors = DefensiveCopies.copyList(fieldErrors);
        idempotencyConflict = DefensiveCopies.copyMap(idempotencyConflict);
        items = DefensiveCopies.copyList(items);
    }

    public ErrorDetail(
            String code,
            String category,
            String message,
            String messageKey,
            boolean retryable,
            List<FieldError> fieldErrors
    ) {
        this(code, category, message, messageKey, retryable, fieldErrors, null, null, null);
    }

    public ErrorDetail(
            String code,
            String category,
            String message,
            String messageKey,
            boolean retryable,
            List<FieldError> fieldErrors,
            java.util.Map<String, Object> idempotencyConflict
    ) {
        this(code, category, message, messageKey, retryable, fieldErrors, idempotencyConflict, null, null);
    }

    public ErrorDetail(
            String code,
            String category,
            String message,
            String messageKey,
            boolean retryable,
            List<FieldError> fieldErrors,
            java.util.Map<String, Object> idempotencyConflict,
            List<BatchErrorItemView> items
    ) {
        this(code, category, message, messageKey, retryable, fieldErrors, idempotencyConflict, items, null);
    }
}
