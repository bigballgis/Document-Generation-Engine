package com.bank.docgen.audit.service;

import static com.bank.docgen.audit.service.ManagementAuditEventTypes.COLLABORATION_ESCALATION_ACTOR_SUMMARY;
import static com.bank.docgen.audit.service.ManagementAuditEventTypes.COLLABORATION_ESCALATION_ACTOR_USERNAME;
import static com.bank.docgen.audit.service.ManagementAuditEventTypes.COLLABORATION_TIMEOUT_ESCALATION;
import static com.bank.docgen.audit.service.ManagementAuditEventTypes.COLLABORATION_WORK_ITEM_CREATED;
import static com.bank.docgen.audit.service.ManagementAuditEventTypes.COLLABORATION_WORK_ITEM_RESOLVED;

import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemTriggerType;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class CollaborationAuditRecorder {

    private final ManagementAuditEventWriter eventWriter;

    CollaborationAuditRecorder(ManagementAuditEventWriter eventWriter) {
        this.eventWriter = eventWriter;
    }

    @Transactional
    void recordCollaborationTimeoutEscalation(
            UUID templateId,
            String groupCode,
            UUID sourceWorkItemId,
            CollaborationWorkItemQueue sourceQueue,
            String statusSummary
    ) {
        eventWriter.persist(
                COLLABORATION_TIMEOUT_ESCALATION,
                templateId,
                groupCode,
                null,
                null,
                null,
                eventWriter.writeJson(List.of(sourceQueue.name(), sourceWorkItemId.toString())),
                false,
                null,
                COLLABORATION_ESCALATION_ACTOR_USERNAME,
                COLLABORATION_ESCALATION_ACTOR_SUMMARY,
                null,
                eventWriter.truncate(statusSummary),
                eventWriter.writeJson(List.of())
        );
    }

    @Transactional
    void recordCollaborationWorkItemCreated(
            UUID templateId,
            String groupCode,
            UUID workItemId,
            CollaborationWorkItemQueue queue,
            CollaborationWorkItemTriggerType triggerType,
            String actorUsername,
            String actorSummary
    ) {
        eventWriter.persist(
                COLLABORATION_WORK_ITEM_CREATED,
                templateId,
                groupCode,
                null,
                null,
                null,
                eventWriter.writeJson(List.of(queue.name(), triggerType.name(), workItemId.toString())),
                false,
                null,
                actorUsername,
                actorSummary,
                null,
                eventWriter.truncate("Collaboration work item created: " + queue.name() + "/" + triggerType.name()),
                eventWriter.writeJson(List.of())
        );
    }

    @Transactional
    void recordCollaborationWorkItemResolved(
            UUID templateId,
            String groupCode,
            UUID workItemId,
            CollaborationWorkItemQueue queue,
            String actorUsername,
            String actorSummary
    ) {
        eventWriter.persist(
                COLLABORATION_WORK_ITEM_RESOLVED,
                templateId,
                groupCode,
                null,
                null,
                null,
                eventWriter.writeJson(List.of(queue.name(), workItemId.toString())),
                false,
                null,
                actorUsername,
                actorSummary,
                null,
                eventWriter.truncate("Collaboration work item resolved: " + queue.name()),
                eventWriter.writeJson(List.of())
        );
    }
}
