package com.bank.docgen.legalhold.web;

import com.bank.docgen.legalhold.service.LegalHoldAccessDeniedException;
import com.bank.docgen.legalhold.service.LegalHoldAlreadyReleasedException;
import com.bank.docgen.legalhold.service.LegalHoldNotFoundException;
import com.bank.docgen.legalhold.service.LegalHoldValidationException;
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
public class LegalHoldExceptionAdvice {

    private final ErrorEnvelopeFactory errorEnvelopeFactory;

    public LegalHoldExceptionAdvice(ErrorEnvelopeFactory errorEnvelopeFactory) {
        this.errorEnvelopeFactory = errorEnvelopeFactory;
    }

    @ExceptionHandler(LegalHoldAccessDeniedException.class)
    public ResponseEntity<ErrorEnvelope> handleAccessDenied(HttpServletRequest request) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.FORBIDDEN,
                ApiErrorCodes.ACCESS_DENIED,
                ApiErrorCategories.AUTHORIZATION,
                "api.error.authorization.accessDenied"
        );
    }

    @ExceptionHandler(LegalHoldNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handleNotFound(
            HttpServletRequest request,
            LegalHoldNotFoundException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.NOT_FOUND,
                ex.errorCode(),
                ApiErrorCategories.NOT_FOUND,
                ex.messageKey()
        );
    }

    @ExceptionHandler(LegalHoldAlreadyReleasedException.class)
    public ResponseEntity<ErrorEnvelope> handleAlreadyReleased(
            HttpServletRequest request,
            LegalHoldAlreadyReleasedException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.CONFLICT,
                ex.errorCode(),
                ApiErrorCategories.CONFLICT,
                ex.messageKey()
        );
    }

    @ExceptionHandler(LegalHoldValidationException.class)
    public ResponseEntity<ErrorEnvelope> handleValidation(
            HttpServletRequest request,
            LegalHoldValidationException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.errorCode(),
                ApiErrorCategories.VALIDATION,
                ex.messageKey()
        );
    }
}
