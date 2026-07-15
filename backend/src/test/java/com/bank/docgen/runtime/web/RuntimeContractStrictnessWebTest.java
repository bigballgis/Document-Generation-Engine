package com.bank.docgen.runtime.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.GenerateRequestBody;
import com.bank.docgen.sharedkernel.api.ErrorEnvelopeFactory;
import com.bank.docgen.sharedkernel.api.GlobalExceptionHandler;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.api.ValidationErrorFieldMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * BDD-CE-C01-004/005 + BDD-CE-C02-001..005/007/008: runtime contract strictness via a standalone
 * MockMvc that wires the strict runtime converter alongside the default lax converter and the
 * {@link GlobalExceptionHandler} that maps Jackson unknown-field / type-mismatch failures to the
 * unified {@code REQUEST_BODY_INVALID} error envelope.
 */
@ExtendWith(MockitoExtension.class)
class RuntimeContractStrictnessWebTest {

    @Mock
    private MessageResolver messageResolver;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Mirror Spring Boot defaults: shared mapper is lax; only the runtime converter enables strictness.
        objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        lenient().when(messageResolver.resolve(anyString())).thenReturn("The request body is invalid.");
        lenient().when(messageResolver.resolveOrDefault(anyString(), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));

        ErrorEnvelopeFactory errorEnvelopeFactory = new ErrorEnvelopeFactory(
                new TraceIdProvider(), messageResolver);
        GlobalExceptionHandler controllerAdvice = new GlobalExceptionHandler(
                errorEnvelopeFactory,
                new ValidationErrorFieldMapper(messageResolver),
                messageResolver);

