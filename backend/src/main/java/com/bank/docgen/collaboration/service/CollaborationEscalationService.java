package com.bank.docgen.collaboration.service;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.collaboration.api.CollaborationTimeoutConfigView;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemStatus;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemTriggerType;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemEntity;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CollaborationEscalationService {

    static final String SYSTEM_ACTOR_USERNAME = ManagementAuditRecorder.COLLABORATION_ESCALATION_ACTOR_USERNAME;
    static final String SYSTEM_ACTOR_SUMMARY = ManagementAuditRecorder.COLLABORATION_ESCALATION_ACTOR_SUMMARY;

    private final CollaborationWorkItemRepository workItemRepository;
    private final CollaborationTimeoutResolver timeoutResolver;
    private final ManagementAuditRecorder auditRecorder;
    private final Clock clock;

    public CollaborationEscalationService(
            CollaborationWorkItemRepository workItemRepository,
            CollaborationTimeoutResolver timeoutResolver,
            ManagementAuditRecorder auditRecorder,
            Clock clock
    ) {
        this.workItemRepository = workItemRepository;
        this.timeoutResolver = timeoutResolver;
        this.auditRecorder = auditRecorder;
        this.clock = clock;
    }

    @Transactional
    public int processDueEscalations() {
        Instant now = clock.instant();
        int created = 0;
        for (CollaborationWorkItemEntity source : workItemRepository.findOpenEscalationCandidates()) {
            if (isOverdue(source, now) && !workItemRepository.existsOpenEscalationForSource(source.getId())) {
                createEscalation(source, now);
                created++;
            }
        }
        return created;
    }

    private boolean isOverdue(CollaborationWorkItemEntity source, Instant now) {
        CollaborationTimeoutConfigView thresholds = timeoutResolver.resolveForGroup(source.getGroupCode());
        long ageHours = Duration.between(source.getCreatedAt(), now).toHours();
        return ageHours >= thresholdHours(source.getQueue(), thresholds);
    }

    private int thresholdHours(CollaborationWorkItemQueue queue, CollaborationTimeoutConfigView thresholds) {
        return switch (queue) {
            case TEST -> thresholds.testThresholdHours();
            case APPROVAL -> thresholds.approvalThresholdHours();
            case PENDING_RELEASE -> thresholds.pendingReleaseThresholdHours();
            case REMEDIATION -> thresholds.remediationThresholdHours();
            case ESCALATION -> Integer.MAX_VALUE;
        };
    }

    private void createEscalation(CollaborationWorkItemEntity source, Instant now) {
        int thresholdHours = thresholdHours(
                source.getQueue(),
                timeoutResolver.resolveForGroup(source.getGroupCode())
        );
        String summary = source.getQueue().name() + " queue to-do exceeded "
                + thresholdHours + " hour threshold";
        CollaborationWorkItemEntity escalation = new CollaborationWorkItemEntity(
                UUID.randomUUID(),
                source.getTemplateId(),
                source.getTemplateExternalId(),
                source.getTemplateName(),
                source.getGroupCode(),
                CollaborationWorkItemQueue.ESCALATION,
                CollaborationWorkItemTriggerType.TIMEOUT_ESCALATION,
                CollaborationWorkItemStatus.OPEN,
                source.getSubmitterUserId(),
                summary
        );
        escalation.setSourceWorkItemId(source.getId());
        escalation.setCreatedAt(now);
        workItemRepository.save(escalation);
        auditRecorder.recordCollaborationTimeoutEscalation(
                source.getTemplateId(),
                source.getGroupCode(),
                source.getId(),
                source.getQueue(),
                summary
        );
    }
}
