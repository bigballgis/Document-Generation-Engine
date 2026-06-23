package com.bank.docgen.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "docgen.idempotency")
public class DocgenIdempotencyProperties {

    /**
     * Cache backend: {@code redis} (default for non-test) or {@code memory}.
     */
    private String cache = "redis";

    private String redisKeyPrefix = "docgen:idempotency:";

    public String getCache() {
        return cache;
    }

    public void setCache(String cache) {
        this.cache = cache;
    }

    public String getRedisKeyPrefix() {
        return redisKeyPrefix;
    }

    public void setRedisKeyPrefix(String redisKeyPrefix) {
        this.redisKeyPrefix = redisKeyPrefix;
    }
}
