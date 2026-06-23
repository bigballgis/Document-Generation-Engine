package com.bank.docgen.authorization.management.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Coverage for the security-critical group/role access checks (OPT-C). Verifies
 * fail-closed behavior for unprivileged roles and group-scope isolation.
 */
class GroupAccessServiceTest {

    private final GroupAccessService service = new GroupAccessService();

    private ManagementSessionClaims session(List<String> roles, List<String> groups) {
        return new ManagementSessionClaims(
                "user", "User", "user@bank.test", AuthSource.LOCAL,
                roles, groups, "/", List.of(), Instant.now().plusSeconds(600));
    }

    @Test
    void globalAdminCanAccessAnyGroupAndWildcardScope() {
        ManagementSessionClaims admin = session(List.of("GLOBAL_ADMIN"), List.of());
        assertThat(service.canAccessGroup(admin, "ANY")).isTrue();
        assertThat(service.accessibleGroupCodes(admin)).containsExactly("*");
    }

    @Test
    void groupScopedUserOnlyAccessesAuthorizedGroups() {
        ManagementSessionClaims user = session(List.of("GROUP_ADMIN"), List.of("G1", "G2"));
        assertThat(service.canAccessGroup(user, "G1")).isTrue();
        assertThat(service.canAccessGroup(user, "G3")).isFalse();
        assertThat(service.accessibleGroupCodes(user)).containsExactly("G1", "G2");
    }

    @Test
    void mastersReviewAndManageRequireAdminRoles() {
        ManagementSessionClaims author = session(List.of("TEMPLATE_AUTHOR"), List.of("G1"));
        assertThat(service.canReviewMasters(author)).isFalse();
        assertThat(service.canManageMasters(author)).isFalse();
        assertThat(service.canReviewMasters(session(List.of("GROUP_ADMIN"), List.of()))).isTrue();
    }

    @Test
    void authoringAllowsDesignerAuthorAndAdmins() {
        assertThat(service.canAuthorTemplates(session(List.of("MASTER_DESIGNER"), List.of()))).isTrue();
        assertThat(service.canAuthorTemplates(session(List.of("TEMPLATE_AUTHOR"), List.of()))).isTrue();
        assertThat(service.canAuthorTemplates(session(List.of("AUDIT_ADMIN"), List.of()))).isFalse();
    }

    @Test
    void testAndApprovalDecisionsAreRoleScoped() {
        assertThat(service.canDecideTemplateTests(session(List.of("TEMPLATE_TESTER"), List.of()))).isTrue();
        assertThat(service.canDecideTemplateTests(session(List.of("TEMPLATE_APPROVER"), List.of()))).isFalse();
        assertThat(service.canDecideTemplateApprovals(session(List.of("TEMPLATE_APPROVER"), List.of()))).isTrue();
        assertThat(service.canDecideTemplateApprovals(session(List.of("TEMPLATE_TESTER"), List.of()))).isFalse();
    }

    @Test
    void publishRestoreAndApiPolicyRequireAdminRoles() {
        ManagementSessionClaims author = session(List.of("TEMPLATE_AUTHOR"), List.of());
        assertThat(service.canPublishTemplates(author)).isFalse();
        assertThat(service.canRestoreOrDeprecateTemplates(author)).isFalse();
        assertThat(service.canManageApiPolicy(author)).isFalse();
        ManagementSessionClaims groupAdmin = session(List.of("GROUP_ADMIN"), List.of());
        assertThat(service.canPublishTemplates(groupAdmin)).isTrue();
        assertThat(service.canManageApiPolicy(groupAdmin)).isTrue();
    }

    @Test
    void stopTemplatesAllowsAuthorsAndDesigners() {
        assertThat(service.canStopTemplates(session(List.of("TEMPLATE_AUTHOR"), List.of()))).isTrue();
        assertThat(service.canStopTemplates(session(List.of("MASTER_DESIGNER"), List.of()))).isTrue();
        assertThat(service.canStopTemplates(session(List.of("TEMPLATE_TESTER"), List.of()))).isFalse();
    }

    @Test
    void manageReleaseVersionStateRequiresAdminRoles() {
        assertThat(service.canManageReleaseVersionState(session(List.of("TEMPLATE_AUTHOR"), List.of()))).isFalse();
        assertThat(service.canManageReleaseVersionState(session(List.of("GROUP_ADMIN"), List.of()))).isTrue();
        assertThat(service.canManageReleaseVersionState(session(List.of("GLOBAL_ADMIN"), List.of()))).isTrue();
    }

    @Test
    void auditReadRequiresAuditOrAdminRoles() {
        assertThat(service.canReadAudit(session(List.of("AUDIT_ADMIN"), List.of()))).isTrue();
        assertThat(service.canReadAudit(session(List.of("GLOBAL_ADMIN"), List.of()))).isTrue();
        assertThat(service.canReadAudit(session(List.of("TEMPLATE_AUTHOR"), List.of()))).isFalse();
    }

    @Test
    void deleteTemplateRequiresGlobalAdminOnly() {
        assertThat(service.canDeleteTemplate(session(List.of("GLOBAL_ADMIN"), List.of()))).isTrue();
        assertThat(service.canDeleteTemplate(session(List.of("GROUP_ADMIN"), List.of()))).isFalse();
        assertThat(service.canDeleteTemplate(session(List.of("TEMPLATE_AUTHOR"), List.of()))).isFalse();
    }

    @Test
    void emptyRolesFailClosedEverywhere() {
        ManagementSessionClaims none = session(List.of(), List.of());
        assertThat(service.canReviewMasters(none)).isFalse();
        assertThat(service.canAuthorTemplates(none)).isFalse();
        assertThat(service.canPublishTemplates(none)).isFalse();
        assertThat(service.canManageApiPolicy(none)).isFalse();
        assertThat(service.canDeleteTemplate(none)).isFalse();
        assertThat(service.canReadAudit(none)).isFalse();
        assertThat(service.canAccessGroup(none, "G1")).isFalse();
    }
}
