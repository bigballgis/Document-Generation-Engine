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

    private final ManagementAuditApiPolicySupport apiPolicy;
    private final ManagementAuditIdentitySupport identity;
    private final ManagementAuditCollaborationSupport collaboration;
    private final ManagementAuditTemplateSupport template;
    private final ManagementAuditContentModuleSupport contentModule;

    public ManagementAuditRecorder(ManagementAuditEventRepository repository, ObjectMapper objectMapper) {
        ManagementAuditEventWriter writer = new ManagementAuditEventWriter(repository, objectMapper);
        this.apiPolicy = new ManagementAuditApiPolicySupport(writer);
        this.identity = new ManagementAuditIdentitySupport(writer);
        this.collaboration = new ManagementAuditCollaborationSupport(writer);
        this.template = new ManagementAuditTemplateSupport(writer);
        this.contentModule = new ManagementAuditContentModuleSupport(writer);
    }

    @Transactional
    public void recordPolicyUpdated(
            UUID templateId, String groupCode, int previousPolicyVersion, int policyVersion,
            List<String> changedAreas, String actorUsername, String actorSummary) {
        recordPolicyUpdated(
                templateId, groupCode, previousPolicyVersion, policyVersion,
                changedAreas, actorUsername, actorSummary, PolicyUpdateAuditDetail.empty());
    }

    @Transactional
    public void recordPolicyUpdated(
            UUID templateId, String groupCode, int previousPolicyVersion, int policyVersion,
            List<String> changedAreas, String actorUsername, String actorSummary, PolicyUpdateAuditDetail detail) {
        apiPolicy.recordPolicyUpdated(
                templateId, groupCode, previousPolicyVersion, policyVersion,
                changedAreas, actorUsername, actorSummary, detail);
    }

    @Transactional
    public void recordCredentialCreated(
            UUID templateId, String groupCode, UUID credentialId, String credentialExternalId,
            String actorUsername, String actorSummary) {
        apiPolicy.recordCredentialCreated(
                templateId, groupCode, credentialId, credentialExternalId, actorUsername, actorSummary);
    }

    @Transactional
    public void recordCredentialRotated(
            UUID templateId, String groupCode, UUID credentialId, String credentialExternalId,
            String actorUsername, String actorSummary, int rotationGeneration, String previousCredentialFingerprint) {
        apiPolicy.recordCredentialRotated(
                templateId, groupCode, credentialId, credentialExternalId,
                actorUsername, actorSummary, rotationGeneration, previousCredentialFingerprint);
    }

    @Transactional
    public void recordCredentialRevoked(
            UUID templateId, String groupCode, UUID credentialId, String credentialExternalId,
            String actorUsername, String actorSummary) {
        apiPolicy.recordCredentialRevoked(
                templateId, groupCode, credentialId, credentialExternalId, actorUsername, actorSummary);
    }

    @Transactional
    public void recordUserEvent(String eventType, String actorUsername, String actorSummary, String statusSummary) {
        identity.recordUserEvent(eventType, actorUsername, actorSummary, statusSummary);
    }

    @Transactional
    public void recordGroupEvent(
            String eventType, String groupCode, String actorUsername, String actorSummary, String statusSummary) {
        identity.recordGroupEvent(eventType, groupCode, actorUsername, actorSummary, statusSummary);
    }

    @Transactional
    public void recordRiskPromptConfigUpdated(
            String scopeType, String groupCode, String actorUsername, String actorSummary, String statusSummary) {
        identity.recordRiskPromptConfigUpdated(scopeType, groupCode, actorUsername, actorSummary, statusSummary);
    }

    @Transactional
    public void recordCollaborationTimeoutConfigUpdated(
            String scopeType, String groupCode, String actorUsername, String actorSummary, String statusSummary) {
        collaboration.recordCollaborationTimeoutConfigUpdated(
                scopeType, groupCode, actorUsername, actorSummary, statusSummary);
    }

    @Transactional
    public void recordCollaborationTimeoutEscalation(
            UUID templateId, String groupCode, UUID sourceWorkItemId,
            CollaborationWorkItemQueue sourceQueue, String statusSummary) {
        collaboration.recordCollaborationTimeoutEscalation(
                templateId, groupCode, sourceWorkItemId, sourceQueue, statusSummary);
    }

    @Transactional
    public void recordCollaborationWorkItemCreated(
            UUID templateId, String groupCode, UUID workItemId, CollaborationWorkItemQueue queue,
            CollaborationWorkItemTriggerType triggerType, String actorUsername, String actorSummary) {
        collaboration.recordCollaborationWorkItemCreated(
                templateId, groupCode, workItemId, queue, triggerType, actorUsername, actorSummary);
    }

    @Transactional
    public void recordCollaborationWorkItemResolved(
            UUID templateId, String groupCode, UUID workItemId, CollaborationWorkItemQueue queue,
            String actorUsername, String actorSummary) {
        collaboration.recordCollaborationWorkItemResolved(
                templateId, groupCode, workItemId, queue, actorUsername, actorSummary);
    }

    @Transactional
    public void recordTemplateExported(
            UUID templateId, String groupCode, String externalId, String actorUsername, String actorSummary) {
        template.recordTemplateExported(templateId, groupCode, externalId, actorUsername, actorSummary);
    }

    @Transactional
    public void recordTemplateImported(
            UUID templateId, String groupCode, String externalId, String importBatchId,
            int developmentVersion, String actorUsername, String actorSummary) {
        template.recordTemplateImported(
                templateId, groupCode, externalId, importBatchId, developmentVersion, actorUsername, actorSummary);
    }

    @Transactional
    public void recordContentModuleCreated(
            UUID moduleId, String groupCode, String moduleCode, String actorUsername, String actorSummary) {
        contentModule.recordContentModuleCreated(moduleId, groupCode, moduleCode, actorUsername, actorSummary);
    }

    @Transactional
    public void recordContentModuleVersionCreated(
            UUID moduleId, String groupCode, String moduleCode, String semanticVersion,
            String actorUsername, String actorSummary) {
        contentModule.recordContentModuleVersionCreated(
                moduleId, groupCode, moduleCode, semanticVersion, actorUsername, actorSummary);
    }

    @Transactional
    public void recordContentModuleVersionUpdated(
            UUID moduleId, String groupCode, String moduleCode, String semanticVersion,
            String actorUsername, String actorSummary) {
        contentModule.recordContentModuleVersionUpdated(
                moduleId, groupCode, moduleCode, semanticVersion, actorUsername, actorSummary);
    }

    @Transactional
    public void recordContentModuleReviewTransition(
            UUID moduleId, String groupCode, String moduleCode, String operation, String semanticVersion,
            String reviewState, String actorUsername, String actorSummary) {
        contentModule.recordContentModuleReviewTransition(
                moduleId, groupCode, moduleCode, operation, semanticVersion, reviewState, actorUsername, actorSummary);
    }

    @Transactional
    public void recordContentModuleLifecycleOperation(
            UUID moduleId, String groupCode, String moduleCode, String operation, String semanticVersion,
            String lifecycleState, String actorUsername, String actorSummary,
            ContentModuleLifecycleAuditDetail impactDetail) {
        contentModule.recordContentModuleLifecycleOperation(
                moduleId, groupCode, moduleCode, operation, semanticVersion,
                lifecycleState, actorUsername, actorSummary, impactDetail);
    }

    @Transactional
    public void recordContentModuleLifecycleOperation(
            UUID moduleId, String groupCode, String moduleCode, String operation, String semanticVersion,
            String lifecycleState, String actorUsername, String actorSummary) {
        recordContentModuleLifecycleOperation(
                moduleId, groupCode, moduleCode, operation, semanticVersion,
                lifecycleState, actorUsername, actorSummary, null);
    }

    @Transactional
    public void recordEscalationDenied(
            String reasonCode, String actorUsername, String actorSummary, String statusSummary) {
        identity.recordEscalationDenied(reasonCode, actorUsername, actorSummary, statusSummary);
    }
}
