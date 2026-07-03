package com.bank.docgen.sharedkernel.health;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Readiness probe for orchestrators (Kubernetes {@code /readyz}, compose health checks).
 * <p>
 * <strong>Scope decision (SOR-O06):</strong> DB-only for v1. Redis, MinIO, and Kafka are
 * required for full functionality but are not probed here — a DB-up instance can serve
 * management auth and enqueue async work; dependency outages surface via domain errors and
 * metrics instead of failing the whole pod during a partial outage.
 */
@Component
public class ReadinessProbe {

    private final JdbcTemplate jdbcTemplate;

    public ReadinessProbe(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isReady() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Integer.valueOf(1).equals(result);
        } catch (Exception ex) {
            return false;
        }
    }
}
