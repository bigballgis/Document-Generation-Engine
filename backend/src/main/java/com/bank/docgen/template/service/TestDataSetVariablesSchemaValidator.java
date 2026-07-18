package com.bank.docgen.template.service;

import com.bank.docgen.sharedkernel.api.FieldError;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import java.util.List;
import java.util.Map;

/**
 * CE-U03 test-data-set facade over {@link VariableSchemaPayloadValidator}.
 *
 * <p>Top-level API code for test-data-set save remains {@code REQUEST_BODY_INVALID}
 * (IBL-A1-C13); runtime/preview use {@link VariableValidationException}.
 */
final class TestDataSetVariablesSchemaValidator {

    private TestDataSetVariablesSchemaValidator() {
    }

    static boolean isCompute(VariableSchemaEntity variable) {
        return VariableSchemaPayloadValidator.isCompute(variable);
    }

    static Map<String, Object> stripComputeKeys(
            List<VariableSchemaEntity> schema,
            Map<String, Object> variables
    ) {
        return VariableSchemaPayloadValidator.stripComputeKeys(schema, variables);
    }

    static List<FieldError> validate(List<VariableSchemaEntity> schema, Map<String, Object> variables) {
        return VariableSchemaPayloadValidator.validate(schema, variables);
    }

    static void validateOrThrow(List<VariableSchemaEntity> schema, Map<String, Object> variables) {
        List<FieldError> errors = validate(schema, variables);
        if (!errors.isEmpty()) {
            throw new TestDataSetSchemaValidationException(errors);
        }
    }
}
