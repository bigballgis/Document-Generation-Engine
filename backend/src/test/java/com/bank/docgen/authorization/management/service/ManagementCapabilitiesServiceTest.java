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
        assertThat(capabilities.decideLegalApprovals()).isTrue();
        assertThat(capabilities.publishTemplates()).isTrue();
        assertThat(capabilities.stopTemplates()).isTrue();
        assertThat(capabilities.restoreOrDeprecateTemplates()).isTrue();
        assertThat(capabilities.manageApiPolicy()).isTrue();
        assertThat(capabilities.deleteTemplates()).isTrue();
        assertThat(capabilities.exportTemplates()).isTrue();
        assertThat(capabilities.viewCollaborationWorkItems()).isTrue();
        assertThat(capabilities.maintainCollaborationTimeoutConfig()).isTrue();
        assertThat(capabilities.authorContentModules()).isTrue();
        assertThat(capabilities.decideContentModuleReviews()).isTrue();
        assertThat(capabilities.manageContentModuleLifecycle()).isTrue();
        assertThat(capabilities.readAudit()).isTrue();
        assertThat(capabilities.manageAssetLibrary()).isTrue();
        assertThat(capabilities.manageLegalHold()).isTrue();
    }

    @Test
    void groupAdminCanManageApiPolicyWithoutApiAdminRole() {
        var capabilities = service.resolve(Set.of(ManagementRole.GROUP_ADMIN));

        assertThat(capabilities.manageApiPolicy()).isTrue();
        assertThat(capabilities.deleteTemplates()).isFalse();
        assertThat(capabilities.manageMasters()).isTrue();
        assertThat(capabilities.readAudit()).isTrue();
        assertThat(capabilities.exportTemplates()).isTrue();
        assertThat(capabilities.viewCollaborationWorkItems()).isTrue();
        assertThat(capabilities.maintainCollaborationTimeoutConfig()).isTrue();
        assertThat(capabilities.authorContentModules()).isTrue();
        assertThat(capabilities.decideContentModuleReviews()).isTrue();
        assertThat(capabilities.manageContentModuleLifecycle()).isTrue();
        assertThat(capabilities.manageAssetLibrary()).isTrue();
        assertThat(capabilities.manageLegalHold()).isFalse();
    }

    @Test
    void masterDesignerCanManageMastersAndAuthorTemplates() {
        var capabilities = service.resolve(Set.of(ManagementRole.MASTER_DESIGNER));

        assertThat(capabilities.authorTemplates()).isTrue();
        assertThat(capabilities.stopTemplates()).isTrue();
        assertThat(capabilities.restoreOrDeprecateTemplates()).isFalse();
        assertThat(capabilities.manageMasters()).isTrue();
        assertThat(capabilities.reviewMasters()).isFalse();
        assertThat(capabilities.manageApiPolicy()).isFalse();
        assertThat(capabilities.deleteTemplates()).isFalse();
        assertThat(capabilities.decideTests()).isFalse();
        assertThat(capabilities.decideApprovals()).isFalse();
        assertThat(capabilities.decideLegalApprovals()).isFalse();
        assertThat(capabilities.exportTemplates()).isFalse();
        assertThat(capabilities.viewCollaborationWorkItems()).isFalse();
        assertThat(capabilities.maintainCollaborationTimeoutConfig()).isFalse();
        assertThat(capabilities.authorContentModules()).isTrue();
        assertThat(capabilities.decideContentModuleReviews()).isFalse();
        assertThat(capabilities.manageContentModuleLifecycle()).isFalse();
        assertThat(capabilities.manageAssetLibrary()).isTrue();
        assertThat(capabilities.manageLegalHold()).isFalse();
    }

    @Test
    void templateAuthorCanAuthorAndExportWithoutLifecycleGovernance() {
        var capabilities = service.resolve(Set.of(ManagementRole.TEMPLATE_AUTHOR));

        assertThat(capabilities.authorTemplates()).isTrue();
        assertThat(capabilities.exportTemplates()).isTrue();
        assertThat(capabilities.viewCollaborationWorkItems()).isTrue();
        assertThat(capabilities.authorContentModules()).isTrue();
        assertThat(capabilities.maintainCollaborationTimeoutConfig()).isFalse();
        assertThat(capabilities.decideContentModuleReviews()).isFalse();
        assertThat(capabilities.manageContentModuleLifecycle()).isFalse();
        assertThat(capabilities.manageApiPolicy()).isFalse();
        assertThat(capabilities.deleteTemplates()).isFalse();
        assertThat(capabilities.manageAssetLibrary()).isTrue();
        assertThat(capabilities.manageLegalHold()).isFalse();
    }

    @Test
    void templateTesterCanDecideTestsOnly() {
        var capabilities = service.resolve(Set.of(ManagementRole.TEMPLATE_TESTER));

        assertThat(capabilities.decideTests()).isTrue();
        assertThat(capabilities.authorTemplates()).isFalse();
        assertThat(capabilities.manageApiPolicy()).isFalse();
        assertThat(capabilities.deleteTemplates()).isFalse();
        assertThat(capabilities.exportTemplates()).isFalse();
        assertThat(capabilities.viewCollaborationWorkItems()).isTrue();
        assertThat(capabilities.maintainCollaborationTimeoutConfig()).isFalse();
        assertThat(capabilities.authorContentModules()).isFalse();
        assertThat(capabilities.decideContentModuleReviews()).isFalse();
        assertThat(capabilities.manageContentModuleLifecycle()).isFalse();
        assertThat(capabilities.manageAssetLibrary()).isTrue();
        assertThat(capabilities.manageLegalHold()).isFalse();
    }

    @Test
    void templateApproverCanDecideApprovalsOnly() {
        var capabilities = service.resolve(Set.of(ManagementRole.TEMPLATE_APPROVER));

        assertThat(capabilities.decideApprovals()).isTrue();
        assertThat(capabilities.decideLegalApprovals()).isFalse();
        assertThat(capabilities.authorTemplates()).isFalse();
        assertThat(capabilities.manageApiPolicy()).isFalse();
        assertThat(capabilities.deleteTemplates()).isFalse();
        assertThat(capabilities.exportTemplates()).isFalse();
        assertThat(capabilities.viewCollaborationWorkItems()).isTrue();
        assertThat(capabilities.decideContentModuleReviews()).isTrue();
        assertThat(capabilities.authorContentModules()).isFalse();
        assertThat(capabilities.manageContentModuleLifecycle()).isFalse();
        assertThat(capabilities.manageAssetLibrary()).isTrue();
        assertThat(capabilities.manageLegalHold()).isFalse();
    }

    @Test
    void legalReviewerCanDecideLegalApprovalsOnly() {
        var capabilities = service.resolve(Set.of(ManagementRole.LEGAL_REVIEWER));

        assertThat(capabilities.decideLegalApprovals()).isTrue();
        assertThat(capabilities.decideApprovals()).isFalse();
        assertThat(capabilities.authorTemplates()).isFalse();
        assertThat(capabilities.viewCollaborationWorkItems()).isTrue();
        assertThat(capabilities.manageAssetLibrary()).isTrue();
        assertThat(capabilities.decideContentModuleReviews()).isFalse();
    }

    @Test
    void auditAdminCanReadAuditOnly() {
        var capabilities = service.resolve(Set.of(ManagementRole.AUDIT_ADMIN));

        assertThat(capabilities.readAudit()).isTrue();
        assertThat(capabilities.manageMasters()).isFalse();
        assertThat(capabilities.manageApiPolicy()).isFalse();
        assertThat(capabilities.deleteTemplates()).isFalse();
        assertThat(capabilities.exportTemplates()).isFalse();
        assertThat(capabilities.viewCollaborationWorkItems()).isFalse();
        assertThat(capabilities.maintainCollaborationTimeoutConfig()).isFalse();
        assertThat(capabilities.authorContentModules()).isFalse();
        assertThat(capabilities.decideContentModuleReviews()).isFalse();
        assertThat(capabilities.manageContentModuleLifecycle()).isFalse();
        assertThat(capabilities.manageAssetLibrary()).isFalse();
        assertThat(capabilities.manageLegalHold()).isFalse();
    }
}
