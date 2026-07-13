package com.bank.docgen.authorization.management.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.docgen.authorization.management.persistence.ManagementUserEntity;
import com.bank.docgen.authorization.management.persistence.ManagementUserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration coverage for LR-B6 session renewal + revocation
 * (BDD-LRP-SESSION-001 scenarios 8.1–8.4 plus boundaries B1/B3/B4/B8).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ManagementAuthSessionRenewalControllerTest {

    private static final String JWT_SECRET = "test-jwt-secret-at-least-32-bytes-long!!";
    private static final Duration ABSOLUTE_TTL = Duration.ofHours(8);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ManagementUserRepository managementUserRepository;

    @Test
    void renewIssuesNewTokenRevokesOldAndKeepsSessionAnchor() throws Exception {
        JsonNode login = loginResult("10000001", "ChangeMe123!");
        String oldToken = login.path("accessToken").asText();
        String loginDeadline = login.path("sessionAbsoluteDeadline").asText();

        MvcResult renewResult = mockMvc.perform(post("/api/management/v1/auth/renew")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.result.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.result.accessTokenExpiresAt").isNotEmpty())
                .andExpect(jsonPath("$.result.session.username").value("10000001"))
                .andReturn();
        JsonNode renew = objectMapper.readTree(renewResult.getResponse().getContentAsString())
                .path("result");
        String newToken = renew.path("accessToken").asText();

        assertThat(newToken).isNotEqualTo(oldToken);
        // The absolute deadline is anchored to first login and survives renewal unchanged,
        // both top-level and inside the embedded session view (frontend fallback chain).
        assertThat(renew.path("sessionAbsoluteDeadline").asText()).isEqualTo(loginDeadline);
        assertThat(renew.path("session").path("absoluteSessionExpiresAt").asText()).isEqualTo(loginDeadline);
        Claims oldClaims = decode(oldToken);
        Claims newClaims = decode(newToken);
        assertThat(newClaims.getId()).isNotBlank().isNotEqualTo(oldClaims.getId());
        assertThat(newClaims.get("sessionStartedAt")).isEqualTo(oldClaims.get("sessionStartedAt"));

        // SCEN-REVOKE-02: the replaced token is rejected, the renewed token keeps working.
        mockMvc.perform(get("/api/management/v1/auth/session")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("SESSION_REVOKED"))
                .andExpect(jsonPath("$.error.messageKey")
                        .value("api.error.authentication.sessionRevoked"));
        mockMvc.perform(get("/api/management/v1/auth/session")
                        .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.username").value("10000001"));
    }

    @Test
    void loginResponseCarriesRenewalContractFields() throws Exception {
        Instant before = Instant.now();
        JsonNode login = loginResult("10000001", "ChangeMe123!");

        Instant accessTokenExpiresAt = Instant.parse(login.path("accessTokenExpiresAt").asText());
        Instant sessionAbsoluteDeadline = Instant.parse(login.path("sessionAbsoluteDeadline").asText());
        // Test profile issues PT5M access tokens; the absolute limit stays PT8H.
        assertThat(accessTokenExpiresAt)
                .isBetween(before.plus(Duration.ofMinutes(4)), before.plus(Duration.ofMinutes(6)));
        assertThat(sessionAbsoluteDeadline)
                .isBetween(before.plus(Duration.ofHours(7)), before.plus(Duration.ofHours(9)));
        assertThat(Instant.parse(login.path("session").path("expiresAt").asText()))
                .isEqualTo(accessTokenExpiresAt);
        assertThat(Instant.parse(login.path("session").path("absoluteSessionExpiresAt").asText()))
                .isEqualTo(sessionAbsoluteDeadline);
    }

    @Test
    void sessionEndpointExposesAbsoluteSessionExpiresAtForRefreshRecovery() throws Exception {
        // After a page refresh the frontend only has the token: GET /auth/session must
        // rebuild the renewal/reminder schedule from session.absoluteSessionExpiresAt.
        JsonNode login = loginResult("10000001", "ChangeMe123!");
        String token = login.path("accessToken").asText();
        String loginDeadline = login.path("sessionAbsoluteDeadline").asText();

        MvcResult result = mockMvc.perform(get("/api/management/v1/auth/session")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.absoluteSessionExpiresAt").isNotEmpty())
                .andReturn();

        JsonNode session = objectMapper.readTree(result.getResponse().getContentAsString()).path("result");
        assertThat(session.path("absoluteSessionExpiresAt").asText()).isEqualTo(loginDeadline);
    }

    @Test
    void logoutMakesTokenReplayFail() throws Exception {
        String token = loginResult("10000002", "ChangeMe123!").path("accessToken").asText();

        mockMvc.perform(post("/api/management/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/management/v1/auth/session")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("SESSION_REVOKED"))
                .andExpect(jsonPath("$.error.category").value("AUTHENTICATION"))
                .andExpect(jsonPath("$.error.retryable").value(false))
                .andExpect(jsonPath("$.metadata.traceId").isNotEmpty());
    }

    @Test
    void renewBeyondAbsoluteLimitIsRejected() throws Exception {
        Instant sessionStartedAt = Instant.now().minus(Duration.ofHours(9));
        String overLimitToken = craftManagementToken(
                UUID.randomUUID().toString(), sessionStartedAt, Instant.now().plusSeconds(240));

        mockMvc.perform(post("/api/management/v1/auth/renew")
                        .header("Authorization", "Bearer " + overLimitToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("SESSION_ABSOLUTE_LIMIT_REACHED"))
                .andExpect(jsonPath("$.error.category").value("AUTHENTICATION"))
                .andExpect(jsonPath("$.error.retryable").value(false))
                .andExpect(jsonPath("$.error.messageKey")
                        .value("api.error.authentication.sessionAbsoluteLimitReached"));
    }

    @Test
    void renewClampsExpiryToAbsoluteDeadlineNearTheLimit() throws Exception {
        Instant sessionStartedAt = Instant.now().minus(ABSOLUTE_TTL.minus(Duration.ofMinutes(4)))
                .truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        Instant absoluteDeadline = sessionStartedAt.plus(ABSOLUTE_TTL);
        String nearLimitToken = craftManagementToken(
                UUID.randomUUID().toString(), sessionStartedAt, Instant.now().plusSeconds(180));

        MvcResult result = mockMvc.perform(post("/api/management/v1/auth/renew")
                        .header("Authorization", "Bearer " + nearLimitToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode renew = objectMapper.readTree(result.getResponse().getContentAsString()).path("result");
        assertThat(Instant.parse(renew.path("accessTokenExpiresAt").asText())).isEqualTo(absoluteDeadline);
        assertThat(Instant.parse(renew.path("sessionAbsoluteDeadline").asText())).isEqualTo(absoluteDeadline);
    }

    @Test
    void legacyTokenWithoutSessionClaimsIsRejected() throws Exception {
        SecretKey secretKey = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        String legacyToken = Jwts.builder()
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
                .signWith(secretKey)
                .compact();

        mockMvc.perform(get("/api/management/v1/auth/session")
                        .header("Authorization", "Bearer " + legacyToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("SESSION_EXPIRED"));
    }

    @Test
    void renewForDisabledAccountIsRejected() throws Exception {
        String token = loginResult("10000006", "ChangeMe123!").path("accessToken").asText();
        ManagementUserEntity user = managementUserRepository
                .findByUsernameAndDeletedAtIsNull("10000006").orElseThrow();
        user.disable();
        managementUserRepository.save(user);
        try {
            mockMvc.perform(post("/api/management/v1/auth/renew")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("SESSION_EXPIRED"));
        } finally {
            user.enable();
            managementUserRepository.save(user);
        }
    }

    @Test
    void renewWithoutAuthorizationHeaderIsRejected() throws Exception {
        mockMvc.perform(post("/api/management/v1/auth/renew"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("SESSION_EXPIRED"));
    }

    private JsonNode loginResult(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/management/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("result");
    }

    private Claims decode(String token) {
        SecretKey secretKey = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }

    private String craftManagementToken(String jti, Instant sessionStartedAt, Instant expiresAt) {
        SecretKey secretKey = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .id(jti)
                .subject("10000001")
                .claim("displayName", "Global Admin")
                .claim("email", "global.admin@example.com")
                .claim("authSource", "LOCAL")
                .claim("roles", List.of("GLOBAL_ADMIN"))
                .claim("groups", List.of("*"))
                .claim("defaultRoute", "route.dashboard-home")
                .claim("visibleRoutes", List.of("route.dashboard-home"))
                .claim("sessionStartedAt", sessionStartedAt.getEpochSecond())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }
}
