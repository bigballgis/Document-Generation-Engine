package com.bank.docgen.runtime.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.runtime.web.RuntimeStrictJacksonHttpMessageConverter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * BDD-CE-C01 / BDD-CE-C02 — runtime request DTO strictness (context + unknown-field).
 * Mirrors {@link RuntimeStrictJacksonHttpMessageConverter}: fail-on-unknown on a mapper copy only.
 */
class RuntimeStrictRequestDeserializationTest {

    private ObjectMapper strictMapper;
    private ObjectMapper laxMapper;

    @BeforeEach
    void setUp() {
        laxMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        strictMapper = RuntimeStrictJacksonHttpMessageConverter.createStrictMapper(laxMapper);
    }

    @Test
    void acceptsFullWhitelistContext() throws Exception {
        String json = """
                {
                  "output": {"format": "DOCX", "mode": "SYNC_STREAM"},
                  "variables": {"customerName": "Alice"},
                  "requestId": "req-1",
                  "idempotencyKey": "idem-1",
                  "context": {
                    "sourceSystem": "LOS",
                    "channel": "API",
                    "businessRequestId": "BR-1",
                    "upstreamTraceId": "tr-1",
                    "scenario": "onboarding",
                    "locale": "en-US"
                  }
                }
                """;

        GenerateRequestBody body = strictMapper.readValue(json, GenerateRequestBody.class);

        assertThat(body.context()).isNotNull();
        assertThat(body.context().sourceSystem()).isEqualTo("LOS");
        assertThat(body.context().locale()).isEqualTo("en-US");
    }

    @Test
    void acceptsPartialOrMissingContext() throws Exception {
        String withPartial = """
                {
                  "output": {"format": "DOCX", "mode": "SYNC_STREAM"},
                  "variables": {"x": 1},
                  "requestId": "req-1",
                  "idempotencyKey": "idem-1",
                  "context": {"channel": "MOBILE", "locale": "zh-CN"}
                }
                """;
        GenerateRequestBody partial = strictMapper.readValue(withPartial, GenerateRequestBody.class);
        assertThat(partial.context().channel()).isEqualTo("MOBILE");
        assertThat(partial.context().sourceSystem()).isNull();

        String without = """
                {
                  "output": {"format": "DOCX", "mode": "SYNC_STREAM"},
                  "variables": {"x": 1},
                  "requestId": "req-1",
                  "idempotencyKey": "idem-1"
                }
                """;
        GenerateRequestBody omitted = strictMapper.readValue(without, GenerateRequestBody.class);
        assertThat(omitted.context()).isNull();
    }

    @Test
    void rejectsUnknownContextSubfield() {
        String json = """
                {
                  "output": {"format": "DOCX", "mode": "SYNC_STREAM"},
                  "variables": {"x": 1},
                  "requestId": "req-1",
                  "idempotencyKey": "idem-1",
                  "context": {"channel": "API", "customerName": "secret"}
                }
                """;

        assertThatThrownBy(() -> strictMapper.readValue(json, GenerateRequestBody.class))
                .isInstanceOf(UnrecognizedPropertyException.class)
                .satisfies(ex -> {
                    UnrecognizedPropertyException upe = (UnrecognizedPropertyException) ex;
                    assertThat(upe.getPropertyName()).isEqualTo("customerName");
                });
    }

    @Test
    void rejectsNonStringContextValue() {
        String json = """
                {
                  "output": {"format": "DOCX", "mode": "SYNC_STREAM"},
                  "variables": {"x": 1},
                  "requestId": "req-1",
                  "idempotencyKey": "idem-1",
                  "context": {"locale": 123}
                }
                """;

        assertThatThrownBy(() -> strictMapper.readValue(json, GenerateRequestBody.class))
                .isInstanceOf(MismatchedInputException.class);
    }

    @Test
    void rejectsTopLevelUnknownField() {
        String json = """
                {
                  "output": {"format": "DOCX", "mode": "SYNC_STREAM"},
                  "variables": {"x": 1},
                  "requestId": "req-1",
                  "idempotencyKey": "idem-1",
                  "foo": "bar"
                }
                """;

        assertThatThrownBy(() -> strictMapper.readValue(json, GenerateRequestBody.class))
                .isInstanceOf(UnrecognizedPropertyException.class)
                .satisfies(ex -> assertThat(((UnrecognizedPropertyException) ex).getPropertyName()).isEqualTo("foo"));
    }

