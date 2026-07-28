package com.bank.docgen.sharedkernel.document.compute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * CE-K03 FORMAT_DATE timezone + FORMAT_AMOUNT ISO currency matrix.
 * Peeled from VariableComputeEngineTest (AI-SCALE #169).
 */
class VariableComputeEngineFormatLocaleTest {

    private final VariableComputeEngine engine = VariableComputeEngine.INSTANCE;

    /**
    * PQH-F8 — FORMAT_DATE timezone / as-of (BDD-PQH-F8-001…011).
    */
    @Nested
    class FormatDateTimezone {

        @Test
        void dateOnlyIsoIsZoneIndependent() {
            // BDD-PQH-F8-001
            String result = String.valueOf(engine.evaluateSingle(
                    "x",
                    "FORMAT_DATE(${signDate})",
                    Map.of("signDate", "2024-01-15"),
                    "en-US"
            ));
            assertThat(result).isEqualTo(medium(LocalDate.of(2024, 1, 15), "en-US"));
        }

        @Test
        void localeChangesDisplayNotCalendarDay() {
            // BDD-PQH-F8-002
            Map<String, Object> bindings = Map.of("signDate", "2024-01-15");
            String zh = String.valueOf(engine.evaluateSingle(
                    "x", "FORMAT_DATE(${signDate})", bindings, "zh-CN"));
            String en = String.valueOf(engine.evaluateSingle(
                    "x", "FORMAT_DATE(${signDate})", bindings, "en-US"));
            assertThat(zh).isEqualTo(medium(LocalDate.of(2024, 1, 15), "zh-CN"));
            assertThat(en).isEqualTo(medium(LocalDate.of(2024, 1, 15), "en-US"));
            assertThat(zh).isNotEqualTo(en);
        }

        @Test
        void unaryInstantAndDateUseUtcCalendarDay() {
            // BDD-PQH-F8-003
            Instant instant = Instant.parse("2024-01-15T23:30:00Z");
            String fromInstant = String.valueOf(engine.evaluateSingle(
                    "x",
                    "FORMAT_DATE(${eventAt})",
                    Map.of("eventAt", instant),
                    "en-US"
            ));
            String fromDate = String.valueOf(engine.evaluateSingle(
                    "x",
                    "FORMAT_DATE(${eventAt})",
                    Map.of("eventAt", Date.from(instant)),
                    "en-US"
            ));
            assertThat(fromInstant).isEqualTo(medium(LocalDate.of(2024, 1, 15), "en-US"));
            assertThat(fromDate).isEqualTo(medium(LocalDate.of(2024, 1, 15), "en-US"));
        }

        @Test
        void binaryZoneShiftsInstantCalendarDay() {
            // BDD-PQH-F8-004
            Instant instant = Instant.parse("2024-01-15T23:30:00Z");
            String result = String.valueOf(engine.evaluateSingle(
                    "x",
                    "FORMAT_DATE(${eventAt}, 'Asia/Shanghai')",
                    Map.of("eventAt", instant),
                    "en-US"
            ));
            assertThat(result).isEqualTo(medium(LocalDate.of(2024, 1, 16), "en-US"));
            assertThat(result).isNotEqualTo(medium(LocalDate.of(2024, 1, 15), "en-US"));
        }

        @Test
        void isoDatetimeStringDoesNotPrefixTruncate() {
            // BDD-PQH-F8-005
            String result = String.valueOf(engine.evaluateSingle(
                    "x",
                    "FORMAT_DATE(${eventAt}, 'Asia/Shanghai')",
                    Map.of("eventAt", "2024-01-15T23:30:00Z"),
                    "en-US"
            ));
            assertThat(result).isEqualTo(medium(LocalDate.of(2024, 1, 16), "en-US"));
            assertThat(result).isNotEqualTo(medium(LocalDate.of(2024, 1, 15), "en-US"));
        }

        @Test
        void unaryOffsetDateTimeUsesEmbeddedLocalDate() {
            // BDD-PQH-F8-006
            OffsetDateTime eventAt = OffsetDateTime.parse("2024-01-15T23:30:00+08:00");
            String result = String.valueOf(engine.evaluateSingle(
                    "x",
                    "FORMAT_DATE(${eventAt})",
                    Map.of("eventAt", eventAt),
                    "en-US"
            ));
            assertThat(result).isEqualTo(medium(LocalDate.of(2024, 1, 15), "en-US"));
        }

        @Test
        void binaryOffsetDateTimeConvertsViaInstantAndZone() {
            // BDD-PQH-F8-007
            OffsetDateTime eventAt = OffsetDateTime.parse("2024-01-15T23:30:00+08:00");
            String result = String.valueOf(engine.evaluateSingle(
                    "x",
                    "FORMAT_DATE(${eventAt}, 'UTC')",
                    Map.of("eventAt", eventAt),
                    "en-US"
            ));
            assertThat(result).isEqualTo(medium(LocalDate.of(2024, 1, 15), "en-US"));
        }

        @Test
        void binaryBlankOrInvalidZoneFailsClosed() {
            // BDD-PQH-F8-008
            Instant instant = Instant.parse("2024-01-15T23:30:00Z");
            Map<String, Object> nullTz = new java.util.LinkedHashMap<>();
            nullTz.put("eventAt", instant);
            nullTz.put("tz", null);
            assertThatThrownBy(() -> engine.evaluateSingle(
                    "x", "FORMAT_DATE(${eventAt}, ${tz})", nullTz, "en-US"))
                    .isInstanceOf(VariableComputeException.class);

            assertThatThrownBy(() -> engine.evaluateSingle(
                    "x",
                    "FORMAT_DATE(${eventAt}, ${tz})",
                    Map.of("eventAt", instant, "tz", ""),
                    "en-US"
            )).isInstanceOf(VariableComputeException.class);

            assertThatThrownBy(() -> engine.evaluateSingle(
                    "x",
                    "FORMAT_DATE(${eventAt}, 'Not/AZone')",
                    Map.of("eventAt", instant),
                    "en-US"
            )).isInstanceOf(VariableComputeException.class);
        }

        @Test
        void illegalArityFailsClosed() {
            // BDD-PQH-F8-009
            assertThatThrownBy(() -> engine.evaluateSingle("x", "FORMAT_DATE()", Map.of(), "en-US"))
                    .isInstanceOf(VariableComputeException.class);

            assertThatThrownBy(() -> engine.evaluateSingle(
                    "x",
                    "FORMAT_DATE(${d}, 'UTC', 'extra')",
                    Map.of("d", "2024-01-15"),
                    "en-US"
            )).isInstanceOf(VariableComputeException.class);
        }

        @Test
        void zoneArgIsNotALocaleTag() {
            // BDD-PQH-F8-010
            Instant instant = Instant.parse("2024-01-15T23:30:00Z");
            assertThatThrownBy(() -> engine.evaluateSingle(
                    "x",
                    "FORMAT_DATE(${eventAt}, 'en-US')",
                    Map.of("eventAt", instant),
                    "en-US"
            )).isInstanceOf(VariableComputeException.class);

            String ok = String.valueOf(engine.evaluateSingle(
                    "x",
                    "FORMAT_DATE(${signDate})",
                    Map.of("signDate", "2024-01-15"),
                    "en-US"
            ));
            assertThat(ok).isEqualTo(medium(LocalDate.of(2024, 1, 15), "en-US"));
        }

        @Test
        void dateOnlyWithUnusedZoneStillSucceeds() {
            // BDD-PQH-F8-011
            String result = String.valueOf(engine.evaluateSingle(
                    "x",
                    "FORMAT_DATE(${signDate}, 'Asia/Shanghai')",
                    Map.of("signDate", "2024-01-15"),
                    "en-US"
            ));
            assertThat(result).isEqualTo(medium(LocalDate.of(2024, 1, 15), "en-US"));
        }

        @Test
        void unaryZonedDateTimeUsesEmbeddedLocalDate() {
            ZonedDateTime eventAt = ZonedDateTime.parse("2024-01-15T23:30:00+08:00[Asia/Shanghai]");
            String result = String.valueOf(engine.evaluateSingle(
                    "x",
                    "FORMAT_DATE(${eventAt})",
                    Map.of("eventAt", eventAt),
                    "en-US"
            ));
            assertThat(result).isEqualTo(medium(LocalDate.of(2024, 1, 15), "en-US"));
        }

        @Test
        void nullValueFailsClosed() {
            assertThatThrownBy(() -> engine.evaluateSingle("x", "FORMAT_DATE(null)", Map.of(), "en-US"))
                    .isInstanceOf(VariableComputeException.class);
        }

        @Test
        void ambiguousLocalDateTimeStringFailsClosed() {
            assertThatThrownBy(() -> engine.evaluateSingle(
                    "x",
                    "FORMAT_DATE(${eventAt})",
                    Map.of("eventAt", "2024-01-15T23:30:00"),
                    "en-US"
            )).isInstanceOf(VariableComputeException.class);
        }

        private static String medium(LocalDate date, String localeTag) {
            Locale locale = Locale.forLanguageTag(localeTag);
            return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(date);
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
}
