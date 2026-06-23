package com.bank.docgen.infrastructure.resilience;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.template.service.TemplateValidationException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.Test;

class ResilienceFailureMapperTest {

    @Test
    void mapsOpenCircuitToServiceUnavailable() {
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
                .isInstanceOf(TemplateValidationException.class)
                .hasMessage("api.error.generation.serviceUnavailable");
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
    void mapsCallNotPermittedDirectly() {
        RuntimeException mapped = ResilienceFailureMapper.map(CallNotPermittedException.createCallNotPermittedException(
                CircuitBreaker.ofDefaults("test")
        ));

        assertThatThrownBy(() -> {
            throw mapped;
        })
                .isInstanceOf(TemplateValidationException.class)
                .hasMessage("api.error.generation.serviceUnavailable");
    }
}
