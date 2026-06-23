package com.bank.docgen.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "docgen.runtime.rate-limit")
public record RuntimeRateLimitProperties(
        boolean enabled,
        int requestsPerMinute,
        int burstCapacity
) {
    public RuntimeRateLimitProperties {
        if (requestsPerMinute <= 0) {
            requestsPerMinute = 120;
        }
        if (burstCapacity <= 0) {
            burstCapacity = requestsPerMinute;
        }
    }
}
