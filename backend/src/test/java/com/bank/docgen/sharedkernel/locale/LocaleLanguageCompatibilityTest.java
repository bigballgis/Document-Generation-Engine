package com.bank.docgen.sharedkernel.locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * BDD-IBL-E1-008 / E1-C8 — primary BCP-47 language subtag compatibility.
 */
class LocaleLanguageCompatibilityTest {

    @Test
    void compatible_whenPrimarySubtagsMatchIgnoringCaseAndRegion() {
        assertThat(LocaleLanguageCompatibility.areCompatible("en", "en-US")).isTrue();
        assertThat(LocaleLanguageCompatibility.areCompatible("en-US", "en-GB")).isTrue();
        assertThat(LocaleLanguageCompatibility.areCompatible("zh", "zh-CN")).isTrue();
        assertThat(LocaleLanguageCompatibility.areCompatible("EN-us", "en-gb")).isTrue();
    }

    @Test
    void incompatible_whenPrimarySubtagsDiffer() {
        assertThat(LocaleLanguageCompatibility.areCompatible("en", "zh")).isFalse();
        assertThat(LocaleLanguageCompatibility.areCompatible("en-US", "zh-CN")).isFalse();
    }

    @Test
    void requireValidTag_rejectsBlankAndUnparseable() {
        assertThatThrownBy(() -> LocaleLanguageCompatibility.requireValidTag(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LocaleLanguageCompatibility.requireValidTag("   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LocaleLanguageCompatibility.requireValidTag("!!!"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requireValidTag_normalizesTrimmedTag() {
        assertThat(LocaleLanguageCompatibility.requireValidTag(" en-US ")).isEqualTo("en-US");
    }

    @Test
    void blankRequestLocale_skipsCompatibility() {
        assertThat(LocaleLanguageCompatibility.isBlankOrMissing(null)).isTrue();
        assertThat(LocaleLanguageCompatibility.isBlankOrMissing("  ")).isTrue();
        assertThat(LocaleLanguageCompatibility.isBlankOrMissing("en-US")).isFalse();
    }
}
