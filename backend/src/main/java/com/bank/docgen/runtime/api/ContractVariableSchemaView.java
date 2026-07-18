package com.bank.docgen.runtime.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import com.bank.docgen.template.domain.VariablePiiCategory;
import com.bank.docgen.template.domain.VariableType;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Caller-facing per-field variable schema on {@code /contract} callable versions (IBL-A4).
 * Omits internal id, defaultValue plaintext, and computeExpression plaintext.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContractVariableSchemaView(
        String variableKey,
        VariableType variableType,
        boolean required,
        boolean computed,
        VariablePiiCategory piiCategory,
        List<String> enumValues,
        String description
) {
    public ContractVariableSchemaView {
        enumValues = enumValues == null ? null : DefensiveCopies.copyList(enumValues);
    }
}
