package com.bank.docgen.documentbrand.web;

import com.bank.docgen.documentbrand.service.DocumentBrandCatalogException;
import com.bank.docgen.documentbrand.service.DocumentBrandResolveException;
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
public class DocumentBrandExceptionAdvice {

    private final ErrorEnvelopeFactory errorEnvelopeFactory;

    public DocumentBrandExceptionAdvice(ErrorEnvelopeFactory errorEnvelopeFactory) {
        this.errorEnvelopeFactory = errorEnvelopeFactory;
    }

    @ExceptionHandler(DocumentBrandCatalogException.class)
    public ResponseEntity<ErrorEnvelope> handleCatalog(
            HttpServletRequest request,
            DocumentBrandCatalogException ex
    ) {
        HttpStatus status = ApiErrorCodes.ACCESS_DENIED.equals(ex.errorCode())
                ? HttpStatus.FORBIDDEN
                : ApiErrorCodes.GROUP_NOT_FOUND.equals(ex.errorCode())
                        || ApiErrorCodes.DOCUMENT_BRAND_UNKNOWN.equals(ex.errorCode())
                        || ApiErrorCodes.LEGAL_ENTITY_UNKNOWN.equals(ex.errorCode())
                        || ApiErrorCodes.DOCUMENT_BRAND_SURFACE_RETIRED.equals(ex.errorCode())
                        || ApiErrorCodes.LEGAL_ENTITY_SURFACE_RETIRED.equals(ex.errorCode())
                ? HttpStatus.NOT_FOUND
                : HttpStatus.UNPROCESSABLE_ENTITY;
        String category = status == HttpStatus.FORBIDDEN
                ? ApiErrorCategories.AUTHORIZATION
                : status == HttpStatus.NOT_FOUND
                        ? ApiErrorCategories.NOT_FOUND
                        : ApiErrorCategories.VALIDATION;
        return errorEnvelopeFactory.domainError(
                request,
                status,
                ex.errorCode(),
                category,
                ex.messageKey()
        );
    }

    @ExceptionHandler(DocumentBrandResolveException.class)
    public ResponseEntity<ErrorEnvelope> handleResolve(
            HttpServletRequest request,
            DocumentBrandResolveException ex
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
