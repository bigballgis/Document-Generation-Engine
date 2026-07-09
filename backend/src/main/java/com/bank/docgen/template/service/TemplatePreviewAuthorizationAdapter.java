package com.bank.docgen.template.service;

import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.port.RenderableTemplateSnapshot;
import com.bank.docgen.template.port.TemplatePreviewAuthorizationPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TemplatePreviewAuthorizationAdapter implements TemplatePreviewAuthorizationPort {

    private final TemplateService templateService;
    private final GroupAccessService groupAccessService;

    public TemplatePreviewAuthorizationAdapter(
            TemplateService templateService,
            GroupAccessService groupAccessService
    ) {
        this.templateService = templateService;
        this.groupAccessService = groupAccessService;
    }

    @Override
    public RenderableTemplateSnapshot requireReadableSnapshot(UUID templateId, ManagementSessionClaims session) {
        TemplateEntity template = templateService.requireReadableTemplate(templateId, session);
        return new RenderableTemplateSnapshot(template.getId(), template.getMasterId(), template.getGroupCode());
    }

    @Override
    public void requirePreviewAuthor(ManagementSessionClaims session) {
        if (!groupAccessService.canAuthorTemplates(session)) {
            throw new TemplateAccessDeniedException();
        }
    }
}
