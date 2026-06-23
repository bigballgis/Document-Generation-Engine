package com.bank.docgen.runtime.security;

import com.bank.docgen.infrastructure.config.RuntimeRateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class RuntimeRateLimitService {

    private final RuntimeRateLimitProperties properties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RuntimeRateLimitService(RuntimeRateLimitProperties properties) {
        this.properties = properties;
    }

    public boolean enabled() {
        return properties.enabled();
    }

    public ConsumptionProbe tryConsume(String credentialExternalId, String accessAccount) {
        String key = credentialExternalId + ":" + accessAccount;
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> newBucket());
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    private Bucket newBucket() {
        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(properties.burstCapacity())
                .refillGreedy(properties.requestsPerMinute(), Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(bandwidth).build();
    }
}
