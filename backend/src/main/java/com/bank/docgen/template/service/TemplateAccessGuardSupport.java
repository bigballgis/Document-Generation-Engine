package com.bank.docgen.template.service;

import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import java.util.UUID;

/**
 * Package-private template access / draft / author guards.
 */
final class TemplateAccessGuardSupport {

    private final TemplateRepository templateRepository;
    private final GroupAccessService groupAccessService;

    TemplateAccessGuardSupport(TemplateRepository templateRepository, GroupAccessService groupAccessService) {
        this.templateRepository = templateRepository;
        this.groupAccessService = groupAccessService;
    }

    void assertDraft(TemplateEntity template) {
        if (template.getLifecycleStatus() != TemplateLifecycleStatus.DRAFT) {
            throw new TemplateValidationException("api.error.template.invalidState");
        }
    }

    void assertCanAuthorTemplates(ManagementSessionClaims session) {
        if (!groupAccessService.canAuthorTemplates(session)) {
            throw new TemplateAccessDeniedException();
        }
    }

    TemplateEntity requireReadable(UUID templateId, ManagementSessionClaims session) {
        TemplateEntity template = templateRepository.findByIdAndDeletedAtIsNull(templateId)
                .orElseThrow(TemplateNotFoundException::new);
        if (!groupAccessService.canAccessGroup(session, template.getGroupCode())) {
            throw new TemplateAccessDeniedException();
        }
        return template;
    }

    TemplateEntity requireWritable(UUID templateId, ManagementSessionClaims session) {
        TemplateEntity template = requireReadable(templateId, session);
        assertCanAuthorTemplates(session);
        return template;
    }

    TemplateEntity requireByExternalId(String externalId) {
        return templateRepository.findByExternalIdAndDeletedAtIsNull(externalId)
                .orElseThrow(TemplateNotFoundException::new);
    }
}
