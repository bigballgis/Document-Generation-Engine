package com.bank.docgen.sharedkernel.locale;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * IBL-E1 / ADR-0061 — primary BCP-47 language subtag compatibility (case-insensitive).
 */
public final class LocaleLanguageCompatibility {

    private static final Pattern BCP47_LANGUAGE_TAG = Pattern.compile("(?i)[a-z]{2,8}(-[a-z0-9]{1,8})*");

    private LocaleLanguageCompatibility() {
    }

    public static boolean isBlankOrMissing(String localeTag) {
        return localeTag == null || localeTag.isBlank();
    }

    /**
     * Validates and returns a trimmed BCP-47 tag with a non-blank primary language subtag.
     *
     * @throws IllegalArgumentException when blank or unparseable
     */
    public static String requireValidTag(String localeTag) {
        if (isBlankOrMissing(localeTag)) {
            throw new IllegalArgumentException("locale tag required");
        }
        String trimmed = localeTag.trim();
        if (!BCP47_LANGUAGE_TAG.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("locale tag unparseable");
        }
        Locale parsed = Locale.forLanguageTag(trimmed);
        if (parsed.getLanguage() == null || parsed.getLanguage().isBlank()) {
            throw new IllegalArgumentException("locale tag unparseable");
        }
        return trimmed;
    }

    public static boolean areCompatible(String leftTag, String rightTag) {
        if (isBlankOrMissing(leftTag) || isBlankOrMissing(rightTag)) {
            return false;
        }
        String left = primaryLanguage(leftTag);
        String right = primaryLanguage(rightTag);
        if (left.isBlank() || right.isBlank()) {
            return false;
        }
        return left.equalsIgnoreCase(right);
    }

    public static String primaryLanguage(String localeTag) {
        if (isBlankOrMissing(localeTag)) {
            return "";
        }
        Locale parsed = Locale.forLanguageTag(localeTag.trim());
        return parsed.getLanguage() == null ? "" : parsed.getLanguage();
    }
}
