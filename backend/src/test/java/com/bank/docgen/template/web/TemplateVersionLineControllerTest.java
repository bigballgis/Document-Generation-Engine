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

import java.io.ByteArrayOutputStream;

import java.time.Instant;

import java.util.List;

import org.apache.poi.xwpf.usermodel.XWPFDocument;

import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import org.apache.poi.xwpf.usermodel.XWPFRun;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.http.MediaType;

import org.springframework.mock.web.MockMultipartFile;

import org.springframework.test.context.ActiveProfiles;

import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.MvcResult;



@SpringBootTest

@AutoConfigureMockMvc

@ActiveProfiles("test")

class TemplateVersionLineControllerTest {



    @Autowired

    private MockMvc mockMvc;



    @Autowired

    private ObjectMapper objectMapper;



    @Autowired

    private MasterDocumentRepository masterDocumentRepository;



    @Autowired

    private TemplateRepository templateRepository;



    @Autowired

    private TestDataSetRepository testDataSetRepository;



    private byte[] sampleDocx;

    private ManagementSessionClaims groupAdmin;

    private ManagementSessionClaims templateAuthor;

    private ManagementSessionClaims globalAdmin;

    private ManagementSessionClaims tester;

    private ManagementSessionClaims approver;



    @BeforeEach

    void setUp() throws Exception {

        testDataSetRepository.deleteAll();

        templateRepository.deleteAll();

        masterDocumentRepository.deleteAll();

        sampleDocx = buildSampleDocx("Dear {{anchor:HEADER}} customer");

        groupAdmin = session("10000002", List.of("GROUP_ADMIN"), List.of("RETAIL", "CORP"));

        templateAuthor = session("10000003", List.of("TEMPLATE_AUTHOR"), List.of("RETAIL"));

        globalAdmin = session("10000001", List.of("GLOBAL_ADMIN"), List.of("*"));

        tester = session("10000006", List.of("TEMPLATE_TESTER"), List.of("RETAIL"));

        approver = session("10000007", List.of("TEMPLATE_APPROVER"), List.of("RETAIL"));

    }



    @Test

    void listVersionLinesIncludesInFlightDevAndPublishedReleaseRows() throws Exception {

        String masterId = uploadAndApproveMaster();

        String templateId = createTemplate(masterId);

        configureTemplate(templateId);

        runLifecycle(templateId);

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

        String masterId = uploadAndApproveMaster();

        String templateId = createTemplate(masterId);

        configureTemplate(templateId);

        runLifecycle(templateId);

        String publishedDevVersionId = publishedDevVersionId(templateId);



        mockMvc.perform(get("/api/management/v1/templates/" + templateId + "/version-lines/" + publishedDevVersionId)

                        .with(authentication(new ManagementAuthentication(templateAuthor))))

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.result.devVersionId").value(publishedDevVersionId))

                .andExpect(jsonPath("$.result.releaseVersion").value("1.0.0"))

                .andExpect(jsonPath("$.result.lineKind").value("PUBLISHED"))

                .andExpect(jsonPath("$.result.variables[0].variableKey").value("customerName"))

                .andExpect(jsonPath("$.result.bindings[0].anchorId").value("HEADER"));

    }



    @Test

    void getReleaseDetailReturnsVariablesBindingsAndRulesReadOnly() throws Exception {

        String masterId = uploadAndApproveMaster();

        String templateId = createTemplate(masterId);

        configureTemplate(templateId);

        runLifecycle(templateId);



        mockMvc.perform(get("/api/management/v1/templates/" + templateId + "/releases/1.0.0")

                        .with(authentication(new ManagementAuthentication(templateAuthor))))

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.result.releaseVersion").value("1.0.0"))

                .andExpect(jsonPath("$.result.readOnly").value(true))

                .andExpect(jsonPath("$.result.variables[0].variableKey").value("customerName"))

                .andExpect(jsonPath("$.result.bindings[0].anchorId").value("HEADER"));

    }



