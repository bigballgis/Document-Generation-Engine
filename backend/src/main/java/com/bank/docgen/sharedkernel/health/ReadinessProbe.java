package com.bank.docgen.sharedkernel.health;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Readiness probe for orchestrators (Kubernetes {@code /readyz}, compose health checks).
 * <p>
 * <strong>Traffic gating (SOR-O06):</strong> only PostgreSQL {@code SELECT 1} determines HTTP
 * 200 vs 503. Optional contributors report Redis, MinIO, and Kafka state for diagnostics.
 */
@Component
public class ReadinessProbe {

    private final JdbcTemplate jdbcTemplate;
    private final List<ComponentReadinessContributor> optionalContributors;

    public ReadinessProbe(JdbcTemplate jdbcTemplate, List<ComponentReadinessContributor> optionalContributors) {
        this.jdbcTemplate = jdbcTemplate;
        this.optionalContributors = optionalContributors == null ? List.of() : List.copyOf(optionalContributors);
    }

    public ReadinessReport check() {
        Map<String, ComponentCheck> checks = new LinkedHashMap<>();
        boolean postgresUp = probePostgres(checks);
        for (ComponentReadinessContributor contributor : optionalContributors) {
            checks.put(contributor.componentName(), contributor.check());
        }
        String status = postgresUp ? "UP" : "DOWN";
        return ReadinessReport.of(status, checks);
    }

    /** @deprecated use {@link #check()} */
    @Deprecated(forRemoval = false)
    public boolean isReady() {
        return check().trafficReady();
    }

    private boolean probePostgres(Map<String, ComponentCheck> checks) {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            boolean up = Integer.valueOf(1).equals(result);
            checks.put("postgres", new ComponentCheck(up ? "UP" : "DOWN", null));
            return up;
        } catch (Exception ex) {
            checks.put("postgres", new ComponentCheck("DOWN", null));
            return false;
        }
    }
}
