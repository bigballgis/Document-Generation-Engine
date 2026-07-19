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
            CollaborationWorkItemRepository workItemRepository, ManagementAuditRecorder auditRecorder) {
        this.workItemRepository = workItemRepository;
        this.auditRecorder = auditRecorder;
    }

    CollaborationWorkItemEntity refreshSubmitForTest(
            CollaborationWorkItemEntity existing, TemplateEntity template,
            ManagementSessionClaims session, String summary, Instant now) {
        return refresh(existing, template, session.username(), summary,
                CollaborationWorkItemTriggerType.SUBMIT_FOR_TEST, now, true);
    }

    CollaborationWorkItemEntity createSubmitForTest(
            TemplateEntity template, ManagementSessionClaims session,
            String summary, Instant now, String actorSummary) {
        return create(template, session.username(), session, summary,
                CollaborationWorkItemQueue.TEST,
                CollaborationWorkItemTriggerType.SUBMIT_FOR_TEST, now, actorSummary);
    }

    CollaborationWorkItemEntity refreshSubmitForApproval(
            CollaborationWorkItemEntity existing, TemplateEntity template,
            ManagementSessionClaims session, String summary, Instant now) {
        return refresh(existing, template, session.username(), summary,
                CollaborationWorkItemTriggerType.SUBMIT_FOR_APPROVAL, now, true);
    }

    CollaborationWorkItemEntity createSubmitForApproval(
            TemplateEntity template, ManagementSessionClaims session,
            String summary, Instant now, String actorSummary) {
        return create(template, session.username(), session, summary,
                CollaborationWorkItemQueue.APPROVAL,
                CollaborationWorkItemTriggerType.SUBMIT_FOR_APPROVAL, now, actorSummary);
    }

    CollaborationWorkItemEntity refreshSubmitForLegalReview(
            CollaborationWorkItemEntity existing, TemplateEntity template,
            ManagementSessionClaims session, String summary, Instant now) {
        return refresh(existing, template, session.username(), summary,
                CollaborationWorkItemTriggerType.SUBMIT_FOR_APPROVAL, now, true);
    }

    CollaborationWorkItemEntity createSubmitForLegalReview(
            TemplateEntity template, ManagementSessionClaims session,
            String summary, Instant now, String actorSummary) {
        return create(template, session.username(), session, summary,
                CollaborationWorkItemQueue.LEGAL,
                CollaborationWorkItemTriggerType.SUBMIT_FOR_APPROVAL, now, actorSummary);
    }

    CollaborationWorkItemEntity refreshRemediation(
            CollaborationWorkItemEntity existing, TemplateEntity template, String submitterUserId,
            String summary, CollaborationWorkItemTriggerType triggerType, Instant now) {
        return refresh(existing, template, submitterUserId, summary, triggerType, now, false);
    }

    CollaborationWorkItemEntity createRemediation(
            TemplateEntity template, String submitterUserId, ManagementSessionClaims session,
            String summary, CollaborationWorkItemTriggerType triggerType, Instant now, String actorSummary) {
        return create(template, submitterUserId, session, summary,
                CollaborationWorkItemQueue.REMEDIATION, triggerType, now, actorSummary);
    }

    CollaborationWorkItemEntity refreshPendingRelease(
            CollaborationWorkItemEntity existing, TemplateEntity template,
            String submitterUserId, String summary, Instant now) {
        return refresh(existing, template, submitterUserId, summary,
                CollaborationWorkItemTriggerType.APPROVAL_PENDING_RELEASE, now, false);
    }

    CollaborationWorkItemEntity createPendingRelease(
            TemplateEntity template, String submitterUserId, ManagementSessionClaims session,
            String summary, Instant now, String actorSummary) {
        return create(template, submitterUserId, session, summary,
                CollaborationWorkItemQueue.PENDING_RELEASE,
                CollaborationWorkItemTriggerType.APPROVAL_PENDING_RELEASE, now, actorSummary);
    }

    private CollaborationWorkItemEntity refresh(
            CollaborationWorkItemEntity existing,
            TemplateEntity template,
            String submitterUserId,
            String summary,
            CollaborationWorkItemTriggerType triggerType,
            Instant now,
            boolean resetCreatedAt
    ) {
        existing.setTemplateName(template.getName());
        existing.setSubmitterUserId(submitterUserId);
        existing.setSummaryText(summary);
        existing.setTriggerType(triggerType);
        if (resetCreatedAt) {
            existing.setCreatedAt(now);
        }
        existing.setUpdatedAt(now);
        return workItemRepository.save(existing);
    }

    private CollaborationWorkItemEntity create(
            TemplateEntity template,
            String submitterUserId,
            ManagementSessionClaims session,
            String summary,
            CollaborationWorkItemQueue queue,
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
                queue,
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
                queue,
                triggerType,
                session.username(),
                actorSummary
        );
        return saved;
    }
}
