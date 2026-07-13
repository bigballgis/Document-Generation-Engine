package com.bank.docgen.runtime.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Counts runtime API HTTP 429 rate-limit denials (LR-D3 — alert family {@code alert-429-surge}).
 */
@Component
public class RateLimitDeniedMetrics {

    static final String METRIC_NAME = "docgen.http.rate_limit.denied";

    private final Counter denied;

    public RateLimitDeniedMetrics(MeterRegistry meterRegistry) {
        this.denied = Counter.builder(METRIC_NAME)
                .description("Runtime API requests rejected with HTTP 429 by the rate limiter")
                .register(meterRegistry);
    }

    public void record() {
        denied.increment();
    }
}
