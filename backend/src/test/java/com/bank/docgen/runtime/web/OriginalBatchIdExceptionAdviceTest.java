package com.bank.docgen.runtime.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.runtime.service.OriginalBatchIdFormatException;
import com.bank.docgen.runtime.service.OriginalBatchNotFoundException;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.ErrorEnvelope;
import com.bank.docgen.sharedkernel.api.ErrorEnvelopeFactory;
import com.bank.docgen.sharedkernel.api.FieldError;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * BDD-CE-C05-005 / 007 — opaque 404 and format 400 mapping.
 */
@ExtendWith(MockitoExtension.class)
class OriginalBatchIdExceptionAdviceTest {

    @Mock
    private MessageResolver messageResolver;

    private RuntimeExceptionAdvice advice;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        advice = new RuntimeExceptionAdvice(
                new ErrorEnvelopeFactory(new TraceIdProvider(), messageResolver)
        );
        request = new MockHttpServletRequest("POST", "/api/dev/v1/templates/TPL/versions/1.0.0/batch-generate");
    }

    @Test
    void bddCeC05_005_mapsOriginalBatchNotFoundToOpaque404() {
        when(messageResolver.resolve("api.error.batch.originalBatchNotFound"))
                .thenReturn("Original batch was not found.");

        ResponseEntity<ErrorEnvelope> response = advice.handleOriginalBatchNotFound(
                request,
                new OriginalBatchNotFoundException()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.ORIGINAL_BATCH_NOT_FOUND);
        assertThat(response.getBody().error().category()).isEqualTo(ApiErrorCategories.BATCH);
        assertThat(response.getBody().error().messageKey())
                .isEqualTo("api.error.batch.originalBatchNotFound");
        assertThat(response.getBody().error().retryable()).isFalse();
        assertThat(response.getBody().error().message()).isEqualTo("Original batch was not found.");
        assertThat(response.getBody().error().message()).doesNotContain("credential");
        assertThat(response.getBody().error().message()).doesNotContain("other");
    }

    @Test
    void bddCeC05_007_mapsFormatExceptionToRequestBodyInvalid() {
        when(messageResolver.resolve("api.error.validation.requestBodyInvalid"))
                .thenReturn("The request body is invalid.");

        ResponseEntity<ErrorEnvelope> response = advice.handleOriginalBatchIdFormat(
                request,
                new OriginalBatchIdFormatException()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.REQUEST_BODY_INVALID);
        assertThat(response.getBody().error().category()).isEqualTo(ApiErrorCategories.VALIDATION);
        assertThat(response.getBody().error().fieldErrors())
                .extracting(FieldError::field, FieldError::reason)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("originalBatchId", "PATTERN_MISMATCH"));
    }
}
