package com.bank.docgen.template.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.ErrorEnvelope;
import com.bank.docgen.sharedkernel.api.ErrorEnvelopeFactory;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.template.service.BindingVersionConflictException;
import com.bank.docgen.template.service.TemplateAccessDeniedException;
import com.bank.docgen.template.service.TemplateNotFoundException;
import com.bank.docgen.template.service.TemplateValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class TemplateExceptionAdviceTest {

    @Mock
    private MessageResolver messageResolver;

    private TemplateExceptionAdvice advice;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        advice = new TemplateExceptionAdvice(
                new ErrorEnvelopeFactory(new TraceIdProvider(), messageResolver)
        );
        request = new MockHttpServletRequest("GET", "/api/management/v1/templates/456");
    }

    @Test
    void templateNotFoundMapsToNotFoundEnvelope() {
        when(messageResolver.resolve("api.error.template.notFound"))
                .thenReturn("Template not found.");

        ResponseEntity<ErrorEnvelope> response = advice.handleTemplateNotFound(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.TEMPLATE_NOT_FOUND);
        assertThat(response.getBody().error().category()).isEqualTo(ApiErrorCategories.TEMPLATE);
        assertThat(response.getBody().error().messageKey()).isEqualTo("api.error.template.notFound");
    }

    @Test
    void templateAccessDeniedMapsToForbiddenEnvelope() {
        when(messageResolver.resolve("api.error.template.accessDenied"))
                .thenReturn("Access denied.");

        ResponseEntity<ErrorEnvelope> response = advice.handleTemplateAccessDenied(
                request,
                new TemplateAccessDeniedException()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.ACCESS_DENIED);
        assertThat(response.getBody().error().category()).isEqualTo(ApiErrorCategories.TEMPLATE);
    }

    @Test
    void templateValidationUsesExceptionMessageKey() {
        when(messageResolver.resolve("api.error.template.bindingInvalid"))
                .thenReturn("Template binding is invalid.");

        ResponseEntity<ErrorEnvelope> response = advice.handleTemplateValidation(
                request,
                new TemplateValidationException("api.error.template.bindingInvalid")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.TEMPLATE_VALIDATION_FAILED);
        assertThat(response.getBody().error().messageKey()).isEqualTo("api.error.template.bindingInvalid");
    }

    @Test
    void bindingVersionConflictMapsToConflictEnvelope() {
        // BDD-CE-U21-DAC-007 / U21-D8
        when(messageResolver.resolve("api.error.template.bindingVersionConflict"))
                .thenReturn("This binding was updated elsewhere. Reload before saving.");

        ResponseEntity<ErrorEnvelope> response = advice.handleBindingVersionConflict(
                request,
                new BindingVersionConflictException()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.BINDING_VERSION_CONFLICT);
        assertThat(response.getBody().error().category()).isEqualTo(ApiErrorCategories.CONFLICT);
        assertThat(response.getBody().error().messageKey())
                .isEqualTo("api.error.template.bindingVersionConflict");
        assertThat(response.getBody().error().retryable()).isTrue();
    }
}
