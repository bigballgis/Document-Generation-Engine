package com.bank.docgen.legalhold.web;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * IBL-D5 / F23 — management API create / release / fail-closed paths (CE-G04-001…005, C20…C22).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LegalHoldControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createInvocationSet_release_andBlockDoubleRelease() throws Exception {
        String token = login("10000001");

        MvcResult created = mockMvc.perform(post("/api/management/v1/legal-holds")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "scopeType": "INVOCATION_SET",
                                  "reason": "ibl-d5-depth",
                                  "invocationExternalIds": ["INV-D5-A", "INV-D5-B"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.status").value("ACTIVE"))
                .andExpect(jsonPath("$.result.scopeType").value("INVOCATION_SET"))
                .andExpect(jsonPath("$.result.invocationCount").value(2))
                .andExpect(jsonPath("$.metadata.traceId").isNotEmpty())
                .andReturn();

        String holdId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("result")
                .path("id")
                .asText();

        mockMvc.perform(get("/api/management/v1/legal-holds/" + holdId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(holdId))
                .andExpect(jsonPath("$.result.invocationExternalIds[0]").exists());

        mockMvc.perform(post("/api/management/v1/legal-holds/" + holdId + "/release")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("RELEASED"))
                .andExpect(jsonPath("$.result.releasedByUsername").value("10000001"));

        mockMvc.perform(post("/api/management/v1/legal-holds/" + holdId + "/release")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("LEGAL_HOLD_ALREADY_RELEASED"));
    }

    @Test
    void create_mixedScope_returns422() throws Exception {
        String token = login("10000001");

        mockMvc.perform(post("/api/management/v1/legal-holds")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "scopeType": "TEMPLATE_WINDOW",
                                  "templateId": "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee",
                                  "effectiveFrom": "2026-01-01T00:00:00Z",
                                  "invocationExternalIds": ["INV-1"]
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("REQUEST_BODY_INVALID"));
    }

    @Test
    void create_emptyInvocationSet_returns422() throws Exception {
        String token = login("10000001");

        mockMvc.perform(post("/api/management/v1/legal-holds")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "scopeType": "INVOCATION_SET",
                                  "invocationExternalIds": []
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void groupAdmin_list_returns403() throws Exception {
        String token = login("10000002");

        mockMvc.perform(get("/api/management/v1/legal-holds")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void unauthenticated_create_returns401() throws Exception {
        mockMvc.perform(post("/api/management/v1/legal-holds")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "scopeType": "INVOCATION_SET",
                                  "invocationExternalIds": ["INV-1"]
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void get_unknownHold_returns404() throws Exception {
        String token = login("10000001");

        mockMvc.perform(get("/api/management/v1/legal-holds/aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("LEGAL_HOLD_NOT_FOUND"));
    }

    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/management/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"ChangeMe123!"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("result").path("accessToken").asText();
    }
}
