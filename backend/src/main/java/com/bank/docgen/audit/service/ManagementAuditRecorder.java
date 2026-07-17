package com.bank.docgen.audit.service;

import com.bank.docgen.audit.api.ContentModuleLifecycleAuditDetail;
import com.bank.docgen.audit.api.PolicyUpdateAuditDetail;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemTriggerType;
import com.bank.docgen.legalhold.persistence.LegalHoldEntity;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Facade for management-plane audit recording. Domain logic lives in package-private recorders;
 * event type constants are in {@link ManagementAuditEventTypes}.
 */
@Service
public class ManagementAuditRecorder {

    private final ApiPolicyAuditRecorder apiPolicyAuditRecorder;
    private final IdentityAuditRecorder identityAuditRecorder;
    private final CollaborationAuditRecorder collaborationAuditRecorder;
    private final ContentModuleAuditRecorder contentModuleAuditRecorder;
    private final TemplateTransferAuditRecorder templateTransferAuditRecorder;
    private final TestDataSetAuditRecorder testDataSetAuditRecorder;
    private final InvocationRegenerationAuditRecorder invocationRegenerationAuditRecorder;
    private final AssetLibraryAuditRecorder assetLibraryAuditRecorder;
    private final LegalHoldAuditRecorder legalHoldAuditRecorder;

