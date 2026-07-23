package com.bank.docgen.runtime.security;

import com.bank.docgen.infrastructure.config.RuntimeRateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Process-local Bucket4j rate limiter (default when {@code docgen.runtime.rate-limit.distributed=false}).
 */
public class RuntimeRateLimitService implements RuntimeRateLimiter {

    private final RuntimeRateLimitProperties properties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RuntimeRateLimitService(RuntimeRateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean enabled() {
        return properties.enabled();
    }

    @Override
    public RateLimitDecision tryConsumeKey(String key) {
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return RateLimitDecision.allowed();
        }
        return RateLimitDecision.denied(probe.getNanosToWaitForRefill());
    }

    private Bucket newBucket() {
        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(properties.burstCapacity())
                .refillGreedy(properties.requestsPerMinute(), Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(bandwidth).build();
    }
}
