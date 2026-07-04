package com.bank.docgen.contentmodule.web;

import com.bank.docgen.contentmodule.service.ContentModuleAccessDeniedException;
import com.bank.docgen.contentmodule.service.ContentModuleGovernanceException;
import com.bank.docgen.contentmodule.service.ContentModuleNotFoundException;
import com.bank.docgen.contentmodule.service.ContentModuleValidationException;
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
public class ContentModuleExceptionAdvice {

    private final ErrorEnvelopeFactory errorEnvelopeFactory;

    public ContentModuleExceptionAdvice(ErrorEnvelopeFactory errorEnvelopeFactory) {
        this.errorEnvelopeFactory = errorEnvelopeFactory;
    }

    @ExceptionHandler(ContentModuleNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handleContentModuleNotFound(HttpServletRequest request) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.NOT_FOUND,
                ApiErrorCodes.CONTENT_MODULE_NOT_FOUND,
                ApiErrorCategories.CONTENT_MODULE,
                "api.error.contentModule.notFound"
        );
    }

    @ExceptionHandler(ContentModuleAccessDeniedException.class)
    public ResponseEntity<ErrorEnvelope> handleContentModuleAccessDenied(HttpServletRequest request) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.FORBIDDEN,
                ApiErrorCodes.ACCESS_DENIED,
                ApiErrorCategories.CONTENT_MODULE,
                "api.error.contentModule.accessDenied"
        );
    }

    @ExceptionHandler(ContentModuleValidationException.class)
    public ResponseEntity<ErrorEnvelope> handleContentModuleValidation(
            HttpServletRequest request,
            ContentModuleValidationException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                ApiErrorCodes.CONTENT_MODULE_VALIDATION_FAILED,
                ApiErrorCategories.CONTENT_MODULE,
                ex.messageKey()
        );
    }

    @ExceptionHandler(ContentModuleGovernanceException.class)
    public ResponseEntity<ErrorEnvelope> handleContentModuleGovernance(
            HttpServletRequest request,
            ContentModuleGovernanceException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                ex.httpStatus(),
                ex.errorCode(),
                ApiErrorCategories.CONTENT_MODULE,
                ex.messageKey()
        );
    }
}
