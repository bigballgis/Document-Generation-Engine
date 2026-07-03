package com.bank.docgen.rendering.service;

public class PreviewConcurrencyLimitException extends RuntimeException {

    public PreviewConcurrencyLimitException() {
        super("Preview concurrency limit exceeded");
    }
}
