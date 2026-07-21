package com.bank.docgen.template.service;

import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateEntity;
import org.springframework.stereotype.Component;

@Component
public class TemplateExportAccessService {

    private final GroupAccessService groupAccessService;

    public TemplateExportAccessService(GroupAccessService groupAccessService) {
        this.groupAccessService = groupAccessService;
    }

    public void assertCanExport(TemplateEntity template, ManagementSessionClaims session) {
        if (!canExport(template, session)) {
            throw new TemplateAccessDeniedException();
        }
    }

    /** CE-E03: non-throwing per-template export authorization check. */
    public boolean canExport(TemplateEntity template, ManagementSessionClaims session) {
        if (!groupAccessService.canExportTemplates(session)) {
            return false;
        }
        if (!groupAccessService.canAccessGroup(session, template.getGroupCode())) {
            return false;
        }
        if (isTemplateAuthorOnly(session) && !session.username().equals(template.getCreatedBy())) {
            return false;
        }
        return true;
    }

    public void assertCanImportForGroup(String groupCode, ManagementSessionClaims session) {
        if (!groupAccessService.canExportTemplates(session)) {
            throw new TemplateAccessDeniedException();
        }
        if (!groupAccessService.canAccessGroup(session, groupCode)) {
            throw new TemplateAccessDeniedException();
        }
    }

    private boolean isTemplateAuthorOnly(ManagementSessionClaims session) {
        if (session.roles().contains("GLOBAL_ADMIN") || session.roles().contains("GROUP_ADMIN")) {
            return false;
        }
        return session.roles().contains("DOCUMENT_AUTHOR");
    }
}
