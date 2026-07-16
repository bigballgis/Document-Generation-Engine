package com.bank.docgen.library.web;

import com.bank.docgen.library.service.AssetLibraryAccessDeniedException;
import com.bank.docgen.library.service.AssetLibraryConflictException;
import com.bank.docgen.library.service.AssetLibraryNotFoundException;
import com.bank.docgen.library.service.AssetLibraryValidationException;
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
public class AssetLibraryExceptionAdvice {

    private final ErrorEnvelopeFactory errorEnvelopeFactory;

    public AssetLibraryExceptionAdvice(ErrorEnvelopeFactory errorEnvelopeFactory) {
        this.errorEnvelopeFactory = errorEnvelopeFactory;
    }

    @ExceptionHandler(AssetLibraryAccessDeniedException.class)
    public ResponseEntity<ErrorEnvelope> handleAccessDenied(HttpServletRequest request) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.FORBIDDEN,
                ApiErrorCodes.ACCESS_DENIED,
                ApiErrorCategories.AUTHORIZATION,
                "api.error.assetLibrary.accessDenied"
        );
    }

    @ExceptionHandler(AssetLibraryNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handleNotFound(
            HttpServletRequest request,
            AssetLibraryNotFoundException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.NOT_FOUND,
                ApiErrorCodes.ASSET_LIBRARY_ASSET_NOT_FOUND,
                ApiErrorCategories.NOT_FOUND,
                ex.messageKey()
        );
    }

    @ExceptionHandler(AssetLibraryConflictException.class)
    public ResponseEntity<ErrorEnvelope> handleConflict(
            HttpServletRequest request,
            AssetLibraryConflictException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.CONFLICT,
                ex.errorCode(),
                ApiErrorCategories.CONFLICT,
                ex.messageKey()
        );
    }

    @ExceptionHandler(AssetLibraryValidationException.class)
    public ResponseEntity<ErrorEnvelope> handleValidation(
            HttpServletRequest request,
            AssetLibraryValidationException ex
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
