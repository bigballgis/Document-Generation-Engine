package com.bank.docgen.template.service;

import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateEntity;
import org.springframework.stereotype.Component;

@Component
public class TemplateExportAccessSupport {

    private final GroupAccessService groupAccessService;

    public TemplateExportAccessSupport(GroupAccessService groupAccessService) {
        this.groupAccessService = groupAccessService;
    }

    public void assertCanExport(TemplateEntity template, ManagementSessionClaims session) {
        assertCanImportForGroup(template.getGroupCode(), session);
        if (isTemplateAuthorOnly(session) && !session.username().equals(template.getCreatedBy())) {
            throw new TemplateAccessDeniedException();
        }
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
        return session.roles().contains("TEMPLATE_AUTHOR");
    }
}
