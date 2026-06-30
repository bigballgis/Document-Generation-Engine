package com.bank.docgen.master.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.web.ManagementAuthentication;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MasterRevisionLineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MasterDocumentRepository masterDocumentRepository;

    private byte[] sampleDocx;
    private ManagementSessionClaims retailGroupAdmin;
    private ManagementSessionClaims globalAdmin;

    @BeforeEach
    void setUp() throws Exception {
        masterDocumentRepository.deleteAll();
        sampleDocx = buildSampleDocx("{{anchor:HEADER}} body");
        retailGroupAdmin = session("10000002", List.of("GROUP_ADMIN"), List.of("RETAIL", "CORP"));
        globalAdmin = session("10000001", List.of("GLOBAL_ADMIN"), List.of("*"));
    }

    @Test
    void listRevisionLinesReturnsSingleCurrentLineWithPagination() throws Exception {
        String masterId = uploadMaster(retailGroupAdmin);
        String revisionLineId = currentRevisionLineId(masterId);

        mockMvc.perform(get("/api/management/v1/masters/" + masterId + "/revision-lines")
                        .param("page", "0")
                        .param("size", "20")
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content.length()").value(1))
                .andExpect(jsonPath("$.result.page").value(0))
                .andExpect(jsonPath("$.result.size").value(20))
                .andExpect(jsonPath("$.result.totalElements").value(1))
                .andExpect(jsonPath("$.result.totalPages").value(1))
                .andExpect(jsonPath("$.result.content[0].id").value(revisionLineId))
                .andExpect(jsonPath("$.result.content[0].lineLabel").value("CURRENT"))
                .andExpect(jsonPath("$.result.content[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.result.content[0].originalFilename").value("master.docx"))
                .andExpect(jsonPath("$.result.content[0].anchorCount").value(1))
                .andExpect(jsonPath("$.result.content[0].updatedBy").value("10000002"))
                .andExpect(jsonPath("$.result.content[0].current").value(true))
                .andExpect(jsonPath("$.result.content[0].revisionSequence").value(1));
    }

    @Test
    void listRevisionLinesShowsLiveStatusForCurrentLineAfterApprove() throws Exception {
        String masterId = uploadMaster(retailGroupAdmin);
        String revisionLineId = currentRevisionLineId(masterId);

        mockMvc.perform(post("/api/management/v1/masters/" + masterId + "/submit-review")
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin)))
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

        mockMvc.perform(get("/api/management/v1/masters/" + masterId + "/revision-lines")
                        .param("page", "0")
                        .param("size", "20")
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].id").value(revisionLineId))
                .andExpect(jsonPath("$.result.content[0].current").value(true))
                .andExpect(jsonPath("$.result.content[0].status").value("APPROVED"))
                .andExpect(jsonPath("$.result.content[0].revisionSequence").value(1));
    }

    @Test
    void listRevisionLinesIsGroupScoped() throws Exception {
        String masterId = uploadMaster(retailGroupAdmin);

        ManagementSessionClaims corpOnlyAdmin = session("10000004", List.of("GROUP_ADMIN"), List.of("CORP"));
        mockMvc.perform(get("/api/management/v1/masters/" + masterId + "/revision-lines")
                        .with(authentication(new ManagementAuthentication(corpOnlyAdmin))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void getRevisionLineDetailReturnsOverviewAnchorsAndReviewHistory() throws Exception {
        String masterId = uploadMaster(retailGroupAdmin);
        String revisionLineId = currentRevisionLineId(masterId);

        mockMvc.perform(post("/api/management/v1/masters/" + masterId + "/submit-review")
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin)))
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

        mockMvc.perform(get("/api/management/v1/masters/" + masterId + "/revision-lines/" + revisionLineId)
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(revisionLineId))
                .andExpect(jsonPath("$.result.masterId").value(masterId))
                .andExpect(jsonPath("$.result.lineLabel").value("CURRENT"))
                .andExpect(jsonPath("$.result.status").value("APPROVED"))
                .andExpect(jsonPath("$.result.originalFilename").value("master.docx"))
                .andExpect(jsonPath("$.result.changeSummary").value("Initial anchor catalog"))
                .andExpect(jsonPath("$.result.current").value(true))
                .andExpect(jsonPath("$.result.revisionSequence").value(1))
                .andExpect(jsonPath("$.result.anchors[0].anchorId").value("HEADER"))
                .andExpect(jsonPath("$.result.reviewHistory.length()").value(2))
                .andExpect(jsonPath("$.result.reviewHistory[0].decision").value("APPROVED"));
    }

    @Test
    void getRevisionLineDetailReturns404WhenIdMismatch() throws Exception {
        String masterId = uploadMaster(retailGroupAdmin);
        UUID wrongId = UUID.randomUUID();

        mockMvc.perform(get("/api/management/v1/masters/" + masterId + "/revision-lines/" + wrongId)
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MASTER_NOT_FOUND"));
    }

    @Test
    void downloadRevisionLineReturnsDocxWhenCurrentLineId() throws Exception {
        String masterId = uploadMaster(retailGroupAdmin);
        String revisionLineId = currentRevisionLineId(masterId);

        mockMvc.perform(get("/api/management/v1/masters/" + masterId + "/revision-lines/" + revisionLineId + "/download")
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document").toString()))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"master.docx\""));
    }

    @Test
    void downloadRevisionLineReturns404WhenIdMismatch() throws Exception {
        String masterId = uploadMaster(retailGroupAdmin);
        UUID wrongId = UUID.randomUUID();

        mockMvc.perform(get("/api/management/v1/masters/" + masterId + "/revision-lines/" + wrongId + "/download")
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MASTER_NOT_FOUND"));
    }

    @Test
    void listRevisionLinesAfterReplaceReturnsHistoricalAndCurrentRows() throws Exception {
        String masterId = uploadMaster(retailGroupAdmin);
        String historicalLineId = currentRevisionLineId(masterId);
        replaceMasterFile(masterId, "file-b.docx", "{{anchor:HEADER}} {{anchor:FOOTER}} revised");
        String currentLineId = currentRevisionLineId(masterId);

        mockMvc.perform(get("/api/management/v1/masters/" + masterId + "/revision-lines")
                        .param("page", "0")
                        .param("size", "20")
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(2))
                .andExpect(jsonPath("$.result.content.length()").value(2))
                .andExpect(jsonPath("$.result.content[0].id").value(currentLineId))
                .andExpect(jsonPath("$.result.content[0].lineLabel").value("CURRENT"))
                .andExpect(jsonPath("$.result.content[0].originalFilename").value("file-b.docx"))
                .andExpect(jsonPath("$.result.content[0].current").value(true))
                .andExpect(jsonPath("$.result.content[0].revisionSequence").value(2))
                .andExpect(jsonPath("$.result.content[1].id").value(historicalLineId))
                .andExpect(jsonPath("$.result.content[1].lineLabel").value("HISTORICAL"))
                .andExpect(jsonPath("$.result.content[1].originalFilename").value("master.docx"))
                .andExpect(jsonPath("$.result.content[1].current").value(false))
                .andExpect(jsonPath("$.result.content[1].revisionSequence").value(1));
    }

    @Test
    void getHistoricalRevisionLineDetailReturnsSnapshotAnchors() throws Exception {
        String masterId = uploadMaster(retailGroupAdmin);
        String historicalLineId = currentRevisionLineId(masterId);
        replaceMasterFile(masterId, "file-b.docx", "{{anchor:HEADER}} {{anchor:FOOTER}} revised");

        mockMvc.perform(get("/api/management/v1/masters/" + masterId + "/revision-lines/" + historicalLineId)
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(historicalLineId))
                .andExpect(jsonPath("$.result.lineLabel").value("HISTORICAL"))
                .andExpect(jsonPath("$.result.current").value(false))
                .andExpect(jsonPath("$.result.revisionSequence").value(1))
                .andExpect(jsonPath("$.result.originalFilename").value("master.docx"))
                .andExpect(jsonPath("$.result.anchors.length()").value(1))
                .andExpect(jsonPath("$.result.anchors[0].anchorId").value("HEADER"));
    }

    @Test
    void downloadHistoricalRevisionLineReturnsSupersededFilename() throws Exception {
        String masterId = uploadMaster(retailGroupAdmin);
        String historicalLineId = currentRevisionLineId(masterId);
        replaceMasterFile(masterId, "file-b.docx", "{{anchor:HEADER}} {{anchor:FOOTER}} revised");

        mockMvc.perform(get("/api/management/v1/masters/" + masterId + "/revision-lines/" + historicalLineId + "/download")
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"master.docx\""));
    }

    @Test
    void getHistoricalRevisionLineIsGroupScoped() throws Exception {
        String masterId = uploadMaster(retailGroupAdmin);
        String historicalLineId = currentRevisionLineId(masterId);
        replaceMasterFile(masterId, "file-b.docx", "{{anchor:HEADER}} {{anchor:FOOTER}} revised");
        ManagementSessionClaims corpOnlyAdmin = session("10000004", List.of("GROUP_ADMIN"), List.of("CORP"));

        mockMvc.perform(get("/api/management/v1/masters/" + masterId + "/revision-lines/" + historicalLineId)
                        .with(authentication(new ManagementAuthentication(corpOnlyAdmin))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/management/v1/masters/" + masterId + "/revision-lines/" + historicalLineId + "/download")
                        .with(authentication(new ManagementAuthentication(corpOnlyAdmin))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void listRevisionLinesSupportsPagination() throws Exception {
        String masterId = uploadMaster(retailGroupAdmin);
        for (int index = 0; index < 24; index++) {
            replaceMasterFile(masterId, "file-" + index + ".docx", "{{anchor:HEADER}} body " + index);
        }

        mockMvc.perform(get("/api/management/v1/masters/" + masterId + "/revision-lines")
                        .param("page", "0")
                        .param("size", "20")
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(25))
                .andExpect(jsonPath("$.result.totalPages").value(2))
                .andExpect(jsonPath("$.result.content.length()").value(20));

        mockMvc.perform(get("/api/management/v1/masters/" + masterId + "/revision-lines")
                        .param("page", "1")
                        .param("size", "20")
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(25))
                .andExpect(jsonPath("$.result.content.length()").value(5));
    }

    private void replaceMasterFile(String masterId, String filename, String docxText) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                filename,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                buildSampleDocx(docxText)
        );
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/management/v1/masters/" + masterId + "/file")
                        .file(file)
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin))))
                .andExpect(status().isOk());
    }

    private String currentRevisionLineId(String masterId) {
        return masterDocumentRepository.findById(UUID.fromString(masterId))
                .orElseThrow()
                .getCurrentRevisionLineId()
                .toString();
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
