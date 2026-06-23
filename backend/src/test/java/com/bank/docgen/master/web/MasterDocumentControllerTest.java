package com.bank.docgen.master.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.web.ManagementAuthentication;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
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
import com.bank.docgen.master.persistence.MasterDocumentRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MasterDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MasterDocumentRepository masterDocumentRepository;

    private byte[] sampleDocx;
    private ManagementSessionClaims retailGroupAdmin;
    private ManagementSessionClaims globalAdmin;
    private ManagementSessionClaims templateAuthor;

    @BeforeEach
    void setUp() throws Exception {
        masterDocumentRepository.deleteAll();
        sampleDocx = buildSampleDocx("{{anchor:HEADER}} body");
        retailGroupAdmin = session("10000002", List.of("GROUP_ADMIN"), List.of("RETAIL", "CORP"));
        globalAdmin = session("10000001", List.of("GLOBAL_ADMIN"), List.of("*"));
        templateAuthor = session("10000003", List.of("TEMPLATE_AUTHOR"), List.of("RETAIL"));
    }

    @Test
    void uploadSubmitAndApproveMasterDocument() throws Exception {
        String masterId = uploadMaster(retailGroupAdmin);

        mockMvc.perform(post("/api/management/v1/masters/" + masterId + "/submit-review")
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"changeSummary":"Initial anchor catalog"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("PENDING_REVIEW"));

        mockMvc.perform(post("/api/management/v1/masters/" + masterId + "/review")
                        .with(authentication(new ManagementAuthentication(globalAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"APPROVED","commentSummary":"Looks good"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("APPROVED"))
                .andExpect(jsonPath("$.result.anchors[0].anchorId").value("HEADER"));
    }

    @Test
    void rejectReviewReturnsMasterToDraft() throws Exception {
        String masterId = uploadMaster(retailGroupAdmin);

        mockMvc.perform(post("/api/management/v1/masters/" + masterId + "/submit-review")
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"changeSummary":"Needs review"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/management/v1/masters/" + masterId + "/review")
                        .with(authentication(new ManagementAuthentication(globalAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"REJECTED","commentSummary":"Fix anchors"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("DRAFT"))
                .andExpect(jsonPath("$.result.reviewHistory[0].decision").value("REJECTED"));
    }

    @Test
    void listMastersIsGroupScopedForGroupAdmin() throws Exception {
        uploadMaster(retailGroupAdmin);

        mockMvc.perform(get("/api/management/v1/masters").with(authentication(new ManagementAuthentication(retailGroupAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(1));

        ManagementSessionClaims corpOnlyAdmin = session("10000004", List.of("GROUP_ADMIN"), List.of("CORP"));
        mockMvc.perform(get("/api/management/v1/masters").with(authentication(new ManagementAuthentication(corpOnlyAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(0));
    }

    @Test
    void templateAuthorCannotUploadMaster() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "master.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                sampleDocx
        );
        mockMvc.perform(multipart("/api/management/v1/masters")
                        .file(file)
                        .param("groupCode", "RETAIL")
                        .param("name", "Retail Letter Master")
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void impactAnalysisReturnsEmptyTemplateList() throws Exception {
        String masterId = uploadMaster(retailGroupAdmin);

        mockMvc.perform(get("/api/management/v1/masters/" + masterId + "/impact-analysis")
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.masterId").value(masterId))
                .andExpect(jsonPath("$.result.referencedTemplateIds").isArray())
                .andExpect(jsonPath("$.result.referencedTemplateIds.length()").value(0))
                .andExpect(jsonPath("$.result.retestRequired").value(false));
    }

    private String uploadMaster(ManagementSessionClaims session) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "master.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                sampleDocx
        );
        MvcResult result = mockMvc.perform(multipart("/api/management/v1/masters")
                        .file(file)
                        .param("groupCode", "RETAIL")
                        .param("name", "Retail Letter Master")
                        .param("description", "Sample master")
                        .with(authentication(new ManagementAuthentication(session))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("DRAFT"))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("result").path("id").asText();
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

    private ManagementSessionClaims session(
            String username,
            List<String> roles,
            List<String> groups
    ) {
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
