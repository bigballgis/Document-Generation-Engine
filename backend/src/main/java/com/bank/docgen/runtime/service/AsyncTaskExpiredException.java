package com.bank.docgen.runtime.service;

public class AsyncTaskExpiredException extends RuntimeException {

    public String messageKey() {
        return "api.error.runtime.asyncTaskExpired";
    }
}