    @Test
    void rejectsBodyDuplicatePathFields() {
        String json = """
                {
                  "output": {"format": "DOCX", "mode": "SYNC_STREAM"},
                  "variables": {"x": 1},
                  "requestId": "req-1",
                  "idempotencyKey": "idem-1",
                  "templateId": "TPL-1",
                  "releaseVersion": "1.0.0"
                }
                """;

        assertThatThrownBy(() -> strictMapper.readValue(json, GenerateRequestBody.class))
                .isInstanceOf(UnrecognizedPropertyException.class);
    }

    @Test
    void rejectsNestedOutputUnknownProperty() {
        String json = """
                {
                  "output": {"format": "DOCX", "mode": "SYNC_STREAM", "extra": true},
                  "variables": {"x": 1},
                  "requestId": "req-1",
                  "idempotencyKey": "idem-1"
                }
                """;

        assertThatThrownBy(() -> strictMapper.readValue(json, GenerateRequestBody.class))
                .isInstanceOf(UnrecognizedPropertyException.class)
                .satisfies(ex -> assertThat(((UnrecognizedPropertyException) ex).getPropertyName()).isEqualTo("extra"));
    }

    @Test
    void rejectsNestedEncryptionUnknownProperty() {
        String json = """
                {
                  "output": {"format": "DOCX", "mode": "SYNC_STREAM"},
                  "variables": {"x": 1},
                  "encryption": {"enabled": false, "debug": true},
                  "requestId": "req-1",
                  "idempotencyKey": "idem-1"
                }
                """;

        assertThatThrownBy(() -> strictMapper.readValue(json, GenerateRequestBody.class))
                .isInstanceOf(UnrecognizedPropertyException.class)
                .satisfies(ex -> assertThat(((UnrecognizedPropertyException) ex).getPropertyName()).isEqualTo("debug"));
    }

    @Test
    void allowsOpenVariableKeysWithoutUnknownField() throws Exception {
        String json = """
                {
                  "output": {"format": "DOCX", "mode": "SYNC_STREAM"},
                  "variables": {"customerName": "Alice", "anyBusinessKey": 42},
                  "requestId": "req-1",
                  "idempotencyKey": "idem-1"
                }
                """;

        GenerateRequestBody body = strictMapper.readValue(json, GenerateRequestBody.class);
        assertThat(body.variables()).containsKeys("customerName", "anyBusinessKey");
    }

    @Test
    void batchAcceptsContextAndOriginalBatchIdWithoutLineage() throws Exception {
        String json = """
                {
                  "output": {"format": "DOCX", "mode": "SYNC_STREAM"},
                  "items": [{"itemId": "i1", "variables": {"a": 1}}],
                  "requestId": "req-1",
                  "idempotencyKey": "idem-1",
                  "originalBatchId": "BATCH-abc123",
                  "context": {"scenario": "retry"}
                }
                """;

        BatchGenerateRequestBody body = strictMapper.readValue(json, BatchGenerateRequestBody.class);
        assertThat(body.originalBatchId()).isEqualTo("BATCH-abc123");
        assertThat(body.context().scenario()).isEqualTo("retry");
    }

    @Test
    void rejectsUnknownPropertyOnBatchItem() {
        String json = """
                {
                  "output": {"format": "DOCX", "mode": "SYNC_STREAM"},
                  "items": [{"itemId": "i1", "variables": {"a": 1}, "orphan": true}],
                  "requestId": "req-1",
                  "idempotencyKey": "idem-1"
                }
                """;

        assertThatThrownBy(() -> strictMapper.readValue(json, BatchGenerateRequestBody.class))
                .isInstanceOf(UnrecognizedPropertyException.class)
                .satisfies(ex -> assertThat(((UnrecognizedPropertyException) ex).getPropertyName()).isEqualTo("orphan"));
    }

    @Test
    void managementStyleDtoRemainsLenientOnSharedMapper() throws Exception {
        record ManagementStyleBody(String name) {
        }

        ManagementStyleBody body = laxMapper.readValue(
                "{\"name\":\"ok\",\"unknownClientField\":1}",
                ManagementStyleBody.class
        );
        assertThat(body.name()).isEqualTo("ok");
    }
}
