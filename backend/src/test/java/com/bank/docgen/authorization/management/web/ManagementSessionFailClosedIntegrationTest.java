package com.bank.docgen.authorization.management.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.docgen.authorization.management.session.SessionRevocationStore;
import com.bank.docgen.authorization.management.session.SessionRevocationUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

/**
 * SCEN-FAILCLOSED-01 + boundary B6: a revocation-list outage rejects every bearer request
 * (401 SESSION_VALIDATION_UNAVAILABLE) and fails logout writes with a retryable 503.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ManagementSessionFailClosedIntegrationTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SessionRevocationStore sessionRevocationStore;

    @Test
    void revocationOutageFailsClosedOnEveryBearerRequest() throws Exception {
        String token = login();
        when(sessionRevocationStore.isRevoked(anyString()))
                .thenThrow(new SessionRevocationUnavailableException(new RuntimeException("redis down")));

        mockMvc.perform(get("/api/management/v1/auth/session")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("SESSION_VALIDATION_UNAVAILABLE"))
                .andExpect(jsonPath("$.error.category").value("AUTHENTICATION"))
                .andExpect(jsonPath("$.error.retryable").value(true))
                .andExpect(jsonPath("$.error.messageKey")
                        .value("api.error.authentication.sessionValidationUnavailable"))
                .andExpect(jsonPath("$.metadata.traceId").isNotEmpty());

        mockMvc.perform(post("/api/management/v1/auth/renew")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("SESSION_VALIDATION_UNAVAILABLE"));
    }

    @Test
    void logoutAnswers503WhenRevocationWriteFails() throws Exception {
        String token = login();
        when(sessionRevocationStore.isRevoked(anyString())).thenReturn(false);
        doThrow(new SessionRevocationUnavailableException(new RuntimeException("redis down")))
                .when(sessionRevocationStore).revoke(anyString(), any(Instant.class));

        mockMvc.perform(post("/api/management/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("SESSION_VALIDATION_UNAVAILABLE"))
                .andExpect(jsonPath("$.error.retryable").value(true));
    }

    @Test
    void revokedTokenIsRejectedEndToEnd() throws Exception {
        String token = login();
        when(sessionRevocationStore.isRevoked(anyString())).thenReturn(true);

        mockMvc.perform(get("/api/management/v1/auth/session")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("SESSION_REVOKED"));
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/management/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"10000001","password":"ChangeMe123!"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("result").path("accessToken").asText();
    }
}