        RuntimeStrictJacksonHttpMessageConverter strictConverter =
                new RuntimeStrictJacksonHttpMessageConverter(objectMapper);
        MappingJackson2HttpMessageConverter laxConverter =
                new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(new StubRuntimeController())
                .setControllerAdvice(controllerAdvice)
                .setMessageConverters(strictConverter, laxConverter)
                .build();
    }

    @Test
    void bddCeC01_001_singleGenerateAcceptsFullWhitelistContext() throws Exception {
        mockMvc.perform(post("/runtime/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"output":{"format":"DOCX","mode":"SYNC_STREAM"},
                                 "variables":{"name":"Alice"},
                                 "requestId":"r1","idempotencyKey":"k1",
                                 "context":{"sourceSystem":"LOS","channel":"API",
                                            "businessRequestId":"BR-1","upstreamTraceId":"tr-1",
                                            "scenario":"onboarding","locale":"en-US"}}"""))
                .andExpect(status().isOk());
    }

    @Test
    void bddCeC01_002_singleGenerateAcceptsMissingContext() throws Exception {
        mockMvc.perform(post("/runtime/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"output":{"format":"DOCX","mode":"SYNC_STREAM"},
                                 "variables":{"name":"Alice"},
                                 "requestId":"r1","idempotencyKey":"k1"}"""))
                .andExpect(status().isOk());
    }

    @Test
    void bddCeC01_004_contextUnknownSubFieldRejected() throws Exception {
        MvcResult result = mockMvc.perform(post("/runtime/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"output":{"format":"DOCX","mode":"SYNC_STREAM"},
                                 "variables":{"name":"Alice"},
                                 "requestId":"r1","idempotencyKey":"k1",
                                 "context":{"customerName":"Alice"}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("REQUEST_BODY_INVALID"))
                .andExpect(jsonPath("$.error.category").value("VALIDATION"))
                .andExpect(jsonPath("$.error.messageKey").value("api.error.validation.requestBodyInvalid"))
                .andExpect(jsonPath("$.error.retryable").value(false))
                .andExpect(jsonPath("$.error.fieldErrors[0].reason").value("UNKNOWN_FIELD"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("context.customerName"))
                .andReturn();
        assertEnvelopeComplete(result);
    }

    @Test
    void bddCeC01_005_contextFieldTypeMismatchRejectedAsInvalidType() throws Exception {
        MvcResult result = mockMvc.perform(post("/runtime/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"output":{"format":"DOCX","mode":"SYNC_STREAM"},
                                 "variables":{"name":"Alice"},
                                 "requestId":"r1","idempotencyKey":"k1",
                                 "context":{"locale":123}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("REQUEST_BODY_INVALID"))
                .andExpect(jsonPath("$.error.fieldErrors[0].reason").value("INVALID_TYPE"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("context.locale"))
                .andReturn();
        assertEnvelopeComplete(result);
    }

    @Test
    void bddCeC02_001_topLevelUnknownFieldRejected() throws Exception {
        mockMvc.perform(post("/runtime/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"output":{"format":"DOCX","mode":"SYNC_STREAM"},
                                 "variables":{"name":"Alice"},
                                 "requestId":"r1","idempotencyKey":"k1",
                                 "foo":"bar"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("REQUEST_BODY_INVALID"))
                .andExpect(jsonPath("$.error.fieldErrors[0].reason").value("UNKNOWN_FIELD"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("foo"));
    }

    @Test
    void bddCeC02_002_pathFieldTemplateIdInBodyRejected() throws Exception {
        // Jackson reports the first unrecognized property; either path duplicate is sufficient fail-closed proof.
        mockMvc.perform(post("/runtime/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"output":{"format":"DOCX","mode":"SYNC_STREAM"},
                                 "variables":{"name":"Alice"},
                                 "requestId":"r1","idempotencyKey":"k1",
                                 "templateId":"TPL-001","releaseVersion":"1.0.0"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("REQUEST_BODY_INVALID"))
                .andExpect(jsonPath("$.error.fieldErrors[0].reason").value("UNKNOWN_FIELD"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field",
                        org.hamcrest.Matchers.anyOf(
                                org.hamcrest.Matchers.equalTo("templateId"),
                                org.hamcrest.Matchers.equalTo("releaseVersion"))));
    }

    @Test
    void bddCeC02_003_nestedOutputUnknownFieldRejected() throws Exception {
        mockMvc.perform(post("/runtime/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"output":{"format":"DOCX","mode":"SYNC_STREAM","watermark":"yes"},
                                 "variables":{"name":"Alice"},
                                 "requestId":"r1","idempotencyKey":"k1"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("REQUEST_BODY_INVALID"))
                .andExpect(jsonPath("$.error.fieldErrors[0].reason").value("UNKNOWN_FIELD"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("output.watermark"));
    }

    @Test
    void bddCeC02_003_nestedEncryptionUnknownFieldRejected() throws Exception {
        mockMvc.perform(post("/runtime/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"output":{"format":"PDF","mode":"SYNC_STREAM"},
                                 "variables":{"name":"Alice"},
                                 "requestId":"r1","idempotencyKey":"k1",
                                 "encryption":{"enabled":false,"salt":"x"}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("REQUEST_BODY_INVALID"))
                .andExpect(jsonPath("$.error.fieldErrors[0].reason").value("UNKNOWN_FIELD"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("encryption.salt"));
    }

    @Test
    void bddCeC02_004_batchItemUnknownFieldRejected() throws Exception {
        mockMvc.perform(post("/runtime/batch-generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"output":{"format":"DOCX","mode":"SYNC_STREAM"},
                                 "items":[{"itemId":"i1","variables":{"a":1},"priority":"high"}],
                                 "requestId":"r1","idempotencyKey":"k1"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("REQUEST_BODY_INVALID"))
                .andExpect(jsonPath("$.error.fieldErrors[0].reason").value("UNKNOWN_FIELD"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field",
                        org.hamcrest.Matchers.containsString("priority")));
    }

    @Test
    void bddCeC02_005_variablesOpenKeysNotRejected() throws Exception {
        // variables is an open map (additionalProperties: true); arbitrary business keys must not
        // trigger CE-C02 UNKNOWN_FIELD rejection.
        mockMvc.perform(post("/runtime/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"output":{"format":"DOCX","mode":"SYNC_STREAM"},
                                 "variables":{"customerName":"Alice","amount":100,"ssn":"xxx"},
                                 "requestId":"r1","idempotencyKey":"k1"}"""))
                .andExpect(status().isOk());
    }

    @Test
    void bddCeC02_008_batchOriginalBatchIdAcceptedWithoutLineageCheck() throws Exception {
        // OpenAPI-declared optional field must be bindable; lineage validation runs in BatchGenerationService (CE-C05).
        mockMvc.perform(post("/runtime/batch-generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"output":{"format":"DOCX","mode":"SYNC_STREAM"},
                                 "items":[{"itemId":"i1","variables":{"a":1}}],
                                 "requestId":"r1","idempotencyKey":"k1",
                                 "originalBatchId":"BATCH-ABC123"}"""))
                .andExpect(status().isOk());
    }

    @Test
    void bddCeC02_006_managementDtoStrictnessUnchangedUnknownFieldIgnored() throws Exception {
        // The strict converter must NOT claim a management DTO; the default lax converter ignores
        // unknown fields, so a management endpoint stays 200 (not 400) for contract-foreign fields.
        mockMvc.perform(post("/management/sample")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"u1","password":"p1","unexpectedField":"x"}"""))
                .andExpect(status().isOk());
    }

    @Test
    void bddCeC02_007_errorEnvelopeFieldsComplete() throws Exception {
        MvcResult result = mockMvc.perform(post("/runtime/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"output":{"format":"DOCX","mode":"SYNC_STREAM"},
                                 "variables":{"name":"Alice"},
                                 "requestId":"r1","idempotencyKey":"k1",
                                 "rogue":"value"}"""))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertEnvelopeComplete(result);
        // fieldErrors non-empty with field/reason/message on each entry
        String body = result.getResponse().getContentAsString();
        var root = objectMapper.readTree(body);
        var fieldErrors = root.path("error").path("fieldErrors");
        assertThat(fieldErrors.isArray()).isTrue();
        assertThat(fieldErrors.size()).isGreaterThan(0);
        assertThat(fieldErrors.get(0).path("field").asText()).isNotBlank();
        assertThat(fieldErrors.get(0).path("reason").asText()).isNotBlank();
        assertThat(fieldErrors.get(0).path("message").asText()).isNotBlank();
    }

    private void assertEnvelopeComplete(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        var root = objectMapper.readTree(body);
        assertThat(root.has("metadata")).isTrue();
        assertThat(root.path("metadata").path("auditId").asText()).isNotBlank();
        assertThat(root.path("metadata").path("traceId").asText()).isNotBlank();
        assertThat(root.path("error").path("code").asText()).isEqualTo("REQUEST_BODY_INVALID");
        assertThat(root.path("error").path("category").asText()).isEqualTo("VALIDATION");
        assertThat(root.path("error").path("message").asText()).isNotBlank();
        assertThat(root.path("error").path("messageKey").asText())
                .isEqualTo("api.error.validation.requestBodyInvalid");
        assertThat(root.path("error").path("retryable").asBoolean()).isFalse();
        assertThat(root.path("error").path("fieldErrors").isArray()).isTrue();
        assertThat(root.path("error").path("fieldErrors").size()).isGreaterThan(0);
    }

    @RestController
    static class StubRuntimeController {

        @PostMapping("/runtime/generate")
        public String generate(@RequestBody GenerateRequestBody body) {
            return "ok";
        }

        @PostMapping("/runtime/batch-generate")
        public String batchGenerate(@RequestBody BatchGenerateRequestBody body) {
            return "ok";
        }

        @PostMapping("/management/sample")
        public String managementSample(@RequestBody ManagementSampleDto body) {
            return "ok";
        }
    }

    public record ManagementSampleDto(String username, String password) {
    }
}
