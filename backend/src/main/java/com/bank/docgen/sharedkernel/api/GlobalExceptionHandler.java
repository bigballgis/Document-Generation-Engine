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
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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

    /**
     * LR-A3: Spring multipart oversize must return a translated JSON envelope
     * ({@code api.error.master.docxTooLarge}), never a raw Tomcat/HTML error page.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorEnvelope> handleMaxUploadSizeExceeded(
            HttpServletRequest request,
            MaxUploadSizeExceededException ignored
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                HttpStatus.PAYLOAD_TOO_LARGE,
                ApiErrorCodes.MASTER_VALIDATION_FAILED,
                ApiErrorCategories.MASTER,
                "api.error.master.docxTooLarge"
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
