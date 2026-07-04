package com.bank.docgen.audit.web;

import com.bank.docgen.audit.service.AuditAccessDeniedException;
import com.bank.docgen.audit.service.AuditValidationException;
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
public class AuditExceptionAdvice {

    private final ErrorEnvelopeFactory errorEnvelopeFactory;

    public AuditExceptionAdvice(ErrorEnvelopeFactory errorEnvelopeFactory) {
        this.errorEnvelopeFactory = errorEnvelopeFactory;
    }

    @ExceptionHandler(AuditAccessDeniedException.class)
    public ResponseEntity<ErrorEnvelope> handleAuditAccessDenied(HttpServletRequest request) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.FORBIDDEN,
                ApiErrorCodes.ACCESS_DENIED,
                ApiErrorCategories.AUDIT,
                "api.error.authorization.accessDenied"
        );
    }

    @ExceptionHandler(AuditValidationException.class)
    public ResponseEntity<ErrorEnvelope> handleAuditValidation(
            HttpServletRequest request,
            AuditValidationException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.errorCode(),
                ApiErrorCategories.AUDIT,
                ex.messageKey()
        );
    }
}
