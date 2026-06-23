package com.bank.docgen.sharedkernel.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FieldError(
        @JsonProperty("field") String field,
        String reason,
        String message
) {
}
