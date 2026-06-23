package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.VariableType;

public record VariableSchemaView(
        String id,
        String variableKey,
        VariableType variableType,
        boolean required,
        String defaultValue,
        String enumValues,
        String description
) {
}
