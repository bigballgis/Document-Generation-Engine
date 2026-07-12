package com.bank.docgen.audit.service;

import com.bank.docgen.audit.api.PolicyUpdateAuditDetail;
import java.util.List;
import java.util.UUID;

/**
 * Package-private API policy / credential management-audit record helpers.
 */
final class ManagementAuditApiPolicySupport {

    private final ManagementAuditEventWriter writer;

    ManagementAuditApiPolicySupport(ManagementAuditEventWriter writer) {
        this.writer = writer;
    }

    void recordPolicyUpdated(
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
                ManagementAuditRecorder.API_POLICY_UPDATED,
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

    void recordCredentialCreated(
            UUID templateId,
            String groupCode,
            UUID credentialId,
            String credentialExternalId,
            String actorUsername,
            String actorSummary
    ) {
        writer.persistCredential(
                ManagementAuditRecorder.API_CREDENTIAL_CREATED,
                templateId,
                groupCode,
                credentialId,
                credentialExternalId,
                actorUsername,
                actorSummary,
                "Credential created"
        );
    }

    void recordCredentialRotated(
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
                ManagementAuditRecorder.API_CREDENTIAL_ROTATED,
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

    void recordCredentialRevoked(
            UUID templateId,
            String groupCode,
            UUID credentialId,
            String credentialExternalId,
            String actorUsername,
            String actorSummary
    ) {
        writer.persistCredential(
                ManagementAuditRecorder.API_CREDENTIAL_REVOKED,
                templateId,
                groupCode,
                credentialId,
                credentialExternalId,
                actorUsername,
                actorSummary,
                "Credential revoked"
        );
    }
}
