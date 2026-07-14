package com.bank.docgen.template.web;

import com.bank.docgen.master.service.MasterCurrentRevisionUnavailableException;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.ErrorEnvelope;
import com.bank.docgen.sharedkernel.api.ErrorEnvelopeFactory;
import com.bank.docgen.template.service.TemplateAccessDeniedException;
import com.bank.docgen.template.service.TemplateGovernanceException;
import com.bank.docgen.template.service.TemplateNotFoundException;
import com.bank.docgen.template.service.TemplateValidationException;
import com.bank.docgen.template.service.TestDataSetImmutableException;
import com.bank.docgen.template.service.TestDataSetNotFoundException;
import com.bank.docgen.template.service.TestDataSetSchemaValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(0)
public class TemplateExceptionAdvice {

    private final ErrorEnvelopeFactory errorEnvelopeFactory;

    public TemplateExceptionAdvice(ErrorEnvelopeFactory errorEnvelopeFactory) {
        this.errorEnvelopeFactory = errorEnvelopeFactory;
    }

    @ExceptionHandler(TemplateNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handleTemplateNotFound(HttpServletRequest request) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.NOT_FOUND,
                ApiErrorCodes.TEMPLATE_NOT_FOUND,
                ApiErrorCategories.TEMPLATE,
                "api.error.template.notFound"
        );
    }

    @ExceptionHandler(TestDataSetNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handleTestDataSetNotFound(HttpServletRequest request) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.NOT_FOUND,
                ApiErrorCodes.TEST_DATA_SET_NOT_FOUND,
                ApiErrorCategories.TEMPLATE,
                "api.error.template.testDataSetNotFound"
        );
    }

    @ExceptionHandler(TestDataSetImmutableException.class)
    public ResponseEntity<ErrorEnvelope> handleTestDataSetImmutable(
            HttpServletRequest request,
            TestDataSetImmutableException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.CONFLICT,
                ApiErrorCodes.TEMPLATE_VALIDATION_FAILED,
                ApiErrorCategories.TEMPLATE,
                ex.messageKey()
        );
    }

    @ExceptionHandler(TemplateGovernanceException.class)
    public ResponseEntity<ErrorEnvelope> handleTemplateGovernance(
            HttpServletRequest request,
            TemplateGovernanceException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                ex.httpStatus(),
                ex.errorCode(),
                ApiErrorCategories.TEMPLATE,
                ex.messageKey()
        );
    }

    @ExceptionHandler(TemplateAccessDeniedException.class)
    public ResponseEntity<ErrorEnvelope> handleTemplateAccessDenied(
            HttpServletRequest request,
            TemplateAccessDeniedException ignored
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.FORBIDDEN,
                ApiErrorCodes.ACCESS_DENIED,
                ApiErrorCategories.TEMPLATE,
                "api.error.template.accessDenied"
        );
    }

    @ExceptionHandler(TestDataSetSchemaValidationException.class)
    public ResponseEntity<ErrorEnvelope> handleTestDataSetSchemaValidation(
            HttpServletRequest request,
            TestDataSetSchemaValidationException ex
    ) {
        return errorEnvelopeFactory.validationError(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.messageKey(),
                ex.fieldErrors()
        );
    }

    @ExceptionHandler(TemplateValidationException.class)
    public ResponseEntity<ErrorEnvelope> handleTemplateValidation(
            HttpServletRequest request,
            TemplateValidationException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                ApiErrorCodes.TEMPLATE_VALIDATION_FAILED,
                ApiErrorCategories.TEMPLATE,
                ex.messageKey()
        );
    }

    /**
     * CE-K01: publish fails closed when the current master revision cannot be pinned.
     * Mapped here so template lifecycle endpoints return a stable envelope (also handled
     * globally by {@code MasterExceptionAdvice}).
     */
    @ExceptionHandler(MasterCurrentRevisionUnavailableException.class)
    public ResponseEntity<ErrorEnvelope> handleCurrentRevisionUnavailable(
            HttpServletRequest request,
            MasterCurrentRevisionUnavailableException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                ApiErrorCodes.MASTER_VALIDATION_FAILED,
                ApiErrorCategories.MASTER,
                ex.messageKey()
        );
    }
}
