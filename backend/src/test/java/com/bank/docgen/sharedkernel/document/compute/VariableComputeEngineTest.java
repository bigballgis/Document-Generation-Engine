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
 * CE-K03 compute DSL matrix — ≥5 boundary cases per whitelist function + SPELL_AMOUNT table.
 */
class VariableComputeEngineTest {

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

    /**
     * IBL-A2 — ISO-currency FORMAT_AMOUNT (BDD-IBL-A2-001…009).
     */
    @Nested
    class FormatAmountIsoCurrency {

        @Test
        void eurWithEnUsDoesNotRenderDollar() {
            // BDD-IBL-A2-001
            String result = String.valueOf(engine.evaluateSingle(
                    "x",
                    "FORMAT_AMOUNT(${principal}, 'EUR')",
                    Map.of("principal", new BigDecimal("1234.56")),
                    "en-US"
            ));
            assertIdentifiesCurrency(result, "EUR", "€");
            assertThat(result).doesNotContain("$");
        }

        @Test
        void usdWithZhCnIdentifiesUsdNotCny() {
            // BDD-IBL-A2-002
            String result = String.valueOf(engine.evaluateSingle(
                    "x",
                    "FORMAT_AMOUNT(${principal}, 'USD')",
                    Map.of("principal", new BigDecimal("1234.56")),
                    "zh-CN"
            ));
            assertIdentifiesCurrency(result, "USD", "$");
        }

        @Test
        void cnyWithZhCnIdentifiesCnyNotUsd() {
            // BDD-IBL-A2-003
            String result = String.valueOf(engine.evaluateSingle(
                    "x",
                    "FORMAT_AMOUNT(${principal}, 'CNY')",
                    Map.of("principal", new BigDecimal("1234.56")),
                    "zh-CN"
            ));
            assertThat(result).satisfiesAnyOf(
                    s -> assertThat(s).contains("CNY"),
                    s -> assertThat(s).contains("¥"),
                    s -> assertThat(s).contains("￥")
            );
            assertThat(result).doesNotContain("$");
        }

        @Test
        void unaryStillUsesLocaleDefaultCurrency() {
            // BDD-IBL-A2-004
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
            assertThat(zh).isNotBlank();
            assertThat(en).isNotBlank();
        }

        @Test
        void sameIsoDifferentLocalesKeepEurIdentity() {
            // BDD-IBL-A2-005
            Map<String, Object> bindings = Map.of("principal", new BigDecimal("1234.56"));
            String en = String.valueOf(engine.evaluateSingle(
                    "x", "FORMAT_AMOUNT(${principal}, 'EUR')", bindings, "en-US"));
            String de = String.valueOf(engine.evaluateSingle(
                    "x", "FORMAT_AMOUNT(${principal}, 'EUR')", bindings, "de-DE"));
            assertIdentifiesCurrency(en, "EUR", "€");
            assertIdentifiesCurrency(de, "EUR", "€");
            assertThat(en).doesNotContain("$");
            assertThat(de).doesNotContain("$");
            assertThat(en).isNotEqualTo(de);
        }

        @Test
        void currencyFromVariableBinding() {
            // BDD-IBL-A2-006
            String result = String.valueOf(engine.evaluateSingle(
                    "x",
                    "FORMAT_AMOUNT(${principal}, ${ccy})",
                    Map.of("principal", 100, "ccy", "GBP"),
                    "en-US"
            ));
            assertIdentifiesCurrency(result, "GBP", "£");
            assertThat(result).doesNotContain("$");
        }

