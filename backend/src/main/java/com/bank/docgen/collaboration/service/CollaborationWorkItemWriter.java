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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CollaborationWorkItemWriter {

    static final String SUBMIT_FOR_TEST_SUMMARY_KEY = "api.collaboration.workItem.submitForTest.summary";
    static final String REMEDIATION_SUMMARY_KEY = "api.collaboration.workItem.remediation.summary";
    static final String SUBMIT_FOR_APPROVAL_SUMMARY_KEY = "api.collaboration.workItem.submitForApproval.summary";
    static final String APPROVAL_FAILURE_REMEDIATION_SUMMARY_KEY =
            "api.collaboration.workItem.approvalFailureRemediation.summary";
    static final String PENDING_RELEASE_SUMMARY_KEY = "api.collaboration.workItem.pendingRelease.summary";

    private final CollaborationWorkItemRepository workItemRepository;
    private final MessageResolver messageResolver;
    private final ManagementAuditRecorder auditRecorder;
    private final CollaborationWorkItemPersistSupport persistSupport;

    public CollaborationWorkItemWriter(
            CollaborationWorkItemRepository workItemRepository,
            MessageResolver messageResolver,
            ManagementAuditRecorder auditRecorder
    ) {
        this.workItemRepository = workItemRepository;
        this.messageResolver = messageResolver;
        this.auditRecorder = auditRecorder;
        this.persistSupport = new CollaborationWorkItemPersistSupport(workItemRepository, auditRecorder);
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
                .map(existing -> persistSupport.refreshSubmitForTest(existing, template, session, summary, now))
                .orElseGet(() -> persistSupport.createSubmitForTest(
                        template, session, summary, now, actorSummary(session)));
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
        return resolveOpenWorkItems(template, session, CollaborationWorkItemQueue.TEST);
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
                .map(existing -> persistSupport.refreshRemediation(
                        existing,
                        template,
                        submitterUserId,
                        summary,
                        CollaborationWorkItemTriggerType.TEST_FAILURE_OR_RETURN_TO_DRAFT,
                        now))
                .orElseGet(() -> persistSupport.createRemediation(
                        template,
                        submitterUserId,
                        session,
                        summary,
                        CollaborationWorkItemTriggerType.TEST_FAILURE_OR_RETURN_TO_DRAFT,
                        now,
                        actorSummary(session)));
    }

    @Transactional
    public CollaborationWorkItemEntity upsertSubmitForApprovalWorkItem(
            TemplateEntity template,
            ManagementSessionClaims session
    ) {
        String summary = messageResolver.resolve(SUBMIT_FOR_APPROVAL_SUMMARY_KEY);
        Instant now = Instant.now();
        return workItemRepository
                .findOpenByTemplateIdAndQueue(template.getId(), CollaborationWorkItemQueue.APPROVAL)
                .map(existing -> persistSupport.refreshSubmitForApproval(existing, template, session, summary, now))
                .orElseGet(() -> persistSupport.createSubmitForApproval(
                        template, session, summary, now, actorSummary(session)));
    }

    /**
     * Resolves every OPEN APPROVAL work item for the template. Idempotent no-op when none exist.
     * Returns the carried-forward orchestrator for downstream remediation / pending-release routing.
     */
    @Transactional
    public Optional<String> resolveOpenApprovalWorkItems(
            TemplateEntity template,
            ManagementSessionClaims session
    ) {
        return resolveOpenWorkItems(template, session, CollaborationWorkItemQueue.APPROVAL);
    }

    @Transactional
    public CollaborationWorkItemEntity upsertApprovalFailureRemediationWorkItem(
            TemplateEntity template,
            String submitterUserId,
            ManagementSessionClaims session
    ) {
        String summary = messageResolver.resolve(APPROVAL_FAILURE_REMEDIATION_SUMMARY_KEY);
        Instant now = Instant.now();
        return workItemRepository
                .findOpenByTemplateIdAndQueue(template.getId(), CollaborationWorkItemQueue.REMEDIATION)
                .map(existing -> persistSupport.refreshRemediation(
                        existing,
                        template,
                        submitterUserId,
                        summary,
                        CollaborationWorkItemTriggerType.APPROVAL_FAILURE_OR_RETURN_TO_DRAFT,
                        now))
                .orElseGet(() -> persistSupport.createRemediation(
                        template,
                        submitterUserId,
                        session,
                        summary,
                        CollaborationWorkItemTriggerType.APPROVAL_FAILURE_OR_RETURN_TO_DRAFT,
                        now,
                        actorSummary(session)));
    }

    @Transactional
    public CollaborationWorkItemEntity upsertPendingReleaseWorkItem(
            TemplateEntity template,
            String submitterUserId,
            ManagementSessionClaims session
    ) {
        String summary = messageResolver.resolve(PENDING_RELEASE_SUMMARY_KEY);
        Instant now = Instant.now();
        return workItemRepository
                .findOpenByTemplateIdAndQueue(template.getId(), CollaborationWorkItemQueue.PENDING_RELEASE)
                .map(existing -> persistSupport.refreshPendingRelease(existing, template, submitterUserId, summary, now))
                .orElseGet(() -> persistSupport.createPendingRelease(
                        template, submitterUserId, session, summary, now, actorSummary(session)));
    }

    @Transactional
    public void resolveOpenPendingReleaseWorkItems(
            TemplateEntity template,
            ManagementSessionClaims session
    ) {
        resolveOpenWorkItems(template, session, CollaborationWorkItemQueue.PENDING_RELEASE);
    }

    private Optional<String> resolveOpenWorkItems(
            TemplateEntity template,
            ManagementSessionClaims session,
            CollaborationWorkItemQueue queue
    ) {
        List<CollaborationWorkItemEntity> openItems = workItemRepository
                .findAllOpenByTemplateIdAndQueue(template.getId(), queue);
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
                    queue,
                    session.username(),
                    actorSummary(session)
            );
        }
        return Optional.ofNullable(carriedForwardSubmitter);
    }

    private String actorSummary(ManagementSessionClaims session) {
        return session.displayName() + " (" + session.username() + ")";
    }
}
