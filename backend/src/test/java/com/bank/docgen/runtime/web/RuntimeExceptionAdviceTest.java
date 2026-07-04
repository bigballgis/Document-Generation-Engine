package com.bank.docgen.runtime.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.runtime.service.IdempotencyConflictException;
import com.bank.docgen.runtime.service.IdempotencyHashException;
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
class RuntimeExceptionAdviceTest {

    @Mock
    private MessageResolver messageResolver;

    private RuntimeExceptionAdvice advice;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        advice = new RuntimeExceptionAdvice(
                new ErrorEnvelopeFactory(new TraceIdProvider(), messageResolver)
        );
        request = new MockHttpServletRequest("POST", "/api/runtime/v1/generate");
    }

    @Test
    void idempotencyConflictIncludesSafeSummary() {
        when(messageResolver.resolve("api.error.runtime.idempotencyConflict"))
                .thenReturn("The idempotency key was already used with a different request.");

        ResponseEntity<ErrorEnvelope> response = advice.handleIdempotencyConflict(
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
    void idempotencyHashFailureMapsToStableInternalErrorEnvelope() {
        when(messageResolver.resolve("api.error.idempotency.hashFailed"))
                .thenReturn("Unable to compute the idempotency fingerprint.");

        ResponseEntity<ErrorEnvelope> response = advice.handleIdempotencyHashFailure(
                request,
                new IdempotencyHashException("SHA-256", new RuntimeException("missing provider"))
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.IDEMPOTENCY_HASH_FAILED);
        assertThat(response.getBody().error().category()).isEqualTo(ApiErrorCategories.IDEMPOTENCY);
        assertThat(response.getBody().error().messageKey()).isEqualTo("api.error.idempotency.hashFailed");
    }
}
