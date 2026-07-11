package com.bank.docgen.rendering.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.rendering.service.SseEmitterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SseEmitterMetricsTest {

    private MeterRegistry registry;
    private SseEmitterRegistry previewRegistry;
    private SseEmitterRegistry batchRegistry;
    private SseEmitterMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        previewRegistry = new SseEmitterRegistry();
        batchRegistry = new SseEmitterRegistry();
        metrics = new SseEmitterMetrics(previewRegistry, batchRegistry);
        metrics.bindTo(registry);
    }

    @AfterEach
    void tearDown() {
        previewRegistry.shutdown();
        batchRegistry.shutdown();
    }

    @Test
    void registersActiveGaugeAtZero() {
        assertThat(registry.find("docgen.sse.emitters.active").gauge()).isNotNull();
        assertThat(registry.find("docgen.sse.emitters.active").gauge().value()).isZero();
    }

    @Test
    void gaugeSumsActiveEmittersAcrossRegistries() {
        UUID previewId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        previewRegistry.register(previewId);
        batchRegistry.register(batchId);

        assertThat(registry.find("docgen.sse.emitters.active").gauge().value()).isEqualTo(2.0);

        previewRegistry.complete(previewId);
        assertThat(registry.find("docgen.sse.emitters.active").gauge().value()).isEqualTo(1.0);

        batchRegistry.complete(batchId);
        assertThat(registry.find("docgen.sse.emitters.active").gauge().value()).isZero();
    }
}
