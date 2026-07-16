package com.bank.docgen.library.service;

public class AssetLibraryNotFoundException extends RuntimeException {

    private final String messageKey;

    public AssetLibraryNotFoundException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}
