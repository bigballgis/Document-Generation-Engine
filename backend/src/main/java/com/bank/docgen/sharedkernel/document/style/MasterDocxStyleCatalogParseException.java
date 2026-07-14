package com.bank.docgen.sharedkernel.document.style;

/**
 * Fail-closed signal when master {@code styles.xml} is missing or cannot be parsed.
 */
public class MasterDocxStyleCatalogParseException extends RuntimeException {

    public MasterDocxStyleCatalogParseException(String message) {
        super(message);
    }

    public MasterDocxStyleCatalogParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
