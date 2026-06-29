package com.bank.docgen.authoring.structured;

public class StructuredContentSchemaException extends RuntimeException {

    private final String messageKey;

    public StructuredContentSchemaException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public StructuredContentSchemaException(String messageKey, Throwable cause) {
        super(messageKey, cause);
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}
