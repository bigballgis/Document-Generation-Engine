package com.bank.docgen.template.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.bank.docgen.authorization.management.web.ManagementAuthentication;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Peeled from TemplatePlatformSliceTest (AI-SCALE #169).
 */
class TemplatePlatformDatasetContractSliceTest extends TemplatePlatformSliceFixtures {

    @Test
    void testDataSetCrudAndPreviewUsesStoredVariables() throws Exception {
        String templateId = createConfiguredTemplate();

        MvcResult createResult = mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/test-data-sets")
                        .with(authentication(new ManagementAuthentication(templateAuthor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Retail sample",
                                  "description":"Synthetic customer",
                                  "variables":{"customerName":"DatasetCustomer"}
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.name").value("Retail sample"))
                .andReturn();
        String testDataSetId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("result").path("testDataSetId").asText();

        mockMvc.perform(get("/api/management/v1/templates/" + templateId + "/test-data-sets")
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(1));

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/previews/test-generate")
                        .with(authentication(new ManagementAuthentication(templateAuthor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"testDataSetId":"%s"}
                                """.formatted(testDataSetId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.result.testDataSetId").value(testDataSetId));

        mockMvc.perform(get("/api/management/v1/templates/" + templateId + "/test-data-sets")
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].locked").value(true))
                .andExpect(jsonPath("$.result[0].datasetVersion").value(1));

        mockMvc.perform(put("/api/management/v1/templates/" + templateId + "/test-data-sets/" + testDataSetId)
                        .with(authentication(new ManagementAuthentication(templateAuthor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Updated sample",
                                  "variables":{"customerName":"Updated"}
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.messageKey").value("api.error.template.testDataSetLocked"));

        MvcResult deriveResult = mockMvc.perform(
                        post("/api/management/v1/templates/" + templateId + "/test-data-sets/" + testDataSetId + "/derive")
                                .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.locked").value(false))
                .andExpect(jsonPath("$.result.datasetVersion").value(2))
                .andReturn();
        String derivedId = objectMapper.readTree(deriveResult.getResponse().getContentAsString())
                .path("result").path("testDataSetId").asText();

        mockMvc.perform(delete("/api/management/v1/templates/" + templateId + "/test-data-sets/" + testDataSetId)
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/management/v1/templates/" + templateId + "/test-data-sets/" + derivedId)
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isNoContent());
    }
    @Test
    void testDataSetMaintenanceDeniedForTesterRole() throws Exception {
        String templateId = createConfiguredTemplate();

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/test-data-sets")
                        .with(authentication(new ManagementAuthentication(tester)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Tester sample",
                                  "variables":{"customerName":"Tester"}
                                }
                                """))
                .andExpect(status().isForbidden());
    }
    @Test
    void batchTestOverThreeSamplesCreatesSummary() throws Exception {
        String templateId = createConfiguredTemplate();

        String id1 = createTestDataSet(templateId, "Sample A");
        String id2 = createTestDataSet(templateId, "Sample B");
        String id3 = createTestDataSet(templateId, "Sample C");

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/previews/batch-test")
                        .with(authentication(new ManagementAuthentication(templateAuthor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"testDataSetIds":["%s","%s","%s"]}
                                """.formatted(id1, id2, id3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalSamples").value(3))
                .andExpect(jsonPath("$.result.succeededCount").value(3))
                .andExpect(jsonPath("$.result.failedCount").value(0))
                .andExpect(jsonPath("$.result.warningCount").value(0))
                .andExpect(jsonPath("$.result.samples.length()").value(3));
    }
    @Test
    void coverageSummaryReturnsDimensionsWithoutVariablePlaintext() throws Exception {
        String templateId = createConfiguredTemplate();

        mockMvc.perform(get("/api/management/v1/templates/" + templateId + "/coverage")
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.templateId").value(templateId))
                .andExpect(jsonPath("$.result.dimensions.length()").value(3))
                .andExpect(jsonPath("$.result.appliedThreshold.scopeType").value("GLOBAL"));
    }
    @Test
    void managementCallerContractReturnsNonSensitiveContractView() throws Exception {
        String templateId = createPublishedTemplate();
        configureApiAndCredential(templateId);

        mockMvc.perform(get("/api/management/v1/templates/" + templateId + "/api/contract")
                        .param("environment", "dev")
                        .with(authentication(new ManagementAuthentication(groupAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.templateId").value("TPL-RETAIL-LETTER"))
                .andExpect(jsonPath("$.result.paths[0]").value("/api/dev/v1/templates/TPL-RETAIL-LETTER/contract"))
                .andExpect(jsonPath("$.result.callableVersions[0].releaseVersion").value("1.0.0"))
                .andExpect(jsonPath("$.result.errorCodes[?(@.code=='BATCH_LIMIT_EXCEEDED')]").exists())
                .andExpect(jsonPath("$.result.errorCodes[?(@.code=='RATE_LIMIT_EXCEEDED')].retryable").value(true))
                .andExpect(jsonPath("$.result.errorCodes[?(@.code=='REQUEST_BODY_INVALID')].category")
                        .value("VALIDATION"))
                .andExpect(jsonPath("$.result.examples").isArray())
                .andExpect(jsonPath("$.result.examples").isEmpty())
                .andExpect(jsonPath("$.result.apiPolicy.policyVersion").value(3))
                .andExpect(jsonPath("$.result.apiPolicy.updatedAt").exists())
                .andExpect(jsonPath("$.result.apiPolicy.updatedBy").exists())
                .andExpect(jsonPath("$.result.apiPolicy.allowedOutputFormats[0]").value("DOCX"))
                .andExpect(jsonPath("$.result.apiPolicy.batchLimits.syncMaxItems").exists())
                .andExpect(jsonPath("$.result.apiPolicy.adGroupAuthorizationSummary.authorizationScopeSummary").exists())
                .andExpect(jsonPath("$.result.apiPolicy.credentialSummary.status").value("ACTIVE"))
                .andExpect(jsonPath("$.result.apiPolicy.credentialSummary.credentialId").exists())
                .andExpect(jsonPath("$.result.apiPolicy.credentialSummary.credentialExternalId").doesNotExist())
                .andExpect(jsonPath("$.result.defaultRoute.updatedAt").exists())
                .andExpect(jsonPath("$.result.defaultRoute.updatedBy").exists());
    }
    @Test
    void runtimeCallerContractExcludesManagementDetail() throws Exception {
        String templateId = createPublishedTemplate();
        CredentialBundle credential = configureApiAndCredential(templateId);

        mockMvc.perform(get("/api/dev/v1/templates/TPL-RETAIL-LETTER/contract")
                        .header("X-Api-Credential-Id", credential.externalId())
                        .header("X-Api-Credential-Secret", credential.secret())
                        .header("X-Access-Account", "svc-caller"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.apiPolicy.policyVersion").value(3))
                .andExpect(jsonPath("$.result.apiPolicy.allowedOutputFormats[0]").value("DOCX"))
                .andExpect(jsonPath("$.result.apiPolicy.updatedAt").doesNotExist())
                .andExpect(jsonPath("$.result.apiPolicy.updatedBy").doesNotExist())
                .andExpect(jsonPath("$.result.apiPolicy.adGroupAuthorizationSummary.authorizationScopeSummary")
                        .doesNotExist())
                .andExpect(jsonPath("$.result.apiPolicy.credentialSummary.status").value("ACTIVE"))
                .andExpect(jsonPath("$.result.apiPolicy.credentialSummary.credentialId").doesNotExist())
                .andExpect(jsonPath("$.result.apiPolicy.credentialSummary.credentialExternalId").doesNotExist())
                .andExpect(jsonPath("$.result.apiPolicy.credentialSummary.fingerprintSummary").doesNotExist())
                .andExpect(jsonPath("$.result.defaultRoute.updatedAt").doesNotExist())
                .andExpect(jsonPath("$.result.defaultRoute.updatedBy").doesNotExist());
    }
}
