package com.bank.docgen.documentbrand.web;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * BDD-SYS-NORM-D1-010 — LegalEntity / defaultLegalEntity management APIs fail-closed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LegalEntityRetiredSurfaceWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void list_returns404SurfaceRetired_bddD1010() throws Exception {
        String token = login("10000001");

        mockMvc.perform(get("/api/management/v1/legal-entities")
                        .param("groupCode", "RETAIL")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("LEGAL_ENTITY_SURFACE_RETIRED"))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    void create_returns404SurfaceRetired_bddD1010() throws Exception {
        String token = login("10000001");

        mockMvc.perform(post("/api/management/v1/legal-entities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "groupCode": "RETAIL",
                                  "legalEntityCode": "LE-HK-001",
                                  "displayName": "HK Entity",
                                  "documentBrandCode": "HK-RETAIL-LETTER"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("LEGAL_ENTITY_SURFACE_RETIRED"));
    }

    @Test
    void get_returns404SurfaceRetired_bddD1010() throws Exception {
        String token = login("10000001");

        mockMvc.perform(get("/api/management/v1/legal-entities/LE-HK-001")
                        .param("groupCode", "RETAIL")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("LEGAL_ENTITY_SURFACE_RETIRED"));
    }

    @Test
    void update_returns404SurfaceRetired_bddD1010() throws Exception {
        String token = login("10000001");

        mockMvc.perform(put("/api/management/v1/legal-entities/LE-HK-001")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "groupCode": "RETAIL",
                                  "displayName": "Renamed"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("LEGAL_ENTITY_SURFACE_RETIRED"));
    }

    @Test
    void getDefaultLegalEntity_returns404SurfaceRetired_bddD1010() throws Exception {
        String token = login("10000001");

        mockMvc.perform(get("/api/management/v1/groups/RETAIL/default-legal-entity")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("LEGAL_ENTITY_SURFACE_RETIRED"));
    }

    @Test
    void putDefaultLegalEntity_returns404SurfaceRetired_bddD1010() throws Exception {
        String token = login("10000001");

        mockMvc.perform(put("/api/management/v1/groups/RETAIL/default-legal-entity")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"defaultLegalEntityCode":"LE-HK-001"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("LEGAL_ENTITY_SURFACE_RETIRED"));
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
