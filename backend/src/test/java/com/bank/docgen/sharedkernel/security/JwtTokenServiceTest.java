package com.bank.docgen.sharedkernel.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.authorization.management.domain.AuthSource;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

    private static final String SECRET = "test-jwt-secret-at-least-32-bytes-long!!";

    private final JwtTokenService jwtTokenService =
            new JwtTokenService(new JwtProperties(SECRET, "PT15M"));

    @Test
    void createAndParseAccessToken() {
        String token = jwtTokenService.createAccessToken("12345678");

        assertThat(token).isNotBlank();
        assertThat(jwtTokenService.parseSubject(token)).isEqualTo("12345678");
    }

    @Test
    void createAndParseManagementTokenPreservesImmutableSessionClaims() {
        List<String> roles = new ArrayList<>(List.of("GLOBAL_ADMIN"));
        List<String> groups = new ArrayList<>(List.of("*"));
        List<String> routes = new ArrayList<>(List.of("/templates"));
        Instant sessionStartedAt = Instant.parse("2030-01-01T00:00:00Z");
        Instant expiresAt = Instant.parse("2030-01-01T01:00:00Z");
        ManagementSessionClaims session = new ManagementSessionClaims(
                "10000001",
                "Admin",
                "admin@example.com",
                AuthSource.LOCAL,
                roles,
                groups,
                "/templates",
                routes,
                "jti-immutable",
                sessionStartedAt,
                expiresAt
        );

        String token = jwtTokenService.createManagementToken(session);
        ManagementSessionClaims parsed = jwtTokenService.parseManagementToken(token);

        roles.add("DOCUMENT_AUTHOR");
        groups.add("RETAIL");
        routes.add("/masters");

        assertThat(parsed.username()).isEqualTo("10000001");
        assertThat(parsed.roles()).containsExactly("GLOBAL_ADMIN");
        assertThat(parsed.authorizedGroupCodes()).containsExactly("*");
        assertThat(parsed.visibleRoutes()).containsExactly("/templates");
        assertThatThrownBy(() -> parsed.roles().add("X")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void managementTokenRoundTripsJtiAndSessionStartedAt() {
        Instant sessionStartedAt = Instant.now().minus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = Instant.now().plus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);
        ManagementSessionClaims claims = managementClaims("jti-J1", sessionStartedAt, expiresAt);

        ManagementSessionClaims parsed =
                jwtTokenService.parseManagementToken(jwtTokenService.createManagementToken(claims));

        assertThat(parsed.jti()).isEqualTo("jti-J1");
        assertThat(parsed.sessionStartedAt()).isEqualTo(sessionStartedAt);
        assertThat(parsed.expiresAt()).isEqualTo(expiresAt);
        assertThat(parsed.username()).isEqualTo("10000001");
        assertThat(parsed.roles()).containsExactly("GLOBAL_ADMIN");
    }

    @Test
    void managementTokenHonoursClampedExpiryFromClaims() {
        // Renewal near the absolute limit issues a token shorter than the configured TTL.
        Instant clampedExpiry = Instant.now().plus(4, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);
        ManagementSessionClaims claims =
                managementClaims("jti-J2", Instant.now().minus(1, ChronoUnit.HOURS), clampedExpiry);

        ManagementSessionClaims parsed =
                jwtTokenService.parseManagementToken(jwtTokenService.createManagementToken(claims));

        assertThat(parsed.expiresAt()).isEqualTo(clampedExpiry);
    }

    @Test
    void legacyManagementTokenWithoutSessionClaimsIsRejected() {
        String legacyToken = legacyTokenBuilder().compact();

        assertThatThrownBy(() -> jwtTokenService.parseManagementToken(legacyToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void managementTokenWithJtiButWithoutSessionStartedAtIsRejected() {
        String token = legacyTokenBuilder().id("jti-J3").compact();

        assertThatThrownBy(() -> jwtTokenService.parseManagementToken(token))
                .isInstanceOf(JwtException.class);
    }

    private io.jsonwebtoken.JwtBuilder legacyTokenBuilder() {
        SecretKey secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("10000001")
                .claim("displayName", "Global Admin")
                .claim("email", "global.admin@example.com")
                .claim("authSource", "LOCAL")
                .claim("roles", List.of("GLOBAL_ADMIN"))
                .claim("groups", List.of("*"))
                .claim("defaultRoute", "route.dashboard-home")
                .claim("visibleRoutes", List.of("route.dashboard-home"))
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(secretKey);
    }

    private ManagementSessionClaims managementClaims(String jti, Instant sessionStartedAt, Instant expiresAt) {
        return new ManagementSessionClaims(
                "10000001",
                "Global Admin",
                "global.admin@example.com",
                AuthSource.LOCAL,
                List.of("GLOBAL_ADMIN"),
                List.of("*"),
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                jti,
                sessionStartedAt,
                expiresAt
        );
    }
}
