package com.bank.docgen.runtime.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RateLimitBackendUnavailableMetricsTest {

    private MeterRegistry registry;
    private RateLimitBackendUnavailableMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new RateLimitBackendUnavailableMetrics(registry);
    }

    @Test
    void registersCounter() {
        assertThat(registry.find("docgen.http.rate_limit.backend_unavailable").counter()).isNotNull();
        assertThat(registry.find("docgen.http.rate_limit.backend_unavailable").counter().count()).isZero();
    }

    @Test
    void incrementsOnRecord() {
        metrics.record();
        metrics.record();

        assertThat(registry.find("docgen.http.rate_limit.backend_unavailable").counter().count()).isEqualTo(2.0);
    }
}
