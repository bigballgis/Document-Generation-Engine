package com.bank.docgen.authorization.management.session;

/**
 * The revocation list backend (Redis) cannot be reached. Token validation treats this as
 * fail-closed (401 SESSION_VALIDATION_UNAVAILABLE); revocation writes surface it as 503.
 */
public class SessionRevocationUnavailableException extends RuntimeException {

    public SessionRevocationUnavailableException(Throwable cause) {
        super("Session revocation store is unavailable", cause);
    }
}
