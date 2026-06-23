package com.bank.docgen.sharedkernel.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.infrastructure.storage.ObjectStorageException;
import com.bank.docgen.rendering.DocxAssemblyException;
import com.bank.docgen.runtime.service.IdempotencyConflictException;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private MessageResolver messageResolver;

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(new TraceIdProvider(), messageResolver);
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
    void idempotencyConflictIncludesSafeSummary() {
        when(messageResolver.resolve("api.error.runtime.idempotencyConflict"))
                .thenReturn("The idempotency key was already used with a different request.");

        ResponseEntity<ErrorEnvelope> response = handler.handleIdempotencyConflict(
                request,
                new IdempotencyConflictException("idem-conflict-1")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().error().category()).isEqualTo(ApiErrorCategories.IDEMPOTENCY);
        assertThat(response.getBody().error().idempotencyConflict())
                .containsEntry("idempotencyKey", "idem-conflict-1")
                .containsEntry("conflictType", IdempotencyConflictException.REQUEST_SEMANTICS_MISMATCH);
    }

    @Test
    void objectStorageExceptionMapsToInternalErrorEnvelope() {
        when(messageResolver.resolve("api.error.storage.operationFailed"))
                .thenReturn("Object storage operation failed.");

        ResponseEntity<ErrorEnvelope> response = handler.handleObjectStorage(
                request,
                new ObjectStorageException("Failed to read object", new RuntimeException("io"))
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.INTERNAL_ERROR);
        assertThat(response.getBody().error().messageKey()).isEqualTo("api.error.storage.operationFailed");
    }

    @Test
    void docxAssemblyExceptionMapsToRenderingFailed() {
        when(messageResolver.resolve("api.error.rendering.generationFailed"))
                .thenReturn("Document generation failed.");

        ResponseEntity<ErrorEnvelope> response = handler.handleDocxAssembly(
                request,
                new DocxAssemblyException(new RuntimeException("docx"))
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.RENDERING_FAILED);
        assertThat(response.getBody().error().category()).isEqualTo(ApiErrorCategories.RENDERING);
    }

    @Test
    void illegalStateExceptionMapsToInternalError() {
        when(messageResolver.resolve("api.error.generation.internalError"))
                .thenReturn("An internal error occurred.");

        ResponseEntity<ErrorEnvelope> response = handler.handleIllegalState(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error().messageKey()).isEqualTo("api.error.generation.internalError");
    }
}
