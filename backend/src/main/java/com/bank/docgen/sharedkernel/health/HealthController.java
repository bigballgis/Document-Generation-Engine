package com.bank.docgen.sharedkernel.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final ReadinessProbe readinessProbe;

    public HealthController(ReadinessProbe readinessProbe) {
        this.readinessProbe = readinessProbe;
    }

    @GetMapping("/healthz")
    public ResponseEntity<ReadinessReport> liveness() {
        return ResponseEntity.ok(ReadinessReport.of("UP", java.util.Map.of()));
    }

    @GetMapping("/readyz")
    public ResponseEntity<ReadinessReport> readiness() {
        ReadinessReport report = readinessProbe.check();
        if (report.trafficReady()) {
            return ResponseEntity.ok(report);
        }
        return ResponseEntity.status(503).body(report);
    }
}
