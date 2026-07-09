package com.bank.docgen.rendering;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * SLO metrics for PDF conversion (F8-B1 / BDD-F8-B1-003).
 */
@Component
public class PdfConversionMetrics {

    private static final String DURATION_METRIC = "docgen.pdf.conversion.duration";
    private static final String OUTCOME_METRIC = "docgen.pdf.conversion.outcome";

    private final MeterRegistry meterRegistry;

    public PdfConversionMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void record(Duration duration, String result) {
        Timer.builder(DURATION_METRIC)
                .description("LibreOffice PDF conversion latency")
                .tag("result", result)
                .register(meterRegistry)
                .record(duration.toNanos(), TimeUnit.NANOSECONDS);
        Counter.builder(OUTCOME_METRIC)
                .description("PDF conversion success and failure count")
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }
}
