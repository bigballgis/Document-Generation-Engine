package com.bank.docgen.runtime.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.ErrorEnvelope;
import com.bank.docgen.sharedkernel.api.ErrorEnvelopeFactory;
import com.bank.docgen.sharedkernel.api.FieldError;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.document.variable.VariableValidationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * BDD-IBL-A1 envelope: 422 VARIABLE_VALIDATION_FAILED + fieldErrors.
 */
@ExtendWith(MockitoExtension.class)
class IblA1VariableValidationExceptionAdviceTest {

    @Mock
    private MessageResolver messageResolver;

    private RuntimeExceptionAdvice advice;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        advice = new RuntimeExceptionAdvice(
                new ErrorEnvelopeFactory(new TraceIdProvider(), messageResolver)
        );
        request = new MockHttpServletRequest("POST", "/api/uat/v1/templates/TPL/default/generate");
    }

    @Test
    void mapsVariableValidationExceptionTo422AggregateCode() {
        when(messageResolver.resolve(VariableValidationException.MESSAGE_KEY))
                .thenReturn("One or more template variables failed validation.");

        ResponseEntity<ErrorEnvelope> response = advice.handleVariableValidationFailed(
                request,
                new VariableValidationException(List.of(
                        new FieldError("customerName", "REQUIRED", "Field is required.")
                ))
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.VARIABLE_VALIDATION_FAILED);
        assertThat(response.getBody().error().category()).isEqualTo(ApiErrorCategories.VALIDATION);
        assertThat(response.getBody().error().retryable()).isFalse();
        assertThat(response.getBody().error().messageKey()).isEqualTo(VariableValidationException.MESSAGE_KEY);
        assertThat(response.getBody().error().fieldErrors()).hasSize(1);
        assertThat(response.getBody().error().fieldErrors().getFirst().field()).isEqualTo("customerName");
        assertThat(response.getBody().error().fieldErrors().getFirst().reason()).isEqualTo("REQUIRED");
    }
}
