package com.bank.docgen.master.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.web.ManagementAuthentication;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * CE-U06 — displayLabel write path + documentSequence on anchor views
 * (BDD-CE-U06-MAC-003…009 backend).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MasterRevisionLineAnchorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MasterDocumentRepository masterDocumentRepository;

    private byte[] sampleDocx;
    private ManagementSessionClaims retailGroupAdmin;
    private ManagementSessionClaims templateTester;
    private ManagementSessionClaims globalAdmin;

    @BeforeEach
    void setUp() throws Exception {
        masterDocumentRepository.deleteAll();
        sampleDocx = buildSampleDocx("{{anchor:HEADER}} before {{anchor:FOOTER}} after");
        retailGroupAdmin = session("10000002", List.of("GROUP_ADMIN"), List.of("RETAIL", "CORP"));
        templateTester = session("10000006", List.of("TEMPLATE_TESTER"), List.of("RETAIL"));
        globalAdmin = session("10000001", List.of("GLOBAL_ADMIN"), List.of("*"));
    }

    @Test
    void getRevisionLineExposesDocumentSequenceInDocumentOrder() throws Exception {
        String masterId = uploadMaster(retailGroupAdmin);
        String revisionLineId = currentRevisionLineId(masterId);

        mockMvc.perform(get("/api/management/v1/masters/" + masterId + "/revision-lines/" + revisionLineId)
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.anchors.length()").value(2))
                .andExpect(jsonPath("$.result.anchors[0].anchorId").value("HEADER"))
                .andExpect(jsonPath("$.result.anchors[0].documentSequence").value(0))
                .andExpect(jsonPath("$.result.anchors[1].anchorId").value("FOOTER"))
                .andExpect(jsonPath("$.result.anchors[1].documentSequence").value(1));
    }

    @Test
    void patchDisplayLabelOnCurrentWritableLinePersistsAndKeepsSequence() throws Exception {
        String masterId = uploadMaster(retailGroupAdmin);
        String revisionLineId = currentRevisionLineId(masterId);

        mockMvc.perform(patch(anchorPath(masterId, revisionLineId, "HEADER"))
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayLabel":"Header block"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.anchorId").value("HEADER"))
                .andExpect(jsonPath("$.result.displayLabel").value("Header block"))
                .andExpect(jsonPath("$.result.documentSequence").value(0));

        mockMvc.perform(get("/api/management/v1/masters/" + masterId + "/revision-lines/" + revisionLineId)
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.anchors[0].anchorId").value("HEADER"))
                .andExpect(jsonPath("$.result.anchors[0].displayLabel").value("Header block"))
                .andExpect(jsonPath("$.result.anchors[0].documentSequence").value(0))
                .andExpect(jsonPath("$.result.anchors[1].anchorId").value("FOOTER"))
                .andExpect(jsonPath("$.result.anchors[1].documentSequence").value(1));

        mockMvc.perform(get("/api/management/v1/masters/" + masterId)
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.anchors[0].anchorId").value("HEADER"))
                .andExpect(jsonPath("$.result.anchors[0].displayLabel").value("Header block"))
                .andExpect(jsonPath("$.result.anchors[0].documentSequence").value(0));
    }

    @Test
    void patchDisplayLabelRejectedWithoutManageMasters() throws Exception {
        String masterId = uploadMaster(retailGroupAdmin);
        String revisionLineId = currentRevisionLineId(masterId);

        mockMvc.perform(get("/api/management/v1/masters/" + masterId + "/revision-lines/" + revisionLineId)
                        .with(authentication(new ManagementAuthentication(templateTester))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.anchors[0].displayLabel").value("HEADER"));

        mockMvc.perform(patch(anchorPath(masterId, revisionLineId, "HEADER"))
                        .with(authentication(new ManagementAuthentication(templateTester)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayLabel":"Should not save"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void patchDisplayLabelRejectedOnHistoricalRevisionLine() throws Exception {
        String masterId = uploadMaster(retailGroupAdmin);
        String historicalLineId = currentRevisionLineId(masterId);
        replaceMasterFile(masterId, "file-b.docx", "{{anchor:HEADER}} {{anchor:FOOTER}} revised");
        String currentLineId = currentRevisionLineId(masterId);

        mockMvc.perform(patch(anchorPath(masterId, historicalLineId, "HEADER"))
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayLabel":"Historical edit"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.messageKey").value("api.error.master.invalidState"));

        mockMvc.perform(patch(anchorPath(masterId, currentLineId, "HEADER"))
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayLabel":"Current edit"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.displayLabel").value("Current edit"));
    }

    @Test
    void patchDisplayLabelRejectedWhilePendingReview() throws Exception {
        String masterId = uploadMaster(retailGroupAdmin);
        String revisionLineId = currentRevisionLineId(masterId);

        mockMvc.perform(post("/api/management/v1/masters/" + masterId + "/submit-review")
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"changeSummary":"Ready for review"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch(anchorPath(masterId, revisionLineId, "HEADER"))
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayLabel":"During review"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.messageKey").value("api.error.master.invalidState"));
    }

    @Test
    void patchDisplayLabelRejectsBlankAndWhitespaceOnly() throws Exception {
        String masterId = uploadMaster(retailGroupAdmin);
        String revisionLineId = currentRevisionLineId(masterId);

        mockMvc.perform(patch(anchorPath(masterId, revisionLineId, "HEADER"))
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayLabel":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.messageKey").value("api.error.validation.requestBodyInvalid"));

        mockMvc.perform(get("/api/management/v1/masters/" + masterId + "/revision-lines/" + revisionLineId)
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.anchors[0].displayLabel").value("HEADER"));
    }

    @Test
    void patchDisplayLabelOnlyDoesNotTriggerRetestRequired() throws Exception {
        String masterId = uploadMaster(retailGroupAdmin);
        String revisionLineId = currentRevisionLineId(masterId);

        mockMvc.perform(post("/api/management/v1/masters/" + masterId + "/submit-review")
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"changeSummary":"Baseline"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/management/v1/masters/" + masterId + "/review")
                        .with(authentication(new ManagementAuthentication(globalAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"APPROVED","commentSummary":"ok"}
                                """))
                .andExpect(status().isOk());

        replaceMasterFile(masterId, "file-b.docx", "{{anchor:HEADER}} before {{anchor:FOOTER}} after");
        revisionLineId = currentRevisionLineId(masterId);

        mockMvc.perform(patch(anchorPath(masterId, revisionLineId, "HEADER"))
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayLabel":"Readable header"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/management/v1/masters/" + masterId + "/impact-analysis")
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.retestRequired").value(false))
                .andExpect(jsonPath("$.result.anchorDelta.addedAnchors.length()").value(0))
                .andExpect(jsonPath("$.result.anchorDelta.removedAnchors.length()").value(0))
                .andExpect(jsonPath("$.result.anchorDelta.renamedAnchors.length()").value(0));
    }

    @Test
    void patchDisplayLabelTrimsWhitespace() throws Exception {
        String masterId = uploadMaster(retailGroupAdmin);
        String revisionLineId = currentRevisionLineId(masterId);

        mockMvc.perform(patch(anchorPath(masterId, revisionLineId, "FOOTER"))
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayLabel":"  Footer block  "}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.displayLabel").value("Footer block"))
                .andExpect(jsonPath("$.result.documentSequence").value(1));
    }

    private String anchorPath(String masterId, String revisionLineId, String anchorId) {
        return "/api/management/v1/masters/" + masterId
                + "/revision-lines/" + revisionLineId
                + "/anchors/" + anchorId;
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
        return com.bank.docgen.master.support.TestMasterDocxFactory.buildWithAnchorText(text);
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
