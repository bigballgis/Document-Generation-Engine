package com.bank.docgen.sharedkernel.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private final ErrorEnvelopeFactory errorEnvelopeFactory;
    private final ValidationErrorFieldMapper validationErrorFieldMapper;

    public GlobalExceptionHandler(
            ErrorEnvelopeFactory errorEnvelopeFactory,
            ValidationErrorFieldMapper validationErrorFieldMapper
    ) {
        this.errorEnvelopeFactory = errorEnvelopeFactory;
        this.validationErrorFieldMapper = validationErrorFieldMapper;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorEnvelope> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(validationErrorFieldMapper::toFieldError)
                .toList();
        return errorEnvelopeFactory.validationError(
                request,
                "api.error.validation.requestBodyInvalid",
                fieldErrors
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorEnvelope> handleIllegalState(HttpServletRequest request) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorCodes.INTERNAL_ERROR,
                ApiErrorCategories.GENERATION,
                "api.error.generation.internalError"
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorEnvelope> handleUnexpected(HttpServletRequest request, Exception ignored) {
        return errorEnvelopeFactory.unexpectedError(request);
    }
}
