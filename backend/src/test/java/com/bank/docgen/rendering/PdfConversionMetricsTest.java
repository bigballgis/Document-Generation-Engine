package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PdfConversionMetricsTest {

    private MeterRegistry registry;
    private PdfConversionMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new PdfConversionMetrics(registry);
    }

    @Test
    void recordsDurationTimer() {
        metrics.record(Duration.ofSeconds(2), "success");

        var timer = registry.find("docgen.pdf.conversion.duration")
                .tag("result", "success")
                .timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void incrementsOutcomeCounterForSuccessAndFailure() {
        metrics.record(Duration.ofMillis(100), "success");
        metrics.record(Duration.ofMillis(200), "failure");

        assertThat(registry.find("docgen.pdf.conversion.outcome")
                .tag("result", "success")
                .counter()
                .count()).isEqualTo(1.0);
        assertThat(registry.find("docgen.pdf.conversion.outcome")
                .tag("result", "failure")
                .counter()
                .count()).isEqualTo(1.0);
    }
}
