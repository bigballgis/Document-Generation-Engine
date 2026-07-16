package com.bank.docgen.rendering;

/**
 * CE-O01: PDF/A archival profile cannot be combined with request encryption on PDF output.
 */
public class PdfArchivalEncryptionMutexException extends RuntimeException {

    private final String messageKey;

    public PdfArchivalEncryptionMutexException() {
        this("api.error.generation.pdfArchivalEncryptionMutex");
    }

    public PdfArchivalEncryptionMutexException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}
