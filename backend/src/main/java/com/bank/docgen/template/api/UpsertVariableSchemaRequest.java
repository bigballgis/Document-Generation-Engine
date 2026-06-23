package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.VariableType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpsertVariableSchemaRequest(
        @NotBlank String variableKey,
        @NotNull VariableType variableType,
        boolean required,
        String defaultValue,
        String enumValues,
        String description
) {
}
