package com.bank.docgen.sharedkernel.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private static final String CLAIM_DISPLAY_NAME = "displayName";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_AUTH_SOURCE = "authSource";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_GROUPS = "groups";
    private static final String CLAIM_DEFAULT_ROUTE = "defaultRoute";
    private static final String CLAIM_VISIBLE_ROUTES = "visibleRoutes";
    private static final String CLAIM_SESSION_STARTED_AT = "sessionStartedAt";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String subject) {
        Instant now = Instant.now();
        Duration ttl = Duration.parse(jwtProperties.accessTokenTtl());
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Issues a management token whose expiry comes from {@code session.expiresAt()} so renewal
     * can clamp it to the absolute session deadline (LR-B6). {@code jti} and
     * {@code sessionStartedAt} are mandatory revocation/limit claims.
     */
    public String createManagementToken(ManagementSessionClaims session) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(session.jti())
                .subject(session.username())
                .claim(CLAIM_DISPLAY_NAME, session.displayName())
                .claim(CLAIM_EMAIL, session.email())
                .claim(CLAIM_AUTH_SOURCE, session.authSource().name())
                .claim(CLAIM_ROLES, session.roles())
                .claim(CLAIM_GROUPS, session.authorizedGroupCodes())
                .claim(CLAIM_DEFAULT_ROUTE, session.defaultRoute())
                .claim(CLAIM_VISIBLE_ROUTES, session.visibleRoutes())
                .claim(CLAIM_SESSION_STARTED_AT, session.sessionStartedAt().getEpochSecond())
                .issuedAt(Date.from(now))
                .expiration(Date.from(session.expiresAt()))
                .signWith(secretKey)
                .compact();
    }

    public String parseSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public ManagementSessionClaims parseManagementToken(String token) {
        var claims = parseClaims(token);
        String jti = claims.getId();
        Object sessionStartedAtRaw = claims.get(CLAIM_SESSION_STARTED_AT);
        if (jti == null || jti.isBlank() || !(sessionStartedAtRaw instanceof Number sessionStartedAtSeconds)) {
            // Pre-LR-B6 tokens without revocation/limit claims are rejected outright
            // (spec B8 / [ASSUMED-LEGACY-TOKEN]): a one-time re-login replaces them.
            throw new MalformedJwtException("Management token is missing required session claims");
        }
        return new ManagementSessionClaims(
                claims.getSubject(),
                claims.get(CLAIM_DISPLAY_NAME, String.class),
                claims.get(CLAIM_EMAIL, String.class),
                com.bank.docgen.authorization.management.domain.AuthSource.valueOf(
                        claims.get(CLAIM_AUTH_SOURCE, String.class)),
                claims.get(CLAIM_ROLES, List.class),
                claims.get(CLAIM_GROUPS, List.class),
                claims.get(CLAIM_DEFAULT_ROUTE, String.class),
                claims.get(CLAIM_VISIBLE_ROUTES, List.class),
                jti,
                Instant.ofEpochSecond(sessionStartedAtSeconds.longValue()),
                claims.getExpiration().toInstant()
        );
    }

    public Instant accessTokenExpiresAt() {
        return Instant.now().plus(Duration.parse(jwtProperties.accessTokenTtl()));
    }

    public boolean isExpired(String token) {
        try {
            parseClaims(token);
            return false;
        } catch (ExpiredJwtException ex) {
            return true;
        }
    }

    private io.jsonwebtoken.Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
