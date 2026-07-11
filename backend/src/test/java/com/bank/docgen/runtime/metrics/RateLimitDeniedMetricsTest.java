package com.bank.docgen.runtime.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RateLimitDeniedMetricsTest {

    private MeterRegistry registry;
    private RateLimitDeniedMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new RateLimitDeniedMetrics(registry);
    }

    @Test
    void registersCounter() {
        assertThat(registry.find("docgen.http.rate_limit.denied").counter()).isNotNull();
        assertThat(registry.find("docgen.http.rate_limit.denied").counter().count()).isZero();
    }

    @Test
    void incrementsOnRecord() {
        metrics.record();
        metrics.record();
        metrics.record();

        assertThat(registry.find("docgen.http.rate_limit.denied").counter().count()).isEqualTo(3.0);
    }
}
