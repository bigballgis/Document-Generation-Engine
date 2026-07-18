package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.runtime.api.ContractVariableSchemaView;
import com.bank.docgen.runtime.domain.ContractVariableCompatibility;
import com.bank.docgen.template.domain.VariablePiiCategory;
import com.bank.docgen.template.domain.VariableType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * BDD-IBL-A4-006…009 — consumer contract breaking / non-breaking gate.
 */
class ContractVariableCompatibilityClassifierTest {

    private static final List<ContractVariableSchemaView> GOLDEN = List.of(
            field("customerName", VariableType.TEXT, true, false, VariablePiiCategory.NONE, null),
            field(
                    "letterType",
                    VariableType.ENUM,
                    true,
                    false,
                    VariablePiiCategory.NONE,
                    List.of("OFFER")
            ),
            field("optionalNote", VariableType.TEXT, false, false, VariablePiiCategory.NONE, null)
    );

    @Test
    void renameIsBreaking() {
        List<ContractVariableSchemaView> candidate = replaceKey(GOLDEN, "customerName", "clientName");
        assertThat(ContractVariableCompatibilityClassifier.classify(GOLDEN, candidate))
                .isEqualTo(ContractVariableCompatibility.BREAKING);
    }

    @Test
    void deleteFieldIsBreaking() {
        List<ContractVariableSchemaView> candidate = GOLDEN.stream()
                .filter(view -> !"optionalNote".equals(view.variableKey()))
                .toList();
        assertThat(ContractVariableCompatibilityClassifier.classify(GOLDEN, candidate))
                .isEqualTo(ContractVariableCompatibility.BREAKING);
    }

    @Test
    void typeChangeIsBreaking() {
        List<ContractVariableSchemaView> candidate = mutate(
                GOLDEN,
                "customerName",
                view -> new ContractVariableSchemaView(
                        view.variableKey(),
                        VariableType.NUMBER,
                        view.required(),
                        view.computed(),
                        view.piiCategory(),
                        view.enumValues(),
                        view.description()
                )
        );
        assertThat(ContractVariableCompatibilityClassifier.classify(GOLDEN, candidate))
                .isEqualTo(ContractVariableCompatibility.BREAKING);
    }

    @Test
    void requiredTighteningIsBreaking() {
        List<ContractVariableSchemaView> candidate = mutate(
                GOLDEN,
                "optionalNote",
                view -> new ContractVariableSchemaView(
                        view.variableKey(),
                        view.variableType(),
                        true,
                        view.computed(),
                        view.piiCategory(),
                        view.enumValues(),
                        view.description()
                )
        );
        assertThat(ContractVariableCompatibilityClassifier.classify(GOLDEN, candidate))
                .isEqualTo(ContractVariableCompatibility.BREAKING);
    }

    @Test
    void enumShrinkIsBreaking() {
        List<ContractVariableSchemaView> baseline = List.of(
                field(
                        "letterType",
                        VariableType.ENUM,
                        true,
                        false,
                        VariablePiiCategory.NONE,
                        List.of("OFFER", "REMINDER")
                )
        );
        List<ContractVariableSchemaView> candidate = List.of(
                field(
                        "letterType",
                        VariableType.ENUM,
                        true,
                        false,
                        VariablePiiCategory.NONE,
                        List.of("OFFER")
                )
        );
        assertThat(ContractVariableCompatibilityClassifier.classify(baseline, candidate))
                .isEqualTo(ContractVariableCompatibility.BREAKING);
    }

    @Test
    void enterableToComputedIsBreaking() {
        List<ContractVariableSchemaView> candidate = mutate(
                GOLDEN,
                "customerName",
                view -> new ContractVariableSchemaView(
                        view.variableKey(),
                        view.variableType(),
                        view.required(),
                        true,
                        view.piiCategory(),
                        view.enumValues(),
                        view.description()
                )
        );
        assertThat(ContractVariableCompatibilityClassifier.classify(GOLDEN, candidate))
                .isEqualTo(ContractVariableCompatibility.BREAKING);
    }

