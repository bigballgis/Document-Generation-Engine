package com.bank.docgen.template.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.docgen.apimgmt.persistence.ApiCredentialRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.web.ManagementAuthentication;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TemplateImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApiCredentialRepository apiCredentialRepository;

    @Autowired
    private ApiPolicyRepository apiPolicyRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TemplateVersionRepository templateVersionRepository;

    @Autowired
    private VariableSchemaRepository variableSchemaRepository;

    @Autowired
    private AnchorBindingRepository anchorBindingRepository;

    @Autowired
    private MasterDocumentRepository masterDocumentRepository;

    private ManagementSessionClaims groupAdmin;
    private ManagementSessionClaims templateAuthor;
    private ManagementSessionClaims globalAdmin;
    private ManagementSessionClaims tester;

    @BeforeEach
    void setUp() {
        variableSchemaRepository.deleteAll();
        anchorBindingRepository.deleteAll();
        templateVersionRepository.deleteAll();
        templateRepository.deleteAll();
        masterDocumentRepository.deleteAll();
        apiPolicyRepository.deleteAll();
        apiCredentialRepository.deleteAll();
        groupAdmin = session("10000002", List.of("GROUP_ADMIN"), List.of("RETAIL"));
        templateAuthor = session("10000003", List.of("TEMPLATE_AUTHOR"), List.of("RETAIL"));
        globalAdmin = session("10000001", List.of("GLOBAL_ADMIN"), List.of("*"));
        tester = session("10000006", List.of("TEMPLATE_TESTER"), List.of("RETAIL"));
    }

    @Test
    void importBundle_landsTemplateInDraft() throws Exception {
        String masterId = createApprovedMaster();
        String exportedTemplateId = publishTemplate(masterId);
        JsonNode exportPayload = exportPublishedTemplate(exportedTemplateId);
        clearImportedTemplateArtifacts();

        mockMvc.perform(post("/api/management/v1/templates/import")
                        .with(authentication(new ManagementAuthentication(groupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "masterId":"%s",
                                  "bundle":%s
                                }
                                """.formatted(masterId, exportPayload.path("result").path("bundle").toString())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.template.lifecycleStatus").value("DRAFT"))
                .andExpect(jsonPath("$.result.template.externalId").value("TPL-EXPORT-LETTER"))
                .andExpect(jsonPath("$.result.template.variables[0].variableKey").value("customerName"))
                .andExpect(jsonPath("$.result.importSummary.newDevelopmentVersion").value(1))
                .andExpect(jsonPath("$.result.importSummary.importBatchId").isNotEmpty());
    }

    @Test
    void importBundle_rejectsExistingExternalId() throws Exception {
        String masterId = createApprovedMaster();
        String exportedTemplateId = publishTemplate(masterId);
        JsonNode exportPayload = exportPublishedTemplate(exportedTemplateId);

        mockMvc.perform(post("/api/management/v1/templates/import")
                        .with(authentication(new ManagementAuthentication(groupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "masterId":"%s",
                                  "bundle":%s
                                }
                                """.formatted(masterId, exportPayload.path("result").path("bundle").toString())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.messageKey").value("api.error.template.importConflict"));
    }

    @Test
    void importBundle_deniesTester() throws Exception {
        String masterId = createApprovedMaster();
        String exportedTemplateId = publishTemplate(masterId);
        JsonNode exportPayload = exportPublishedTemplate(exportedTemplateId);
        clearImportedTemplateArtifacts();

        mockMvc.perform(post("/api/management/v1/templates/import")
                        .with(authentication(new ManagementAuthentication(tester)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "masterId":"%s",
                                  "bundle":%s
                                }
                                """.formatted(masterId, exportPayload.path("result").path("bundle").toString())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.messageKey").value("api.error.template.accessDenied"));
    }

    @Test
    void importBundle_rejectsUnsupportedBundleFormat() throws Exception {
        String masterId = createApprovedMaster();

        mockMvc.perform(post("/api/management/v1/templates/import")
                        .with(authentication(new ManagementAuthentication(groupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "masterId":"%s",
                                  "bundle":{
                                    "format":"legacy",
                                    "metadata":{
                                      "templateId":"%s",
                                      "externalId":"TPL-BAD",
                                      "groupCode":"RETAIL",
                                      "name":"Bad",
                                      "description":"Bad",
                                      "masterId":"%s",
                                      "lifecycleStatus":"PUBLISHED",
                                      "releaseVersion":"1.0.0",
                                      "devVersionId":"%s",
                                      "devVersionNumber":1,
                                      "exportedAt":"2026-06-26T00:00:00Z"
                                    },
                                    "variables":[],
                                    "bindings":[],
                                    "rules":[],
                                    "contentModuleReferences":[]
                                  }
                                }
                                """.formatted(masterId, java.util.UUID.randomUUID(), masterId, java.util.UUID.randomUUID())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.messageKey").value("api.error.template.importBundleUnsupportedFormat"));
    }

    private void clearImportedTemplateArtifacts() {
        variableSchemaRepository.deleteAll();
        anchorBindingRepository.deleteAll();
        apiPolicyRepository.deleteAll();
        templateVersionRepository.deleteAll();
        templateRepository.deleteAll();
    }

    private JsonNode exportPublishedTemplate(String templateId) throws Exception {
        MvcResult export = mockMvc.perform(get("/api/management/v1/templates/" + templateId + "/export")
                        .with(authentication(new ManagementAuthentication(groupAdmin))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode payload = objectMapper.readTree(export.getResponse().getContentAsString());
        assertThat(payload.path("result").path("bundle").path("policySnapshot").has("credentialId")).isFalse();
        return payload;
    }

    private String publishTemplate(String masterId) throws Exception {
        String templateId = createDraftTemplate(masterId);
        configureTemplate(templateId);
        runLifecycle(templateId);
        return templateId;
    }

    private String createApprovedMaster() throws Exception {
        MvcResult upload = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/management/v1/masters")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "file",
                                "master.docx",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                minimalDocx()))
                        .param("groupCode", "RETAIL")
                        .param("name", "Retail Master")
                        .with(authentication(new ManagementAuthentication(groupAdmin))))
                .andExpect(status().isOk())
                .andReturn();
        String masterId = objectMapper.readTree(upload.getResponse().getContentAsString())
                .path("result").path("id").asText();
        mockMvc.perform(post("/api/management/v1/masters/" + masterId + "/submit-review")
                        .with(authentication(new ManagementAuthentication(groupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changeSummary\":\"Initial\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/management/v1/masters/" + masterId + "/review")
                        .with(authentication(new ManagementAuthentication(globalAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\",\"commentSummary\":\"Approved\"}"))
                .andExpect(status().isOk());
        return masterId;
    }

    private String createDraftTemplate(String masterId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/management/v1/templates")
                        .with(authentication(new ManagementAuthentication(templateAuthor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalId":"TPL-EXPORT-LETTER",
                                  "groupCode":"RETAIL",
                                  "name":"Export Letter",
                                  "description":"Export test template",
                                  "masterId":"%s"
                                }
                                """.formatted(masterId)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("result").path("id").asText();
    }

    private void configureTemplate(String templateId) throws Exception {
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
                .andExpect(status().isOk());
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
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/bindings/validate")
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isOk());
    }

    private void runLifecycle(String templateId) throws Exception {
        MvcResult sample = mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/test-data-sets")
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
        String sampleId = objectMapper.readTree(sample.getResponse().getContentAsString())
                .path("result").path("testDataSetId").asText();

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/previews/test-generate")
                        .with(authentication(new ManagementAuthentication(templateAuthor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variables\":{\"customerName\":\"Alice\"}}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/previews/batch-test")
                        .with(authentication(new ManagementAuthentication(templateAuthor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"testDataSetIds\":[\"" + sampleId + "\"]}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/lifecycle/submit-test")
                        .with(authentication(new ManagementAuthentication(templateAuthor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commentSummary\":\"Ready\"}"))
                .andExpect(status().isOk());
        ManagementSessionClaims testerSession = session("10000006", List.of("TEMPLATE_TESTER"), List.of("RETAIL"));
        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/lifecycle/test-decision")
                        .with(authentication(new ManagementAuthentication(testerSession)))
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
                .andExpect(status().isOk());
        ManagementSessionClaims approver = session("10000007", List.of("TEMPLATE_APPROVER"), List.of("RETAIL"));
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
                .andExpect(status().isOk());
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
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/lifecycle/publish")
                        .with(authentication(new ManagementAuthentication(groupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"releaseVersion\":\"1.0.0\",\"fidelityViewedConfirmed\":true}"))
                .andExpect(status().isOk());
    }

    private byte[] minimalDocx() throws Exception {
        try (var document = new org.apache.poi.xwpf.usermodel.XWPFDocument();
                var output = new java.io.ByteArrayOutputStream()) {
            org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph = document.createParagraph();
            org.apache.poi.xwpf.usermodel.XWPFRun run = paragraph.createRun();
            run.setText("Dear {{anchor:HEADER}} customer");
            document.write(output);
            return output.toByteArray();
        }
    }

    private ManagementSessionClaims session(String username, List<String> roles, List<String> groups) {
        return new ManagementSessionClaims(
                username,
                username,
                username + "@example.com",
                AuthSource.LOCAL,
                roles,
                groups,
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                Instant.now().plusSeconds(3600)
        );
    }
}
