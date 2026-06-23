package com.bank.docgen.authorization.management.web;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.docgen.authorization.management.domain.ManagementRoute;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ManagementAuthControllerTest {

    private static final String JWT_SECRET = "test-jwt-secret-at-least-32-bytes-long!!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginSucceedsForSeededGlobalAdmin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/management/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"10000001","password":"ChangeMe123!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.result.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.result.session.username").value("10000001"))
                .andExpect(jsonPath("$.result.session.defaultRoute")
                        .value(ManagementRoute.DASHBOARD_HOME.routeKey()))
                .andReturn();

        String token = readAccessToken(result);
        mockMvc.perform(get("/api/management/v1/auth/session")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.username").value("10000001"))
                .andExpect(jsonPath("$.result.visibleRoutes").isArray());
    }

    @Test
    void loginFailsForWrongPassword() throws Exception {
        mockMvc.perform(post("/api/management/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"10000001","password":"WrongPassword!"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.metadata.auditId").isNotEmpty())
                .andExpect(jsonPath("$.metadata.traceId").isNotEmpty());
    }

    @Test
    void sessionRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/management/v1/auth/session"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("SESSION_EXPIRED"));
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        SecretKey secretKey = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("10000001")
                .claim("displayName", "Global Admin")
                .claim("email", "global.admin@example.com")
                .claim("authSource", "LOCAL")
                .claim("roles", List.of("GLOBAL_ADMIN"))
                .claim("groups", List.of("*"))
                .claim("defaultRoute", ManagementRoute.GLOBAL_GOVERNANCE_HOME.routeKey())
                .claim("visibleRoutes", List.of(ManagementRoute.GLOBAL_GOVERNANCE_HOME.routeKey()))
                .issuedAt(Date.from(Instant.now().minusSeconds(7200)))
                .expiration(Date.from(Instant.now().minusSeconds(3600)))
                .signWith(secretKey)
                .compact();

        mockMvc.perform(get("/api/management/v1/auth/session")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("SESSION_EXPIRED"));
    }

    @Test
    void logoutAcceptsAuthenticatedSession() throws Exception {
        String token = loginAndGetToken("10000002", "ChangeMe123!");

        mockMvc.perform(post("/api/management/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void templateAuthorDefaultRouteMatchesProductBaseline() throws Exception {
        mockMvc.perform(post("/api/management/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"10000003","password":"ChangeMe123!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.session.defaultRoute")
                        .value(ManagementRoute.DASHBOARD_HOME.routeKey()))
                .andExpect(jsonPath("$.result.session.visibleRoutes.length()").value(2));
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/management/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return readAccessToken(result);
    }

    private String readAccessToken(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("result").path("accessToken").asText();
    }
}
