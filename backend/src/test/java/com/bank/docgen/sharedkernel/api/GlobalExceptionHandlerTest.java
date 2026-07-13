package com.bank.docgen.sharedkernel.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private MessageResolver messageResolver;

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        ErrorEnvelopeFactory errorEnvelopeFactory = new ErrorEnvelopeFactory(
                new TraceIdProvider(),
                messageResolver
        );
        handler = new GlobalExceptionHandler(
                errorEnvelopeFactory,
                new ValidationErrorFieldMapper(messageResolver),
                messageResolver
        );
        request = new MockHttpServletRequest("POST", "/api/management/v1/groups");
    }

    @Test
    void validationErrorsUseMessageKeysAndFieldContract() {
        when(messageResolver.resolveOrDefault("api.error.validation.fieldRequired", "must not be blank"))
                .thenReturn("This field is required.");
        when(messageResolver.resolve("api.error.validation.requestBodyInvalid"))
                .thenReturn("The request body is invalid.");

        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "request");
        bindingResult.addError(new org.springframework.validation.FieldError(
                "request",
                "groupCode",
                null,
                false,
                new String[]{NotBlank.class.getSimpleName()},
                null,
                "must not be blank"
        ));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorEnvelope> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().messageKey()).isEqualTo("api.error.validation.requestBodyInvalid");
        assertThat(response.getBody().error().fieldErrors()).hasSize(1);
        assertThat(response.getBody().error().fieldErrors().getFirst().field()).isEqualTo("groupCode");
        assertThat(response.getBody().error().fieldErrors().getFirst().reason()).isEqualTo("REQUIRED");
        assertThat(response.getBody().error().fieldErrors().getFirst().message())
                .isEqualTo("This field is required.");
    }

    @Test
    void illegalStateExceptionMapsToInternalError() {
        when(messageResolver.resolve("api.error.generation.internalError"))
                .thenReturn("An internal error occurred.");

        ResponseEntity<ErrorEnvelope> response = handler.handleIllegalState(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error().messageKey()).isEqualTo("api.error.generation.internalError");
    }

    @Test
    void unexpectedExceptionMapsToRetryableInternalError() {
        when(messageResolver.resolveOrDefault("api.error.generation.internalError", "An internal error occurred."))
                .thenReturn("An internal error occurred.");

        ResponseEntity<ErrorEnvelope> response = handler.handleUnexpected(request, new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.INTERNAL_ERROR);
        assertThat(response.getBody().error().retryable()).isTrue();
    }

    @Test
    void maxUploadSizeExceededMapsToReadableDocxTooLargeEnvelope() {
        // A4 (Spring multipart): oversize must return JSON envelope + docxTooLarge, not raw HTML/500
        when(messageResolver.resolve("api.error.master.docxTooLarge"))
                .thenReturn("The uploaded DOCX exceeds the maximum allowed size.");

        ResponseEntity<ErrorEnvelope> response = handler.handleMaxUploadSizeExceeded(
                request,
                new MaxUploadSizeExceededException(50L * 1024L * 1024L)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.MASTER_VALIDATION_FAILED);
        assertThat(response.getBody().error().category()).isEqualTo(ApiErrorCategories.MASTER);
        assertThat(response.getBody().error().messageKey()).isEqualTo("api.error.master.docxTooLarge");
        assertThat(response.getBody().error().message())
                .isEqualTo("The uploaded DOCX exceeds the maximum allowed size.");
        assertThat(response.getBody().error().retryable()).isFalse();
    }

    @Test
    void unrecognizedPropertyMapsToRequestBodyInvalidWithUnknownField() throws Exception {
        when(messageResolver.resolve("api.error.validation.requestBodyInvalid"))
                .thenReturn("The request body is invalid.");
        when(messageResolver.resolveOrDefault("api.error.validation.fieldUnknown", "Unknown field."))
                .thenReturn("Unknown field.");

        ObjectMapper mapper = new ObjectMapper();
        UnrecognizedPropertyException unrecognized = UnrecognizedPropertyException.from(
                mapper.createParser("{}"),
                Object.class,
                "foo",
                java.util.Set.of("output", "variables")
        );
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error",
                unrecognized,
                null
        );

        ResponseEntity<ErrorEnvelope> response = handler.handleMessageNotReadable(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.REQUEST_BODY_INVALID);
        assertThat(response.getBody().error().category()).isEqualTo(ApiErrorCategories.VALIDATION);
        assertThat(response.getBody().error().messageKey()).isEqualTo("api.error.validation.requestBodyInvalid");
        assertThat(response.getBody().error().retryable()).isFalse();
        assertThat(response.getBody().error().fieldErrors()).hasSize(1);
        assertThat(response.getBody().error().fieldErrors().getFirst().field()).isEqualTo("foo");
        assertThat(response.getBody().error().fieldErrors().getFirst().reason()).isEqualTo("UNKNOWN_FIELD");
    }
}