        @Test
        void blankOrNullCurrencyFailsClosed() {
            // BDD-IBL-A2-007
            Map<String, Object> nullCcy = new java.util.LinkedHashMap<>();
            nullCcy.put("principal", 100);
            nullCcy.put("ccy", null);
            assertThatThrownBy(() -> engine.evaluateSingle(
                    "x", "FORMAT_AMOUNT(${principal}, ${ccy})", nullCcy, "en-US"))
                    .isInstanceOf(VariableComputeException.class);

            assertThatThrownBy(() -> engine.evaluateSingle(
                    "x",
                    "FORMAT_AMOUNT(${principal}, ${ccy})",
                    Map.of("principal", 100, "ccy", ""),
                    "en-US"
            )).isInstanceOf(VariableComputeException.class);

            assertThatThrownBy(() -> engine.evaluateSingle(
                    "x",
                    "FORMAT_AMOUNT(${principal}, ${ccy})",
                    Map.of("principal", 100, "ccy", "   "),
                    "en-US"
            )).isInstanceOf(VariableComputeException.class);
        }

        @Test
        void invalidIsoCurrencyFailsClosed() {
            // BDD-IBL-A2-008
            assertThatThrownBy(() -> engine.evaluateSingle(
                    "x",
                    "FORMAT_AMOUNT(${principal}, 'NOTACURRENCY')",
                    Map.of("principal", 100),
                    "en-US"
            )).isInstanceOf(VariableComputeException.class);

            assertThatThrownBy(() -> engine.evaluateSingle(
                    "x",
                    "FORMAT_AMOUNT(${principal}, 'en-US')",
                    Map.of("principal", 100),
                    "en-US"
            )).isInstanceOf(VariableComputeException.class);
        }

        @Test
        void illegalArityFailsClosed() {
            // BDD-IBL-A2-009
            assertThatThrownBy(() -> engine.evaluateSingle(
                    "x",
                    "FORMAT_AMOUNT(${principal}, 'EUR', 'USD')",
                    Map.of("principal", 100),
                    "en-US"
            )).isInstanceOf(VariableComputeException.class);

            assertThatThrownBy(() -> engine.evaluateSingle("x", "FORMAT_AMOUNT()", Map.of(), "en-US"))
                    .isInstanceOf(VariableComputeException.class);
        }

        @Test
        void lowercaseCurrencyNormalized() {
            String result = String.valueOf(engine.evaluateSingle(
                    "x",
                    "FORMAT_AMOUNT(${principal}, 'eur')",
                    Map.of("principal", new BigDecimal("10")),
                    "en-US"
            ));
            assertIdentifiesCurrency(result, "EUR", "€");
            assertThat(result).doesNotContain("$");
        }

        @Test
        void binaryUsesCurrencyDefaultFractionDigits() {
            // A2-C5 — JPY default fraction digits = 0
            String result = String.valueOf(engine.evaluateSingle(
                    "x",
                    "FORMAT_AMOUNT(${principal}, 'JPY')",
                    Map.of("principal", new BigDecimal("1234")),
                    "en-US"
            ));
            assertIdentifiesCurrency(result, "JPY", "¥");
            assertThat(result).doesNotContain(".");
        }

        private static void assertIdentifiesCurrency(String formatted, String isoCode, String symbol) {
            assertThat(formatted).satisfiesAnyOf(
                    s -> assertThat(s).contains(isoCode),
                    s -> assertThat(s).contains(symbol)
            );
        }
    }

    @Nested
    class SpellAmount {

        @Test
        void zero() {
            assertThat(engine.evaluateSingle("x", "SPELL_AMOUNT(${p})", Map.of("p", 0), null))
                    .isEqualTo("零元整");
        }

        @Test
        void wholeYuan() {
            assertThat(engine.evaluateSingle("x", "SPELL_AMOUNT(${p})", Map.of("p", 100), null))
                    .isEqualTo("壹佰元整");
        }

        @Test
        void jiaoFen() {
            assertThat(engine.evaluateSingle("x", "SPELL_AMOUNT(${p})", Map.of("p", new BigDecimal("1.23")), null))
                    .isEqualTo("壹元贰角叁分");
        }

        @Test
        void yiLevel() {
            assertThat(engine.evaluateSingle("x", "SPELL_AMOUNT(${p})", Map.of("p", 100_000_000), null))
                    .isEqualTo("壹亿元整");
        }

