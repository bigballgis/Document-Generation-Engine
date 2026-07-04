package com.bank.docgen.rendering;

/**
 * Raised when the isolated PDF conversion pool is saturated (SOR-P03).
 * Mapped to HTTP 503 with {@code retryable=true} so callers can back off instead of
 * blocking servlet threads behind a full executor queue.
 */
public class PdfConversionCapacityExceededException extends RuntimeException {

    public String messageKey() {
        return "api.error.generation.pdfConversionCapacityExceeded";
    }
}
