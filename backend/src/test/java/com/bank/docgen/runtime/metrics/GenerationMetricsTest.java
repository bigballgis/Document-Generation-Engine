package com.bank.docgen.runtime.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GenerationMetricsTest {

    private MeterRegistry registry;
    private GenerationMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new GenerationMetrics(registry);
    }

    @Test
    void recordsSuccessWithExpectedTags() {
        metrics.record(Duration.ofMillis(120), "success", "pdf", "sync");

        var timer = registry.find("docgen.generation.duration")
                .tag("outcome", "success")
                .tag("format", "pdf")
                .tag("mode", "sync")
                .timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void recordsFailureWithExpectedTags() {
        metrics.record(Duration.ofMillis(50), "failure", "docx", "async");

        var timer = registry.find("docgen.generation.duration")
                .tag("outcome", "failure")
                .tag("format", "docx")
                .tag("mode", "async")
                .timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void doesNotRegisterTemplateIdTag() {
        metrics.record(Duration.ofMillis(10), "success", "docx", "sync");

        assertThat(registry.getMeters()).allSatisfy(meter ->
                assertThat(meter.getId().getTags()).noneMatch(tag -> "templateId".equals(tag.getKey())));
    }
}
