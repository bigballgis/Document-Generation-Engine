package com.bank.docgen.authorization.management.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.authorization.management.domain.ManagementRole;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ManagementCapabilitiesServiceTest {

    private final ManagementCapabilitiesService service =
            new ManagementCapabilitiesService(new GroupAccessService());

    @Test
    void globalAdminHasAllCapabilities() {
        var capabilities = service.resolve(Set.of(ManagementRole.GLOBAL_ADMIN));

        assertThat(capabilities.manageMasters()).isTrue();
        assertThat(capabilities.reviewMasters()).isTrue();
        assertThat(capabilities.authorTemplates()).isTrue();
        assertThat(capabilities.decideTests()).isTrue();
        assertThat(capabilities.decideApprovals()).isTrue();
        assertThat(capabilities.publishTemplates()).isTrue();
        assertThat(capabilities.manageApiPolicy()).isTrue();
        assertThat(capabilities.readAudit()).isTrue();
    }

    @Test
    void groupAdminCanManageApiPolicyWithoutApiAdminRole() {
        var capabilities = service.resolve(Set.of(ManagementRole.GROUP_ADMIN));

        assertThat(capabilities.manageApiPolicy()).isTrue();
        assertThat(capabilities.manageMasters()).isTrue();
        assertThat(capabilities.readAudit()).isTrue();
    }

    @Test
    void masterDesignerCanAuthorTemplatesOnly() {
        var capabilities = service.resolve(Set.of(ManagementRole.MASTER_DESIGNER));

        assertThat(capabilities.authorTemplates()).isTrue();
        assertThat(capabilities.manageMasters()).isFalse();
        assertThat(capabilities.manageApiPolicy()).isFalse();
        assertThat(capabilities.decideTests()).isFalse();
        assertThat(capabilities.decideApprovals()).isFalse();
    }

    @Test
    void templateTesterCanDecideTestsOnly() {
        var capabilities = service.resolve(Set.of(ManagementRole.TEMPLATE_TESTER));

        assertThat(capabilities.decideTests()).isTrue();
        assertThat(capabilities.authorTemplates()).isFalse();
        assertThat(capabilities.manageApiPolicy()).isFalse();
    }

    @Test
    void templateApproverCanDecideApprovalsOnly() {
        var capabilities = service.resolve(Set.of(ManagementRole.TEMPLATE_APPROVER));

        assertThat(capabilities.decideApprovals()).isTrue();
        assertThat(capabilities.authorTemplates()).isFalse();
        assertThat(capabilities.manageApiPolicy()).isFalse();
    }

    @Test
    void auditAdminCanReadAuditOnly() {
        var capabilities = service.resolve(Set.of(ManagementRole.AUDIT_ADMIN));

        assertThat(capabilities.readAudit()).isTrue();
        assertThat(capabilities.manageMasters()).isFalse();
        assertThat(capabilities.manageApiPolicy()).isFalse();
    }
}
