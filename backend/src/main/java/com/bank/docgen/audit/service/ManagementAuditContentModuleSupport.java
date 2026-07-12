package com.bank.docgen.audit.service;

import com.bank.docgen.audit.api.ContentModuleLifecycleAuditDetail;
import java.util.UUID;

/**
 * Package-private content-module management-audit record helpers.
 */
final class ManagementAuditContentModuleSupport {

    private final ManagementAuditEventWriter writer;

    ManagementAuditContentModuleSupport(ManagementAuditEventWriter writer) {
        this.writer = writer;
    }

    void recordContentModuleCreated(
            UUID moduleId,
            String groupCode,
            String moduleCode,
            String actorUsername,
            String actorSummary
    ) {
        recordContentModuleEvent(
                ManagementAuditRecorder.CONTENT_MODULE_CREATED,
                moduleId,
                groupCode,
                "Module created: " + moduleCode,
                actorUsername,
                actorSummary
        );
    }

    void recordContentModuleVersionCreated(
            UUID moduleId,
            String groupCode,
            String moduleCode,
            String semanticVersion,
            String actorUsername,
            String actorSummary
    ) {
        recordContentModuleEvent(
                ManagementAuditRecorder.CONTENT_MODULE_VERSION_CREATED,
                moduleId,
                groupCode,
                "Version created: " + moduleCode + "@" + semanticVersion,
                actorUsername,
                actorSummary
        );
    }

    void recordContentModuleVersionUpdated(
            UUID moduleId,
            String groupCode,
            String moduleCode,
            String semanticVersion,
            String actorUsername,
            String actorSummary
    ) {
        recordContentModuleEvent(
                ManagementAuditRecorder.CONTENT_MODULE_VERSION_UPDATED,
                moduleId,
                groupCode,
                "Draft updated: " + moduleCode + "@" + semanticVersion,
                actorUsername,
                actorSummary
        );
    }

    void recordContentModuleReviewTransition(
            UUID moduleId,
            String groupCode,
            String moduleCode,
            String operation,
            String semanticVersion,
            String reviewState,
            String actorUsername,
            String actorSummary
    ) {
        recordContentModuleEvent(
                ManagementAuditRecorder.CONTENT_MODULE_REVIEW_TRANSITION,
                moduleId,
                groupCode,
                operation + " on " + moduleCode + "@" + semanticVersion + " -> " + reviewState,
                actorUsername,
                actorSummary
        );
    }

    void recordContentModuleLifecycleOperation(
            UUID moduleId,
            String groupCode,
            String moduleCode,
            String operation,
            String semanticVersion,
            String lifecycleState,
            String actorUsername,
            String actorSummary,
            ContentModuleLifecycleAuditDetail impactDetail
    ) {
        writer.persistContentModule(
                ManagementAuditRecorder.CONTENT_MODULE_LIFECYCLE_OPERATION,
                moduleId,
                groupCode,
                operation + " on " + moduleCode + "@" + semanticVersion + " -> " + lifecycleState,
                actorUsername,
                actorSummary,
                writer.writeContentModuleLifecyclePayload(impactDetail)
        );
    }

    private void recordContentModuleEvent(
            String eventType,
            UUID moduleId,
            String groupCode,
            String statusSummary,
            String actorUsername,
            String actorSummary
    ) {
        writer.persistContentModule(
                eventType,
                moduleId,
                groupCode,
                statusSummary,
                actorUsername,
                actorSummary,
                writer.emptyJsonArray()
        );
    }
}
