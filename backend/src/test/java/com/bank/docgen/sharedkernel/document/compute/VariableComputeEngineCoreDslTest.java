package com.bank.docgen.sharedkernel.document.compute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * CE-K03 compute DSL core — security, coalesce/aggregates/filter/format, topology.
 * Peeled from VariableComputeEngineTest (AI-SCALE #169).
 */
class VariableComputeEngineCoreDslTest {

    private final VariableComputeEngine engine = VariableComputeEngine.INSTANCE;

    @Nested
    class SecurityAndBounds {

        @Test
        void unknownFunctionFails() {
            assertThatThrownBy(() -> engine.evaluateSingle("x", "FOO(${a})", Map.of("a", 1), null))
                    .isInstanceOf(VariableComputeException.class)
                    .satisfies(ex -> {
                        VariableComputeException vce = (VariableComputeException) ex;
                        assertThat(vce.variableKey()).isEqualTo("x");
                        assertThat(vce.expressionSummary()).contains("FOO");
                    });
        }

        @Test
        void methodCallConstructFails() {
            assertThatThrownBy(() -> engine.evaluateSingle("x", "${a}.toString()", Map.of("a", 1), null))
                    .isInstanceOf(VariableComputeException.class);
        }

        @Test
        void expressionTooLongFails() {
            String expression = "COALESCE(" + "a".repeat(ComputeDslLimits.MAX_EXPRESSION_LENGTH) + ")";
            assertThatThrownBy(() -> engine.validateExpression("x", expression, Set.of("a")))
                    .isInstanceOf(VariableComputeException.class);
        }

        @Test
        void nestingDepthExceededFails() {
            String expression = "COALESCE(COALESCE(COALESCE(COALESCE(COALESCE(COALESCE(COALESCE(COALESCE(COALESCE(${a},1),1),1),1),1),1),1),1),1)";
            assertThatThrownBy(() -> engine.validateExpression("x", expression, Set.of("a")))
                    .isInstanceOf(VariableComputeException.class);
        }

        @Test
        void whitelistDoesNotIncludeScriptEngines() {
            assertThat(ComputeExpressionParser.whitelist())
                    .containsExactlyInAnyOrder(
                            "COALESCE", "SUM", "COUNT", "AVG", "FILTER",
                            "FORMAT_AMOUNT", "FORMAT_DATE", "SPELL_AMOUNT"
                    )
                    .doesNotContain("eval", "Groovy", "SpEL");
        }
    
    }

    @Nested
    class Coalesce {

        @Test
        void returnsFirstNonNull() {
            Object result = engine.evaluateSingle(
                    "x",
                    "COALESCE(${a}, ${b}, 'N/A')",
                    Map.of("b", "ok"),
                    null
            );
            assertThat(result).isEqualTo("ok");
        }

        @Test
        void allMissingReturnsNull() {
            assertThat(engine.evaluateSingle("x", "COALESCE(null, null)", Map.of(), null)).isNull();
        }

        @Test
        void prefersFirstLiteral() {
            assertThat(engine.evaluateSingle("x", "COALESCE(null, 2, 3)", Map.of(), null))
                    .isEqualTo(new BigDecimal("2"));
        }

        @Test
        void booleanFalseIsNotNull() {
            assertThat(engine.evaluateSingle("x", "COALESCE(false, true)", Map.of(), null)).isEqualTo(false);
        }

        @Test
        void emptyArgsFail() {
            assertThatThrownBy(() -> engine.evaluateSingle("x", "COALESCE()", Map.of(), null))
                    .isInstanceOf(VariableComputeException.class);
        }
    
    }

    @Nested
    class SumCountAvg {

        @Test
        void sumCountAvgOnNumbers() {
            Map<String, Object> bindings = Map.of("nums", List.of(1, 2, 3));
            assertThat(engine.evaluateSingle("s", "SUM(${nums})", bindings, null))
                    .isEqualTo(new BigDecimal("6"));
            assertThat(engine.evaluateSingle("c", "COUNT(${nums})", bindings, null)).isEqualTo(3L);
            assertThat(((BigDecimal) engine.evaluateSingle("a", "AVG(${nums})", bindings, null))
                    .compareTo(new BigDecimal("2"))).isZero();
        }

        @Test
        void sumEmptyIsZero() {
            assertThat(engine.evaluateSingle("s", "SUM(${nums})", Map.of("nums", List.of()), null))
                    .isEqualTo(BigDecimal.ZERO);
        }

        @Test
        void countEmptyIsZero() {
            assertThat(engine.evaluateSingle("c", "COUNT(${nums})", Map.of("nums", List.of()), null))
                    .isEqualTo(0L);
        }

        @Test
        void avgEmptyFails() {
            assertThatThrownBy(() -> engine.evaluateSingle("a", "AVG(${nums})", Map.of("nums", List.of()), null))
                    .isInstanceOf(VariableComputeException.class);
        }

        @Test
        void sumNonNumericFails() {
            assertThatThrownBy(() ->
                    engine.evaluateSingle("s", "SUM(${nums})", Map.of("nums", List.of("x")), null))
                    .isInstanceOf(VariableComputeException.class);
        }
    
    }

    @Nested
    class FilterFn {

        @Test
        void filterPlusSum() {
            List<Map<String, Object>> items = List.of(
                    Map.of("amount", 10),
                    Map.of("amount", -5),
                    Map.of("amount", 20)
            );
            Object result = engine.evaluateSingle(
                    "x",
                    "SUM(FILTER(${items}, amount, GT, 0))",
                    Map.of("items", items),
                    null
            );
            assertThat(result).isEqualTo(new BigDecimal("30"));
        }

        @Test
        void illegalOpFails() {
            assertThatThrownBy(() -> engine.evaluateSingle(
                    "x",
                    "FILTER(${items}, amount, LIKE, 'x')",
                    Map.of("items", List.of(Map.of("amount", 1))),
                    null
            )).isInstanceOf(VariableComputeException.class);
        }

        @Test
        void eqFilter() {
            List<Map<String, Object>> items = List.of(
                    Map.of("status", "A"),
                    Map.of("status", "B")
            );
            assertThat(engine.evaluateSingle(
                    "x",
                    "COUNT(FILTER(${items}, status, EQ, 'A'))",
                    Map.of("items", items),
                    null
            )).isEqualTo(1L);
        }

        @Test
        void isNullFilter() {
            List<Map<String, Object>> items = List.of(
                    mapOfNullable("v", null),
                    Map.of("v", 1)
            );
            assertThat(engine.evaluateSingle(
                    "x",
                    "COUNT(FILTER(${items}, v, IS_NULL))",
                    Map.of("items", items),
                    null
            )).isEqualTo(1L);
        }

        @Test
        void neFilter() {
            List<Map<String, Object>> items = List.of(Map.of("n", 1), Map.of("n", 2));
            assertThat(engine.evaluateSingle(
                    "x",
                    "COUNT(FILTER(${items}, n, NE, 1))",
                    Map.of("items", items),
                    null
            )).isEqualTo(1L);
        }

        private static Map<String, Object> mapOfNullable(String key, Object value) {
            java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
            map.put(key, value);
            return map;
        }
    
    }

    @Nested
    class FormatFunctions {

        @Test
        void formatAmountDefaultsZhCn() {
            String zh = String.valueOf(engine.evaluateSingle(
                    "x",
                    "FORMAT_AMOUNT(${principal})",
                    Map.of("principal", new BigDecimal("1234.5")),
                    null
            ));
            assertThat(zh).contains("1").contains("234");
        }

        @Test
        void formatAmountRespectsLocale() {
            String zh = String.valueOf(engine.evaluateSingle(
                    "x",
                    "FORMAT_AMOUNT(${principal})",
                    Map.of("principal", new BigDecimal("1234.5")),
                    "zh-CN"
            ));
            String en = String.valueOf(engine.evaluateSingle(
                    "x",
                    "FORMAT_AMOUNT(${principal})",
                    Map.of("principal", new BigDecimal("1234.5")),
                    "en-US"
            ));
            assertThat(zh).isNotEqualTo(en);
        }

        @Test
        void formatDateDefaultsZhCn() {
            String result = String.valueOf(engine.evaluateSingle(
                    "x",
                    "FORMAT_DATE(${signDate})",
                    Map.of("signDate", "2024-01-15"),
                    null
            ));
            assertThat(result).isNotBlank();
        }

        @Test
        void formatDateEnUsDistinct() {
            String zh = String.valueOf(engine.evaluateSingle(
                    "x", "FORMAT_DATE(${signDate})", Map.of("signDate", "2024-01-15"), "zh-CN"));
            String en = String.valueOf(engine.evaluateSingle(
                    "x", "FORMAT_DATE(${signDate})", Map.of("signDate", "2024-01-15"), "en-US"));
            assertThat(zh).isNotEqualTo(en);
        }

        @Test
        void formatAmountNullFails() {
            assertThatThrownBy(() -> engine.evaluateSingle("x", "FORMAT_AMOUNT(null)", Map.of(), null))
                    .isInstanceOf(VariableComputeException.class);
        }
    
    }

    @Nested
    class TopologyAndAssembly {

        @Test
        void evaluatesBeforeBindingInjectionOrder() {
            List<ComputeVariableDefinition> defs = List.of(
                    new ComputeVariableDefinition("principal", null, false),
                    new ComputeVariableDefinition("principalCn", "SPELL_AMOUNT(${principal})", true)
            );
            Map<String, Object> result = engine.evaluateAll(defs, Map.of("principal", 100), "zh-CN");
            assertThat(result.get("principalCn")).isEqualTo("壹佰元整");
        }

        @Test
        void callerComputeKeyIgnored() {
            List<ComputeVariableDefinition> defs = List.of(
                    new ComputeVariableDefinition("principal", null, false),
                    new ComputeVariableDefinition("principalCn", "SPELL_AMOUNT(${principal})", true)
            );
            Map<String, Object> result = engine.evaluateAll(
                    defs,
                    Map.of("principal", 100, "principalCn", "HACKED"),
                    null
            );
            assertThat(result.get("principalCn")).isEqualTo("壹佰元整");
        }

        @Test
        void missingReferenceFailsAtValidate() {
            assertThatThrownBy(() -> engine.validateExpression(
                    "x",
                    "SPELL_AMOUNT(${missing})",
                    Set.of("principal")
            )).isInstanceOf(VariableComputeException.class);

            List<ComputeVariableDefinition> defs = List.of(
                    new ComputeVariableDefinition("x", "SPELL_AMOUNT(${missing})", true)
            );
            assertThatThrownBy(() -> engine.evaluateAll(defs, Map.of(), null))
                    .isInstanceOf(VariableComputeException.class);
        }

        @Test
        void cyclicDependencyFails() {
            List<ComputeVariableDefinition> defs = List.of(
                    new ComputeVariableDefinition("a", "COALESCE(${b}, 1)", true),
                    new ComputeVariableDefinition("b", "COALESCE(${a}, 2)", true)
            );
            assertThatThrownBy(() -> engine.evaluateAll(defs, Map.of(), null))
                    .isInstanceOf(VariableComputeException.class)
                    .hasMessageContaining("Circular");
        }

        @Test
        void computeDependsOnCompute() {
            List<ComputeVariableDefinition> defs = List.of(
                    new ComputeVariableDefinition("n", null, false),
                    new ComputeVariableDefinition("doubled", "SUM(${n})", true),
                    new ComputeVariableDefinition("label", "COALESCE(${doubled}, 0)", true)
            );
            Map<String, Object> result = engine.evaluateAll(
                    defs,
                    Map.of("n", List.of(1, 2)),
                    null
            );
            assertThat(result.get("doubled")).isEqualTo(new BigDecimal("3"));
            assertThat(result.get("label")).isEqualTo(new BigDecimal("3"));
        }
    
    }
}
