package com.bank.docgen.runtime.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.infrastructure.resilience.GenerationServiceUnavailableException;
import com.bank.docgen.infrastructure.resilience.GenerationTimeoutException;
import com.bank.docgen.rendering.PdfConversionCapacityExceededException;
import com.bank.docgen.runtime.domain.InvocationErrorEnvelope;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.template.service.TemplateValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * BDD-PRR-D01B-012…015 — IRC mapper aligns with D01A generation taxonomy.
 */
class FailedSyncInvocationErrorMapperTest {

    private MessageResolver messageResolver;

    @BeforeEach
    void setUp() {
        messageResolver = mock(MessageResolver.class);
        when(messageResolver.resolve("api.error.generation.generationServiceUnavailable"))
                .thenReturn("Generation service is temporarily unavailable.");
        when(messageResolver.resolve("api.error.generation.generationTimeout"))
                .thenReturn("Generation timed out.");
        when(messageResolver.resolve("api.error.generation.pdfConversionCapacityExceeded"))
                .thenReturn("PDF conversion capacity exceeded.");
        when(messageResolver.resolve("api.error.template.validationFailed"))
                .thenReturn("Template validation failed.");
    }

    @Test
    void mapsGenerationServiceUnavailable() {
        InvocationErrorEnvelope envelope = FailedSyncInvocationErrorMapper.from(
                new GenerationServiceUnavailableException(),
                messageResolver
        );

        assertThat(envelope).isNotNull();
        assertThat(envelope.code()).isEqualTo(ApiErrorCodes.GENERATION_SERVICE_UNAVAILABLE);
        assertThat(envelope.category()).isEqualTo(ApiErrorCategories.GENERATION);
        assertThat(envelope.retryable()).isTrue();
        assertThat(envelope.messageKey()).isEqualTo("api.error.generation.generationServiceUnavailable");
        assertThat(envelope.message()).isEqualTo("Generation service is temporarily unavailable.");
        assertThat(envelope.message()).doesNotContain("GenerationServiceUnavailableException");
    }

    @Test
    void mapsGenerationTimeout() {
        InvocationErrorEnvelope envelope = FailedSyncInvocationErrorMapper.from(
                new GenerationTimeoutException(),
                messageResolver
        );

        assertThat(envelope).isNotNull();
        assertThat(envelope.code()).isEqualTo(ApiErrorCodes.GENERATION_TIMEOUT);
        assertThat(envelope.category()).isEqualTo(ApiErrorCategories.GENERATION);
        assertThat(envelope.retryable()).isTrue();
        assertThat(envelope.messageKey()).isEqualTo("api.error.generation.generationTimeout");
    }

    @Test
    void mapsPdfConversionCapacityExceeded() {
        InvocationErrorEnvelope envelope = FailedSyncInvocationErrorMapper.from(
                new PdfConversionCapacityExceededException(),
                messageResolver
        );

        assertThat(envelope).isNotNull();
        assertThat(envelope.code()).isEqualTo(ApiErrorCodes.PDF_CONVERSION_CAPACITY_EXCEEDED);
        assertThat(envelope.category()).isEqualTo(ApiErrorCategories.GENERATION);
        assertThat(envelope.retryable()).isTrue();
        assertThat(envelope.messageKey()).isEqualTo("api.error.generation.pdfConversionCapacityExceeded");
    }

    @Test
    void mapsGenerationUnavailableThroughCauseChain() {
        RuntimeException wrapped = new RuntimeException(
                "wrapper",
                new GenerationServiceUnavailableException()
        );

        InvocationErrorEnvelope envelope = FailedSyncInvocationErrorMapper.from(wrapped, messageResolver);

        assertThat(envelope).isNotNull();
        assertThat(envelope.code()).isEqualTo(ApiErrorCodes.GENERATION_SERVICE_UNAVAILABLE);
        assertThat(envelope.retryable()).isTrue();
    }

    @Test
    void preservesTemplateValidationOverGenerationUnavailable() {
        InvocationErrorEnvelope envelope = FailedSyncInvocationErrorMapper.from(
                new TemplateValidationException("api.error.template.validationFailed"),
                messageResolver
        );

        assertThat(envelope).isNotNull();
        assertThat(envelope.code()).isEqualTo(ApiErrorCodes.TEMPLATE_VALIDATION_FAILED);
        assertThat(envelope.code()).isNotEqualTo(ApiErrorCodes.GENERATION_SERVICE_UNAVAILABLE);
    }
}
