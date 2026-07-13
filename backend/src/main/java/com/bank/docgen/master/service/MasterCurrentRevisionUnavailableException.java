package com.bank.docgen.master.service;

/**
 * CE-K01: raised by the publish flow when a template's master has no resolvable
 * current revision line (deleted, missing, or storage object unavailable). The
 * publish transaction is fail-closed and rolled back.
 */
public class MasterCurrentRevisionUnavailableException extends RuntimeException {

    public MasterCurrentRevisionUnavailableException() {
        super("api.error.master.currentRevisionUnavailable");
    }

    public String messageKey() {
        return "api.error.master.currentRevisionUnavailable";
    }
}
