package com.bank.docgen.audit.service;

import static com.bank.docgen.audit.service.ManagementAuditEventTypes.CONTENT_MODULE_BULK_REPIN;
import static com.bank.docgen.audit.service.ManagementAuditEventTypes.CONTENT_MODULE_CREATED;
import static com.bank.docgen.audit.service.ManagementAuditEventTypes.CONTENT_MODULE_LIFECYCLE_OPERATION;
import static com.bank.docgen.audit.service.ManagementAuditEventTypes.CONTENT_MODULE_REVIEW_TRANSITION;
import static com.bank.docgen.audit.service.ManagementAuditEventTypes.CONTENT_MODULE_SHARED_GROUP_CODES_UPDATED;
import static com.bank.docgen.audit.service.ManagementAuditEventTypes.CONTENT_MODULE_VERSION_CREATED;
import static com.bank.docgen.audit.service.ManagementAuditEventTypes.CONTENT_MODULE_VERSION_UPDATED;

import com.bank.docgen.audit.api.ContentModuleLifecycleAuditDetail;
import com.bank.docgen.template.api.BulkRepinItemView;
import com.bank.docgen.template.api.BulkRepinSummaryView;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    void recordContentModuleSharedGroupCodesUpdated(
            UUID moduleId,
            String groupCode,
            String moduleCode,
            String actorUsername,
            String actorSummary
    ) {
        recordContentModuleEvent(
                CONTENT_MODULE_SHARED_GROUP_CODES_UPDATED,
                moduleId,
                groupCode,
                "Shared group codes updated: " + moduleCode,
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
        recordContentModuleReviewTransition(
                moduleId, groupCode, moduleCode, operation, semanticVersion, reviewState,
                actorUsername, actorSummary, false, null
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
            String actorSummary,
            boolean selfApprovalException,
            String exceptionReason
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("selfApprovalException", selfApprovalException);
        if (exceptionReason != null) {
            payload.put("exceptionReason", exceptionReason);
        }
        recordContentModuleEvent(
                CONTENT_MODULE_REVIEW_TRANSITION,
                moduleId,
                groupCode,
                operation + " on " + moduleCode + "@" + semanticVersion + " -> " + reviewState,
                actorUsername,
                actorSummary,
                eventWriter.writeJsonMap(payload)
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

    @Transactional
    void recordContentModuleBulkRepin(
            UUID moduleId,
            String groupCode,
            String actorUsername,
            String actorSummary,
            boolean dryRun,
            String fromSemanticVersion,
            String toSemanticVersion,
            boolean useLatestApproved,
            BulkRepinSummaryView summary,
            List<BulkRepinItemView> items
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("dryRun", dryRun);
        payload.put("fromSemanticVersion", fromSemanticVersion);
        payload.put("toSemanticVersion", toSemanticVersion);
        payload.put("useLatestApproved", useLatestApproved);
        payload.put("wouldApplyCount", summary.wouldApplyCount());
        payload.put("appliedCount", summary.appliedCount());
        payload.put("skippedLockedCount", summary.skippedLockedCount());
        payload.put("skippedAlreadyAtTargetCount", summary.skippedAlreadyAtTargetCount());
        payload.put("skippedNoMatchCount", summary.skippedNoMatchCount());
        payload.put("failedCount", summary.failedCount());
        List<Map<String, Object>> itemPayloads = new ArrayList<>();
        for (BulkRepinItemView item : items) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("templateId", item.templateId());
            row.put("templateVersionId", item.templateVersionId());
            row.put("referenceKey", item.referenceKey());
            row.put("beforeSemanticVersion", item.beforeSemanticVersion());
            row.put("afterSemanticVersion", item.afterSemanticVersion());
            row.put("status", item.status() == null ? null : item.status().name());
            row.put("errorCode", item.errorCode());
            itemPayloads.add(row);
        }
        payload.put("items", itemPayloads);
        String summaryText = "Bulk re-pin dryRun=" + dryRun
                + " applied=" + summary.appliedCount()
                + " wouldApply=" + summary.wouldApplyCount()
                + " failed=" + summary.failedCount();
        recordContentModuleEvent(
                CONTENT_MODULE_BULK_REPIN,
                moduleId,
                groupCode,
                summaryText,
                actorUsername,
                actorSummary,
                eventWriter.writeJsonMap(payload)
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
