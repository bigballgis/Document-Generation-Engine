package com.bank.docgen.documentbrand.service;

import java.util.regex.Pattern;

public final class DocumentBrandCodes {

    public static final String PLATFORM_DEFAULT = "PLATFORM_DEFAULT";
    public static final String PLATFORM_DEFAULT_LOGO_REF = "platform/document-brands/PLATFORM_DEFAULT/logo";
    public static final String PLATFORM_DEFAULT_DISPLAY_NAME = "Platform default document brand";

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9][A-Z0-9_-]{0,63}$");

    private DocumentBrandCodes() {
    }

    public static String requireValidCode(String raw, String blankMessageKey) {
        if (raw == null || raw.isBlank()) {
            throw new DocumentBrandCatalogException(
                    com.bank.docgen.sharedkernel.api.ApiErrorCodes.REQUEST_BODY_INVALID,
                    blankMessageKey
            );
        }
        String trimmed = raw.trim();
        if (!CODE_PATTERN.matcher(trimmed).matches()) {
            throw new DocumentBrandCatalogException(
                    com.bank.docgen.sharedkernel.api.ApiErrorCodes.DOCUMENT_BRAND_CODE_INVALID,
                    "api.error.documentBrand.codeInvalid"
            );
        }
        return trimmed;
    }

    public static String normalizeOptional(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim();
    }
}
