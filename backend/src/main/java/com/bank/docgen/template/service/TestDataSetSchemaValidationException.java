package com.bank.docgen.template.service;

import com.bank.docgen.sharedkernel.api.FieldError;
import java.util.List;

public class TestDataSetSchemaValidationException extends RuntimeException {

    public static final String MESSAGE_KEY = "api.error.template.testDataSetSchemaInvalid";

    private final List<FieldError> fieldErrors;

    public TestDataSetSchemaValidationException(List<FieldError> fieldErrors) {
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
