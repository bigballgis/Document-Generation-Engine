package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.VariablePiiCategory;
import com.bank.docgen.template.domain.VariableType;

public record VariableSchemaView(
        String id,
        String variableKey,
        VariableType variableType,
        boolean required,
        String defaultValue,
        String enumValues,
        String description,
        String computeExpression,
        VariablePiiCategory piiCategory
) {
    public VariableSchemaView(
            String id,
            String variableKey,
            VariableType variableType,
            boolean required,
            String defaultValue,
            String enumValues,
            String description,
            String computeExpression
    ) {
        this(
                id,
                variableKey,
                variableType,
                required,
                defaultValue,
                enumValues,
                description,
                computeExpression,
                VariablePiiCategory.NONE
        );
    }
}
