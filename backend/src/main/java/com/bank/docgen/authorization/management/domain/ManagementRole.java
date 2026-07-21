package com.bank.docgen.authorization.management.domain;

import com.bank.docgen.authorization.management.service.RoleNotAssignableException;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Assignable management roles after ADR-0070 six-role compression.
 */
public enum ManagementRole {
    GLOBAL_ADMIN,
    GROUP_ADMIN,
    /** Union of former MASTER_DESIGNER and TEMPLATE_AUTHOR. */
    DOCUMENT_AUTHOR,
    TEMPLATE_TESTER,
    /** IBL-E3 / ADR-0064 — forced LEGAL-stage reviewer under LEGAL_THEN_COMPLIANCE. */
    LEGAL_REVIEWER,
    AUDIT_ADMIN;

    private static final Set<String> RETIRED_CODES = Set.of(
            "TEMPLATE_APPROVER",
            "MASTER_DESIGNER",
            "TEMPLATE_AUTHOR"
    );

    public static Set<ManagementRole> assignableRoles() {
        return Arrays.stream(values()).collect(Collectors.toUnmodifiableSet());
    }

    public static Set<String> retiredCodes() {
        return RETIRED_CODES;
    }

    public static boolean isRetiredCode(String code) {
        return code != null && RETIRED_CODES.contains(code.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * Parse an assignable role code. Retired and unknown codes fail closed with
     * {@link RoleNotAssignableException} (API → 422 {@code ROLE_NOT_ASSIGNABLE}).
     */
    @JsonCreator
    public static ManagementRole fromCode(String code) {
        return parseAssignable(code);
    }

    public static ManagementRole parseAssignable(String code) {
        if (code == null || code.isBlank()) {
            throw new RoleNotAssignableException(code);
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        try {
            return ManagementRole.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new RoleNotAssignableException(normalized);
        }
    }
}
