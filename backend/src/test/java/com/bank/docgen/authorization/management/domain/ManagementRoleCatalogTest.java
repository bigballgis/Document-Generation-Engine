package com.bank.docgen.authorization.management.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * BDD-SYS-NORM-ROLE-001…005 / ADR-0070 — assignable management catalog is exactly six roles.
 */
class ManagementRoleCatalogTest {

    private static final Set<String> SIX_ROLES = Set.of(
            "GLOBAL_ADMIN",
            "GROUP_ADMIN",
            "DOCUMENT_AUTHOR",
            "TEMPLATE_TESTER",
            "LEGAL_REVIEWER",
            "AUDIT_ADMIN"
    );

    private static final Set<String> RETIRED = Set.of(
            "TEMPLATE_APPROVER",
            "MASTER_DESIGNER",
            "TEMPLATE_AUTHOR"
    );

    @Test
    void assignableCatalogIsExactlySixRoles() {
        Set<String> catalog = Arrays.stream(ManagementRole.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertThat(catalog).containsExactlyInAnyOrderElementsOf(SIX_ROLES);
        assertThat(ManagementRole.assignableRoles()).containsExactlyInAnyOrderElementsOf(
                Arrays.stream(ManagementRole.values()).collect(Collectors.toSet())
        );
    }

    @Test
    void catalogDoesNotIncludeRetiredRoles() {
        Set<String> catalog = Arrays.stream(ManagementRole.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertThat(catalog).doesNotContainAnyElementsOf(RETIRED);
        assertThat(ManagementRole.retiredCodes()).containsExactlyInAnyOrderElementsOf(RETIRED);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "TEMPLATE_APPROVER",
            "MASTER_DESIGNER",
            "TEMPLATE_AUTHOR",
            "UNKNOWN_ROLE",
            "APPROVER"
    })
    void parseAssignableRejectsRetiredOrUnknown(String code) {
        assertThatThrownBy(() -> ManagementRole.parseAssignable(code))
                .isInstanceOf(com.bank.docgen.authorization.management.service.RoleNotAssignableException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "GLOBAL_ADMIN",
            "GROUP_ADMIN",
            "DOCUMENT_AUTHOR",
            "TEMPLATE_TESTER",
            "LEGAL_REVIEWER",
            "AUDIT_ADMIN"
    })
    void parseAssignableAcceptsSixRoles(String code) {
        assertThat(ManagementRole.parseAssignable(code).name()).isEqualTo(code);
    }
}
