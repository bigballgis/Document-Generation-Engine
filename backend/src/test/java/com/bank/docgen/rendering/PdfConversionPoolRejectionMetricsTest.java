package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PdfConversionPoolRejectionMetricsTest {

    private MeterRegistry registry;
    private PdfConversionPoolRejectionMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new PdfConversionPoolRejectionMetrics(registry);
    }

    @Test
    void registersRejectionCounter() {
        assertThat(registry.find("docgen.pdf.conversion.pool.rejections").counter()).isNotNull();
        assertThat(registry.find("docgen.pdf.conversion.pool.rejections").counter().count()).isZero();
    }

    @Test
    void incrementsOnRecord() {
        metrics.record();
        metrics.record();

        assertThat(registry.find("docgen.pdf.conversion.pool.rejections").counter().count()).isEqualTo(2.0);
    }
}
