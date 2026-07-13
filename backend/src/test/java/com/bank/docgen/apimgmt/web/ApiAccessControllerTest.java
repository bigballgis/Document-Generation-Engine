package com.bank.docgen.apimgmt.web;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * BDD-MGMT-UI-D3-006: cross-package alerts must answer 200 on supported paths (not 500).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiAccessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listAlertsOnCanonicalApiAccessPathReturnsOkEnvelope() throws Exception {
        String token = loginAsGlobalAdmin();

        mockMvc.perform(get("/api/management/v1/api-access/alerts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray())
                .andExpect(jsonPath("$.metadata.traceId").isNotEmpty());
    }

    @Test
    void listAlertsOnLegacyApiPoliciesAliasPathReturnsOkEnvelope() throws Exception {
        String token = loginAsGlobalAdmin();

        mockMvc.perform(get("/api/management/v1/api-policies/alerts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray())
                .andExpect(jsonPath("$.metadata.traceId").isNotEmpty());
    }

    @Test
    void readinessSummaryOnCanonicalPathReturnsOkEnvelope() throws Exception {
        String token = loginAsGlobalAdmin();

        mockMvc.perform(get("/api/management/v1/api-access/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.publishedInScopeCount").isNumber())
                .andExpect(jsonPath("$.result.attentionCount").isNumber())
                .andExpect(jsonPath("$.result.pendingReleaseNeedingSetupCount").isNumber())
                .andExpect(jsonPath("$.metadata.traceId").isNotEmpty());
    }

    private String loginAsGlobalAdmin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/management/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"10000001","password":"ChangeMe123!"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("result").path("accessToken").asText();
    }
}
