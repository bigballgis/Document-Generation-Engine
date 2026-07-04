package com.bank.docgen.runtime.web;

import com.bank.docgen.runtime.domain.InvocationViewValidationException;
import com.bank.docgen.runtime.service.InvocationNotFoundException;
import com.bank.docgen.runtime.service.InvocationRecordExpiredException;
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
public class RuntimeInvocationExceptionAdvice {

    private final ErrorEnvelopeFactory errorEnvelopeFactory;

    public RuntimeInvocationExceptionAdvice(ErrorEnvelopeFactory errorEnvelopeFactory) {
        this.errorEnvelopeFactory = errorEnvelopeFactory;
    }

    @ExceptionHandler(InvocationNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handleInvocationNotFound(HttpServletRequest request) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.NOT_FOUND,
                ApiErrorCodes.INVOCATION_NOT_FOUND,
                ApiErrorCategories.RUNTIME,
                "api.error.runtime.invocationNotFound"
        );
    }

    @ExceptionHandler(InvocationRecordExpiredException.class)
    public ResponseEntity<ErrorEnvelope> handleInvocationRecordExpired(HttpServletRequest request) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.GONE,
                ApiErrorCodes.INVOCATION_RECORD_EXPIRED,
                ApiErrorCategories.RUNTIME,
                "api.error.runtime.invocationRecordExpired"
        );
    }

    @ExceptionHandler(InvocationViewValidationException.class)
    public ResponseEntity<ErrorEnvelope> handleInvocationViewInvalid(HttpServletRequest request) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.BAD_REQUEST,
                ApiErrorCodes.INVOCATION_VIEW_INVALID,
                ApiErrorCategories.VALIDATION,
                "api.error.runtime.invocationViewInvalid"
        );
    }
}
