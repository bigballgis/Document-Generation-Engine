package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.sharedkernel.api.FieldError;
import com.bank.docgen.template.domain.VariableType;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * IBL-A1 / CE-U03 shared VariableSchema payload rules.
 */
class VariableSchemaPayloadValidatorTest {

    private final UUID versionId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void bddIblA1_001_missingRequired_returnsRequired() {
        List<VariableSchemaEntity> schema = List.of(
                variable("customerName", VariableType.TEXT, true, null, null)
        );

        List<FieldError> errors = VariableSchemaPayloadValidator.validate(schema, Map.of());

        assertThat(errors).anySatisfy(error -> {
            assertThat(error.field()).isEqualTo("customerName");
            assertThat(error.reason()).isEqualTo("REQUIRED");
        });
    }

    @Test
    void bddIblA1_002_invalidAmountType_returnsInvalidType() {
        List<VariableSchemaEntity> schema = List.of(
                variable("principalAmount", VariableType.AMOUNT, true, null, null)
        );

        List<FieldError> errors = VariableSchemaPayloadValidator.validate(
                schema,
                Map.of("principalAmount", "not-a-number")
        );

        assertThat(errors).anySatisfy(error -> {
            assertThat(error.field()).isEqualTo("principalAmount");
            assertThat(error.reason()).isEqualTo("INVALID_TYPE");
        });
    }

    @Test
    void bddIblA1_003_enumNotAllowed_returnsEnumNotAllowed() {
        List<VariableSchemaEntity> schema = List.of(
                variable("letterType", VariableType.ENUM, true, null, "[\"OFFER\",\"CONFIRM\"]")
        );

        List<FieldError> errors = VariableSchemaPayloadValidator.validate(
                schema,
                Map.of("letterType", "NOT_IN_ENUM")
        );

        assertThat(errors).anySatisfy(error -> {
            assertThat(error.field()).isEqualTo("letterType");
            assertThat(error.reason()).isEqualTo("ENUM_NOT_ALLOWED");
        });
    }

    @Test
    void bddIblA1_004_validPayload_noErrors() {
        List<VariableSchemaEntity> schema = List.of(
                variable("customerName", VariableType.TEXT, true, null, null),
                variable("principalAmount", VariableType.AMOUNT, true, null, null),
                variable("letterType", VariableType.ENUM, true, null, "OFFER,CONFIRM")
        );

        List<FieldError> errors = VariableSchemaPayloadValidator.validate(
                schema,
                Map.of(
                        "customerName", "Acme Bank",
                        "principalAmount", "1000.50",
                        "letterType", "OFFER"
                )
        );

        assertThat(errors).isEmpty();
    }

    @Test
    void bddIblA1_008_computeFieldOmitted_notRequired() {
        List<VariableSchemaEntity> schema = List.of(
                variable("customerName", VariableType.TEXT, true, null, null),
                compute("amountWords", VariableType.COMPUTED, "${principal}")
        );

        List<FieldError> errors = VariableSchemaPayloadValidator.validate(
                schema,
                Map.of("customerName", "Acme")
        );

        assertThat(errors).isEmpty();
    }

    @Test
    void unknownField_returnsUnknownField() {
        List<VariableSchemaEntity> schema = List.of(
                variable("customerName", VariableType.TEXT, false, null, null)
        );

        List<FieldError> errors = VariableSchemaPayloadValidator.validate(
                schema,
                Map.of("mystery", "x")
        );

        assertThat(errors).anySatisfy(error -> {
            assertThat(error.field()).isEqualTo("mystery");
            assertThat(error.reason()).isEqualTo("UNKNOWN_FIELD");
        });
    }

    @Test
    void defaultValuePresent_missingRequiredStillFails() {
        List<VariableSchemaEntity> schema = List.of(
                variable("customerName", VariableType.TEXT, true, "Default Corp", null)
        );

        List<FieldError> errors = VariableSchemaPayloadValidator.validate(schema, Map.of());

        assertThat(errors).anySatisfy(error -> {
            assertThat(error.field()).isEqualTo("customerName");
            assertThat(error.reason()).isEqualTo("REQUIRED");
        });
    }

    @Test
    void stripComputeKeys_removesComputeEntries() {
        List<VariableSchemaEntity> schema = List.of(
                variable("customerName", VariableType.TEXT, true, null, null),
                compute("amountWords", VariableType.TEXT, "${principal}")
        );

        Map<String, Object> stripped = VariableSchemaPayloadValidator.stripComputeKeys(
                schema,
                Map.of("customerName", "Acme", "amountWords", "should-drop")
        );

        assertThat(stripped).containsEntry("customerName", "Acme").doesNotContainKey("amountWords");
    }

    private VariableSchemaEntity variable(
            String key,
            VariableType type,
            boolean required,
            String defaultValue,
            String enumValues
    ) {
        return new VariableSchemaEntity(
                UUID.randomUUID(),
                versionId,
                key,
                type,
                required,
                defaultValue,
                enumValues,
                null,
                null
        );
    }

    private VariableSchemaEntity compute(String key, VariableType type, String expression) {
        return new VariableSchemaEntity(
                UUID.randomUUID(),
                versionId,
                key,
                type,
                false,
                null,
                null,
                null,
                expression
        );
    }
}
