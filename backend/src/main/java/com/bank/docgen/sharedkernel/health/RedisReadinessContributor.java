package com.bank.docgen.sharedkernel.health;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class RedisReadinessContributor implements ComponentReadinessContributor {

    private final StringRedisTemplate redisTemplate;

    public RedisReadinessContributor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String componentName() {
        return "redis";
    }

    @Override
    public ComponentCheck check() {
        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
        if (factory == null) {
            return new ComponentCheck("DOWN", null);
        }
        try (RedisConnection connection = factory.getConnection()) {
            String pong = connection.ping();
            boolean up = "PONG".equalsIgnoreCase(pong);
            return new ComponentCheck(up ? "UP" : "DOWN", null);
        } catch (RuntimeException ex) {
            return new ComponentCheck("DOWN", null);
        }
    }
}