    public ManagementAuditRecorder(
            ApiPolicyAuditRecorder apiPolicyAuditRecorder,
            IdentityAuditRecorder identityAuditRecorder,
            CollaborationAuditRecorder collaborationAuditRecorder,
            ContentModuleAuditRecorder contentModuleAuditRecorder,
            TemplateTransferAuditRecorder templateTransferAuditRecorder,
            TestDataSetAuditRecorder testDataSetAuditRecorder,
            InvocationRegenerationAuditRecorder invocationRegenerationAuditRecorder,
            AssetLibraryAuditRecorder assetLibraryAuditRecorder,
            LegalHoldAuditRecorder legalHoldAuditRecorder
    ) {
        this.apiPolicyAuditRecorder = apiPolicyAuditRecorder;
        this.identityAuditRecorder = identityAuditRecorder;
        this.collaborationAuditRecorder = collaborationAuditRecorder;
        this.contentModuleAuditRecorder = contentModuleAuditRecorder;
        this.templateTransferAuditRecorder = templateTransferAuditRecorder;
        this.testDataSetAuditRecorder = testDataSetAuditRecorder;
        this.invocationRegenerationAuditRecorder = invocationRegenerationAuditRecorder;
        this.assetLibraryAuditRecorder = assetLibraryAuditRecorder;
        this.legalHoldAuditRecorder = legalHoldAuditRecorder;
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
        apiPolicyAuditRecorder.recordPolicyUpdated(
                templateId, groupCode, previousPolicyVersion, policyVersion,
                changedAreas, actorUsername, actorSummary
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
        apiPolicyAuditRecorder.recordPolicyUpdated(
                templateId, groupCode, previousPolicyVersion, policyVersion,
                changedAreas, actorUsername, actorSummary, detail
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
        apiPolicyAuditRecorder.recordCredentialCreated(
                templateId, groupCode, credentialId, credentialExternalId, actorUsername, actorSummary
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
        apiPolicyAuditRecorder.recordCredentialRotated(
                templateId, groupCode, credentialId, credentialExternalId,
                actorUsername, actorSummary, rotationGeneration, previousCredentialFingerprint
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
        apiPolicyAuditRecorder.recordCredentialRevoked(
                templateId, groupCode, credentialId, credentialExternalId, actorUsername, actorSummary
        );
    }

    @Transactional
    public void recordUserEvent(
            String eventType,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        identityAuditRecorder.recordUserEvent(eventType, actorUsername, actorSummary, statusSummary);
    }

    @Transactional
    public void recordGroupEvent(
            String eventType,
            String groupCode,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        identityAuditRecorder.recordGroupEvent(eventType, groupCode, actorUsername, actorSummary, statusSummary);
    }

    @Transactional
    public void recordRiskPromptConfigUpdated(
            String scopeType,
            String groupCode,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        identityAuditRecorder.recordRiskPromptConfigUpdated(
                scopeType, groupCode, actorUsername, actorSummary, statusSummary
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
        identityAuditRecorder.recordCollaborationTimeoutConfigUpdated(
                scopeType, groupCode, actorUsername, actorSummary, statusSummary
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
        collaborationAuditRecorder.recordCollaborationTimeoutEscalation(
                templateId, groupCode, sourceWorkItemId, sourceQueue, statusSummary
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
        collaborationAuditRecorder.recordCollaborationWorkItemCreated(
                templateId, groupCode, workItemId, queue, triggerType, actorUsername, actorSummary
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
        collaborationAuditRecorder.recordCollaborationWorkItemResolved(
                templateId, groupCode, workItemId, queue, actorUsername, actorSummary
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
        templateTransferAuditRecorder.recordTemplateExported(
                templateId, groupCode, externalId, actorUsername, actorSummary
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
        templateTransferAuditRecorder.recordTemplateImported(
                templateId,
                groupCode,
                externalId,
                importBatchId,
                developmentVersion,
                actorUsername,
                actorSummary,
                null,
                null
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
            String actorSummary,
            String bundleFormat,
            Integer materializedClauseCount
    ) {
        templateTransferAuditRecorder.recordTemplateImported(
                templateId,
                groupCode,
                externalId,
                importBatchId,
                developmentVersion,
                actorUsername,
                actorSummary,
                bundleFormat,
                materializedClauseCount
        );
    }

    @Transactional
    public void recordTemplateImportDryRun(
            String groupCode,
            String externalId,
            boolean readyToCommit,
            int blockingCount,
            String bundleFormat,
            String actorUsername,
            String actorSummary
    ) {
        templateTransferAuditRecorder.recordTemplateImportDryRun(
                groupCode,
                externalId,
                readyToCommit,
                blockingCount,
                bundleFormat,
                actorUsername,
                actorSummary
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
        contentModuleAuditRecorder.recordContentModuleCreated(
                moduleId, groupCode, moduleCode, actorUsername, actorSummary
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
        contentModuleAuditRecorder.recordContentModuleVersionCreated(
                moduleId, groupCode, moduleCode, semanticVersion, actorUsername, actorSummary
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
        contentModuleAuditRecorder.recordContentModuleVersionUpdated(
                moduleId, groupCode, moduleCode, semanticVersion, actorUsername, actorSummary
        );
    }

    @Transactional
    public void recordContentModuleSharedGroupCodesUpdated(
            UUID moduleId,
            String groupCode,
            String moduleCode,
            String actorUsername,
            String actorSummary
    ) {
        contentModuleAuditRecorder.recordContentModuleSharedGroupCodesUpdated(
                moduleId, groupCode, moduleCode, actorUsername, actorSummary
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
        recordContentModuleReviewTransition(
                moduleId, groupCode, moduleCode, operation, semanticVersion, reviewState,
                actorUsername, actorSummary, false, null
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
            String actorSummary,
            boolean selfApprovalException,
            String exceptionReason
    ) {
        contentModuleAuditRecorder.recordContentModuleReviewTransition(
                moduleId, groupCode, moduleCode, operation, semanticVersion, reviewState,
                actorUsername, actorSummary, selfApprovalException, exceptionReason
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
        contentModuleAuditRecorder.recordContentModuleLifecycleOperation(
                moduleId, groupCode, moduleCode, operation, semanticVersion, lifecycleState,
                actorUsername, actorSummary, impactDetail
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
        contentModuleAuditRecorder.recordContentModuleLifecycleOperation(
                moduleId, groupCode, moduleCode, operation, semanticVersion, lifecycleState,
                actorUsername, actorSummary
        );
    }

    @Transactional
    public void recordEscalationDenied(
            String reasonCode,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        identityAuditRecorder.recordEscalationDenied(reasonCode, actorUsername, actorSummary, statusSummary);
    }

    @Transactional
    public void recordTestDataPiiExplicitConfirm(
            UUID templateId,
            String groupCode,
            String testDataSetId,
            int datasetVersion,
            String variablesHash,
            List<String> piiFieldKeys,
            Map<String, String> piiCategories,
            String piiConfirmReason,
            String actorUsername,
            String actorSummary
    ) {
        testDataSetAuditRecorder.recordTestDataPiiExplicitConfirm(
                templateId,
                groupCode,
                testDataSetId,
                datasetVersion,
                variablesHash,
                piiFieldKeys,
                piiCategories,
                piiConfirmReason,
                actorUsername,
                actorSummary
        );
    }

    @Transactional
    public void recordInvocationRegenerated(
            com.bank.docgen.apimgmt.api.InvocationRegeneratedAuditDetail detail
    ) {
        invocationRegenerationAuditRecorder.record(detail);
    }

    @Transactional
    public void recordAssetLibraryUpload(
            String assetKey,
            String assetClass,
            String actorUsername,
            String actorSummary,
            String contentSha256
    ) {
        assetLibraryAuditRecorder.recordUpload(
                assetKey, assetClass, actorUsername, actorSummary, contentSha256
        );
    }

    @Transactional
    public void recordAssetLibraryDisable(
            String assetKey,
            String assetClass,
            String actorUsername,
            String actorSummary,
            String contentSha256
    ) {
        assetLibraryAuditRecorder.recordDisable(
                assetKey, assetClass, actorUsername, actorSummary, contentSha256
        );
    }

    @Transactional
    public void recordAssetLibraryReupload(
            String assetKey,
            String assetClass,
            String actorUsername,
            String actorSummary,
            String contentSha256
    ) {
        assetLibraryAuditRecorder.recordReupload(
                assetKey, assetClass, actorUsername, actorSummary, contentSha256
        );
    }

    @Transactional
    public void recordLibraryExport(
            String exportBatchId,
            String scopeSelection,
            int includedCount,
            int skippedCount,
            int failedCount,
            int omittedUnauthorizedOrUnknownCount,
            String actorUsername,
            String actorSummary
    ) {
        templateTransferAuditRecorder.recordLibraryExport(
                exportBatchId,
                scopeSelection,
                includedCount,
                skippedCount,
                failedCount,
                omittedUnauthorizedOrUnknownCount,
                actorUsername,
                actorSummary
        );
    }

    @Transactional
    public void recordLegalHoldCreated(
            LegalHoldEntity hold,
            String actorUsername,
            String actorSummary
    ) {
        legalHoldAuditRecorder.recordCreated(hold, actorUsername, actorSummary);
    }

    @Transactional
    public void recordLegalHoldReleased(
            LegalHoldEntity hold,
            String actorUsername,
            String actorSummary
    ) {
        legalHoldAuditRecorder.recordReleased(hold, actorUsername, actorSummary);
    }
}
