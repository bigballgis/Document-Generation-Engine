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

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.web.ManagementAuthentication;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskRepository;
import com.bank.docgen.runtime.persistence.GenerationIdempotencyRepository;
import com.bank.docgen.runtime.service.IdempotencyConstants;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TestDataSetRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
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
class TemplatePlatformSliceTest {

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

    @Autowired
    private GenerationIdempotencyRepository generationIdempotencyRepository;

    @Autowired
    private GenerationAsyncTaskRepository generationAsyncTaskRepository;

    private byte[] sampleDocx;
    private ManagementSessionClaims groupAdmin;
    private ManagementSessionClaims templateAuthor;
    private ManagementSessionClaims globalAdmin;
    private ManagementSessionClaims tester;
    private ManagementSessionClaims approver;

    @BeforeEach
    void setUp() throws Exception {
        generationAsyncTaskRepository.deleteAll();
        generationIdempotencyRepository.deleteAll();
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
    void testFailDecisionWithoutStructuredFieldsReturns422() throws Exception {
        String masterId = uploadAndApproveMaster();
        String templateId = createTemplate(masterId);
        configureTemplate(templateId);

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
        String masterId = uploadAndApproveMaster();
        String templateId = createTemplate(masterId);
        configureTemplate(templateId);
        String previewId = testGenerate(templateId);
        mockMvc.perform(get("/api/management/v1/templates/" + templateId + "/previews/" + previewId)
                        .with(authentication(new ManagementAuthentication(templateAuthor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.result.fidelityWarnings[0].messageKey")
                        .value("generation.warning.fidelity.controlledStyleFallback"));

        runLifecycle(templateId);
        CredentialBundle credential = configureApiAndCredential(templateId);
        runtimeGenerate(credential);
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
                                  "masterId":"%s"
                                }
                                """.formatted(masterId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("TEMPLATE_VALIDATION_FAILED"));
    }

    @Test
    void savesCompositionRulesAndReturnsThemOnTemplateDetail() throws Exception {
        String masterId = uploadAndApproveMaster();
        String templateId = createTemplate(masterId);

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
        String masterId = uploadAndApproveMaster();
        String templateId = createTemplate(masterId);
        configureTemplate(templateId);
        runLifecycle(templateId);
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
        String masterId = uploadAndApproveMaster();
        String templateId = createTemplate(masterId);
        configureTemplate(templateId);
        runLifecycle(templateId);
        CredentialBundle credential = configureApiAndCredential(templateId);

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/versions/1.0.0/deactivate")
                        .with(authentication(new ManagementAuthentication(groupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Deactivate version only","confirmed":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.lifecycleStatus").value("PUBLISHED"));

        mockMvc.perform(post("/api/dev/v1/templates/TPL-RETAIL-LETTER/versions/1.0.0/generate")
                        .header("X-Api-Credential-Id", credential.externalId())
                        .header("X-Api-Credential-Secret", credential.secret())
                        .header("X-Access-Account", "svc-caller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generateBody("idem-version-stopped-1")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.messageKey").value("api.error.runtime.versionNotCallable"));
    }

    @Test
    void patchMetadataUpdatesDraftTemplate() throws Exception {
        String masterId = uploadAndApproveMaster();
        String templateId = createTemplate(masterId);

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
        String masterId = uploadAndApproveMaster();
        String templateId = createTemplate(masterId);

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
                .andExpect(jsonPath("$.result.length()").value(0));
    }

    @Test
    void runtimeGenerateDeniedForUnauthorizedAccessAccount() throws Exception {
        String masterId = uploadAndApproveMaster();
        String templateId = createTemplate(masterId);
        configureTemplate(templateId);
        runLifecycle(templateId);
        CredentialBundle credential = configureApiAndCredential(templateId);

        mockMvc.perform(post("/api/dev/v1/templates/TPL-RETAIL-LETTER/versions/1.0.0/generate")
                        .header("X-Api-Credential-Id", credential.externalId())
                        .header("X-Api-Credential-Secret", credential.secret())
                        .header("X-Access-Account", "svc-denied")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generateBody("idem-denied-1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.error.category").value("AUTHORIZATION"))
                .andExpect(jsonPath("$.metadata.auditId").isNotEmpty());
    }

    @Test
    void runtimeDownloadAfterSyncGenerate() throws Exception {
        String masterId = uploadAndApproveMaster();
        String templateId = createTemplate(masterId);
        configureTemplate(templateId);
        runLifecycle(templateId);
        CredentialBundle credential = configureApiAndCredential(templateId);
        String documentId = syncGenerateDocumentId(credential, "idem-download-success");

        mockMvc.perform(get("/api/dev/v1/documents/" + documentId + "/download")
                        .header("X-Api-Credential-Id", credential.externalId())
                        .header("X-Api-Credential-Secret", credential.secret())
                        .header("X-Access-Account", "svc-caller"))
                .andExpect(status().isOk())
                .andExpect(header().string("documentId", documentId))
                .andExpect(header().string("download.oneTime", "false"))
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    }

    @Test
    void runtimeDownloadUnknownDocumentReturns404() throws Exception {
        String masterId = uploadAndApproveMaster();
        String templateId = createTemplate(masterId);
        configureTemplate(templateId);
        runLifecycle(templateId);
        CredentialBundle credential = configureApiAndCredential(templateId);
        syncGenerateDocumentId(credential, "idem-download-404");

        mockMvc.perform(get("/api/dev/v1/documents/DOC-NOT-FOUND/download")
                        .header("X-Api-Credential-Id", credential.externalId())
                        .header("X-Api-Credential-Secret", credential.secret())
                        .header("X-Access-Account", "svc-caller"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("DOCUMENT_NOT_FOUND"));
    }

    @Test
    void runtimeDownloadExpiredReturns410() throws Exception {
        String masterId = uploadAndApproveMaster();
        String templateId = createTemplate(masterId);
        configureTemplate(templateId);
        runLifecycle(templateId);
        CredentialBundle credential = configureApiAndCredential(templateId);
        String documentId = syncGenerateDocumentId(credential, "idem-download-expired");

        generationIdempotencyRepository.findAll().stream()
                .filter(record -> documentId.equals(record.getDocumentId()))
                .findFirst()
                .ifPresent(record -> {
                    record.markDownloadExpired(Instant.now().minusSeconds(30));
                    generationIdempotencyRepository.save(record);
                });

        mockMvc.perform(get("/api/dev/v1/documents/" + documentId + "/download")
                        .header("X-Api-Credential-Id", credential.externalId())
                        .header("X-Api-Credential-Secret", credential.secret())
                        .header("X-Access-Account", "svc-caller"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error.code").value("DOWNLOAD_URL_EXPIRED"));
    }

    @Test
    void runtimeSyncBatchGenerateReturnsBatchResult() throws Exception {
        CredentialBundle credential = preparePublishedTemplateWithBatchPolicy();

        mockMvc.perform(post("/api/dev/v1/templates/TPL-RETAIL-LETTER/versions/1.0.0/batch-generate")
                        .header("X-Api-Credential-Id", credential.externalId())
                        .header("X-Api-Credential-Secret", credential.secret())
                        .header("X-Access-Account", "svc-caller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchSyncBody("idem-batch-sync-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.batch.batchId").isNotEmpty())
                .andExpect(jsonPath("$.result.batch.summary.totalCount").value(2))
                .andExpect(jsonPath("$.result.batch.summary.successCount").value(2))
                .andExpect(jsonPath("$.result.batch.items[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.result.batch.items[0].documentId").isNotEmpty());
    }

    @Test
    void runtimeSyncBatchRejectsDuplicateItemId() throws Exception {
        CredentialBundle credential = preparePublishedTemplateWithBatchPolicy();

        mockMvc.perform(post("/api/dev/v1/templates/TPL-RETAIL-LETTER/versions/1.0.0/batch-generate")
                        .header("X-Api-Credential-Id", credential.externalId())
                        .header("X-Api-Credential-Secret", credential.secret())
                        .header("X-Access-Account", "svc-caller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchSyncBodyWithDuplicateItemIds("idem-batch-dup-1")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("ITEM_ID_DUPLICATED"));
    }

    @Test
    void runtimeAsyncBatchAcceptsAndReturnsTaskResult() throws Exception {
        CredentialBundle credential = preparePublishedTemplateWithBatchPolicy();

        MvcResult accepted = mockMvc.perform(post("/api/dev/v1/templates/TPL-RETAIL-LETTER/default/batch-generate")
                        .header("X-Api-Credential-Id", credential.externalId())
                        .header("X-Api-Credential-Secret", credential.secret())
                        .header("X-Access-Account", "svc-caller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchAsyncBody("idem-batch-async-1")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.result.task.taskId").isNotEmpty())
                .andExpect(jsonPath("$.result.task.status").value("SUCCEEDED"))
                .andReturn();

        String taskId = objectMapper.readTree(accepted.getResponse().getContentAsString())
                .path("result").path("task").path("taskId").asText();

        mockMvc.perform(get("/api/dev/v1/templates/TPL-RETAIL-LETTER/tasks/" + taskId)
                        .header("X-Api-Credential-Id", credential.externalId())
                        .header("X-Api-Credential-Secret", credential.secret())
                        .header("X-Access-Account", "svc-caller")
                        .header("X-Request-Id", "req-task-query-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.task.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.result.batch.summary.successCount").value(1));
    }

    @Test
    void runtimeCancelCompletedTaskReturns409() throws Exception {
        CredentialBundle credential = preparePublishedTemplateWithBatchPolicy();

        MvcResult accepted = mockMvc.perform(post("/api/dev/v1/templates/TPL-RETAIL-LETTER/default/batch-generate")
                        .header("X-Api-Credential-Id", credential.externalId())
                        .header("X-Api-Credential-Secret", credential.secret())
                        .header("X-Access-Account", "svc-caller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchAsyncBody("idem-batch-cancel-1")))
                .andExpect(status().isAccepted())
                .andReturn();

        String taskId = objectMapper.readTree(accepted.getResponse().getContentAsString())
                .path("result").path("task").path("taskId").asText();

        mockMvc.perform(post("/api/dev/v1/templates/TPL-RETAIL-LETTER/tasks/" + taskId + "/cancel")
                        .header("X-Api-Credential-Id", credential.externalId())
                        .header("X-Api-Credential-Secret", credential.secret())
                        .header("X-Access-Account", "svc-caller"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ASYNC_TASK_CANCELLATION_NOT_ALLOWED"));
    }

    @Test
    void runtimeSyncGenerateRejectsEncryptionFieldsWhenDisabled() throws Exception {
        CredentialBundle credential = preparePublishedTemplateWithBatchPolicy();

        mockMvc.perform(post("/api/dev/v1/templates/TPL-RETAIL-LETTER/versions/1.0.0/generate")
                        .header("X-Api-Credential-Id", credential.externalId())
                        .header("X-Api-Credential-Secret", credential.secret())
                        .header("X-Access-Account", "svc-caller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generateBodyWithEncryptionDisabled("idem-encrypt-invalid-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ENCRYPTION_PARAMETER_INVALID"));
    }

    @Test
    void runtimeSyncGenerateRejectsEnabledEncryptionWhenPolicyDisallows() throws Exception {
        CredentialBundle credential = preparePublishedTemplateWithBatchPolicy();

        mockMvc.perform(post("/api/dev/v1/templates/TPL-RETAIL-LETTER/versions/1.0.0/generate")
                        .header("X-Api-Credential-Id", credential.externalId())
                        .header("X-Api-Credential-Secret", credential.secret())
                        .header("X-Access-Account", "svc-caller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generateBodyWithEncryptionEnabled("idem-encrypt-not-allowed-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ENCRYPTION_NOT_ALLOWED"));
    }

    @Test
    void runtimeSyncGenerateAppliesDocxEncryptionWhenEnabled() throws Exception {
        String masterId = uploadAndApproveMaster();
        String templateId = createTemplate(masterId);
        configureTemplate(templateId);
        runLifecycle(templateId);
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
                                  "docxEncryptionEnabled":true,
                                  "pdfEncryptionEnabled":false
                                }
                                """))
                .andExpect(status().isOk());
        MvcResult credentialResult = mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/api/credentials")
                        .with(authentication(new ManagementAuthentication(groupAdmin))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(credentialResult.getResponse().getContentAsString()).path("result");
        CredentialBundle credential = new CredentialBundle(body.path("externalId").asText(), body.path("secret").asText());

        MvcResult result = mockMvc.perform(post("/api/dev/v1/templates/TPL-RETAIL-LETTER/versions/1.0.0/generate")
                        .header("X-Api-Credential-Id", credential.externalId())
                        .header("X-Api-Credential-Secret", credential.secret())
                        .header("X-Access-Account", "svc-caller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generateBodyWithEncryptionEnabled("idem-encrypt-success-1")))
                .andExpect(status().isOk())
                .andExpect(header().string("documentId", org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyString())))
                .andReturn();

        byte[] encryptedDocx = result.getResponse().getContentAsByteArray();
        org.apache.poi.poifs.filesystem.POIFSFileSystem fs =
                new org.apache.poi.poifs.filesystem.POIFSFileSystem(new java.io.ByteArrayInputStream(encryptedDocx));
        org.apache.poi.poifs.crypt.EncryptionInfo info = new org.apache.poi.poifs.crypt.EncryptionInfo(fs);
        org.apache.poi.poifs.crypt.Decryptor decryptor = info.getDecryptor();
        org.junit.jupiter.api.Assertions.assertTrue(decryptor.verifyPassword("SecretPass1234"));
        fs.close();
    }

    @Test
    void runtimeSyncGenerateAppliesPdfEncryptionWhenEnabled() throws Exception {
        String masterId = uploadAndApproveMaster();
        String templateId = createTemplate(masterId);
        configureTemplate(templateId);
        runLifecycle(templateId);
        mockMvc.perform(put("/api/management/v1/templates/" + templateId + "/api/policy")
                        .with(authentication(new ManagementAuthentication(groupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "allowedAdGroups":["RETAIL_API"],
                                  "defaultRouteReleaseVersion":"1.0.0",
                                  "outputFormats":["PDF"],
                                  "outputModes":["SYNC_STREAM"],
                                  "batchEnabled":false,
                                  "maxBatchSize":10,
                                  "docxEncryptionEnabled":false,
                                  "pdfEncryptionEnabled":true
                                }
                                """))
                .andExpect(status().isOk());
        MvcResult credentialResult = mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/api/credentials")
                        .with(authentication(new ManagementAuthentication(groupAdmin))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(credentialResult.getResponse().getContentAsString()).path("result");
        CredentialBundle credential = new CredentialBundle(body.path("externalId").asText(), body.path("secret").asText());

        MvcResult result = mockMvc.perform(post("/api/dev/v1/templates/TPL-RETAIL-LETTER/versions/1.0.0/generate")
                        .header("X-Api-Credential-Id", credential.externalId())
                        .header("X-Api-Credential-Secret", credential.secret())
                        .header("X-Access-Account", "svc-caller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generateBodyWithPdfEncryptionEnabled("idem-pdf-encrypt-success-1")))
                .andExpect(status().isOk())
                .andExpect(header().string("documentId", org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyString())))
                .andExpect(header().string("output.format", "PDF"))
                .andReturn();

        byte[] encryptedPdf = result.getResponse().getContentAsByteArray();
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> org.apache.pdfbox.Loader.loadPDF(encryptedPdf));
        try (org.apache.pdfbox.pdmodel.PDDocument document =
                org.apache.pdfbox.Loader.loadPDF(encryptedPdf, "SecretPass1234")) {
            org.junit.jupiter.api.Assertions.assertEquals(1, document.getNumberOfPages());
        }
    }

    @Test
    void testDataSetCrudAndPreviewUsesStoredVariables() throws Exception {
        String masterId = uploadAndApproveMaster();
        String templateId = createTemplate(masterId);
        configureTemplate(templateId);

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
        String masterId = uploadAndApproveMaster();
        String templateId = createTemplate(masterId);
        configureTemplate(templateId);

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
        String masterId = uploadAndApproveMaster();
        String templateId = createTemplate(masterId);
        configureTemplate(templateId);

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
                .andExpect(jsonPath("$.result.warningCount").value(3))
                .andExpect(jsonPath("$.result.samples.length()").value(3));
    }

    private String createTestDataSet(String templateId, String name) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/test-data-sets")
                        .with(authentication(new ManagementAuthentication(templateAuthor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"%s",
                                  "variables":{"customerName":"DatasetCustomer"}
                                }
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("result").path("testDataSetId").asText();
    }

    @Test
    void runtimeSyncBatchGenerateAppliesPdfEncryptionWhenEnabled() throws Exception {
        String masterId = uploadAndApproveMaster();
        String templateId = createTemplate(masterId);
        configureTemplate(templateId);
        runLifecycle(templateId);
        mockMvc.perform(put("/api/management/v1/templates/" + templateId + "/api/policy")
                        .with(authentication(new ManagementAuthentication(groupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "allowedAdGroups":["RETAIL_API"],
                                  "defaultRouteReleaseVersion":"1.0.0",
                                  "outputFormats":["PDF"],
                                  "outputModes":["SYNC_STREAM"],
                                  "batchEnabled":true,
                                  "maxBatchSize":10,
                                  "docxEncryptionEnabled":false,
                                  "pdfEncryptionEnabled":true
                                }
                                """))
                .andExpect(status().isOk());
        MvcResult credentialResult = mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/api/credentials")
                        .with(authentication(new ManagementAuthentication(groupAdmin))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(credentialResult.getResponse().getContentAsString()).path("result");
        CredentialBundle credential = new CredentialBundle(body.path("externalId").asText(), body.path("secret").asText());

        mockMvc.perform(post("/api/dev/v1/templates/TPL-RETAIL-LETTER/versions/1.0.0/batch-generate")
                        .header("X-Api-Credential-Id", credential.externalId())
                        .header("X-Api-Credential-Secret", credential.secret())
                        .header("X-Access-Account", "svc-caller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchSyncPdfEncryptionBody("idem-batch-pdf-encrypt-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.batch.items[0].encryptionSummary.enabled").value(true))
                .andExpect(jsonPath("$.result.batch.items[0].output.format").value("PDF"));
    }

    @Test
    void managementCallerContractReturnsNonSensitiveContractView() throws Exception {
        String masterId = uploadAndApproveMaster();
        String templateId = createTemplate(masterId);
        configureTemplate(templateId);
        runLifecycle(templateId);
        configureApiAndCredential(templateId);

        mockMvc.perform(get("/api/management/v1/templates/" + templateId + "/api/contract")
                        .param("environment", "dev")
                        .with(authentication(new ManagementAuthentication(groupAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.templateId").value("TPL-RETAIL-LETTER"))
                .andExpect(jsonPath("$.result.paths[0]").value("/api/dev/v1/templates/TPL-RETAIL-LETTER/contract"))
                .andExpect(jsonPath("$.result.callableVersions[0].releaseVersion").value("1.0.0"))
                .andExpect(jsonPath("$.result.errorCodes[?(@.code=='BATCH_LIMIT_EXCEEDED')]").exists())
                .andExpect(jsonPath("$.result.apiPolicy.policyVersion").value(2));
    }

    private CredentialBundle preparePublishedTemplateWithBatchPolicy() throws Exception {
        String masterId = uploadAndApproveMaster();
        String templateId = createTemplate(masterId);
        configureTemplate(templateId);
        runLifecycle(templateId);
        return configureBatchApiAndCredential(templateId);
    }

    private String uploadAndApproveMaster() throws Exception {
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
                .andExpect(jsonPath("$.result.lifecycleStatus").value("DRAFT"))
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

    private String testGenerate(String templateId) throws Exception {
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

    private void runLifecycle(String templateId) throws Exception {
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
                                {"decision":"PASSED","commentSummary":"Looks good"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.lifecycleStatus").value("APPROVAL"));

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/lifecycle/approval-decision")
                        .with(authentication(new ManagementAuthentication(approver)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"APPROVED","commentSummary":"Approved"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.lifecycleStatus").value("PENDING_RELEASE"));

        configurePublishApiPolicy(templateId);

        mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/lifecycle/publish")
                        .with(authentication(new ManagementAuthentication(groupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"releaseVersion":"1.0.0"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.lifecycleStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.result.releaseVersion").value("1.0.0"));
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.policyVersion").value(1));
    }

    private CredentialBundle configureApiAndCredential(String templateId) throws Exception {
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

        MvcResult credentialResult = mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/api/credentials")
                        .with(authentication(new ManagementAuthentication(groupAdmin))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.secret").isNotEmpty())
                .andReturn();
        JsonNode body = objectMapper.readTree(credentialResult.getResponse().getContentAsString()).path("result");
        return new CredentialBundle(body.path("externalId").asText(), body.path("secret").asText());
    }

    private CredentialBundle configureBatchApiAndCredential(String templateId) throws Exception {
        mockMvc.perform(put("/api/management/v1/templates/" + templateId + "/api/policy")
                        .with(authentication(new ManagementAuthentication(groupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "allowedAdGroups":["RETAIL_API"],
                                  "defaultRouteReleaseVersion":"1.0.0",
                                  "outputFormats":["DOCX"],
                                  "outputModes":["SYNC_STREAM","ASYNC_TASK"],
                                  "batchEnabled":true,
                                  "maxBatchSize":10,
                                  "docxEncryptionEnabled":false,
                                  "pdfEncryptionEnabled":false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.policyVersion").value(2));

        MvcResult credentialResult = mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/api/credentials")
                        .with(authentication(new ManagementAuthentication(groupAdmin))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.secret").isNotEmpty())
                .andReturn();
        JsonNode body = objectMapper.readTree(credentialResult.getResponse().getContentAsString()).path("result");
        return new CredentialBundle(body.path("externalId").asText(), body.path("secret").asText());
    }

    private String syncGenerateDocumentId(CredentialBundle credential, String idempotencyKey) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/dev/v1/templates/TPL-RETAIL-LETTER/versions/1.0.0/generate")
                        .header("X-Api-Credential-Id", credential.externalId())
                        .header("X-Api-Credential-Secret", credential.secret())
                        .header("X-Access-Account", "svc-caller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generateBody(idempotencyKey)))
                .andExpect(status().isOk())
                .andExpect(header().string("documentId", org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyString())))
                .andReturn();
        return result.getResponse().getHeader("documentId");
    }

    private void runtimeGenerate(CredentialBundle credential) throws Exception {
        mockMvc.perform(get("/api/dev/v1/templates/TPL-RETAIL-LETTER/contract")
                        .header("X-Api-Credential-Id", credential.externalId())
                        .header("X-Api-Credential-Secret", credential.secret())
                        .header("X-Access-Account", "svc-caller"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.templateId").value("TPL-RETAIL-LETTER"))
                .andExpect(jsonPath("$.result.defaultRoute.url").exists())
                .andExpect(jsonPath("$.result.errorCodes[0].code").value("INVALID_CREDENTIALS"));

        mockMvc.perform(get("/api/dev/v1/templates/TPL-RETAIL-LETTER/versions")
                        .header("X-Api-Credential-Id", credential.externalId())
                        .header("X-Api-Credential-Secret", credential.secret())
                        .header("X-Access-Account", "svc-caller"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.templateId").value("TPL-RETAIL-LETTER"))
                .andExpect(jsonPath("$.result.versions[0].releaseVersion").value("1.0.0"));

        mockMvc.perform(post("/api/dev/v1/templates/TPL-RETAIL-LETTER/versions/1.0.0/generate")
                        .header("X-Api-Credential-Id", credential.externalId())
                        .header("X-Api-Credential-Secret", credential.secret())
                        .header("X-Access-Account", "svc-caller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generateBody("idem-runtime-1")))
                .andExpect(status().isOk())
                .andExpect(header().string("documentId", org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyString())))
                .andExpect(header().string("fidelityWarningCodes", "CONTROLLED_STYLE_FALLBACK"))
                .andExpect(header().string("idempotencyStatus", IdempotencyConstants.STATUS_NEW))
                .andExpect(header().string("routeType", "EXPLICIT_VERSION"));

        mockMvc.perform(post("/api/dev/v1/templates/TPL-RETAIL-LETTER/default/generate")
                        .header("X-Api-Credential-Id", credential.externalId())
                        .header("X-Api-Credential-Secret", credential.secret())
                        .header("X-Access-Account", "svc-caller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generateBody("idem-runtime-1")))
                .andExpect(status().isOk())
                .andExpect(header().string("idempotencyStatus", IdempotencyConstants.STATUS_REPLAYED))
                .andExpect(header().string("routeType", "DEFAULT_ROUTE"));
    }

    private String generateBody(String idempotencyKey) {
        return """
                {
                  "output":{"format":"DOCX","mode":"SYNC_STREAM"},
                  "variables":{"customerName":"Bob"},
                  "requestId":"req-1",
                  "idempotencyKey":"%s"
                }
                """.formatted(idempotencyKey);
    }

    private String generateBodyWithEncryptionDisabled(String idempotencyKey) {
        return """
                {
                  "output":{"format":"DOCX","mode":"SYNC_STREAM"},
                  "variables":{"customerName":"Bob"},
                  "encryption":{"enabled":false,"openPassword":"SecretPass1234"},
                  "requestId":"req-encrypt-1",
                  "idempotencyKey":"%s"
                }
                """.formatted(idempotencyKey);
    }

    private String generateBodyWithEncryptionEnabled(String idempotencyKey) {
        return """
                {
                  "output":{"format":"DOCX","mode":"SYNC_STREAM"},
                  "variables":{"customerName":"Bob"},
                  "encryption":{"enabled":true,"openPassword":"SecretPass1234"},
                  "requestId":"req-encrypt-2",
                  "idempotencyKey":"%s"
                }
                """.formatted(idempotencyKey);
    }

    private String generateBodyWithPdfEncryptionEnabled(String idempotencyKey) {
        return """
                {
                  "output":{"format":"PDF","mode":"SYNC_STREAM"},
                  "variables":{"customerName":"Bob"},
                  "encryption":{"enabled":true,"openPassword":"SecretPass1234"},
                  "requestId":"req-pdf-encrypt-1",
                  "idempotencyKey":"%s"
                }
                """.formatted(idempotencyKey);
    }

    private String batchSyncBody(String idempotencyKey) {
        return """
                {
                  "output":{"format":"DOCX","mode":"SYNC_STREAM"},
                  "items":[
                    {"itemId":"item-1","variables":{"customerName":"Alice"}},
                    {"itemId":"item-2","variables":{"customerName":"Bob"}}
                  ],
                  "requestId":"req-batch-1",
                  "idempotencyKey":"%s"
                }
                """.formatted(idempotencyKey);
    }

    private String batchSyncBodyWithDuplicateItemIds(String idempotencyKey) {
        return """
                {
                  "output":{"format":"DOCX","mode":"SYNC_STREAM"},
                  "items":[
                    {"itemId":"item-dup","variables":{"customerName":"Alice"}},
                    {"itemId":"item-dup","variables":{"customerName":"Bob"}}
                  ],
                  "requestId":"req-batch-dup",
                  "idempotencyKey":"%s"
                }
                """.formatted(idempotencyKey);
    }

    private String batchAsyncBody(String idempotencyKey) {
        return """
                {
                  "output":{"format":"DOCX","mode":"ASYNC_TASK"},
                  "items":[
                    {"itemId":"item-async-1","variables":{"customerName":"Carol"}}
                  ],
                  "requestId":"req-batch-async",
                  "idempotencyKey":"%s"
                }
                """.formatted(idempotencyKey);
    }

    private String batchSyncPdfEncryptionBody(String idempotencyKey) {
        return """
                {
                  "output":{"format":"PDF","mode":"SYNC_STREAM"},
                  "encryption":{"enabled":true,"openPassword":"SecretPass1234"},
                  "items":[
                    {"itemId":"item-pdf-1","variables":{"customerName":"Alice"}}
                  ],
                  "requestId":"req-batch-pdf-encrypt",
                  "idempotencyKey":"%s"
                }
                """.formatted(idempotencyKey);
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

    private record CredentialBundle(String externalId, String secret) {
    }
}
