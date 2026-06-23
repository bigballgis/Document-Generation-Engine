package com.bank.docgen.sharedkernel.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BatchErrorItemView(
        String itemId,
        String status,
        ErrorDetail error
) {
}
