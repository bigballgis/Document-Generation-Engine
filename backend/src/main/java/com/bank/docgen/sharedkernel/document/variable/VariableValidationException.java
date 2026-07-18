package com.bank.docgen.sharedkernel.document.variable;

import com.bank.docgen.sharedkernel.api.FieldError;
import java.util.List;

/**
 * IBL-A1: aggregated VariableSchema validation failure for runtime generate / preview assembly.
 */
public class VariableValidationException extends RuntimeException {

    public static final String MESSAGE_KEY = "api.error.validation.variableValidationFailed";

    private final List<FieldError> fieldErrors;

    public VariableValidationException(List<FieldError> fieldErrors) {
        super(MESSAGE_KEY);
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public String messageKey() {
        return MESSAGE_KEY;
    }

    public List<FieldError> fieldErrors() {
        return fieldErrors;
    }
}
