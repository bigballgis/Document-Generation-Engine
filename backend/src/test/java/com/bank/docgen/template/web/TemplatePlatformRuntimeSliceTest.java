package com.bank.docgen.template.web;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.bank.docgen.authorization.management.web.ManagementAuthentication;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Peeled from TemplatePlatformSliceTest (AI-SCALE #169).
 */
class TemplatePlatformRuntimeSliceTest extends TemplatePlatformSliceFixtures {

    @Test
    void runtimeGenerateDeniedForUnauthorizedAccessAccount() throws Exception {
        String templateId = createPublishedTemplate();
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
        String templateId = createPublishedTemplate();
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
        String templateId = createPublishedTemplate();
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
        String templateId = createPublishedTemplate();
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
        String templateId = createPublishedTemplate();
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
        String templateId = createPublishedTemplate();
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
    void runtimeSyncBatchGenerateAppliesPdfEncryptionWhenEnabled() throws Exception {
        String templateId = createPublishedTemplate();
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
}