    @Test

    void getReleasePublishGateReturnsChecklistWithoutInFlightDev() throws Exception {

        String masterId = uploadAndApproveMaster();

        String templateId = createTemplate(masterId);

        configureTemplate(templateId);

        runLifecycle(templateId);



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

        String masterId = uploadAndApproveMaster();

        String templateId = createTemplate(masterId);

        configureTemplate(templateId);

        runLifecycle(templateId);



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

        String masterId = uploadAndApproveMaster();

        String templateId = createTemplate(masterId);

        configureTemplate(templateId);

        runLifecycle(templateId);



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

        ManagementSessionClaims corpOnlyAuthor = session("10000004", List.of("TEMPLATE_AUTHOR"), List.of("CORP"));



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

        String masterId = uploadAndApproveMaster();

        String templateId = createTemplate(masterId);

        configureTemplate(templateId);

        runLifecycle(templateId);

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

        String masterId = uploadAndApproveMaster();

        String templateId = createTemplate(masterId);

        configureTemplate(templateId);

        runLifecycle(templateId);

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



    private String uploadAndApproveMaster() throws Exception {

        String masterId = uploadMaster(groupAdmin);

        mockMvc.perform(post("/api/management/v1/masters/" + masterId + "/submit-review")

                        .with(authentication(new ManagementAuthentication(groupAdmin)))

                        .contentType(MediaType.APPLICATION_JSON)

                        .content("""

                                {"changeSummary":"Initial anchor catalog"}

                                """))

                .andExpect(status().isOk());

        mockMvc.perform(post("/api/management/v1/masters/" + masterId + "/review")

                        .with(authentication(new ManagementAuthentication(globalAdmin)))

                        .contentType(MediaType.APPLICATION_JSON)

                        .content("""

                                {"decision":"APPROVED","commentSummary":"Looks good"}

                                """))

                .andExpect(status().isOk());

        return masterId;

    }



    private String uploadMaster(ManagementSessionClaims session) throws Exception {

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



    private String createTemplate(String masterId) throws Exception {

        MvcResult result = mockMvc.perform(post("/api/management/v1/templates")

                        .with(authentication(new ManagementAuthentication(templateAuthor)))

                        .contentType(MediaType.APPLICATION_JSON)

                        .content("""

                                {

                                  "externalId":"TPL-RETAIL-LETTER",

                                  "groupCode":"RETAIL",

                                  "name":"Retail Letter",

                                  "description":"Slice template",

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

    }



    private void runLifecycle(String templateId) throws Exception {

        String requiredSampleId = createRequiredTestDataSet(templateId);

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/previews/test-generate")

                        .with(authentication(new ManagementAuthentication(templateAuthor)))

                        .contentType(MediaType.APPLICATION_JSON)

                        .content("""

                                {"variables":{"customerName":"Alice"}}

                                """))

                .andExpect(status().isOk());

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

                .andExpect(status().isOk());

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

                .andExpect(status().isOk());

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

        configurePublishApiPolicy(templateId);

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/lifecycle/publish")

                        .with(authentication(new ManagementAuthentication(groupAdmin)))

                        .contentType(MediaType.APPLICATION_JSON)

                        .content("""

                                {"releaseVersion":"1.0.0","fidelityViewedConfirmed":true}

                                """))

                .andExpect(status().isOk());

    }



    private String createRequiredTestDataSet(String templateId) throws Exception {

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



    private void configurePublishApiPolicy(String templateId) throws Exception {

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

    }



    private byte[] buildSampleDocx(String text) throws Exception {

        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            XWPFParagraph paragraph = document.createParagraph();

            XWPFRun run = paragraph.createRun();

            run.setText(text);

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

                "route.template-authoring-home",

                List.of("route.template-authoring-home"),

                Instant.now().plusSeconds(3600)

        );

    }

}


