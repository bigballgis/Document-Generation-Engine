package com.bank.docgen.infrastructure.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.rendering.PdfConversionCapacityExceededException;
import com.bank.docgen.rendering.RenderingOperationException;
import com.bank.docgen.template.service.TemplateValidationException;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

/**
 * BDD-PRR-D01A-001…006, 010, 011 — resilience failure taxonomy (DEF-LRP-D6-001).
 */
class ResilienceFailureMapperTest {

    @Test
    void mapsOpenCircuitToGenerationServiceUnavailable() {
        CircuitBreaker breaker = CircuitBreaker.of(
                "test",
                CircuitBreakerConfig.custom().slidingWindowSize(1).minimumNumberOfCalls(1).build()
        );
        breaker.transitionToOpenState();

        assertThatThrownBy(() -> ResilienceSupport.execute(
                breaker,
                Retry.of("test", RetryConfig.custom().maxAttempts(1).build()),
                () -> "ok"
        ))
                .isInstanceOf(GenerationServiceUnavailableException.class)
                .satisfies(ex -> {
                    GenerationServiceUnavailableException unavailable =
                            (GenerationServiceUnavailableException) ex;
                    assertThat(unavailable.messageKey())
                            .isEqualTo("api.error.generation.generationServiceUnavailable");
                    assertThat(unavailable.getMessage()).isNull();
                });
    }

    @Test
    void mapsCallNotPermittedDirectlyToServiceUnavailable() {
        RuntimeException mapped = ResilienceFailureMapper.map(
                CallNotPermittedException.createCallNotPermittedException(
                        CircuitBreaker.ofDefaults("test")
                )
        );

        assertThat(mapped).isInstanceOf(GenerationServiceUnavailableException.class);
        assertThat(((GenerationServiceUnavailableException) mapped).messageKey())
                .isEqualTo("api.error.generation.generationServiceUnavailable");
        assertThat(mapped).isNotInstanceOf(TemplateValidationException.class);
    }

    @Test
    void mapsTimeoutToGenerationTimeout() {
        RuntimeException mapped = ResilienceFailureMapper.map(new TimeoutException("timed out"));

        assertThat(mapped).isInstanceOf(GenerationTimeoutException.class);
        assertThat(((GenerationTimeoutException) mapped).messageKey())
                .isEqualTo("api.error.generation.generationTimeout");
        assertThat(mapped.getMessage()).isNull();
        assertThat(mapped).isNotInstanceOf(TemplateValidationException.class);
    }

    @Test
    void mapsNestedTimeoutInCauseChain() {
        RuntimeException mapped = ResilienceFailureMapper.map(
                new RuntimeException("wrapper", new TimeoutException("inner"))
        );

        assertThat(mapped).isInstanceOf(GenerationTimeoutException.class);
    }

    @Test
    void mapsBulkheadFullToGenerationServiceUnavailable() {
        Bulkhead bulkhead = Bulkhead.ofDefaults("test-bh");

        RuntimeException mapped = ResilienceFailureMapper.map(
                BulkheadFullException.createBulkheadFullException(bulkhead)
        );

        assertThat(mapped).isInstanceOf(GenerationServiceUnavailableException.class);
        assertThat(((GenerationServiceUnavailableException) mapped).messageKey())
                .isEqualTo("api.error.generation.generationServiceUnavailable");
        assertThat(mapped.getMessage()).isNull();
    }

    @Test
    void preservesBusinessValidationException() {
        CircuitBreaker breaker = CircuitBreaker.ofDefaults("test");
        Retry retry = Retry.ofDefaults("test");

        assertThatThrownBy(() -> ResilienceSupport.execute(
                breaker,
                retry,
                () -> {
                    throw new TemplateValidationException("api.error.generation.pdfConversionFailed");
                }
        ))
                .isInstanceOf(TemplateValidationException.class)
                .hasMessage("api.error.generation.pdfConversionFailed");
    }

    @Test
    void preservesRenderingOperationException() {
        RuntimeException mapped = ResilienceFailureMapper.map(
                new RenderingOperationException("api.error.generation.pdfConversionFailed")
        );

        assertThat(mapped)
                .isInstanceOf(RenderingOperationException.class)
                .hasMessage("api.error.generation.pdfConversionFailed");
    }

    @Test
    void preservesPdfConversionCapacityExceeded() {
        RuntimeException mapped = ResilienceFailureMapper.map(new PdfConversionCapacityExceededException());

        assertThat(mapped).isInstanceOf(PdfConversionCapacityExceededException.class);
        assertThat(mapped).isNotInstanceOf(GenerationServiceUnavailableException.class);
        assertThat(mapped).isNotInstanceOf(TemplateValidationException.class);
    }

    @Test
    void mapsUnknownCheckedFailureToServiceUnavailableNotTemplateValidation() {
        RuntimeException mapped = ResilienceFailureMapper.map(new Exception("opaque infrastructure fault"));

        assertThat(mapped).isInstanceOf(GenerationServiceUnavailableException.class);
        assertThat(mapped).isNotInstanceOf(TemplateValidationException.class);
        assertThat(((GenerationServiceUnavailableException) mapped).messageKey())
                .isEqualTo("api.error.generation.generationServiceUnavailable");
        assertThat(mapped.getMessage()).isNull();
    }
}
