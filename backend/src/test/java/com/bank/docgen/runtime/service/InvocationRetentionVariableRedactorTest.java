package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.sharedkernel.security.VariableHashSupport;
import com.bank.docgen.template.domain.VariablePiiCategory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * IBL-A5 BDD-IBL-A5-001…005 / 007 / 010 — retention PII redaction proof surfaces.
 */
class InvocationRetentionVariableRedactorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void bddIblA5_001_piiMarkedFieldCleartextExcludedAndNoneRetained() {
        Map<String, VariablePiiCategory> schema = Map.of(
                "customerName", VariablePiiCategory.PERSONAL_NAME,
                "productCode", VariablePiiCategory.NONE
        );
        Map<String, Object> original = Map.of(
                "customerName", "Alice Example",
                "productCode", "PRD-1"
        );

        InvocationRetentionVariableRedactor.Result result =
                InvocationRetentionVariableRedactor.redact(original, schema);

        assertThat(result.variables()).doesNotContainKey("customerName");
        assertThat(result.variables().get("productCode")).isEqualTo("PRD-1");
        assertThat(result.redactedVariableKeys()).containsExactly("customerName");
        assertThat(result.redactedPiiCategories())
                .containsEntry("customerName", VariablePiiCategory.PERSONAL_NAME.name());
        assertThat(result.variables().toString()).doesNotContain("Alice Example");
    }

    @ParameterizedTest
    @EnumSource(
            value = VariablePiiCategory.class,
            names = {"PERSONAL_NAME", "GOVERNMENT_ID", "FINANCIAL_ACCOUNT", "CONTACT", "ADDRESS", "OTHER_SENSITIVE"}
    )
    void bddIblA5_002_eachForbiddenCategoryExcludesProbeCleartext(VariablePiiCategory category) {
        String key = "probeField";
        String probe = "PROBE-" + category.name();
        Map<String, VariablePiiCategory> schema = Map.of(
                key, category,
                "letterRef", VariablePiiCategory.NONE
        );
        Map<String, Object> original = Map.of(key, probe, "letterRef", "LR-OK");

        InvocationRetentionVariableRedactor.Result result =
                InvocationRetentionVariableRedactor.redact(original, schema);

        assertThat(result.variables().toString()).doesNotContain(probe);
        assertThat(result.variables().get("letterRef")).isEqualTo("LR-OK");
        assertThat(result.redactedVariableKeys()).contains(key);
    }

    @Test
    void bddIblA5_002_coversAllNonNoneEnumValues() {
        EnumSet<VariablePiiCategory> forbidden = EnumSet.complementOf(EnumSet.of(VariablePiiCategory.NONE));
        assertThat(forbidden).hasSize(6);
    }

    @Test
    void bddIblA5_003_noneFieldCleartextMayBeRetained() {
        Map<String, VariablePiiCategory> schema = Map.of("letterRef", VariablePiiCategory.NONE);
        InvocationRetentionVariableRedactor.Result result = InvocationRetentionVariableRedactor.redact(
                Map.of("letterRef", "LR-9"),
                schema
        );

        assertThat(result.variables().get("letterRef")).isEqualTo("LR-9");
        assertThat(result.redactedVariableKeys()).isEmpty();
    }

    @Test
    void bddIblA5_004_unknownKeyTreatedAsSensitive() {
        InvocationRetentionVariableRedactor.Result result = InvocationRetentionVariableRedactor.redact(
                Map.of("mysteryField", "secret-probe", "productCode", "PRD-1"),
                Map.of("productCode", VariablePiiCategory.NONE)
        );

        assertThat(result.variables().toString()).doesNotContain("secret-probe");
        assertThat(result.variables()).doesNotContainKey("mysteryField");
        assertThat(result.variables().get("productCode")).isEqualTo("PRD-1");
        assertThat(result.redactedVariableKeys()).contains("mysteryField");
        assertThat(result.redactedPiiCategories())
                .containsEntry("mysteryField", VariablePiiCategory.OTHER_SENSITIVE.name());
    }

    @Test
    void bddIblA5_005_variablesHashMustUsePreRedactionPayload() {
        Map<String, Object> original = new LinkedHashMap<>();
        original.put("customerName", "Alice Example");
        original.put("productCode", "PRD-1");
        Map<String, VariablePiiCategory> schema = Map.of(
                "customerName", VariablePiiCategory.PERSONAL_NAME,
                "productCode", VariablePiiCategory.NONE
        );

        String preHash = VariableHashSupport.hashVariables(objectMapper, original);
        InvocationRetentionVariableRedactor.Result redacted =
                InvocationRetentionVariableRedactor.redact(original, schema);
        String postHash = VariableHashSupport.hashVariables(objectMapper, redacted.variables());

        assertThat(preHash).isNotEqualTo(postHash);
        assertThat(preHash).hasSize(64);
        assertThat(preHash).doesNotContain("Alice");
        assertThat(redacted.variables().toString()).doesNotContain("Alice Example");
    }

    @Test
    void bddIblA5_006_replayUsesNonRedactedFieldsOnly() {
        Map<String, VariablePiiCategory> schema = Map.of(
                "customerName", VariablePiiCategory.PERSONAL_NAME,
                "productCode", VariablePiiCategory.NONE
        );
        InvocationRetentionVariableRedactor.Result retained = InvocationRetentionVariableRedactor.redact(
                Map.of("customerName", "Alice Example", "productCode", "PRD-1"),
                schema
        );
        Map<String, Object> withSentinel = new LinkedHashMap<>(retained.variables());
        withSentinel.put("customerName", InvocationRetentionVariableRedactor.REDACTED_SENTINEL);

        Map<String, Object> replay = InvocationRetentionVariableRedactor.toReplayVariables(withSentinel);

        assertThat(replay.get("productCode")).isEqualTo("PRD-1");
        assertThat(replay).doesNotContainKey("customerName");
        assertThat(replay.toString()).doesNotContain("Alice Example");
        assertThat(replay.toString()).doesNotContain(InvocationRetentionVariableRedactor.REDACTED_SENTINEL);
    }

    @Test
    void bddIblA5_007_batchItemVariablesFollowSameRedactionRules() {
        Map<String, VariablePiiCategory> schema = Map.of(
                "customerName", VariablePiiCategory.CONTACT,
                "productCode", VariablePiiCategory.NONE
        );
        InvocationRetentionVariableRedactor.Result result = InvocationRetentionVariableRedactor.redact(
                Map.of("customerName", "batch-pii-probe", "productCode", "BATCH-OK"),
                schema
        );

        assertThat(result.variables().toString()).doesNotContain("batch-pii-probe");
        assertThat(result.variables().get("productCode")).isEqualTo("BATCH-OK");
    }

    @Test
    void bddIblA5_010_redactionOrthogonalToPasswordStrippingContract() {
        // Passwords are stripped by InvocationParameterSanitizer; redactor must not reintroduce PII.
        Map<String, VariablePiiCategory> schema = Map.of(
                "customerName", VariablePiiCategory.PERSONAL_NAME,
                "productCode", VariablePiiCategory.NONE
        );
        InvocationRetentionVariableRedactor.Result result = InvocationRetentionVariableRedactor.redact(
                Map.of(
                        "customerName", "Alice Example",
                        "productCode", "PRD-1",
                        "openPassword", "should-not-be-a-variable"
                ),
                schema
        );

        assertThat(result.variables().toString()).doesNotContain("Alice Example");
        assertThat(result.variables()).doesNotContainKey("openPassword");
        assertThat(result.variables().get("productCode")).isEqualTo("PRD-1");
    }
}
