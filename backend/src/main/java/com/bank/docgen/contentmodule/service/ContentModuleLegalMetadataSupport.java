package com.bank.docgen.contentmodule.service;

import java.time.Instant;

/**
 * Normalize and validate optional CE-K08 legal metadata on content-module versions.
 */
final class ContentModuleLegalMetadataSupport {

    private ContentModuleLegalMetadataSupport() {
    }

    static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static void validateEffectiveRange(Instant effectiveFrom, Instant effectiveTo) {
        if (effectiveFrom != null && effectiveTo != null && effectiveFrom.isAfter(effectiveTo)) {
            throw new ContentModuleValidationException("api.error.contentModule.invalidEffectiveRange");
        }
    }
}
