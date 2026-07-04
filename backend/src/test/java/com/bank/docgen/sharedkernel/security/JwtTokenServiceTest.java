package com.bank.docgen.sharedkernel.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.authorization.management.domain.AuthSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

    @Test
    void createAndParseAccessToken() {
        JwtProperties properties = new JwtProperties(
                "test-jwt-secret-at-least-32-bytes-long!!",
                "PT15M");
        JwtTokenService jwtTokenService = new JwtTokenService(properties);

        String token = jwtTokenService.createAccessToken("12345678");

        assertThat(token).isNotBlank();
        assertThat(jwtTokenService.parseSubject(token)).isEqualTo("12345678");
    }

    @Test
    void createAndParseManagementTokenPreservesImmutableSessionClaims() {
        JwtProperties properties = new JwtProperties(
                "test-jwt-secret-at-least-32-bytes-long!!",
                "PT15M");
        JwtTokenService jwtTokenService = new JwtTokenService(properties);
        List<String> roles = new ArrayList<>(List.of("GLOBAL_ADMIN"));
        List<String> groups = new ArrayList<>(List.of("*"));
        List<String> routes = new ArrayList<>(List.of("/templates"));
        ManagementSessionClaims session = new ManagementSessionClaims(
                "10000001",
                "Admin",
                "admin@example.com",
                AuthSource.LOCAL,
                roles,
                groups,
                "/templates",
                routes,
                Instant.parse("2030-01-01T00:00:00Z")
        );

        String token = jwtTokenService.createManagementToken(session);
        ManagementSessionClaims parsed = jwtTokenService.parseManagementToken(token);

        roles.add("TEMPLATE_AUTHOR");
        groups.add("RETAIL");
        routes.add("/masters");

        assertThat(parsed.username()).isEqualTo("10000001");
        assertThat(parsed.roles()).containsExactly("GLOBAL_ADMIN");
        assertThat(parsed.authorizedGroupCodes()).containsExactly("*");
        assertThat(parsed.visibleRoutes()).containsExactly("/templates");
        assertThatThrownBy(() -> parsed.roles().add("X")).isInstanceOf(UnsupportedOperationException.class);
    }
}
