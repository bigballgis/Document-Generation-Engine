package com.bank.docgen.authoring.structured.expression;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConditionExpressionEvaluatorTest {

    private final ConditionExpressionEvaluator evaluator = ConditionExpressionEvaluator.INSTANCE;

    @Test
    void extractVariableReferences_deduplicatesAndPreservesOrder() {
        List<String> refs = evaluator.extractVariableReferences("${customerName} != null && ${amount} >= 1000");

        assertThat(refs).containsExactly("customerName", "amount");
    }

    @Test
    void validateSyntax_acceptsBooleanEquality() {
        assertThat(evaluator.validateSyntax("${showNotice} == true")).isEmpty();
    }

    @Test
    void validateSyntax_rejectsTripleEquals() {
        assertThat(evaluator.validateSyntax("${x} === true")).isNotEmpty();
    }

    @Test
    void validateSyntax_rejectsInvalidOperator() {
        assertThat(evaluator.validateSyntax("${x} @ null")).isNotEmpty();
    }

    @Test
    void validateSyntax_rejectsBareIdentifier() {
        assertThat(evaluator.validateSyntax("customerName != null")).isNotEmpty();
    }

    @Test
    void validateSyntax_rejectsCodeInjectionPatterns() {
        assertThat(evaluator.validateSyntax("${x}.class.getName()")).isNotEmpty();
        assertThat(evaluator.validateSyntax("T(java.lang.Runtime).getRuntime()")).isNotEmpty();
    }

    @Test
    void evaluate_booleanAndNullComparisons() {
        Map<String, Object> variables = new java.util.LinkedHashMap<>();
        variables.put("showNotice", true);
        variables.put("customerName", "Alice");
        variables.put("optional", null);

        assertThat(evaluator.evaluate("${showNotice} == true", variables)).isTrue();
        assertThat(evaluator.evaluate("${customerName} != null", variables)).isTrue();
        assertThat(evaluator.evaluate("${optional} == null", variables)).isTrue();
    }

    @Test
    void evaluate_numericAndStringComparisons() {
        Map<String, Object> variables = Map.of(
                "amount", 1500,
                "status", "approved"
        );

        assertThat(evaluator.evaluate("${amount} >= 1000", variables)).isTrue();
        assertThat(evaluator.evaluate("${status} == 'approved'", variables)).isTrue();
    }

    @Test
    void evaluate_logicalCombinationWithParentheses() {
        Map<String, Object> variables = Map.of(
                "a", true,
                "b", false,
                "c", true
        );

        assertThat(evaluator.evaluate("(${a} && ${c}) || !${b}", variables)).isTrue();
    }

    @Test
    void evaluate_missingVariableTreatedAsNull() {
        assertThat(evaluator.evaluate("${missing} == null", Map.of())).isTrue();
    }

    @Test
    void evaluate_malformedExpressionReturnsFalse() {
        assertThat(evaluator.evaluate("${broken} === true", Map.of())).isFalse();
    }

    @Test
    void validateSyntax_rejectsBlankExpression() {
        assertThat(evaluator.validateSyntax("   ")).isNotEmpty();
    }

    @Test
    void extractVariableReferences_returnsEmptyForInvalidSyntax() {
        assertThat(evaluator.extractVariableReferences("${x} === true")).isEmpty();
    }

    @Test
    void evaluate_numericStringComparedAsNumberWhenParseable() {
        Map<String, Object> variables = Map.of("amount", "1500");

        assertThat(evaluator.evaluate("${amount} >= 1000", variables)).isTrue();
    }

    @Test
    void evaluate_booleanStringEqualityUsesParseBoolean() {
        Map<String, Object> variables = Map.of("showNotice", "true");

        assertThat(evaluator.evaluate("${showNotice} == true", variables)).isTrue();
    }
}
