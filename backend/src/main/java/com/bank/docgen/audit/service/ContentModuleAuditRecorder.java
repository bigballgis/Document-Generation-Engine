package com.bank.docgen.audit.service;

import static com.bank.docgen.audit.service.ManagementAuditEventTypes.CONTENT_MODULE_CREATED;
import static com.bank.docgen.audit.service.ManagementAuditEventTypes.CONTENT_MODULE_LIFECYCLE_OPERATION;
import static com.bank.docgen.audit.service.ManagementAuditEventTypes.CONTENT_MODULE_REVIEW_TRANSITION;
import static com.bank.docgen.audit.service.ManagementAuditEventTypes.CONTENT_MODULE_VERSION_CREATED;
import static com.bank.docgen.audit.service.ManagementAuditEventTypes.CONTENT_MODULE_VERSION_UPDATED;

import com.bank.docgen.audit.api.ContentModuleLifecycleAuditDetail;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class ContentModuleAuditRecorder {

    private final ManagementAuditEventWriter eventWriter;

    ContentModuleAuditRecorder(ManagementAuditEventWriter eventWriter) {
        this.eventWriter = eventWriter;
    }

    @Transactional
    void recordContentModuleCreated(
            UUID moduleId,
            String groupCode,
            String moduleCode,
            String actorUsername,
            String actorSummary
    ) {
        recordContentModuleEvent(
                CONTENT_MODULE_CREATED,
                moduleId,
                groupCode,
                "Module created: " + moduleCode,
                actorUsername,
                actorSummary
        );
    }

    @Transactional
    void recordContentModuleVersionCreated(
            UUID moduleId,
            String groupCode,
            String moduleCode,
            String semanticVersion,
            String actorUsername,
            String actorSummary
    ) {
        recordContentModuleEvent(
                CONTENT_MODULE_VERSION_CREATED,
                moduleId,
                groupCode,
                "Version created: " + moduleCode + "@" + semanticVersion,
                actorUsername,
                actorSummary
        );
    }

    @Transactional
    void recordContentModuleVersionUpdated(
            UUID moduleId,
            String groupCode,
            String moduleCode,
            String semanticVersion,
            String actorUsername,
            String actorSummary
    ) {
        recordContentModuleEvent(
                CONTENT_MODULE_VERSION_UPDATED,
                moduleId,
                groupCode,
                "Draft updated: " + moduleCode + "@" + semanticVersion,
                actorUsername,
                actorSummary
        );
    }

    @Transactional
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
                CONTENT_MODULE_REVIEW_TRANSITION,
                moduleId,
                groupCode,
                operation + " on " + moduleCode + "@" + semanticVersion + " -> " + reviewState,
                actorUsername,
                actorSummary
        );
    }

    @Transactional
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
        recordContentModuleEvent(
                CONTENT_MODULE_LIFECYCLE_OPERATION,
                moduleId,
                groupCode,
                operation + " on " + moduleCode + "@" + semanticVersion + " -> " + lifecycleState,
                actorUsername,
                actorSummary,
                eventWriter.writeContentModuleLifecyclePayload(impactDetail)
        );
    }

    @Transactional
    void recordContentModuleLifecycleOperation(
            UUID moduleId,
            String groupCode,
            String moduleCode,
            String operation,
            String semanticVersion,
            String lifecycleState,
            String actorUsername,
            String actorSummary
    ) {
        recordContentModuleLifecycleOperation(
                moduleId,
                groupCode,
                moduleCode,
                operation,
                semanticVersion,
                lifecycleState,
                actorUsername,
                actorSummary,
                null
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
        recordContentModuleEvent(
                eventType,
                moduleId,
                groupCode,
                statusSummary,
                actorUsername,
                actorSummary,
                eventWriter.writeJson(List.of())
        );
    }

    private void recordContentModuleEvent(
            String eventType,
            UUID moduleId,
            String groupCode,
            String statusSummary,
            String actorUsername,
            String actorSummary,
            String auditPayloadJson
    ) {
        eventWriter.persist(
                eventType,
                moduleId,
                groupCode,
                null,
                null,
                null,
                eventWriter.writeJson(List.of("CONTENT_MODULE")),
                false,
                null,
                actorUsername,
                actorSummary,
                null,
                eventWriter.truncate(statusSummary),
                auditPayloadJson
        );
    }
}
