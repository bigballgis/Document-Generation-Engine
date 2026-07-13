package com.bank.docgen.runtime.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.function.LongSupplier;

/**
 * Async Kafka DLT depth gauge (LR-D3 — alert family {@code alert-dlt-depth}).
 * Depth supplier is Kafka AdminClient-backed when transport=kafka; tests inject a stub.
 */
public class AsyncDltDepthMetrics implements MeterBinder {

    static final String METRIC_NAME = "docgen.async.dlt.depth";

    private final LongSupplier depthSupplier;

    public AsyncDltDepthMetrics(LongSupplier depthSupplier) {
        this.depthSupplier = depthSupplier;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder(METRIC_NAME, depthSupplier, supplier -> (double) supplier.getAsLong())
                .description("Approximate message depth on the async batch dead-letter topic")
                .register(registry);
    }
}
