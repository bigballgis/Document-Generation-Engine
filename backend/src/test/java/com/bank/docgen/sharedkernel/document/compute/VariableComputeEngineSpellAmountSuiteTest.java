package com.bank.docgen.sharedkernel.document.compute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * CE-K03 SPELL_AMOUNT Chinese + international matrix.
 * Peeled from VariableComputeEngineTest (AI-SCALE #169).
 */
class VariableComputeEngineSpellAmountSuiteTest {

    private final VariableComputeEngine engine = VariableComputeEngine.INSTANCE;

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
}
