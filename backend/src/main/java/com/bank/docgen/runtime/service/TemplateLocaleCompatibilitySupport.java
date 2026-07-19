package com.bank.docgen.runtime.service;

import com.bank.docgen.sharedkernel.locale.LocaleLanguageCompatibility;
import com.bank.docgen.template.persistence.TemplateEntity;

/**
 * IBL-E1 / E1-C10 — when request locale is non-blank, require language compatibility with template.
 */
public final class TemplateLocaleCompatibilitySupport {

    private TemplateLocaleCompatibilitySupport() {
    }

    public static void assertRequestLocaleCompatible(TemplateEntity template, String requestLocale) {
        if (LocaleLanguageCompatibility.isBlankOrMissing(requestLocale)) {
            return;
        }
        String templateLocale = template.getLocale();
        if (LocaleLanguageCompatibility.isBlankOrMissing(templateLocale)) {
            return;
        }
        if (!LocaleLanguageCompatibility.areCompatible(templateLocale, requestLocale)) {
            throw new TemplateLocaleMismatchException();
        }
    }
}
