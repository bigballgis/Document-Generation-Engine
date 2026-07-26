package com.bank.docgen.template.web;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.bank.docgen.authorization.management.web.ManagementAuthentication;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Peeled from TemplatePlatformSliceTest (AI-SCALE #169).
 */
class TemplatePlatformLifecycleSliceTest extends TemplatePlatformSliceFixtures {

    @Test
    void testFailDecisionWithoutStructuredFieldsReturns422() throws Exception {
        String templateId = createConfiguredTemplate();

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/lifecycle/submit-test")
                        .with(authentication(new ManagementAuthentication(templateAuthor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"commentSummary":"Ready for test"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/lifecycle/test-decision")
                        .with(authentication(new ManagementAuthentication(tester)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"FAILED","commentSummary":"Binding issues"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("TEMPLATE_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.messageKey").value("api.error.template.decisionReasonCategoryRequired"));
    }
    @Test
    void fullTemplateLifecyclePreviewAndRuntimeGeneration() throws Exception {
        String templateId = createConfiguredTemplate();
        String previewId = testGenerate(templateId);
        mockMvc.perform(get("/api/management/v1/templates/" + templateId + "/previews")
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].previewId").value(previewId))
                .andExpect(jsonPath("$.result[0].docxAvailable").value(true))
                .andExpect(jsonPath("$.result[0].pdfAvailable").value(true));
        mockMvc.perform(get("/api/management/v1/templates/" + templateId + "/previews/" + previewId)
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.result.pdfArtifactStorageKey").isNotEmpty())
                .andExpect(jsonPath("$.result.fidelityWarnings").isEmpty());
        mockMvc.perform(get("/api/management/v1/templates/" + templateId + "/previews/" + previewId + "/artifacts/docx")
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".docx")));
        mockMvc.perform(get("/api/management/v1/templates/" + templateId + "/previews/" + previewId + "/artifacts/pdf")
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".pdf")));

        runLifecycle(templateId);
        CredentialBundle credential = configureApiAndCredential(templateId);
        runtimeGenerate(credential);
    }
    @Test
    void preview_emitsRealWarnings_fromValidationEngine() throws Exception {
        String templateId = createDraftTemplate();
        configureTemplateWithImageScalingBinding(templateId);

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/previews/test-generate")
                        .with(authentication(new ManagementAuthentication(templateAuthor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"variables":{"customerName":"Alice"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.result.fidelityWarnings[0].code").value("IMAGE_SCALING_ADJUSTED"))
                .andExpect(jsonPath("$.result.fidelityWarnings[0].messageKey")
                        .value("generation.warning.fidelity.imageScalingAdjusted"))
                .andExpect(jsonPath("$.result.fidelityWarnings[0].artifact").value("HEADER"));
    }
    @Test
    void runtimeSuccess_includesFidelityWarnings() throws Exception {
        String templateId = createDraftTemplate();
        configureTemplateWithImageScalingBinding(templateId);
        runLifecycle(templateId);
        CredentialBundle credential = configureApiAndCredential(templateId);

        mockMvc.perform(post("/api/dev/v1/templates/TPL-RETAIL-LETTER/versions/1.0.0/generate")
                        .header("X-Api-Credential-Id", credential.externalId())
                        .header("X-Api-Credential-Secret", credential.secret())
                        .header("X-Access-Account", "svc-caller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generateBody("idem-fidelity-warn-1")))
                .andExpect(status().isOk())
                .andExpect(header().string("fidelityWarningCodes", "IMAGE_SCALING_ADJUSTED"))
                .andExpect(header().string("fidelityWarningCount", "1"));
    }
    @Test
    void noHardcodedWarning_whenContentClean() throws Exception {
        String templateId = createConfiguredTemplate();

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/previews/test-generate")
                        .with(authentication(new ManagementAuthentication(templateAuthor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"variables":{"customerName":"Alice"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.fidelityWarnings").isEmpty());
    }
    @Test
    void structuredAuthoring_exposesMasterStyleCatalogAndPasteClean() throws Exception {
        String templateId = createDraftTemplate();

        mockMvc.perform(get("/api/management/v1/templates/" + templateId + "/master-style-catalog")
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.catalogVersion").value(org.hamcrest.Matchers.startsWith("master-styles-")))
                .andExpect(jsonPath("$.result.entries[?(@.styleKey == 'BodyText')]").exists());

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/paste-clean")
                        .with(authentication(new ManagementAuthentication(templateAuthor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceHtml":"<p>Hello</p>",
                                  "prePasteStructuredContentJson":"{\\"schemaVersion\\":\\"1.0\\",\\"nodes\\":[]}"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.blocked").value(false))
                .andExpect(jsonPath("$.result.summary.transformedCount").value(1))
                .andExpect(jsonPath("$.result.cleanedStructuredContentJson").exists());

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/paste-clean")
                        .with(authentication(new ManagementAuthentication(templateAuthor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceHtml":"<script>alert(1)</script>",
                                  "prePasteStructuredContentJson":"{\\"schemaVersion\\":\\"1.0\\",\\"nodes\\":[]}"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.blocked").value(true))
                .andExpect(jsonPath("$.result.summary.blockedCount").value(1));
    }
    @Test
    void syncGenerateCompletesWithinBaselineBudget() throws Exception {
        Instant start = Instant.now();
        fullTemplateLifecyclePreviewAndRuntimeGeneration();
        Duration elapsed = Duration.between(start, Instant.now());
        assertTrue(
                elapsed.compareTo(Duration.ofSeconds(30)) <= 0,
                "Sync generate baseline exceeded 30s budget: " + elapsed.toMillis() + "ms"
        );
    }
    @Test
    void rejectsTemplateCreationFromUnapprovedMaster() throws Exception {
        String masterId = uploadMaster(groupAdmin);
        mockMvc.perform(post("/api/management/v1/templates")
                        .with(authentication(new ManagementAuthentication(templateAuthor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalId":"TPL-REJECT",
                                  "groupCode":"RETAIL",
                                  "name":"Reject Template",
                                  "masterId":"%s",
                                  "locale":"zh-CN"
                                }
                                """.formatted(masterId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("TEMPLATE_VALIDATION_FAILED"));
    }
    @Test
    void savesCompositionRulesAndReturnsThemOnTemplateDetail() throws Exception {
        String templateId = createDraftTemplate();

        mockMvc.perform(put("/api/management/v1/templates/" + templateId + "/rules")
                        .with(authentication(new ManagementAuthentication(templateAuthor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rules":[
                                    {
                                      "ruleId":"rule-1",
                                      "conditionExpression":"${customerName} != null",
                                      "targetAnchorId":"HEADER",
                                      "trueBranchRuleId":"",
                                      "falseBranchRuleId":""
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].ruleId").value("rule-1"));

        mockMvc.perform(get("/api/management/v1/templates/" + templateId)
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.rules[0].targetAnchorId").value("HEADER"));
    }
    @Test
    void stopRestoreAndDeprecatePublishedTemplate() throws Exception {
        String templateId = createPublishedTemplate();
        CredentialBundle credential = configureApiAndCredential(templateId);

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/lifecycle/stop")
                        .with(authentication(new ManagementAuthentication(groupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Maintenance window","confirmed":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.lifecycleStatus").value("STOPPED"));

        mockMvc.perform(post("/api/dev/v1/templates/TPL-RETAIL-LETTER/versions/1.0.0/generate")
                        .header("X-Api-Credential-Id", credential.externalId())
                        .header("X-Api-Credential-Secret", credential.secret())
                        .header("X-Access-Account", "svc-caller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generateBody("idem-stopped-1")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.messageKey").value("api.error.runtime.versionNotCallable"));

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/lifecycle/restore")
                        .with(authentication(new ManagementAuthentication(groupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Restore service","confirmed":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.lifecycleStatus").value("PUBLISHED"));

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/lifecycle/stop")
                        .with(authentication(new ManagementAuthentication(groupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Maintenance window","confirmed":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.lifecycleStatus").value("STOPPED"));

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/lifecycle/deprecate")
                        .with(authentication(new ManagementAuthentication(groupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"End of life","confirmed":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.lifecycleStatus").value("DEPRECATED"));
    }
    @Test
    void deactivateVersionBlocksRuntimeWhileTemplateStaysPublished() throws Exception {
        String templateId = createPublishedTemplate();
        CredentialBundle credential = configureApiAndCredential(templateId);

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/versions/1.0.0/deactivate")
                        .with(authentication(new ManagementAuthentication(groupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Cannot deactivate default route target","confirmed":true}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.messageKey")
                        .value("api.error.template.defaultRouteTargetCannotDeactivate"));

        mockMvc.perform(post("/api/dev/v1/templates/TPL-RETAIL-LETTER/versions/1.0.0/generate")
                        .header("X-Api-Credential-Id", credential.externalId())
                        .header("X-Api-Credential-Secret", credential.secret())
                        .header("X-Access-Account", "svc-caller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generateBody("idem-version-stopped-1")))
                .andExpect(status().isOk())
                .andExpect(header().string("documentId", org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyString())));
    }
    @Test
    void patchMetadataUpdatesDraftTemplate() throws Exception {
        String templateId = createDraftTemplate();

        mockMvc.perform(patch("/api/management/v1/templates/" + templateId)
                        .with(authentication(new ManagementAuthentication(templateAuthor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated Letter","description":"Updated description"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("Updated Letter"))
                .andExpect(jsonPath("$.result.description").value("Updated description"));
    }
    @Test
    void deleteTemplateLogicalDeleteExcludesTemplateFromList() throws Exception {
        String templateId = createDraftTemplate();

        mockMvc.perform(delete("/api/management/v1/templates/" + templateId)
                        .with(authentication(new ManagementAuthentication(globalAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Template retired","confirmed":true}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/management/v1/templates")
                        .with(authentication(new ManagementAuthentication(globalAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[?(@.id=='" + templateId + "')]").isEmpty());
    }
}
