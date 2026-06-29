package com.bank.docgen.collaboration.service;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemStatus;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemTriggerType;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemEntity;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemRepository;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CollaborationWorkItemWriter {

    static final String SUBMIT_FOR_TEST_SUMMARY_KEY = "api.collaboration.workItem.submitForTest.summary";
    static final String REMEDIATION_SUMMARY_KEY = "api.collaboration.workItem.remediation.summary";

    private final CollaborationWorkItemRepository workItemRepository;
    private final MessageResolver messageResolver;
    private final ManagementAuditRecorder auditRecorder;

    public CollaborationWorkItemWriter(
            CollaborationWorkItemRepository workItemRepository,
            MessageResolver messageResolver,
            ManagementAuditRecorder auditRecorder
    ) {
        this.workItemRepository = workItemRepository;
        this.messageResolver = messageResolver;
        this.auditRecorder = auditRecorder;
    }

    @Transactional
    public CollaborationWorkItemEntity upsertSubmitForTestWorkItem(
            TemplateEntity template,
            ManagementSessionClaims session
    ) {
        String summary = messageResolver.resolve(SUBMIT_FOR_TEST_SUMMARY_KEY);
        Instant now = Instant.now();
        return workItemRepository
                .findOpenByTemplateIdAndQueue(template.getId(), CollaborationWorkItemQueue.TEST)
                .map(existing -> refreshSubmitForTest(existing, template, session, summary, now))
                .orElseGet(() -> createSubmitForTest(template, session, summary, now));
    }

    /**
     * Resolves every OPEN TEST work item for the template (Spec A). Idempotent no-op when none exist;
     * resolves all OPEN items defensively against drift. Returns the carried-forward orchestrator
     * (submitter of the resolved TEST to-do) for downstream remediation routing, when available.
     */
    @Transactional
    public Optional<String> resolveOpenTestWorkItems(
            TemplateEntity template,
            ManagementSessionClaims session
    ) {
        List<CollaborationWorkItemEntity> openItems = workItemRepository
                .findAllOpenByTemplateIdAndQueue(template.getId(), CollaborationWorkItemQueue.TEST);
        Instant now = Instant.now();
        String carriedForwardSubmitter = null;
        for (CollaborationWorkItemEntity item : openItems) {
            if (carriedForwardSubmitter == null) {
                carriedForwardSubmitter = item.getSubmitterUserId();
            }
            item.setStatus(CollaborationWorkItemStatus.RESOLVED);
            item.setResolvedAt(now);
            item.setUpdatedAt(now);
            workItemRepository.save(item);
            auditRecorder.recordCollaborationWorkItemResolved(
                    template.getId(),
                    template.getGroupCode(),
                    item.getId(),
                    CollaborationWorkItemQueue.TEST,
                    session.username(),
                    actorSummary(session)
            );
        }
        return Optional.ofNullable(carriedForwardSubmitter);
    }

    /**
     * Upserts the single OPEN REMEDIATION to-do for the template (Spec B). At most one OPEN
     * REMEDIATION per template (dedup/refresh), mirroring the SUBMIT_FOR_TEST upsert pattern.
     */
    @Transactional
    public CollaborationWorkItemEntity upsertRemediationWorkItem(
            TemplateEntity template,
            String submitterUserId,
            ManagementSessionClaims session
    ) {
        String summary = messageResolver.resolve(REMEDIATION_SUMMARY_KEY);
        Instant now = Instant.now();
        return workItemRepository
                .findOpenByTemplateIdAndQueue(template.getId(), CollaborationWorkItemQueue.REMEDIATION)
                .map(existing -> refreshRemediation(existing, template, submitterUserId, summary, now))
                .orElseGet(() -> createRemediation(template, submitterUserId, session, summary, now));
    }

    private CollaborationWorkItemEntity refreshSubmitForTest(
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

    private CollaborationWorkItemEntity createSubmitForTest(
            TemplateEntity template,
            ManagementSessionClaims session,
            String summary,
            Instant now
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
                actorSummary(session)
        );
        return saved;
    }

    private CollaborationWorkItemEntity refreshRemediation(
            CollaborationWorkItemEntity existing,
            TemplateEntity template,
            String submitterUserId,
            String summary,
            Instant now
    ) {
        existing.setTemplateName(template.getName());
        existing.setSubmitterUserId(submitterUserId);
        existing.setSummaryText(summary);
        existing.setTriggerType(CollaborationWorkItemTriggerType.TEST_FAILURE_OR_RETURN_TO_DRAFT);
        existing.setUpdatedAt(now);
        return workItemRepository.save(existing);
    }

    private CollaborationWorkItemEntity createRemediation(
            TemplateEntity template,
            String submitterUserId,
            ManagementSessionClaims session,
            String summary,
            Instant now
    ) {
        CollaborationWorkItemEntity created = new CollaborationWorkItemEntity(
                UUID.randomUUID(),
                template.getId(),
                template.getExternalId(),
                template.getName(),
                template.getGroupCode(),
                CollaborationWorkItemQueue.REMEDIATION,
                CollaborationWorkItemTriggerType.TEST_FAILURE_OR_RETURN_TO_DRAFT,
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
                CollaborationWorkItemTriggerType.TEST_FAILURE_OR_RETURN_TO_DRAFT,
                session.username(),
                actorSummary(session)
        );
        return saved;
    }

    private String actorSummary(ManagementSessionClaims session) {
        return session.displayName() + " (" + session.username() + ")";
    }
}
