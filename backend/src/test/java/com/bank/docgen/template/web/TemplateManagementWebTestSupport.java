package com.bank.docgen.template.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.web.ManagementAuthentication;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TestDataSetRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * CQ-05 shared fixtures for template management web slice tests:
 * retail sample DOCX, role sessions, and common master/template lifecycle arrange helpers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class TemplateManagementWebTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected MasterDocumentRepository masterDocumentRepository;

    @Autowired
    protected TemplateRepository templateRepository;

    @Autowired
    protected TestDataSetRepository testDataSetRepository;

    protected byte[] sampleDocx;
    protected ManagementSessionClaims groupAdmin;
    protected ManagementSessionClaims templateAuthor;
    protected ManagementSessionClaims globalAdmin;
    protected ManagementSessionClaims tester;
    protected ManagementSessionClaims approver;

    @BeforeEach
    void setUpTemplateManagementWebSupport() throws Exception {
        resetTemplateRepositories();
        sampleDocx = buildSampleDocx("Dear {{anchor:HEADER}} customer");
        groupAdmin = session("10000002", List.of("GROUP_ADMIN"), List.of("RETAIL", "CORP"));
        templateAuthor = session("10000003", List.of("TEMPLATE_AUTHOR"), List.of("RETAIL"));
        globalAdmin = session("10000001", List.of("GLOBAL_ADMIN"), List.of("*"));
        tester = session("10000006", List.of("TEMPLATE_TESTER"), List.of("RETAIL"));
        approver = session("10000007", List.of("TEMPLATE_APPROVER"), List.of("RETAIL"));
    }

    protected void resetTemplateRepositories() {
        testDataSetRepository.deleteAll();
        templateRepository.deleteAll();
        masterDocumentRepository.deleteAll();
    }

    protected String createDraftTemplate() throws Exception {
        return createTemplate(uploadAndApproveMaster());
    }

    protected String createConfiguredTemplate() throws Exception {
        String templateId = createDraftTemplate();
        configureTemplate(templateId);
        return templateId;
    }

    protected String createPublishedTemplate() throws Exception {
        String templateId = createConfiguredTemplate();
        runLifecycle(templateId);
        return templateId;
    }

    protected String uploadAndApproveMaster() throws Exception {
        String masterId = uploadMaster(groupAdmin);
        mockMvc.perform(post("/api/management/v1/masters/" + masterId + "/submit-review")
                        .with(authentication(new ManagementAuthentication(groupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"changeSummary":"Initial"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/management/v1/masters/" + masterId + "/review")
                        .with(authentication(new ManagementAuthentication(globalAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"APPROVED","commentSummary":"Approved"}
                                """))
                .andExpect(status().isOk());
        return masterId;
    }

    protected String uploadMaster(ManagementSessionClaims session) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "master.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                sampleDocx
        );
        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/management/v1/masters")
                        .file(file)
                        .param("groupCode", "RETAIL")
                        .param("name", "Retail Master")
                        .with(authentication(new ManagementAuthentication(session))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("result").path("id").asText();
    }

    protected String createTemplate(String masterId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/management/v1/templates")
                        .with(authentication(new ManagementAuthentication(templateAuthor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalId":"TPL-RETAIL-LETTER",
                                  "groupCode":"RETAIL",
                                  "name":"Retail Letter",
                                  "description":"Slice template",
                                  "masterId":"%s",
                                  "locale":"zh-CN"
                                }
                                """.formatted(masterId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.lifecycleStatus").value("DRAFT"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("result").path("id").asText();
    }

    protected void configureTemplate(String templateId) throws Exception {
        mockMvc.perform(put("/api/management/v1/templates/" + templateId + "/variables/customerName")
                        .with(authentication(new ManagementAuthentication(templateAuthor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "variableKey":"customerName",
                                  "variableType":"TEXT",
                                  "required":true,
                                  "defaultValue":"Customer",
                                  "description":"Customer name"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.variableType").value("TEXT"));

        mockMvc.perform(put("/api/management/v1/templates/" + templateId + "/bindings/HEADER")
                        .with(authentication(new ManagementAuthentication(templateAuthor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "anchorId":"HEADER",
                                  "declaredContentType":"TEXT",
                                  "structuredContentJson":"{\\"nodes\\":[{\\"type\\":\\"paragraph\\",\\"children\\":[{\\"type\\":\\"variable\\",\\"key\\":\\"customerName\\"}]}]}"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.validationStatus").value("VALID"));

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/bindings/validate")
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.summary.blocking").value(false));
    }

    protected String testGenerate(String templateId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/previews/test-generate")
                        .with(authentication(new ManagementAuthentication(templateAuthor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"variables":{"customerName":"Alice"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("SUCCEEDED"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("result").path("previewId").asText();
    }

    protected void markAllFidelityWarningsViewed(String templateId, String previewId) throws Exception {
        MvcResult previewResult = mockMvc.perform(get("/api/management/v1/templates/" + templateId + "/previews/" + previewId)
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode warnings = objectMapper.readTree(previewResult.getResponse().getContentAsString())
                .path("result")
                .path("fidelityWarnings");
        for (int index = 0; index < warnings.size(); index++) {
            mockMvc.perform(put("/api/management/v1/templates/" + templateId + "/previews/" + previewId
                            + "/fidelity-warnings/viewed")
                            .with(authentication(new ManagementAuthentication(templateAuthor)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"warningIndex":%d}
                                    """.formatted(index)))
                    .andExpect(status().isOk());
        }
    }

    protected void acknowledgeLatestPreviewFidelityWarnings(String templateId) throws Exception {
        MvcResult listResult = mockMvc.perform(get("/api/management/v1/templates/" + templateId + "/previews")
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode previews = objectMapper.readTree(listResult.getResponse().getContentAsString()).path("result");
        for (JsonNode preview : previews) {
            if ("SUCCEEDED".equals(preview.path("status").asText())) {
                markAllFidelityWarningsViewed(templateId, preview.path("previewId").asText());
                return;
            }
        }
    }

    protected void runLifecycle(String templateId) throws Exception {
        runLifecycle(templateId, "1.0.0", true);
    }

    protected void runLifecycle(String templateId, String releaseVersion, boolean configurePolicyBeforePublish)
            throws Exception {
        String requiredSampleId = createRequiredTestDataSet(templateId);
        String previewId = testGenerate(templateId);
        markAllFidelityWarningsViewed(templateId, previewId);
        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/previews/batch-test")
                        .with(authentication(new ManagementAuthentication(templateAuthor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"testDataSetIds":["%s"]}
                                """.formatted(requiredSampleId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/lifecycle/submit-test")
                        .with(authentication(new ManagementAuthentication(templateAuthor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"commentSummary":"Ready for test"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.lifecycleStatus").value("TESTING"));

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/lifecycle/test-decision")
                        .with(authentication(new ManagementAuthentication(tester)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision":"PASSED",
                                  "commentSummary":"Looks good",
                                  "fidelityViewedConfirmed":true,
                                  "coverageViewedConfirmed":true,
                                  "previewViewedConfirmed":true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.lifecycleStatus").value("APPROVAL"));

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/lifecycle/approval-decision")
                        .with(authentication(new ManagementAuthentication(approver)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision":"APPROVED",
                                  "commentSummary":"Approved",
                                  "fidelityViewedConfirmed":true,
                                  "keyEvidenceConfirmed":true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.lifecycleStatus").value("PENDING_RELEASE"));

        if (configurePolicyBeforePublish) {
            configurePublishApiPolicy(templateId);
        }

        acknowledgeLatestPreviewFidelityWarnings(templateId);

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/lifecycle/publish")
                        .with(authentication(new ManagementAuthentication(groupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"releaseVersion":"%s","fidelityViewedConfirmed":true}
                                """.formatted(releaseVersion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.lifecycleStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.result.releaseVersion").value(releaseVersion));
    }

    protected String createRequiredTestDataSet(String templateId) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/test-data-sets")
                        .with(authentication(new ManagementAuthentication(templateAuthor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Required sample",
                                  "required":true,
                                  "variables":{"customerName":"Alice"}
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("result").path("testDataSetId").asText();
    }

    protected void configurePublishApiPolicy(String templateId) throws Exception {
        mockMvc.perform(put("/api/management/v1/templates/" + templateId + "/api/policy")
                        .with(authentication(new ManagementAuthentication(groupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "allowedAdGroups":["RETAIL_API"],
                                  "defaultRouteReleaseVersion":"1.0.0",
                                  "outputFormats":["DOCX"],
                                  "outputModes":["SYNC_STREAM"],
                                  "batchEnabled":false,
                                  "maxBatchSize":10,
                                  "docxEncryptionEnabled":false,
                                  "pdfEncryptionEnabled":false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.policyVersion").value(2));
    }

    protected byte[] buildSampleDocx(String text) throws Exception {
        return com.bank.docgen.master.support.TestMasterDocxFactory.buildWithAnchorText(text);
    }

    protected ManagementSessionClaims session(String username, List<String> roles, List<String> groups) {
        return new ManagementSessionClaims(
                username,
                username,
                username + "@example.com",
                AuthSource.LOCAL,
                roles,
                groups,
                "route.template-authoring-home",
                List.of("route.template-authoring-home"),
                Instant.now().plusSeconds(3600)
        );
    }
}
