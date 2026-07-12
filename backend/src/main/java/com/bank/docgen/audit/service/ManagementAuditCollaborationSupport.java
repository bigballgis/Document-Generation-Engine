package com.bank.docgen.audit.service;

import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemTriggerType;
import java.util.List;
import java.util.UUID;

/**
 * Package-private collaboration timeout / work-item management-audit record helpers.
 */
final class ManagementAuditCollaborationSupport {

    private final ManagementAuditEventWriter writer;

    ManagementAuditCollaborationSupport(ManagementAuditEventWriter writer) {
        this.writer = writer;
    }

    void recordCollaborationTimeoutConfigUpdated(
            String scopeType,
            String groupCode,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        writer.persistIdentity(
                ManagementAuditRecorder.COLLABORATION_TIMEOUT_CONFIG_UPDATED,
                groupCode,
                actorUsername,
                actorSummary,
                scopeType + ": " + statusSummary
        );
    }

    void recordCollaborationTimeoutEscalation(
            UUID templateId,
            String groupCode,
            UUID sourceWorkItemId,
            CollaborationWorkItemQueue sourceQueue,
            String statusSummary
    ) {
        writer.persist(
                ManagementAuditRecorder.COLLABORATION_TIMEOUT_ESCALATION,
                templateId,
                groupCode,
                null,
                null,
                null,
                writer.writeJson(List.of(sourceQueue.name(), sourceWorkItemId.toString())),
                false,
                null,
                ManagementAuditRecorder.COLLABORATION_ESCALATION_ACTOR_USERNAME,
                ManagementAuditRecorder.COLLABORATION_ESCALATION_ACTOR_SUMMARY,
                null,
                writer.truncate(statusSummary),
                writer.emptyJsonArray()
        );
    }

    void recordCollaborationWorkItemCreated(
            UUID templateId,
            String groupCode,
            UUID workItemId,
            CollaborationWorkItemQueue queue,
            CollaborationWorkItemTriggerType triggerType,
            String actorUsername,
            String actorSummary
    ) {
        writer.persist(
                ManagementAuditRecorder.COLLABORATION_WORK_ITEM_CREATED,
                templateId,
                groupCode,
                null,
                null,
                null,
                writer.writeJson(List.of(queue.name(), triggerType.name(), workItemId.toString())),
                false,
                null,
                actorUsername,
                actorSummary,
                null,
                writer.truncate("Collaboration work item created: " + queue.name() + "/" + triggerType.name()),
                writer.emptyJsonArray()
        );
    }

    void recordCollaborationWorkItemResolved(
            UUID templateId,
            String groupCode,
            UUID workItemId,
            CollaborationWorkItemQueue queue,
            String actorUsername,
            String actorSummary
    ) {
        writer.persist(
                ManagementAuditRecorder.COLLABORATION_WORK_ITEM_RESOLVED,
                templateId,
                groupCode,
                null,
                null,
                null,
                writer.writeJson(List.of(queue.name(), workItemId.toString())),
                false,
                null,
                actorUsername,
                actorSummary,
                null,
                writer.truncate("Collaboration work item resolved: " + queue.name()),
                writer.emptyJsonArray()
        );
    }
}
