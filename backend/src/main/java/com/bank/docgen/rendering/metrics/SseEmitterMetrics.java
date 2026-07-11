package com.bank.docgen.rendering.metrics;

import com.bank.docgen.rendering.service.SseEmitterRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Active SSE emitter gauge across preview + batch registries (LR-B3 / LR-D3).
 */
@Component
public class SseEmitterMetrics implements MeterBinder {

    static final String METRIC_NAME = "docgen.sse.emitters.active";

    private final SseEmitterRegistry previewSseRegistry;
    private final SseEmitterRegistry batchSseRegistry;

    public SseEmitterMetrics(
            @Qualifier("previewSseRegistry") SseEmitterRegistry previewSseRegistry,
            @Qualifier("batchSseRegistry") SseEmitterRegistry batchSseRegistry
    ) {
        this.previewSseRegistry = previewSseRegistry;
        this.batchSseRegistry = batchSseRegistry;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder(METRIC_NAME, this, SseEmitterMetrics::activeCount)
                .description("Active SSE emitters (preview + batch registries)")
                .register(registry);
    }

    private double activeCount() {
        return previewSseRegistry.activeCount() + batchSseRegistry.activeCount();
    }
}
