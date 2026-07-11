package com.bank.docgen.runtime.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AsyncDltDepthMetricsTest {

    private MeterRegistry registry;
    private AtomicLong depth;
    private AsyncDltDepthMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        depth = new AtomicLong(0);
        metrics = new AsyncDltDepthMetrics(depth::get);
        metrics.bindTo(registry);
    }

    @Test
    void registersDepthGauge() {
        assertThat(registry.find("docgen.async.dlt.depth").gauge()).isNotNull();
        assertThat(registry.find("docgen.async.dlt.depth").gauge().value()).isZero();
    }

    @Test
    void gaugeReflectsSupplierValue() {
        depth.set(7);
        assertThat(registry.find("docgen.async.dlt.depth").gauge().value()).isEqualTo(7.0);
    }
}
