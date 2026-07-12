package com.bank.docgen.audit.service;

import com.bank.docgen.audit.api.ContentModuleLifecycleAuditDetail;
import com.bank.docgen.audit.api.PolicyUpdateAuditDetail;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemTriggerType;
import com.bank.docgen.audit.persistence.ManagementAuditEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final ManagementAuditEventWriter writer;

    public ManagementAuditRecorder(ManagementAuditEventRepository repository, ObjectMapper objectMapper) {
        this.writer = new ManagementAuditEventWriter(repository, objectMapper);
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
        writer.persist(
                API_POLICY_UPDATED,
                templateId,
                groupCode,
                null,
                previousPolicyVersion,
                policyVersion,
                writer.writeJson(changedAreas),
                rollback,
                detail.rollbackSourcePolicyVersion(),
                actorUsername,
                actorSummary,
                null,
                writer.truncate(writer.buildPolicyStatusSummary(changedAreas, detail)),
                writer.writePolicyPayload(detail)
        );
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
        writer.persistCredential(
                API_CREDENTIAL_CREATED,
                templateId,
                groupCode,
                credentialId,
                credentialExternalId,
                actorUsername,
                actorSummary,
                "Credential created"
        );
    }

    @Transactional
    public void recordCredentialRotated(
            UUID templateId,
            String groupCode,
            UUID credentialId,
            String credentialExternalId,
            String actorUsername,
            String actorSummary,
            int rotationGeneration,
            String previousCredentialFingerprint
    ) {
        writer.persistCredential(
                API_CREDENTIAL_ROTATED,
                templateId,
                groupCode,
                credentialId,
                credentialExternalId,
                actorUsername,
                actorSummary,
                "Credential rotated; generation=" + rotationGeneration
                        + "; previousFingerprint=" + previousCredentialFingerprint
        );
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
        writer.persistCredential(
                API_CREDENTIAL_REVOKED,
                templateId,
                groupCode,
                credentialId,
                credentialExternalId,
                actorUsername,
                actorSummary,
                "Credential revoked"
        );
    }

    @Transactional
    public void recordUserEvent(
            String eventType,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        writer.persistIdentity(eventType, null, actorUsername, actorSummary, statusSummary);
    }

    @Transactional
    public void recordGroupEvent(
            String eventType,
            String groupCode,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        writer.persistIdentity(eventType, groupCode, actorUsername, actorSummary, statusSummary);
    }

    @Transactional
    public void recordRiskPromptConfigUpdated(
            String scopeType,
            String groupCode,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        writer.persistIdentity(
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
        writer.persistIdentity(
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
        writer.persist(
                COLLABORATION_TIMEOUT_ESCALATION,
                templateId,
                groupCode,
                null,
                null,
                null,
                writer.writeJson(List.of(sourceQueue.name(), sourceWorkItemId.toString())),
                false,
                null,
                COLLABORATION_ESCALATION_ACTOR_USERNAME,
                COLLABORATION_ESCALATION_ACTOR_SUMMARY,
                null,
                writer.truncate(statusSummary),
                writer.emptyJsonArray()
        );
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
        writer.persist(
                COLLABORATION_WORK_ITEM_CREATED,
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

    @Transactional
    public void recordCollaborationWorkItemResolved(
            UUID templateId,
            String groupCode,
            UUID workItemId,
            CollaborationWorkItemQueue queue,
            String actorUsername,
            String actorSummary
    ) {
        writer.persist(
                COLLABORATION_WORK_ITEM_RESOLVED,
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

    @Transactional
    public void recordTemplateExported(
            UUID templateId,
            String groupCode,
            String externalId,
            String actorUsername,
            String actorSummary
    ) {
        writer.persist(
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
                writer.truncate("Template exported: " + externalId),
                writer.emptyJsonArray()
        );
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
        writer.persist(
                TEMPLATE_IMPORTED,
                templateId,
                groupCode,
                null,
                null,
                null,
                writer.emptyJsonArray(),
                false,
                null,
                actorUsername,
                actorSummary,
                null,
                writer.truncate("Template imported: " + externalId + " batch=" + importBatchId + " dev=" + developmentVersion),
                writer.emptyJsonArray()
        );
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
        writer.persistContentModule(
                CONTENT_MODULE_LIFECYCLE_OPERATION,
                moduleId,
                groupCode,
                operation + " on " + moduleCode + "@" + semanticVersion + " -> " + lifecycleState,
                actorUsername,
                actorSummary,
                writer.writeContentModuleLifecyclePayload(impactDetail)
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

    @Transactional
    public void recordEscalationDenied(
            String reasonCode,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        writer.persistIdentity(
                IDENTITY_ESCALATION_DENIED,
                null,
                actorUsername,
                actorSummary,
                reasonCode + ": " + statusSummary
        );
    }
}
