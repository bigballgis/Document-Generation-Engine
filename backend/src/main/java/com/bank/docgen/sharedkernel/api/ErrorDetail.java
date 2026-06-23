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
        List<FieldError> fieldErrors
) {
}
