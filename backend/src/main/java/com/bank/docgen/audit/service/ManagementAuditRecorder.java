package com.bank.docgen.audit.service;

import com.bank.docgen.audit.api.ContentModuleLifecycleAuditDetail;
import com.bank.docgen.audit.api.PolicyUpdateAuditDetail;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemTriggerType;
import com.bank.docgen.audit.persistence.ManagementAuditEventEntity;
import com.bank.docgen.audit.persistence.ManagementAuditEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManagementAuditRecorder {

    public static final String API_POLICY_UPDATED = "API_POLICY_UPDATED";
    public static final String API_CREDENTIAL_CREATED = "API_CREDENTIAL_CREATED";
    public static final String API_CREDENTIAL_ROTATED = "API_CREDENTIAL_ROTATED";
    public static final String API_CREDENTIAL_REVOKED = "API_CREDENTIAL_REVOKED";
    public static final String USER_CREATED = "USER_CREATED";
    public static final String USER_UPDATED = "USER_UPDATED";
    public static final String USER_DISABLED = "USER_DISABLED";
    public static final String USER_ENABLED = "USER_ENABLED";
    public static final String USER_PASSWORD_RESET = "USER_PASSWORD_RESET";
    public static final String USER_DELETED = "USER_DELETED";
    public static final String GROUP_CREATED = "GROUP_CREATED";
    public static final String GROUP_UPDATED = "GROUP_UPDATED";
    public static final String GROUP_DISABLED = "GROUP_DISABLED";
    public static final String GROUP_ENABLED = "GROUP_ENABLED";
    public static final String IDENTITY_ESCALATION_DENIED = "IDENTITY_ESCALATION_DENIED";
    public static final String RISK_PROMPT_CONFIG_UPDATED = "RISK_PROMPT_CONFIG_UPDATED";
    public static final String COLLABORATION_TIMEOUT_CONFIG_UPDATED = "COLLABORATION_TIMEOUT_CONFIG_UPDATED";
    public static final String COLLABORATION_TIMEOUT_ESCALATION = "COLLABORATION_TIMEOUT_ESCALATION";
    public static final String COLLABORATION_WORK_ITEM_CREATED = "COLLABORATION_WORK_ITEM_CREATED";
    public static final String COLLABORATION_WORK_ITEM_RESOLVED = "COLLABORATION_WORK_ITEM_RESOLVED";
    public static final String COLLABORATION_ESCALATION_ACTOR_USERNAME = "00000000";
    public static final String COLLABORATION_ESCALATION_ACTOR_SUMMARY = "Collaboration escalation scheduler";
    public static final String CONTENT_MODULE_CREATED = "CONTENT_MODULE_CREATED";
    public static final String CONTENT_MODULE_VERSION_CREATED = "CONTENT_MODULE_VERSION_CREATED";
    public static final String CONTENT_MODULE_VERSION_UPDATED = "CONTENT_MODULE_VERSION_UPDATED";
    public static final String CONTENT_MODULE_REVIEW_TRANSITION = "CONTENT_MODULE_REVIEW_TRANSITION";
    public static final String CONTENT_MODULE_LIFECYCLE_OPERATION = "CONTENT_MODULE_LIFECYCLE_OPERATION";
    public static final String TEMPLATE_EXPORTED = "TEMPLATE_EXPORTED";
    public static final String TEMPLATE_IMPORTED = "TEMPLATE_IMPORTED";

    private static final int STATUS_SUMMARY_MAX = 512;

    private final ManagementAuditEventRepository repository;
    private final ObjectMapper objectMapper;

    public ManagementAuditRecorder(ManagementAuditEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void recordPolicyUpdated(
            UUID templateId,
            String groupCode,
            int previousPolicyVersion,
            int policyVersion,
            List<String> changedAreas,
            String actorUsername,
            String actorSummary
    ) {
        recordPolicyUpdated(
                templateId,
                groupCode,
                previousPolicyVersion,
                policyVersion,
                changedAreas,
                actorUsername,
                actorSummary,
                PolicyUpdateAuditDetail.empty()
        );
    }

    @Transactional
    public void recordPolicyUpdated(
            UUID templateId,
            String groupCode,
            int previousPolicyVersion,
            int policyVersion,
            List<String> changedAreas,
            String actorUsername,
            String actorSummary,
            PolicyUpdateAuditDetail detail
    ) {
        boolean rollback = detail.rollback() != null && detail.rollback();
        repository.save(new ManagementAuditEventEntity(
                UUID.randomUUID(),
                Instant.now(),
                API_POLICY_UPDATED,
                templateId,
                groupCode,
                null,
                previousPolicyVersion,
                policyVersion,
                writeJson(changedAreas),
                rollback,
                detail.rollbackSourcePolicyVersion(),
                actorUsername,
                actorSummary,
                null,
                truncate(buildStatusSummary(changedAreas, detail)),
                writeAuditPayload(detail)
        ));
    }

    private String buildStatusSummary(List<String> changedAreas, PolicyUpdateAuditDetail detail) {
        if (detail.configDiffSummary().isEmpty()) {
            return "Policy updated: " + String.join(", ", changedAreas);
        }
        return "Policy updated: " + String.join("; ", detail.configDiffSummary());
    }

    private String writeAuditPayload(PolicyUpdateAuditDetail detail) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("configDiffSummary", detail.configDiffSummary());
        payload.put("impactPreviewSummary", detail.impactPreviewSummary());
        payload.put("hardBlockSummary", detail.hardBlockSummary());
        payload.put("warningSummary", detail.warningSummary());
        payload.put("confirmed", detail.confirmed());
        payload.put("rollback", detail.rollback());
        payload.put("rollbackSourcePolicyVersion", detail.rollbackSourcePolicyVersion());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    @Transactional
    public void recordCredentialCreated(
            UUID templateId,
            String groupCode,
            UUID credentialId,
            String credentialExternalId,
            String actorUsername,
            String actorSummary
    ) {
        repository.save(new ManagementAuditEventEntity(
                UUID.randomUUID(),
                Instant.now(),
                API_CREDENTIAL_CREATED,
                templateId,
                groupCode,
                credentialId,
                null,
                null,
                writeJson(List.of()),
                false,
                null,
                actorUsername,
                actorSummary,
                fingerprint(credentialExternalId),
                "Credential created",
                writeJson(List.of())
        ));
    }

    @Transactional
    public void recordCredentialRotated(
            UUID templateId,
            String groupCode,
            UUID credentialId,
            String credentialExternalId,
            String actorUsername,
            String actorSummary
    ) {
        repository.save(new ManagementAuditEventEntity(
                UUID.randomUUID(),
                Instant.now(),
                API_CREDENTIAL_ROTATED,
                templateId,
                groupCode,
                credentialId,
                null,
                null,
                writeJson(List.of()),
                false,
                null,
                actorUsername,
                actorSummary,
                fingerprint(credentialExternalId),
                "Credential rotated",
                writeJson(List.of())
        ));
    }

    @Transactional
    public void recordCredentialRevoked(
            UUID templateId,
            String groupCode,
            UUID credentialId,
            String credentialExternalId,
            String actorUsername,
            String actorSummary
    ) {
        repository.save(new ManagementAuditEventEntity(
                UUID.randomUUID(),
                Instant.now(),
                API_CREDENTIAL_REVOKED,
                templateId,
                groupCode,
                credentialId,
                null,
                null,
                writeJson(List.of()),
                false,
                null,
                actorUsername,
                actorSummary,
                fingerprint(credentialExternalId),
                "Credential revoked",
                writeJson(List.of())
        ));
    }

    @Transactional
    public void recordUserEvent(
            String eventType,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        recordIdentityEvent(eventType, null, actorUsername, actorSummary, statusSummary);
    }

    @Transactional
    public void recordGroupEvent(
            String eventType,
            String groupCode,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        recordIdentityEvent(eventType, groupCode, actorUsername, actorSummary, statusSummary);
    }

    @Transactional
    public void recordRiskPromptConfigUpdated(
            String scopeType,
            String groupCode,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        recordIdentityEvent(
                RISK_PROMPT_CONFIG_UPDATED,
                groupCode,
                actorUsername,
                actorSummary,
                scopeType + ": " + statusSummary
        );
    }

    @Transactional
    public void recordCollaborationTimeoutConfigUpdated(
            String scopeType,
            String groupCode,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        recordIdentityEvent(
                COLLABORATION_TIMEOUT_CONFIG_UPDATED,
                groupCode,
                actorUsername,
                actorSummary,
                scopeType + ": " + statusSummary
        );
    }

    @Transactional
    public void recordCollaborationTimeoutEscalation(
            UUID templateId,
            String groupCode,
            UUID sourceWorkItemId,
            CollaborationWorkItemQueue sourceQueue,
            String statusSummary
    ) {
        repository.save(new ManagementAuditEventEntity(
                UUID.randomUUID(),
                Instant.now(),
                COLLABORATION_TIMEOUT_ESCALATION,
                templateId,
                groupCode,
                null,
                null,
                null,
                writeJson(List.of(sourceQueue.name(), sourceWorkItemId.toString())),
                false,
                null,
                COLLABORATION_ESCALATION_ACTOR_USERNAME,
                COLLABORATION_ESCALATION_ACTOR_SUMMARY,
                null,
                truncate(statusSummary),
                writeJson(List.of())
        ));
    }

    @Transactional
    public void recordCollaborationWorkItemCreated(
            UUID templateId,
            String groupCode,
            UUID workItemId,
            CollaborationWorkItemQueue queue,
            CollaborationWorkItemTriggerType triggerType,
            String actorUsername,
            String actorSummary
    ) {
        repository.save(new ManagementAuditEventEntity(
                UUID.randomUUID(),
                Instant.now(),
                COLLABORATION_WORK_ITEM_CREATED,
                templateId,
                groupCode,
                null,
                null,
                null,
                writeJson(List.of(queue.name(), triggerType.name(), workItemId.toString())),
                false,
                null,
                actorUsername,
                actorSummary,
                null,
                truncate("Collaboration work item created: " + queue.name() + "/" + triggerType.name()),
                writeJson(List.of())
        ));
    }

    @Transactional
    public void recordCollaborationWorkItemResolved(
            UUID templateId,
            String groupCode,
            UUID workItemId,
            CollaborationWorkItemQueue queue,
            String actorUsername,
            String actorSummary
    ) {
        repository.save(new ManagementAuditEventEntity(
                UUID.randomUUID(),
                Instant.now(),
                COLLABORATION_WORK_ITEM_RESOLVED,
                templateId,
                groupCode,
                null,
                null,
                null,
                writeJson(List.of(queue.name(), workItemId.toString())),
                false,
                null,
                actorUsername,
                actorSummary,
                null,
                truncate("Collaboration work item resolved: " + queue.name()),
                writeJson(List.of())
        ));
    }

    @Transactional
    public void recordTemplateExported(
            UUID templateId,
            String groupCode,
            String externalId,
            String actorUsername,
            String actorSummary
    ) {
        repository.save(new ManagementAuditEventEntity(
                UUID.randomUUID(),
                Instant.now(),
                TEMPLATE_EXPORTED,
                templateId,
                groupCode,
                null,
                null,
                null,
                null,
                false,
                null,
                actorUsername,
                actorSummary,
                null,
                truncate("Template exported: " + externalId),
                writeJson(List.of())
        ));
    }

    @Transactional
    public void recordTemplateImported(
            UUID templateId,
            String groupCode,
            String externalId,
            String importBatchId,
            int developmentVersion,
            String actorUsername,
            String actorSummary
    ) {
        repository.save(new ManagementAuditEventEntity(
                UUID.randomUUID(),
                Instant.now(),
                TEMPLATE_IMPORTED,
                templateId,
                groupCode,
                null,
                null,
                null,
                writeJson(List.of()),
                false,
                null,
                actorUsername,
                actorSummary,
                null,
                truncate("Template imported: " + externalId + " batch=" + importBatchId + " dev=" + developmentVersion),
                writeJson(List.of())
        ));
    }

    @Transactional
    public void recordContentModuleCreated(
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
    public void recordContentModuleVersionCreated(
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
    public void recordContentModuleVersionUpdated(
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
    public void recordContentModuleReviewTransition(
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
    public void recordContentModuleLifecycleOperation(
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
                writeContentModuleLifecyclePayload(impactDetail)
        );
    }

    @Transactional
    public void recordContentModuleLifecycleOperation(
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
        recordContentModuleEvent(eventType, moduleId, groupCode, statusSummary, actorUsername, actorSummary, writeJson(List.of()));
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
        repository.save(new ManagementAuditEventEntity(
                UUID.randomUUID(),
                Instant.now(),
                eventType,
                moduleId,
                groupCode,
                null,
                null,
                null,
                writeJson(List.of("CONTENT_MODULE")),
                false,
                null,
                actorUsername,
                actorSummary,
                null,
                truncate(statusSummary),
                auditPayloadJson
        ));
    }

    private String writeContentModuleLifecyclePayload(ContentModuleLifecycleAuditDetail detail) {
        if (detail == null) {
            return writeJson(List.of());
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("referenceTemplateCount", detail.referenceTemplateCount());
        payload.put("referenceTemplateListHint", detail.referenceTemplateListHint());
        payload.put("impactedReleaseVersionsHint", detail.impactedReleaseVersionsHint());
        payload.put("defaultRouteAffected", detail.defaultRouteAffected());
        payload.put("recentCallSummary", detail.recentCallSummary());
        payload.put("remediationHint", detail.remediationHint());
        payload.put("templateStopRequired", detail.templateStopRequired());
        payload.put("releaseStopRequired", detail.releaseStopRequired());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return writeJson(List.of());
        }
    }

    @Transactional
    public void recordEscalationDenied(
            String reasonCode,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        recordIdentityEvent(
                IDENTITY_ESCALATION_DENIED,
                null,
                actorUsername,
                actorSummary,
                reasonCode + ": " + statusSummary
        );
    }

    private void recordIdentityEvent(
            String eventType,
            String groupCode,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        repository.save(new ManagementAuditEventEntity(
                UUID.randomUUID(),
                Instant.now(),
                eventType,
                null,
                groupCode,
                null,
                null,
                null,
                writeJson(List.of()),
                false,
                null,
                actorUsername,
                actorSummary,
                null,
                truncate(statusSummary),
                writeJson(List.of())
        ));
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= STATUS_SUMMARY_MAX ? value : value.substring(0, STATUS_SUMMARY_MAX);
    }

    private String fingerprint(String externalId) {
        return externalId == null ? null : "fp-" + externalId;
    }

    private String writeJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }
}
