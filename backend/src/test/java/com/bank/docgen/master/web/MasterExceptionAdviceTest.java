package com.bank.docgen.master.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.master.service.MasterAccessDeniedException;
import com.bank.docgen.master.service.MasterNotFoundException;
import com.bank.docgen.master.service.MasterValidationException;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.ErrorEnvelope;
import com.bank.docgen.sharedkernel.api.ErrorEnvelopeFactory;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class MasterExceptionAdviceTest {

    @Mock
    private MessageResolver messageResolver;

    private MasterExceptionAdvice advice;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        advice = new MasterExceptionAdvice(
                new ErrorEnvelopeFactory(new TraceIdProvider(), messageResolver)
        );
        request = new MockHttpServletRequest("GET", "/api/management/v1/masters/123");
    }

    @Test
    void masterNotFoundMapsToNotFoundEnvelope() {
        when(messageResolver.resolve("api.error.master.notFound"))
                .thenReturn("Master document not found.");

        ResponseEntity<ErrorEnvelope> response = advice.handleMasterNotFound(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.MASTER_NOT_FOUND);
        assertThat(response.getBody().error().category()).isEqualTo(ApiErrorCategories.MASTER);
        assertThat(response.getBody().error().messageKey()).isEqualTo("api.error.master.notFound");
    }

    @Test
    void masterAccessDeniedMapsToForbiddenEnvelope() {
        when(messageResolver.resolve("api.error.master.accessDenied"))
                .thenReturn("Access denied.");

        ResponseEntity<ErrorEnvelope> response = advice.handleMasterAccessDenied(
                request,
                new MasterAccessDeniedException()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.ACCESS_DENIED);
        assertThat(response.getBody().error().category()).isEqualTo(ApiErrorCategories.MASTER);
    }

    @Test
    void masterValidationUsesExceptionMessageKey() {
        when(messageResolver.resolve("api.error.master.invalidStatus"))
                .thenReturn("Invalid master status.");

        ResponseEntity<ErrorEnvelope> response = advice.handleMasterValidation(
                request,
                new MasterValidationException("api.error.master.invalidStatus")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.MASTER_VALIDATION_FAILED);
        assertThat(response.getBody().error().messageKey()).isEqualTo("api.error.master.invalidStatus");
    }
}
