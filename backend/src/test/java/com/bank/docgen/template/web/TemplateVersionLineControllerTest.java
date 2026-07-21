package com.bank.docgen.template.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.docgen.authorization.management.web.ManagementAuthentication;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class TemplateVersionLineControllerTest extends TemplateManagementWebTestSupport {

    @Test
    void listVersionLinesIncludesInFlightDevAndPublishedReleaseRows() throws Exception {
        String templateId = createPublishedTemplate();
        String clonedDevVersionId = cloneRelease(templateId, "1.0.0");

        mockMvc.perform(get("/api/management/v1/templates/" + templateId + "/version-lines")
                        .param("page", "0")
                        .param("size", "20")
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(2))
                .andExpect(jsonPath("$.result.content.length()").value(2))
                .andExpect(jsonPath("$.result.content[0].devVersionId").value(clonedDevVersionId))
                .andExpect(jsonPath("$.result.content[0].devVersionNumber").value(2))
                .andExpect(jsonPath("$.result.content[0].releaseVersion").isEmpty())
                .andExpect(jsonPath("$.result.content[0].lifecycleStatus").value("DRAFT"))
                .andExpect(jsonPath("$.result.content[0].lineKind").value("IN_FLIGHT"))
                .andExpect(jsonPath("$.result.content[1].devVersionNumber").value(1))
                .andExpect(jsonPath("$.result.content[1].releaseVersion").value("1.0.0"))
                .andExpect(jsonPath("$.result.content[1].lifecycleStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.result.content[1].lineKind").value("PUBLISHED"))
                .andExpect(jsonPath("$.result.content[1].defaultRouteTarget").value(true))
                .andExpect(jsonPath("$.result.content[1].cloneable").value(false));
    }

    @Test
    void getVersionLineDetailReturnsVariablesBindingsAndRules() throws Exception {
        String templateId = createPublishedTemplate();
        String publishedDevVersionId = publishedDevVersionId(templateId);

        mockMvc.perform(get("/api/management/v1/templates/" + templateId + "/version-lines/" + publishedDevVersionId)
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.devVersionId").value(publishedDevVersionId))
                .andExpect(jsonPath("$.result.releaseVersion").value("1.0.0"))
                .andExpect(jsonPath("$.result.lineKind").value("PUBLISHED"))
                .andExpect(jsonPath("$.result.variables[0].variableKey").value("customerName"))
                .andExpect(jsonPath("$.result.bindings[0].anchorId").value("HEADER"))
                .andExpect(jsonPath("$.result.masterPin.masterRevisionId").isNotEmpty())
                .andExpect(jsonPath("$.result.masterPin.masterFileHash").isNotEmpty())
                .andExpect(jsonPath("$.result.masterPin.pinOrigin").value("PUBLISHED"));
    }

    @Test
    void getReleaseDetailReturnsVariablesBindingsAndRulesReadOnly() throws Exception {
        String templateId = createPublishedTemplate();

        mockMvc.perform(get("/api/management/v1/templates/" + templateId + "/releases/1.0.0")
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.releaseVersion").value("1.0.0"))
                .andExpect(jsonPath("$.result.readOnly").value(true))
                .andExpect(jsonPath("$.result.variables[0].variableKey").value("customerName"))
                .andExpect(jsonPath("$.result.bindings[0].anchorId").value("HEADER"))
                .andExpect(jsonPath("$.result.masterPin.masterRevisionId").isNotEmpty())
                .andExpect(jsonPath("$.result.masterPin.masterFileHash").isNotEmpty())
                .andExpect(jsonPath("$.result.masterPin.pinOrigin").value("PUBLISHED"));
    }

    @Test
    void getVersionLineDetailOmitsMasterPinForInFlightUnpinnedLine() throws Exception {
        String templateId = createPublishedTemplate();
        String clonedDevVersionId = cloneRelease(templateId, "1.0.0");

        mockMvc.perform(get("/api/management/v1/templates/" + templateId + "/version-lines/" + clonedDevVersionId)
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.lineKind").value("IN_FLIGHT"))
                .andExpect(jsonPath("$.result.masterPin").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void getReleasePublishGateReturnsChecklistWithoutInFlightDev() throws Exception {
        String templateId = createPublishedTemplate();

        mockMvc.perform(get("/api/management/v1/templates/" + templateId + "/releases/1.0.0/publish-gate")
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.templateId").value(templateId))
                .andExpect(jsonPath("$.result.items").isArray())
                .andExpect(jsonPath("$.result.items.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.result.items[0].checkCode").exists())
                .andExpect(jsonPath("$.result.items[0].messageKey").exists());
    }

    @Test
    void clonePublishedReleaseCreatesNewDraftDevLine() throws Exception {
        String templateId = createPublishedTemplate();

        MvcResult result = mockMvc.perform(post("/api/management/v1/templates/" + templateId
                        + "/release-versions/1.0.0/clone")
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.devVersionNumber").value(2))
                .andReturn();
        String newDevVersionId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("result").path("devVersionId").asText();

        mockMvc.perform(get("/api/management/v1/templates/" + templateId + "/dev/" + newDevVersionId)
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.lifecycleStatus").value("DRAFT"))
                .andExpect(jsonPath("$.result.devVersionNumber").value(2))
                .andExpect(jsonPath("$.result.devVersionId").value(newDevVersionId))
                .andExpect(jsonPath("$.result.readOnly").value(false));
    }

    @Test
    void mutationOnPublishedOnlyTemplateReturnsVersionImmutable() throws Exception {
        String templateId = createPublishedTemplate();

        mockMvc.perform(put("/api/management/v1/templates/" + templateId + "/variables/customerName")
                        .with(authentication(new ManagementAuthentication(templateAuthor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "variableKey":"customerName",
                                  "variableType":"TEXT",
                                  "required":true,
                                  "defaultValue":"Updated",
                                  "description":"Customer name"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("TEMPLATE_VERSION_IMMUTABLE"))
                .andExpect(jsonPath("$.error.messageKey").value("api.error.template.versionImmutable"));
    }

    @Test
    void versionLinesAreGroupScoped() throws Exception {
        String masterId = uploadAndApproveMaster();
        String templateId = createTemplate(masterId);
        ManagementSessionClaims corpOnlyAuthor = session("10000004", List.of("DOCUMENT_AUTHOR"), List.of("CORP"));

        mockMvc.perform(get("/api/management/v1/templates/" + templateId + "/version-lines")
                        .with(authentication(new ManagementAuthentication(corpOnlyAuthor))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/release-versions/1.0.0/clone")
                        .with(authentication(new ManagementAuthentication(corpOnlyAuthor))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void abandonInFlightDev_softDeletesDevLine_resetsTemplateToPublished_allowsClone() throws Exception {
        String templateId = createPublishedTemplate();
        String clonedDevVersionId = cloneRelease(templateId, "1.0.0");

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/dev/" + clonedDevVersionId + "/abandon")
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.lifecycleStatus").value("PUBLISHED"));
        mockMvc.perform(get("/api/management/v1/templates/" + templateId + "/version-lines")
                        .param("page", "0")
                        .param("size", "20")
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(1))
                .andExpect(jsonPath("$.result.content[0].releaseVersion").value("1.0.0"))
                .andExpect(jsonPath("$.result.content[0].lineKind").value("PUBLISHED"));
        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/release-versions/1.0.0/clone")
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isCreated());
    }

    @Test
    void cloneBlockedWhileInFlightDevExists() throws Exception {
        String templateId = createPublishedTemplate();
        cloneRelease(templateId, "1.0.0");

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/release-versions/1.0.0/clone")
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("TEMPLATE_DEV_LINE_IN_FLIGHT"))
                .andExpect(jsonPath("$.error.messageKey").value("api.error.template.devLineInFlight"));
    }

    private String cloneRelease(String templateId, String releaseVersion) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/management/v1/templates/" + templateId
                        + "/release-versions/" + releaseVersion + "/clone")
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("result").path("devVersionId").asText();
    }

    private String publishedDevVersionId(String templateId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/management/v1/templates/" + templateId + "/version-lines")
                        .param("page", "0")
                        .param("size", "20")
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("result").path("content");
        for (JsonNode row : content) {
            if ("1.0.0".equals(row.path("releaseVersion").asText(null))) {
                return row.path("devVersionId").asText();
            }
        }
        throw new IllegalStateException("Published version line not found");
    }
}
