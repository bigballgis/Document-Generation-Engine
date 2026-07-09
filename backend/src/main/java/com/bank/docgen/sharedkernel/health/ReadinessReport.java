package com.bank.docgen.sharedkernel.health;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("PMD.UnusedAssignment")
public record ReadinessReport(
        String status,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, ComponentCheck> checks
) {

    public ReadinessReport {
        checks = checks == null ? Map.of() : Map.copyOf(checks);
    }

    public boolean trafficReady() {
        ComponentCheck postgres = checks.get("postgres");
        return postgres != null && "UP".equals(postgres.status());
    }

    public static ReadinessReport of(String status, Map<String, ComponentCheck> checks) {
        return new ReadinessReport(status, new LinkedHashMap<>(checks));
    }
}
