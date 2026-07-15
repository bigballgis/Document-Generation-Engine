package com.bank.docgen.apimgmt.web;

import com.bank.docgen.apimgmt.service.ApiManagementAccessDeniedException;
import com.bank.docgen.apimgmt.service.ApiManagementNotFoundException;
import com.bank.docgen.apimgmt.service.InvocationRegenerationException;
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
public class ApiManagementExceptionAdvice {

    private final ErrorEnvelopeFactory errorEnvelopeFactory;

    public ApiManagementExceptionAdvice(ErrorEnvelopeFactory errorEnvelopeFactory) {
        this.errorEnvelopeFactory = errorEnvelopeFactory;
    }

    @ExceptionHandler(ApiManagementNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handleApiPolicyNotFound(HttpServletRequest request) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.NOT_FOUND,
                ApiErrorCodes.API_POLICY_NOT_FOUND,
                ApiErrorCategories.APIMGMT,
                "api.error.apimgmt.policyNotFound"
        );
    }

    @ExceptionHandler(ApiManagementAccessDeniedException.class)
    public ResponseEntity<ErrorEnvelope> handleApiManagementAccessDenied(HttpServletRequest request) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.FORBIDDEN,
                ApiErrorCodes.ACCESS_DENIED,
                ApiErrorCategories.APIMGMT,
                "api.error.apimgmt.accessDenied"
        );
    }

    @ExceptionHandler(InvocationRegenerationException.class)
    public ResponseEntity<ErrorEnvelope> handleInvocationRegeneration(
            InvocationRegenerationException ex,
            HttpServletRequest request
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                ex.httpStatus(),
                ex.errorCode(),
                ex.category(),
                ex.messageKey()
        );
    }
}
