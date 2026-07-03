package com.bank.docgen.runtime.domain;

import java.util.Locale;

public enum InvocationListView {
    LOGICAL,
    FLAT;

    public static InvocationListView parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return LOGICAL;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "logical" -> LOGICAL;
            case "flat" -> FLAT;
            default -> throw new InvocationViewValidationException();
        };
    }
}
