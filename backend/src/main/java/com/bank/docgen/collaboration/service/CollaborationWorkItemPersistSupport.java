package com.bank.docgen.collaboration.service;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemStatus;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemTriggerType;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemEntity;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateEntity;
import java.time.Instant;
import java.util.UUID;

/**
 * Package-private create/refresh helpers for collaboration work-item persistence.
 */
final class CollaborationWorkItemPersistSupport {

    private final CollaborationWorkItemRepository workItemRepository;
    private final ManagementAuditRecorder auditRecorder;

    CollaborationWorkItemPersistSupport(
            CollaborationWorkItemRepository workItemRepository,
            ManagementAuditRecorder auditRecorder
    ) {
        this.workItemRepository = workItemRepository;
        this.auditRecorder = auditRecorder;
    }

    CollaborationWorkItemEntity refreshSubmitForTest(
            CollaborationWorkItemEntity existing,
            TemplateEntity template,
            ManagementSessionClaims session,
            String summary,
            Instant now
    ) {
        existing.setTemplateName(template.getName());
        existing.setSubmitterUserId(session.username());
        existing.setSummaryText(summary);
        existing.setTriggerType(CollaborationWorkItemTriggerType.SUBMIT_FOR_TEST);
        existing.setCreatedAt(now);
        existing.setUpdatedAt(now);
        return workItemRepository.save(existing);
    }

    CollaborationWorkItemEntity createSubmitForTest(
            TemplateEntity template,
            ManagementSessionClaims session,
            String summary,
            Instant now,
            String actorSummary
    ) {
        CollaborationWorkItemEntity created = new CollaborationWorkItemEntity(
                UUID.randomUUID(),
                template.getId(),
                template.getExternalId(),
                template.getName(),
                template.getGroupCode(),
                CollaborationWorkItemQueue.TEST,
                CollaborationWorkItemTriggerType.SUBMIT_FOR_TEST,
                CollaborationWorkItemStatus.OPEN,
                session.username(),
                summary
        );
        created.setCreatedAt(now);
        created.setUpdatedAt(now);
        CollaborationWorkItemEntity saved = workItemRepository.save(created);
        auditRecorder.recordCollaborationWorkItemCreated(
                template.getId(),
                template.getGroupCode(),
                saved.getId(),
                CollaborationWorkItemQueue.TEST,
                CollaborationWorkItemTriggerType.SUBMIT_FOR_TEST,
                session.username(),
                actorSummary
        );
        return saved;
    }

    CollaborationWorkItemEntity refreshSubmitForApproval(
            CollaborationWorkItemEntity existing,
            TemplateEntity template,
            ManagementSessionClaims session,
            String summary,
            Instant now
    ) {
        existing.setTemplateName(template.getName());
        existing.setSubmitterUserId(session.username());
        existing.setSummaryText(summary);
        existing.setTriggerType(CollaborationWorkItemTriggerType.SUBMIT_FOR_APPROVAL);
        existing.setCreatedAt(now);
        existing.setUpdatedAt(now);
        return workItemRepository.save(existing);
    }

    CollaborationWorkItemEntity createSubmitForApproval(
            TemplateEntity template,
            ManagementSessionClaims session,
            String summary,
            Instant now,
            String actorSummary
    ) {
        CollaborationWorkItemEntity created = new CollaborationWorkItemEntity(
                UUID.randomUUID(),
                template.getId(),
                template.getExternalId(),
                template.getName(),
                template.getGroupCode(),
                CollaborationWorkItemQueue.APPROVAL,
                CollaborationWorkItemTriggerType.SUBMIT_FOR_APPROVAL,
                CollaborationWorkItemStatus.OPEN,
                session.username(),
                summary
        );
        created.setCreatedAt(now);
        created.setUpdatedAt(now);
        CollaborationWorkItemEntity saved = workItemRepository.save(created);
        auditRecorder.recordCollaborationWorkItemCreated(
                template.getId(),
                template.getGroupCode(),
                saved.getId(),
                CollaborationWorkItemQueue.APPROVAL,
                CollaborationWorkItemTriggerType.SUBMIT_FOR_APPROVAL,
                session.username(),
                actorSummary
        );
        return saved;
    }

    CollaborationWorkItemEntity refreshRemediation(
            CollaborationWorkItemEntity existing,
            TemplateEntity template,
            String submitterUserId,
            String summary,
            CollaborationWorkItemTriggerType triggerType,
            Instant now
    ) {
        existing.setTemplateName(template.getName());
        existing.setSubmitterUserId(submitterUserId);
        existing.setSummaryText(summary);
        existing.setTriggerType(triggerType);
        existing.setUpdatedAt(now);
        return workItemRepository.save(existing);
    }

    CollaborationWorkItemEntity createRemediation(
            TemplateEntity template,
            String submitterUserId,
            ManagementSessionClaims session,
            String summary,
            CollaborationWorkItemTriggerType triggerType,
            Instant now,
            String actorSummary
    ) {
        CollaborationWorkItemEntity created = new CollaborationWorkItemEntity(
                UUID.randomUUID(),
                template.getId(),
                template.getExternalId(),
                template.getName(),
                template.getGroupCode(),
                CollaborationWorkItemQueue.REMEDIATION,
                triggerType,
                CollaborationWorkItemStatus.OPEN,
                submitterUserId,
                summary
        );
        created.setCreatedAt(now);
        created.setUpdatedAt(now);
        CollaborationWorkItemEntity saved = workItemRepository.save(created);
        auditRecorder.recordCollaborationWorkItemCreated(
                template.getId(),
                template.getGroupCode(),
                saved.getId(),
                CollaborationWorkItemQueue.REMEDIATION,
                triggerType,
                session.username(),
                actorSummary
        );
        return saved;
    }

    CollaborationWorkItemEntity refreshPendingRelease(
            CollaborationWorkItemEntity existing,
            TemplateEntity template,
            String submitterUserId,
            String summary,
            Instant now
    ) {
        existing.setTemplateName(template.getName());
        existing.setSubmitterUserId(submitterUserId);
        existing.setSummaryText(summary);
        existing.setTriggerType(CollaborationWorkItemTriggerType.APPROVAL_PENDING_RELEASE);
        existing.setUpdatedAt(now);
        return workItemRepository.save(existing);
    }

    CollaborationWorkItemEntity createPendingRelease(
            TemplateEntity template,
            String submitterUserId,
            ManagementSessionClaims session,
            String summary,
            Instant now,
            String actorSummary
    ) {
        CollaborationWorkItemEntity created = new CollaborationWorkItemEntity(
                UUID.randomUUID(),
                template.getId(),
                template.getExternalId(),
                template.getName(),
                template.getGroupCode(),
                CollaborationWorkItemQueue.PENDING_RELEASE,
                CollaborationWorkItemTriggerType.APPROVAL_PENDING_RELEASE,
                CollaborationWorkItemStatus.OPEN,
                submitterUserId,
                summary
        );
        created.setCreatedAt(now);
        created.setUpdatedAt(now);
        CollaborationWorkItemEntity saved = workItemRepository.save(created);
        auditRecorder.recordCollaborationWorkItemCreated(
                template.getId(),
                template.getGroupCode(),
                saved.getId(),
                CollaborationWorkItemQueue.PENDING_RELEASE,
                CollaborationWorkItemTriggerType.APPROVAL_PENDING_RELEASE,
                session.username(),
                actorSummary
        );
        return saved;
    }
}
