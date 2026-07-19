package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.template.persistence.TemplateEntity;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * BDD-IBL-E1-009 / 010 / 011 — runtime context.locale vs template locale.
 */
class IblE1RuntimeTemplateLocaleMismatchTest {

    @Test
    void assertCompatible_mismatchedLocale_throwsTemplateLocaleMismatch_bddE1009() {
        TemplateEntity template = englishTemplate();

        assertThatThrownBy(() -> TemplateLocaleCompatibilitySupport.assertRequestLocaleCompatible(template, "zh-CN"))
                .isInstanceOf(TemplateLocaleMismatchException.class)
                .satisfies(ex -> {
                    TemplateLocaleMismatchException mismatch = (TemplateLocaleMismatchException) ex;
                    assertThat(mismatch.errorCode()).isEqualTo(ApiErrorCodes.TEMPLATE_LOCALE_MISMATCH);
                });
    }

    @Test
    void assertCompatible_matchingLocale_succeeds_bddE1010() {
        TemplateEntity template = englishTemplate();

        assertThatCode(() -> TemplateLocaleCompatibilitySupport.assertRequestLocaleCompatible(template, "en-GB"))
                .doesNotThrowAnyException();
    }

    @Test
    void assertCompatible_omittedLocale_skipsCheck_bddE1011() {
        TemplateEntity template = englishTemplate();

        assertThatCode(() -> TemplateLocaleCompatibilitySupport.assertRequestLocaleCompatible(template, null))
                .doesNotThrowAnyException();
        assertThatCode(() -> TemplateLocaleCompatibilitySupport.assertRequestLocaleCompatible(template, "  "))
                .doesNotThrowAnyException();
    }

    private static TemplateEntity englishTemplate() {
        TemplateEntity template = new TemplateEntity(
                UUID.randomUUID(),
                "TPL-EN",
                "RETAIL",
                "EN",
                null,
                UUID.randomUUID(),
                "author"
        );
        template.setLocale("en-US");
        return template;
    }
}
