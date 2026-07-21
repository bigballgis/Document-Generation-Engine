package com.bank.docgen.template.domain;

import java.util.Locale;

/**
 * Opt-in export dependency-closure profile (SYS-NORM Wave 7 / PP-C1).
 * Omit / null = default CE-E01 / CE-E03 behavior.
 */
public enum TemplateDependencyClosure {
    PROMOTION;

    public static TemplateDependencyClosure parseOptional(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return TemplateDependencyClosure.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unsupported dependencyClosure: " + raw);
        }
    }
}
