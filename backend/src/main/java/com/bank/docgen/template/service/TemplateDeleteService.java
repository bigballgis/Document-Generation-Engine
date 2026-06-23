package com.bank.docgen.template.service;

import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.LifecycleGovernanceRequest;
import com.bank.docgen.template.domain.LifecycleAction;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.persistence.TemplateRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateDeleteService {

    private final TemplateRepository templateRepository;
    private final TemplateLifecycleRecordRepository lifecycleRecordRepository;
    private final GroupAccessService groupAccessService;

    public TemplateDeleteService(
            TemplateRepository templateRepository,
            TemplateLifecycleRecordRepository lifecycleRecordRepository,
            GroupAccessService groupAccessService
    ) {
        this.templateRepository = templateRepository;
        this.lifecycleRecordRepository = lifecycleRecordRepository;
        this.groupAccessService = groupAccessService;
    }

    @Transactional
    public void deleteTemplate(
            UUID templateId,
            LifecycleGovernanceRequest request,
            ManagementSessionClaims session
    ) {
        if (!groupAccessService.canDeleteTemplate(session)) {
            throw new TemplateAccessDeniedException();
        }
        if (!request.confirmed()) {
            throw new TemplateValidationException("api.error.template.confirmationRequired");
        }
        TemplateEntity template = templateRepository.findById(templateId)
                .orElseThrow(TemplateNotFoundException::new);
        if (template.getDeletedAt() != null) {
            throw new TemplateValidationException("api.error.template.alreadyDeleted");
        }
        TemplateLifecycleStatus currentStatus = template.getLifecycleStatus();
        template.setDeletedAt(Instant.now());
        template.setUpdatedBy(session.username());
        templateRepository.save(template);
        lifecycleRecordRepository.save(new TemplateLifecycleRecordEntity(
                UUID.randomUUID(),
                template.getId(),
                LifecycleAction.DELETE,
                currentStatus,
                currentStatus,
                null,
                request.reason(),
                template.getReleaseVersion(),
                session.username()
        ));
    }
}
