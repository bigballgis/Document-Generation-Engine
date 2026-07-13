package com.bank.docgen.rendering;

import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;

/**
 * Raised when DOCX assembly from structured content fails.
 *
 * <p>LR-A4: also raised when an unsupported structured-content node type (e.g.
 * {@code qrBarcodeRef}, {@code attachmentListRef}) is encountered — fail-closed instead of
 * silently dropping content (CD-PIT-07).
 */
public class DocxAssemblyException extends RuntimeException {

    private final String messageKey;
    private final String errorCode;
    private final String category;

    public DocxAssemblyException(Throwable cause) {
        super(cause);
        this.messageKey = "api.error.rendering.generationFailed";
        this.errorCode = ApiErrorCodes.RENDERING_FAILED;
        this.category = ApiErrorCategories.RENDERING;
    }

    public DocxAssemblyException(String messageKey, String message) {
        super(message);
        this.messageKey = messageKey;
        this.errorCode = ApiErrorCodes.RENDERING_FAILED;
        this.category = ApiErrorCategories.RENDERING;
    }

    public DocxAssemblyException(String errorCode, String category, String messageKey, String message) {
        super(message);
        this.errorCode = errorCode;
        this.category = category;
        this.messageKey = messageKey;
    }

    public DocxAssemblyException(
            String errorCode,
            String category,
            String messageKey,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.errorCode = errorCode;
        this.category = category;
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }

    public String errorCode() {
        return errorCode;
    }

    public String category() {
        return category;
    }
}
