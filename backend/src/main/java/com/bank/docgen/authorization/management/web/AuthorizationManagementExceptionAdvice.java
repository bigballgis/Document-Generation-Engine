package com.bank.docgen.authorization.management.web;

import com.bank.docgen.authorization.management.service.InvalidCredentialsException;
import com.bank.docgen.authorization.management.service.ManagementConflictException;
import com.bank.docgen.authorization.management.service.ManagementForbiddenException;
import com.bank.docgen.authorization.management.service.ManagementNotFoundException;
import com.bank.docgen.authorization.management.service.SessionExpiredException;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.ErrorEnvelope;
import com.bank.docgen.sharedkernel.api.ErrorEnvelopeFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(0)
public class AuthorizationManagementExceptionAdvice {

    private final ErrorEnvelopeFactory errorEnvelopeFactory;

    public AuthorizationManagementExceptionAdvice(ErrorEnvelopeFactory errorEnvelopeFactory) {
        this.errorEnvelopeFactory = errorEnvelopeFactory;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorEnvelope> handleInvalidCredentials(HttpServletRequest request) {
        return errorEnvelopeFactory.authenticationError(
                request,
                ApiErrorCodes.INVALID_CREDENTIALS,
                "api.error.authentication.invalidCredentials"
        );
    }

    @ExceptionHandler(SessionExpiredException.class)
    public ResponseEntity<ErrorEnvelope> handleSessionExpired(HttpServletRequest request) {
        return errorEnvelopeFactory.authenticationError(
                request,
                ApiErrorCodes.SESSION_EXPIRED,
                "api.error.authentication.sessionExpired"
        );
    }

    @ExceptionHandler(ManagementNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handleManagementNotFound(
            HttpServletRequest request,
            ManagementNotFoundException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.NOT_FOUND,
                ex.errorCode(),
                ApiErrorCategories.NOT_FOUND,
                ex.messageKey()
        );
    }

    @ExceptionHandler(ManagementConflictException.class)
    public ResponseEntity<ErrorEnvelope> handleManagementConflict(
            HttpServletRequest request,
            ManagementConflictException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.CONFLICT,
                ex.errorCode(),
                ApiErrorCategories.CONFLICT,
                ex.messageKey()
        );
    }

    @ExceptionHandler(ManagementForbiddenException.class)
    public ResponseEntity<ErrorEnvelope> handleManagementForbidden(
            HttpServletRequest request,
            ManagementForbiddenException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.FORBIDDEN,
                ex.errorCode(),
                ApiErrorCategories.AUTHORIZATION,
                ex.messageKey()
        );
    }
}
