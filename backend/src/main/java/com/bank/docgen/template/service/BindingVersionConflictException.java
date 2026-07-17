package com.bank.docgen.template.service;

/**
 * CE-U21 — optimistic-lock mismatch on anchor binding update.
 */
public class BindingVersionConflictException extends RuntimeException {

    public static final String MESSAGE_KEY = "api.error.template.bindingVersionConflict";

    public BindingVersionConflictException() {
        super(MESSAGE_KEY);
    }

    public String messageKey() {
        return MESSAGE_KEY;
    }
}