        @Test
        void negativeRejected() {
            assertThatThrownBy(() -> engine.evaluateSingle("x", "SPELL_AMOUNT(${p})", Map.of("p", -1), null))
                    .isInstanceOf(VariableComputeException.class);
        }

        @Test
        void nullRejected() {
            assertThatThrownBy(() -> engine.evaluateSingle("x", "SPELL_AMOUNT(null)", Map.of(), null))
                    .isInstanceOf(VariableComputeException.class);
        }

        @Test
        void exceedsMaxRejected() {
            assertThatThrownBy(() -> engine.evaluateSingle(
                    "x",
                    "SPELL_AMOUNT(${p})",
                    Map.of("p", new BigDecimal("10000000000000")),
                    null
            )).isInstanceOf(VariableComputeException.class);
        }
    }

    /**
     * IBL-A3 — international amount-in-words (BDD-IBL-A3-001…011).
     */
    @Nested
    class SpellAmountInternational {

        @Test
        void enUsdOneThousandOnly() {
            // BDD-IBL-A3-001 — F3 golden literal
            assertThat(engine.evaluateSingle(
                    "x",
                    "SPELL_AMOUNT(${principal}, 'USD')",
                    Map.of("principal", 1000),
                    "en-US"
            )).isEqualTo("USD One Thousand Only");
        }

        @Test
        void enUsdWithCentsIsEnglishNotChinese() {
            // BDD-IBL-A3-002
            String result = String.valueOf(engine.evaluateSingle(
                    "x",
                    "SPELL_AMOUNT(${principal}, 'USD')",
                    Map.of("principal", new BigDecimal("1.23")),
                    "en"
            ));
            assertThat(result).isEqualTo("USD One And Twenty-Three Cents Only");
            assertThat(result).doesNotContain("元", "角", "分", "整");
            assertThat(result.toLowerCase(java.util.Locale.ROOT)).containsAnyOf("usd", "cent");
        }

        @Test
        void unaryIgnoresLocaleRemainsChinese() {
            // BDD-IBL-A3-003
            assertThat(engine.evaluateSingle(
                    "x",
                    "SPELL_AMOUNT(${principal})",
                    Map.of("principal", 100),
                    "en-US"
            )).isEqualTo("壹佰元整");
        }

        @Test
        void binaryCnyWithZhMatchesUnary() {
            // BDD-IBL-A3-004
            assertThat(engine.evaluateSingle(
                    "x",
                    "SPELL_AMOUNT(${principal}, 'CNY')",
                    Map.of("principal", 100),
                    "zh-CN"
            )).isEqualTo("壹佰元整");
        }

        @Test
        void unaryBoundaryTableRegression() {
            // BDD-IBL-A3-005
            assertThat(engine.evaluateSingle("x", "SPELL_AMOUNT(${p})", Map.of("p", 0), null))
                    .isEqualTo("零元整");
            assertThat(engine.evaluateSingle(
                    "x", "SPELL_AMOUNT(${p})", Map.of("p", new BigDecimal("1.23")), null))
                    .isEqualTo("壹元贰角叁分");
            assertThat(engine.evaluateSingle(
                    "x", "SPELL_AMOUNT(${p})", Map.of("p", 100_000_000), null))
                    .isEqualTo("壹亿元整");
            assertThatThrownBy(() -> engine.evaluateSingle(
                    "x", "SPELL_AMOUNT(${p})", Map.of("p", -1), null))
                    .isInstanceOf(VariableComputeException.class);
        }

        @Test
        void usdWithZhCnFailsClosed() {
            // BDD-IBL-A3-006 — default zh-CN also fails for binary USD
            assertThatThrownBy(() -> engine.evaluateSingle(
                    "x",
                    "SPELL_AMOUNT(${principal}, 'USD')",
                    Map.of("principal", 100),
                    "zh-CN"
            )).isInstanceOf(VariableComputeException.class);

            assertThatThrownBy(() -> engine.evaluateSingle(
                    "x",
                    "SPELL_AMOUNT(${principal}, 'USD')",
                    Map.of("principal", 100),
                    null
            )).isInstanceOf(VariableComputeException.class);
        }

