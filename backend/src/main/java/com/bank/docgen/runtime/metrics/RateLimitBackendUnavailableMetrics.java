package com.bank.docgen.runtime.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Counts runtime API HTTP 503 rate-limit backend outages when distributed coordination fails (PQH-F7).
 */
@Component
public class RateLimitBackendUnavailableMetrics {

    static final String METRIC_NAME = "docgen.http.rate_limit.backend_unavailable";

    private final Counter unavailable;

    public RateLimitBackendUnavailableMetrics(MeterRegistry meterRegistry) {
        this.unavailable = Counter.builder(METRIC_NAME)
                .description(
                        "Runtime API requests rejected with HTTP 503 because the rate-limit backend"
                                + " was unavailable"
                )
                .register(meterRegistry);
    }

    public void record() {
        unavailable.increment();
    }
}
