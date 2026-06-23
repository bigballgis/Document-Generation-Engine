package com.bank.docgen.sharedkernel.health;

import java.util.Map;
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
    public ResponseEntity<Map<String, String>> liveness() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @GetMapping("/readyz")
    public ResponseEntity<Map<String, String>> readiness() {
        if (readinessProbe.isReady()) {
            return ResponseEntity.ok(Map.of("status", "UP"));
        }
        return ResponseEntity.status(503).body(Map.of("status", "DOWN"));
    }
}
