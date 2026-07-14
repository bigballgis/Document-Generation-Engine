package com.bank.docgen.sharedkernel.api;

import org.springframework.http.HttpStatus;

/**
 * CE-G01: raised by template / master / content-module decision services when a
 * self-approval is detected (decision actor equals the most recent submitter) and
 * no valid GROUP_ADMIN / GLOBAL_ADMIN exception intervention applies, or when an
 * exception intervention request is malformed / unauthorized.
 *
 * <p>Carries the unified-envelope error code, category, messageKey and HTTP status
 * so a single advice handler can materialize the response without per-module wiring.
 */
public class LifecycleAuthorizationException extends RuntimeException {

    private final String errorCode;
    private final String category;
    private final String messageKey;
    private final HttpStatus httpStatus;

    public LifecycleAuthorizationException(
            String errorCode,
            String category,
            String messageKey,
            HttpStatus httpStatus
    ) {
        super(messageKey);
        this.errorCode = errorCode;
        this.category = category;
        this.messageKey = messageKey;
        this.httpStatus = httpStatus;
    }

    public String errorCode() {
        return errorCode;
    }

    public String category() {
        return category;
    }

    public String messageKey() {
        return messageKey;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
