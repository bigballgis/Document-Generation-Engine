package com.bank.docgen.rendering;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Counts PDF conversion pool capacity rejections (SOR-P03 fail-fast / AbortPolicy).
 * LR-D3 — alert family {@code alert-pdf-pool-rejections}.
 */
@Component
public class PdfConversionPoolRejectionMetrics {

    static final String METRIC_NAME = "docgen.pdf.conversion.pool.rejections";

    private final Counter rejections;

    public PdfConversionPoolRejectionMetrics(MeterRegistry meterRegistry) {
        this.rejections = Counter.builder(METRIC_NAME)
                .description("PDF conversion tasks rejected because the bounded pool was saturated")
                .register(meterRegistry);
    }

    public void record() {
        rejections.increment();
    }
}