    @Test
    void additiveOptionalFieldIsNonBreaking() {
        List<ContractVariableSchemaView> candidate = new ArrayList<>(GOLDEN);
        candidate.add(field("newOptional", VariableType.TEXT, false, false, VariablePiiCategory.NONE, null));
        assertThat(ContractVariableCompatibilityClassifier.classify(GOLDEN, candidate))
                .isEqualTo(ContractVariableCompatibility.NON_BREAKING);
    }

    @Test
    void enumWidenAndDescriptionOnlyAreNonBreaking() {
        List<ContractVariableSchemaView> widened = mutate(
                GOLDEN,
                "letterType",
                view -> new ContractVariableSchemaView(
                        view.variableKey(),
                        view.variableType(),
                        view.required(),
                        view.computed(),
                        view.piiCategory(),
                        List.of("OFFER", "REMINDER"),
                        view.description()
                )
        );
        assertThat(ContractVariableCompatibilityClassifier.classify(GOLDEN, widened))
                .isEqualTo(ContractVariableCompatibility.NON_BREAKING);

        List<ContractVariableSchemaView> descriptionOnly = mutate(
                GOLDEN,
                "customerName",
                view -> new ContractVariableSchemaView(
                        view.variableKey(),
                        view.variableType(),
                        view.required(),
                        view.computed(),
                        view.piiCategory(),
                        view.enumValues(),
                        "updated description only"
                )
        );
        assertThat(ContractVariableCompatibilityClassifier.classify(GOLDEN, descriptionOnly))
                .isEqualTo(ContractVariableCompatibility.NON_BREAKING);
    }

    @Test
    void requiredRelaxationAndPiiChangeAreNonBreaking() {
        List<ContractVariableSchemaView> relaxed = mutate(
                GOLDEN,
                "customerName",
                view -> new ContractVariableSchemaView(
                        view.variableKey(),
                        view.variableType(),
                        false,
                        view.computed(),
                        view.piiCategory(),
                        view.enumValues(),
                        view.description()
                )
        );
        assertThat(ContractVariableCompatibilityClassifier.classify(GOLDEN, relaxed))
                .isEqualTo(ContractVariableCompatibility.NON_BREAKING);

        List<ContractVariableSchemaView> piiChanged = mutate(
                GOLDEN,
                "customerName",
                view -> new ContractVariableSchemaView(
                        view.variableKey(),
                        view.variableType(),
                        view.required(),
                        view.computed(),
                        VariablePiiCategory.PERSONAL_NAME,
                        view.enumValues(),
                        view.description()
                )
        );
        assertThat(ContractVariableCompatibilityClassifier.classify(GOLDEN, piiChanged))
                .isEqualTo(ContractVariableCompatibility.NON_BREAKING);
    }

    @Test
    void goldenBaselineMatchesItself() {
        assertThat(ContractVariableCompatibilityClassifier.classify(GOLDEN, GOLDEN))
                .isEqualTo(ContractVariableCompatibility.NON_BREAKING);
    }

    private static ContractVariableSchemaView field(
            String key,
            VariableType type,
            boolean required,
            boolean computed,
            VariablePiiCategory piiCategory,
            List<String> enumValues
    ) {
        return new ContractVariableSchemaView(
                key,
                type,
                required,
                computed,
                piiCategory,
                enumValues,
                "baseline-" + key
        );
    }

    private static List<ContractVariableSchemaView> replaceKey(
            List<ContractVariableSchemaView> baseline,
            String oldKey,
            String newKey
    ) {
        return baseline.stream()
                .map(view -> oldKey.equals(view.variableKey())
                        ? new ContractVariableSchemaView(
                                newKey,
                                view.variableType(),
                                view.required(),
                                view.computed(),
                                view.piiCategory(),
                                view.enumValues(),
                                view.description()
                        )
                        : view)
                .toList();
    }

    private static List<ContractVariableSchemaView> mutate(
            List<ContractVariableSchemaView> baseline,
            String key,
            java.util.function.Function<ContractVariableSchemaView, ContractVariableSchemaView> mutator
    ) {
        return baseline.stream()
                .map(view -> key.equals(view.variableKey()) ? mutator.apply(view) : view)
                .toList();
    }
}
