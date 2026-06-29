package com.bank.docgen.authorization.management.service;

import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GroupAccessService {

    public boolean canAccessGroup(ManagementSessionClaims session, String groupCode) {
        if (session.roles().contains("GLOBAL_ADMIN")) {
            return true;
        }
        return session.authorizedGroupCodes().contains(groupCode);
    }

    public List<String> accessibleGroupCodes(ManagementSessionClaims session) {
        if (session.roles().contains("GLOBAL_ADMIN")) {
            return List.of("*");
        }
        return session.authorizedGroupCodes();
    }

    public boolean canReviewMasters(ManagementSessionClaims session) {
        return session.roles().contains("GLOBAL_ADMIN") || session.roles().contains("GROUP_ADMIN");
    }

    public boolean canManageMasters(ManagementSessionClaims session) {
        return session.roles().contains("GLOBAL_ADMIN") || session.roles().contains("GROUP_ADMIN");
    }

    public boolean canAuthorTemplates(ManagementSessionClaims session) {
        return session.roles().contains("GLOBAL_ADMIN")
                || session.roles().contains("GROUP_ADMIN")
                || session.roles().contains("TEMPLATE_AUTHOR")
                || session.roles().contains("MASTER_DESIGNER");
    }

    public boolean canDecideTemplateTests(ManagementSessionClaims session) {
        return session.roles().contains("GLOBAL_ADMIN")
                || session.roles().contains("GROUP_ADMIN")
                || session.roles().contains("TEMPLATE_TESTER");
    }

    public boolean canDecideTemplateApprovals(ManagementSessionClaims session) {
        return session.roles().contains("GLOBAL_ADMIN")
                || session.roles().contains("GROUP_ADMIN")
                || session.roles().contains("TEMPLATE_APPROVER");
    }

    public boolean canPublishTemplates(ManagementSessionClaims session) {
        return session.roles().contains("GLOBAL_ADMIN") || session.roles().contains("GROUP_ADMIN");
    }

    public boolean canStopTemplates(ManagementSessionClaims session) {
        return session.roles().contains("GLOBAL_ADMIN")
                || session.roles().contains("GROUP_ADMIN")
                || session.roles().contains("MASTER_DESIGNER")
                || session.roles().contains("TEMPLATE_AUTHOR");
    }

    public boolean canRestoreOrDeprecateTemplates(ManagementSessionClaims session) {
        return session.roles().contains("GLOBAL_ADMIN")
                || session.roles().contains("GROUP_ADMIN");
    }

    public boolean canManageReleaseVersionState(ManagementSessionClaims session) {
        return session.roles().contains("GLOBAL_ADMIN")
                || session.roles().contains("GROUP_ADMIN");
    }

    public boolean canManageApiPolicy(ManagementSessionClaims session) {
        return session.roles().contains("GLOBAL_ADMIN")
                || session.roles().contains("GROUP_ADMIN");
    }

    public boolean canDeleteTemplate(ManagementSessionClaims session) {
        return session.roles().contains("GLOBAL_ADMIN");
    }

    public boolean canReadAudit(ManagementSessionClaims session) {
        return session.roles().contains("GLOBAL_ADMIN")
                || session.roles().contains("GROUP_ADMIN")
                || session.roles().contains("AUDIT_ADMIN");
    }

    public boolean canAuthorContentModules(ManagementSessionClaims session) {
        return session.roles().contains("GLOBAL_ADMIN")
                || session.roles().contains("GROUP_ADMIN")
                || session.roles().contains("TEMPLATE_AUTHOR")
                || session.roles().contains("MASTER_DESIGNER");
    }

    /**
     * Fail-closed catalog browse for management list/detail APIs (permission-matrix §5.1).
     * Testers may only read referenced modules in test/approval material context, not enumerate.
     */
    public boolean canBrowseContentModuleCatalog(ManagementSessionClaims session) {
        return session.roles().contains("GLOBAL_ADMIN")
                || session.roles().contains("GROUP_ADMIN")
                || session.roles().contains("MASTER_DESIGNER")
                || session.roles().contains("TEMPLATE_AUTHOR")
                || session.roles().contains("TEMPLATE_APPROVER");
    }

    public boolean canViewContentModuleStructure(ManagementSessionClaims session) {
        return canAuthorContentModules(session) || canDecideContentModuleReviews(session);
    }

    public boolean canDecideContentModuleReviews(ManagementSessionClaims session) {
        return session.roles().contains("GLOBAL_ADMIN")
                || session.roles().contains("GROUP_ADMIN")
                || session.roles().contains("TEMPLATE_APPROVER");
    }

    public boolean canManageContentModuleLifecycle(ManagementSessionClaims session) {
        return session.roles().contains("GLOBAL_ADMIN")
                || session.roles().contains("GROUP_ADMIN");
    }

    public boolean canViewCollaborationWorkItems(ManagementSessionClaims session) {
        return session.roles().contains("GLOBAL_ADMIN")
                || session.roles().contains("GROUP_ADMIN")
                || session.roles().contains("TEMPLATE_AUTHOR")
                || session.roles().contains("TEMPLATE_TESTER")
                || session.roles().contains("TEMPLATE_APPROVER");
    }

    public boolean hasCollaborationWorkItemAdminVisibility(ManagementSessionClaims session) {
        return session.roles().contains("GLOBAL_ADMIN")
                || session.roles().contains("GROUP_ADMIN");
    }

    public boolean canMaintainCollaborationTimeoutConfig(ManagementSessionClaims session) {
        return session.roles().contains("GLOBAL_ADMIN")
                || session.roles().contains("GROUP_ADMIN");
    }

    public boolean canExportTemplates(ManagementSessionClaims session) {
        return session.roles().contains("GLOBAL_ADMIN")
                || session.roles().contains("GROUP_ADMIN")
                || session.roles().contains("TEMPLATE_AUTHOR");
    }
}
