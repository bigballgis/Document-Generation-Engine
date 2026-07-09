package com.bank.docgen.rendering;

/**
 * Rendering infrastructure failure with a stable API message key.
 */
public class RenderingOperationException extends RuntimeException {

    private final String messageKey;

    public RenderingOperationException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}
