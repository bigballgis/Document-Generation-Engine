package com.bank.docgen.rendering.goldencorpus;

/**
 * Fail-closed golden-corpus structure or assertion error.
 */
public class GoldenCorpusException extends RuntimeException {

    public GoldenCorpusException(String message) {
        super(message);
    }

    public GoldenCorpusException(String message, Throwable cause) {
        super(message, cause);
    }
}
