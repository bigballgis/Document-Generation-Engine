package com.bank.docgen.master.web;

import com.bank.docgen.master.service.MasterAccessDeniedException;
import com.bank.docgen.master.service.MasterCurrentRevisionUnavailableException;
import com.bank.docgen.master.service.MasterNotFoundException;
import com.bank.docgen.master.service.MasterRevisionInUseException;
import com.bank.docgen.master.service.MasterValidationException;
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
public class MasterExceptionAdvice {

    private final ErrorEnvelopeFactory errorEnvelopeFactory;

    public MasterExceptionAdvice(ErrorEnvelopeFactory errorEnvelopeFactory) {
        this.errorEnvelopeFactory = errorEnvelopeFactory;
    }

    @ExceptionHandler(MasterNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handleMasterNotFound(HttpServletRequest request) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.NOT_FOUND,
                ApiErrorCodes.MASTER_NOT_FOUND,
                ApiErrorCategories.MASTER,
                "api.error.master.notFound"
        );
    }

    @ExceptionHandler(MasterAccessDeniedException.class)
    public ResponseEntity<ErrorEnvelope> handleMasterAccessDenied(
            HttpServletRequest request,
            MasterAccessDeniedException ignored
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.FORBIDDEN,
                ApiErrorCodes.ACCESS_DENIED,
                ApiErrorCategories.MASTER,
                "api.error.master.accessDenied"
        );
    }

    @ExceptionHandler(MasterValidationException.class)
    public ResponseEntity<ErrorEnvelope> handleMasterValidation(
            HttpServletRequest request,
            MasterValidationException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                ApiErrorCodes.MASTER_VALIDATION_FAILED,
                ApiErrorCategories.MASTER,
                ex.messageKey()
        );
    }

    @ExceptionHandler(MasterRevisionInUseException.class)
    public ResponseEntity<ErrorEnvelope> handleRevisionInUse(
            HttpServletRequest request,
            MasterRevisionInUseException ex
    ) {
        return errorEnvelopeFactory.masterRevisionInUse(request, ex);
    }

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