        @Test
        void cnyWithEnUsFailsClosed() {
            // BDD-IBL-A3-007
            assertThatThrownBy(() -> engine.evaluateSingle(
                    "x",
                    "SPELL_AMOUNT(${principal}, 'CNY')",
                    Map.of("principal", 100),
                    "en-US"
            )).isInstanceOf(VariableComputeException.class);
        }

        @Test
        void invalidOrBlankCurrencyFailsClosed() {
            // BDD-IBL-A3-008
            assertThatThrownBy(() -> engine.evaluateSingle(
                    "x",
                    "SPELL_AMOUNT(${principal}, 'NOTACURRENCY')",
                    Map.of("principal", 100),
                    "en-US"
            )).isInstanceOf(VariableComputeException.class);

            Map<String, Object> nullCcy = new java.util.LinkedHashMap<>();
            nullCcy.put("principal", 100);
            nullCcy.put("ccy", null);
            assertThatThrownBy(() -> engine.evaluateSingle(
                    "x", "SPELL_AMOUNT(${principal}, ${ccy})", nullCcy, "en-US"))
                    .isInstanceOf(VariableComputeException.class);

            assertThatThrownBy(() -> engine.evaluateSingle(
                    "x",
                    "SPELL_AMOUNT(${principal}, ${ccy})",
                    Map.of("principal", 100, "ccy", ""),
                    "en-US"
            )).isInstanceOf(VariableComputeException.class);

            assertThatThrownBy(() -> engine.evaluateSingle(
                    "x",
                    "SPELL_AMOUNT(${principal}, 'en-US')",
                    Map.of("principal", 100),
                    "en-US"
            )).isInstanceOf(VariableComputeException.class);
        }

        @Test
        void illegalArityFailsClosed() {
            // BDD-IBL-A3-009
            assertThatThrownBy(() -> engine.evaluateSingle("x", "SPELL_AMOUNT()", Map.of(), "en-US"))
                    .isInstanceOf(VariableComputeException.class);

            assertThatThrownBy(() -> engine.evaluateSingle(
                    "x",
                    "SPELL_AMOUNT(${principal}, 'USD', 'EUR')",
                    Map.of("principal", 100),
                    "en-US"
            )).isInstanceOf(VariableComputeException.class);
        }

        @Test
        void currencyFromVariableBindingEnUsd() {
            // BDD-IBL-A3-010
            assertThat(engine.evaluateSingle(
                    "x",
                    "SPELL_AMOUNT(${principal}, ${ccy})",
                    Map.of("principal", 1000, "ccy", "USD"),
                    "en-US"
            )).isEqualTo("USD One Thousand Only");
        }

        @Test
        void unregisteredEurPairFailsClosed() {
            // BDD-IBL-A3-011 — framework: unregistered pair fails (not silent fallback)
            assertThatThrownBy(() -> engine.evaluateSingle(
                    "x",
                    "SPELL_AMOUNT(${principal}, 'EUR')",
                    Map.of("principal", 100),
                    "en-US"
            )).isInstanceOf(VariableComputeException.class);
            assertThat(SpellAmountSpellerRegistry.isSupported("en", "EUR")).isFalse();
            assertThat(SpellAmountSpellerRegistry.isSupported("en", "USD")).isTrue();
            assertThat(SpellAmountSpellerRegistry.isSupported("zh", "CNY")).isTrue();
        }

        @Test
        void lowercaseUsdNormalizedWithEn() {
            assertThat(engine.evaluateSingle(
                    "x",
                    "SPELL_AMOUNT(${principal}, 'usd')",
                    Map.of("principal", 1000),
                    "en-GB"
            )).isEqualTo("USD One Thousand Only");
        }

        @Test
        void enUsdZeroOnly() {
            assertThat(engine.evaluateSingle(
                    "x",
                    "SPELL_AMOUNT(${principal}, 'USD')",
                    Map.of("principal", 0),
                    "en-US"
            )).isEqualTo("USD Zero Only");
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
