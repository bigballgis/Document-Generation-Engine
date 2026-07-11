package com.bank.docgen.collaboration.web;

import com.bank.docgen.collaboration.service.CollaborationWorkItemAccessDeniedException;
import com.bank.docgen.collaboration.service.CollaborationWorkItemNotFoundException;
import com.bank.docgen.collaboration.service.CollaborationWorkItemValidationException;
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
public class CollaborationExceptionAdvice {

    private final ErrorEnvelopeFactory errorEnvelopeFactory;

    public CollaborationExceptionAdvice(ErrorEnvelopeFactory errorEnvelopeFactory) {
        this.errorEnvelopeFactory = errorEnvelopeFactory;
    }

    @ExceptionHandler(CollaborationWorkItemAccessDeniedException.class)
    public ResponseEntity<ErrorEnvelope> handleCollaborationWorkItemAccessDenied(HttpServletRequest request) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.FORBIDDEN,
                ApiErrorCodes.ACCESS_DENIED,
                ApiErrorCategories.COLLABORATION,
                "api.error.collaboration.accessDenied"
        );
    }

    @ExceptionHandler(CollaborationWorkItemNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handleCollaborationWorkItemNotFound(HttpServletRequest request) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.NOT_FOUND,
                ApiErrorCodes.WORK_ITEM_NOT_FOUND,
                ApiErrorCategories.NOT_FOUND,
                "api.error.collaboration.workItemNotFound"
        );
    }

    @ExceptionHandler(CollaborationWorkItemValidationException.class)
    public ResponseEntity<ErrorEnvelope> handleCollaborationWorkItemValidation(
            HttpServletRequest request,
            CollaborationWorkItemValidationException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                ApiErrorCodes.REQUEST_BODY_INVALID,
                ApiErrorCategories.COLLABORATION,
                ex.getMessageKey()
        );
    }
}
