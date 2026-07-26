package com.bank.docgen.template.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.bank.docgen.authorization.management.web.ManagementAuthentication;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskRepository;
import com.bank.docgen.runtime.persistence.GenerationIdempotencyRepository;
import com.bank.docgen.runtime.service.IdempotencyConstants;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Shared arrange/request fixtures for TemplatePlatformSlice* tests (AI-SCALE #169).
 */
abstract class TemplatePlatformSliceFixtures extends TemplateManagementWebTestSupport {

    @Autowired
    protected GenerationIdempotencyRepository generationIdempotencyRepository;

    @Autowired
    protected GenerationAsyncTaskRepository generationAsyncTaskRepository;

    @BeforeEach
    void resetGenerationStores() {
        generationAsyncTaskRepository.deleteAll();
        generationIdempotencyRepository.deleteAll();
    }

    protected String createTestDataSet(String templateId, String name) throws Exception {
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
    protected CredentialBundle preparePublishedTemplateWithBatchPolicy() throws Exception {
        String templateId = createPublishedTemplate();
        return configureBatchApiAndCredential(templateId);
    }
    protected void configureTemplateWithImageScalingBinding(String templateId) throws Exception {
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
                                  "structuredContentJson":"{\\"nodes\\":[{\\"type\\":\\"imageRef\\",\\"imageRef\\":\\"IMG-1\\",\\"applyScaling\\":true}]}"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.validationStatus").value("VALID"));
    }
    protected CredentialBundle configureApiAndCredential(String templateId) throws Exception {
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
                .andExpect(jsonPath("$.result.policyVersion").value(3));

        MvcResult credentialResult = mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/api/credentials")
                        .with(authentication(new ManagementAuthentication(groupAdmin))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.secret").isNotEmpty())
                .andReturn();
        JsonNode body = objectMapper.readTree(credentialResult.getResponse().getContentAsString()).path("result");
        return new CredentialBundle(body.path("externalId").asText(), body.path("secret").asText());
    }
    protected CredentialBundle configureBatchApiAndCredential(String templateId) throws Exception {
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
                .andExpect(jsonPath("$.result.policyVersion").value(3));

        MvcResult credentialResult = mockMvc.perform(post("/api/management/v1/templates/" + templateId + "/api/credentials")
                        .with(authentication(new ManagementAuthentication(groupAdmin))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.secret").isNotEmpty())
                .andReturn();
        JsonNode body = objectMapper.readTree(credentialResult.getResponse().getContentAsString()).path("result");
        return new CredentialBundle(body.path("externalId").asText(), body.path("secret").asText());
    }
    protected String syncGenerateDocumentId(CredentialBundle credential, String idempotencyKey) throws Exception {
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
    protected void runtimeGenerate(CredentialBundle credential) throws Exception {
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
                .andExpect(header().string("fidelityWarningCount", "0"))
                .andExpect(header().string("fidelityWarningCodes", ""))
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
    protected String generateBody(String idempotencyKey) {
        return """
                {
                  "output":{"format":"DOCX","mode":"SYNC_STREAM"},
                  "variables":{"customerName":"Bob"},
                  "requestId":"req-1",
                  "idempotencyKey":"%s"
                }
                """.formatted(idempotencyKey);
    }
    protected String generateBodyWithEncryptionDisabled(String idempotencyKey) {
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
    protected String generateBodyWithEncryptionEnabled(String idempotencyKey) {
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
    protected String generateBodyWithPdfEncryptionEnabled(String idempotencyKey) {
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
    protected String batchSyncBody(String idempotencyKey) {
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
    protected String batchSyncBodyWithDuplicateItemIds(String idempotencyKey) {
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
    protected String batchAsyncBody(String idempotencyKey) {
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
    protected String batchSyncPdfEncryptionBody(String idempotencyKey) {
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
    protected record CredentialBundle(String externalId, String secret) {
    }
}
