package com.bank.docgen.runtime.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * SLO timer for document generation (F8-B1 / BDD-F8-B1-001).
 * Tags: {@code outcome}, {@code format}, {@code mode} — no template identifiers.
 */
@Component
public class GenerationMetrics {

    private static final String METRIC_NAME = "docgen.generation.duration";

    private final MeterRegistry meterRegistry;

    public GenerationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void record(Duration duration, String outcome, String format, String mode) {
        Timer.builder(METRIC_NAME)
                .description("End-to-end document generation latency")
                .tag("outcome", outcome)
                .tag("format", format)
                .tag("mode", mode)
                .register(meterRegistry)
                .record(duration.toNanos(), TimeUnit.NANOSECONDS);
    }

    public static String normalizeFormat(String outputFormat) {
        if (outputFormat == null || outputFormat.isBlank()) {
            return "docx";
        }
        return switch (outputFormat.toUpperCase(Locale.ROOT)) {
            case "DOCX" -> "docx";
            case "PDF" -> "pdf";
            default -> outputFormat.toLowerCase(Locale.ROOT);
        };
    }
}
