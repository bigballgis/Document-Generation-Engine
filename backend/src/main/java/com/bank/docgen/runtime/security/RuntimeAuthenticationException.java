package com.bank.docgen.runtime.security;

public class RuntimeAuthenticationException extends RuntimeException {

    private final String code;
    private final String messageKey;

    public RuntimeAuthenticationException(String code, String messageKey) {
        super(messageKey);
        this.code = code;
        this.messageKey = messageKey;
    }

    public String code() {
        return code;
    }

    public String messageKey() {
        return messageKey;
    }
}
