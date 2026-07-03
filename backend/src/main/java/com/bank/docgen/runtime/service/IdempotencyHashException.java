package com.bank.docgen.runtime.service;

public class IdempotencyHashException extends RuntimeException {

    private final String algorithm;

    public IdempotencyHashException(String algorithm, Throwable cause) {
        super("Unable to compute idempotency hash using " + algorithm, cause);
        this.algorithm = algorithm;
    }

    public String algorithm() {
        return algorithm;
    }

    public String messageKey() {
        return "api.error.idempotency.hashFailed";
    }
}
