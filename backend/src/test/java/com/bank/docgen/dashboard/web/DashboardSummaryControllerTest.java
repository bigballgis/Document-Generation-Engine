package com.bank.docgen.dashboard.web;

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
 * BDD-PRR-D01C-002 / 008 / 009 — Dashboard summary envelope + auth fail-closed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardSummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void summary_authenticated_returnsBucketCountsEnvelope() throws Exception {
        String token = loginAsGlobalAdmin();

        mockMvc.perform(get("/api/management/v1/dashboard/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.masterPendingReview").isNumber())
                .andExpect(jsonPath("$.result.masterVersionsInProgress").isNumber())
                .andExpect(jsonPath("$.result.templateVersionsInWorkflow").isNumber())
                .andExpect(jsonPath("$.result.publishedVersions").isNumber())
                .andExpect(jsonPath("$.result.stoppedVersions").isNumber())
                .andExpect(jsonPath("$.result.catalogMasters").isNumber())
                .andExpect(jsonPath("$.result.catalogTemplates").isNumber())
                .andExpect(jsonPath("$.metadata.traceId").isNotEmpty());
    }

    @Test
    void summary_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/management/v1/dashboard/summary"))
                .andExpect(status().isUnauthorized());
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
