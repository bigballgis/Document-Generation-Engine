package com.bank.docgen.legalhold.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.legalhold.service.LegalHoldAlreadyReleasedException;
import com.bank.docgen.legalhold.service.LegalHoldNotFoundException;
import com.bank.docgen.legalhold.service.LegalHoldValidationException;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.ErrorEnvelope;
import com.bank.docgen.sharedkernel.api.ErrorEnvelopeFactory;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * IBL-D5 / F23 — fail-closed HTTP mapping for legal-hold domain exceptions (CE-G04-C20…C22).
 */
@ExtendWith(MockitoExtension.class)
class LegalHoldExceptionAdviceTest {

    @Mock
    private TraceIdProvider traceIdProvider;
    @Mock
    private MessageResolver messageResolver;

    private LegalHoldExceptionAdvice advice;

    @BeforeEach
    void setUp() {
        when(traceIdProvider.currentOrNew(any())).thenReturn("trace-hold");
        when(traceIdProvider.newAuditId()).thenReturn("audit-hold");
        when(messageResolver.resolve(any())).thenAnswer(inv -> inv.getArgument(0));
        advice = new LegalHoldExceptionAdvice(new ErrorEnvelopeFactory(traceIdProvider, messageResolver));
    }

    @Test
    void accessDenied_mapsTo403() {
        HttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<ErrorEnvelope> response = advice.handleAccessDenied(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.ACCESS_DENIED);
        assertThat(response.getBody().error().category()).isEqualTo(ApiErrorCategories.AUTHORIZATION);
        assertThat(response.getBody().error().messageKey()).isEqualTo("api.error.authorization.accessDenied");
        verify(messageResolver).resolve(eq("api.error.authorization.accessDenied"));
    }

    @Test
    void notFound_mapsTo404() {
        HttpServletRequest request = new MockHttpServletRequest();
        LegalHoldNotFoundException ex = new LegalHoldNotFoundException();

        ResponseEntity<ErrorEnvelope> response = advice.handleNotFound(request, ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.LEGAL_HOLD_NOT_FOUND);
        assertThat(response.getBody().error().messageKey()).isEqualTo("api.error.notFound.legalHoldNotFound");
    }

    @Test
    void alreadyReleased_mapsTo409() {
        HttpServletRequest request = new MockHttpServletRequest();
        LegalHoldAlreadyReleasedException ex = new LegalHoldAlreadyReleasedException();

        ResponseEntity<ErrorEnvelope> response = advice.handleAlreadyReleased(request, ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.LEGAL_HOLD_ALREADY_RELEASED);
        assertThat(response.getBody().error().category()).isEqualTo(ApiErrorCategories.CONFLICT);
    }

    @Test
    void validation_mapsTo422() {
        HttpServletRequest request = new MockHttpServletRequest();
        LegalHoldValidationException ex =
                new LegalHoldValidationException("api.error.validation.requestBodyInvalid");

        ResponseEntity<ErrorEnvelope> response = advice.handleValidation(request, ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.REQUEST_BODY_INVALID);
        assertThat(response.getBody().error().category()).isEqualTo(ApiErrorCategories.VALIDATION);
        assertThat(response.getBody().error().messageKey())
                .isEqualTo("api.error.validation.requestBodyInvalid");
    }
}
