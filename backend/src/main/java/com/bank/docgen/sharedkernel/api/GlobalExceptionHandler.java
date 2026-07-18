package com.bank.docgen.sharedkernel.api;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.infrastructure.resilience.GenerationServiceUnavailableException;
import com.bank.docgen.infrastructure.resilience.GenerationTimeoutException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private final ErrorEnvelopeFactory errorEnvelopeFactory;
    private final ValidationErrorFieldMapper validationErrorFieldMapper;
    private final MessageResolver messageResolver;

    public GlobalExceptionHandler(
            ErrorEnvelopeFactory errorEnvelopeFactory,
            ValidationErrorFieldMapper validationErrorFieldMapper,
            MessageResolver messageResolver
    ) {
        this.errorEnvelopeFactory = errorEnvelopeFactory;
        this.validationErrorFieldMapper = validationErrorFieldMapper;
        this.messageResolver = messageResolver;
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorEnvelope> handleMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof UnrecognizedPropertyException unrecognized) {
            String field = fieldPath(unrecognized);
            return errorEnvelopeFactory.validationError(
                    request,
                    "api.error.validation.requestBodyInvalid",
                    List.of(new FieldError(
                            field,
                            "UNKNOWN_FIELD",
                            messageResolver.resolveOrDefault(
                                    "api.error.validation.fieldUnknown",
                                    "Unknown field."
                            )
                    ))
            );
        }
        if (cause instanceof InvalidFormatException || cause instanceof MismatchedInputException) {
            String field = fieldPathFromMapping((JsonMappingException) cause);
            return errorEnvelopeFactory.validationError(
                    request,
                    "api.error.validation.requestBodyInvalid",
                    List.of(new FieldError(
                            field,
                            "INVALID_TYPE",
                            messageResolver.resolveOrDefault(
                                    "api.error.validation.fieldInvalid",
                                    "This field is invalid."
                            )
                    ))
            );
        }
        return errorEnvelopeFactory.validationError(
                request,
                "api.error.validation.requestBodyInvalid",
                List.of()
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

    /**
     * CE-G01: self-approval block + exception-intervention failures raised by the
     * template / master / content-module decision services. The exception carries the
     * unified-envelope code / category / messageKey / HTTP status directly so this
     * single handler serves all three modules without per-module advice ambiguity.
     */
    @ExceptionHandler(LifecycleAuthorizationException.class)
    public ResponseEntity<ErrorEnvelope> handleLifecycleAuthorization(
            HttpServletRequest request,
            LifecycleAuthorizationException ex
    ) {
        return errorEnvelopeFactory.domainError(
                request,
                ex.httpStatus(),
                ex.errorCode(),
                ex.category(),
                ex.messageKey()
        );
    }

    /** PRR-D01a / DEF-LRP-D6-001: CB open / bulkhead full → 503, not TEMPLATE_VALIDATION_FAILED. */
    @ExceptionHandler(GenerationServiceUnavailableException.class)
    public ResponseEntity<ErrorEnvelope> handleGenerationServiceUnavailable(
            HttpServletRequest request,
            GenerationServiceUnavailableException ex
    ) {
        return errorEnvelopeFactory.generationServiceUnavailable(request, ex);
    }

    /** PRR-D01a: resilience / mapped timeout → 504 GENERATION_TIMEOUT. */
    @ExceptionHandler(GenerationTimeoutException.class)
    public ResponseEntity<ErrorEnvelope> handleGenerationTimeout(
            HttpServletRequest request,
            GenerationTimeoutException ex
    ) {
        return errorEnvelopeFactory.generationTimeout(request, ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorEnvelope> handleUnexpected(HttpServletRequest request, Exception ignored) {
        return errorEnvelopeFactory.unexpectedError(request);
    }

    private static String fieldPath(UnrecognizedPropertyException ex) {
        String fromPath = pathPrefix(ex);
        String property = ex.getPropertyName();
        if (fromPath == null || fromPath.isBlank()) {
            return property == null ? "body" : property;
        }
        if (property == null || property.isBlank()) {
            return fromPath;
        }
        if (fromPath.endsWith("." + property) || fromPath.equals(property)) {
            return fromPath;
        }
        return fromPath + "." + property;
    }

    private static String fieldPathFromMapping(JsonMappingException ex) {
        String fromPath = pathPrefix(ex);
        return fromPath == null || fromPath.isBlank() ? "body" : fromPath;
    }

    private static String pathPrefix(JsonMappingException ex) {
        return ex.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("."));
    }
}
