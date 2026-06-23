package com.bank.docgen.rendering;

public class EncryptionFailedException extends RuntimeException {

    public String messageKey() {
        return "api.error.encryption.encryptionFailed";
    }
}
