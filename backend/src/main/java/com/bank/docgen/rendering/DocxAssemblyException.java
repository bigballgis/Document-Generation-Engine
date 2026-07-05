package com.bank.docgen.rendering;

/**
 * Raised when DOCX assembly from structured content fails.
 *
 * <p>LR-A4: also raised when an unsupported structured-content node type (e.g.
 * {@code qrBarcodeRef}, {@code attachmentListRef}) is encountered — fail-closed instead of
 * silently dropping content (CD-PIT-07).
 */
public class DocxAssemblyException extends RuntimeException {

    private final String messageKey;

    public DocxAssemblyException(Throwable cause) {
        super(cause);
        this.messageKey = "api.error.rendering.generationFailed";
    }

    public DocxAssemblyException(String messageKey, String message) {
        super(message);
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}
