package com.bank.docgen.infrastructure.storage;

public class ObjectStorageException extends RuntimeException {

    public ObjectStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public String messageKey() {
        return "api.error.storage.operationFailed";
    }
}
