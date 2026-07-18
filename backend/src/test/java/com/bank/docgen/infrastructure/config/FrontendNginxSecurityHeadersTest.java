package com.bank.docgen.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * BDD-PRR-D01B-007…009 — frontend nginx edge emits CSP + standard security headers
 * without removing probe / API / SSE / .mjs locations.
 */
class FrontendNginxSecurityHeadersTest {

    @Test
    void nginxConfDeclaresSecurityHeadersAndKeepsFunctionalLocations() throws Exception {
        Path nginxConf = resolveFrontendNginxConf();
        assertThat(nginxConf).exists();
        String content = Files.readString(nginxConf, StandardCharsets.UTF_8);

        assertThat(content).contains("Content-Security-Policy");
        assertThat(content).containsPattern("(?i)default-src\\s+'self'");
        assertThat(content).doesNotContain("TODO CSP");

        assertThat(content).contains("X-Content-Type-Options");
        assertThat(content).contains("nosniff");
        assertThat(content).contains("X-Frame-Options");
        assertThat(content).containsPattern("X-Frame-Options\\s+\"?(DENY|SAMEORIGIN)\"?");
        assertThat(content).contains("Referrer-Policy");

        assertThat(content).contains("location /healthz");
        assertThat(content).contains("location /readyz");
        assertThat(content).contains("location /api/");
        assertThat(content).contains("progress-stream");
        assertThat(content).contains("location ~ \\.mjs$");
    }

    private static Path resolveFrontendNginxConf() {
        Path fromBackendModule = Path.of("..", "frontend", "nginx.conf").normalize().toAbsolutePath();
        if (Files.isRegularFile(fromBackendModule)) {
            return fromBackendModule;
        }
        Path fromRepoRoot = Path.of("frontend", "nginx.conf").normalize().toAbsolutePath();
        if (Files.isRegularFile(fromRepoRoot)) {
            return fromRepoRoot;
        }
        throw new IllegalStateException(
                "frontend/nginx.conf not found from " + Path.of(".").toAbsolutePath()
        );
    }
}
