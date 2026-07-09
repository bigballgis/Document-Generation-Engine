package com.bank.docgen.audit.service;

import static com.bank.docgen.audit.service.ManagementAuditEventTypes.API_CREDENTIAL_CREATED;
import static com.bank.docgen.audit.service.ManagementAuditEventTypes.API_CREDENTIAL_REVOKED;
import static com.bank.docgen.audit.service.ManagementAuditEventTypes.API_CREDENTIAL_ROTATED;
import static com.bank.docgen.audit.service.ManagementAuditEventTypes.API_POLICY_UPDATED;

import com.bank.docgen.audit.api.PolicyUpdateAuditDetail;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class ApiPolicyAuditRecorder {

    private final ManagementAuditEventWriter eventWriter;

    ApiPolicyAuditRecorder(ManagementAuditEventWriter eventWriter) {
        this.eventWriter = eventWriter;
    }

    @Transactional
    void recordPolicyUpdated(
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
        eventWriter.persist(
                API_POLICY_UPDATED,
                templateId,
                groupCode,
                null,
                previousPolicyVersion,
                policyVersion,
                eventWriter.writeJson(changedAreas),
                rollback,
                detail.rollbackSourcePolicyVersion(),
                actorUsername,
                actorSummary,
                null,
                eventWriter.truncate(buildStatusSummary(changedAreas, detail)),
                eventWriter.writePolicyPayload(detail)
        );
    }

    @Transactional
    void recordCredentialCreated(
            UUID templateId,
            String groupCode,
            UUID credentialId,
            String credentialExternalId,
            String actorUsername,
            String actorSummary
    ) {
        eventWriter.persist(
                API_CREDENTIAL_CREATED,
                templateId,
                groupCode,
                credentialId,
                null,
                null,
                eventWriter.writeJson(List.of()),
                false,
                null,
                actorUsername,
                actorSummary,
                eventWriter.fingerprint(credentialExternalId),
                "Credential created",
                eventWriter.writeJson(List.of())
        );
    }

    @Transactional
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
        eventWriter.persist(
                API_CREDENTIAL_ROTATED,
                templateId,
                groupCode,
                credentialId,
                null,
                null,
                eventWriter.writeJson(List.of()),
                false,
                null,
                actorUsername,
                actorSummary,
                eventWriter.fingerprint(credentialExternalId),
                "Credential rotated; generation=" + rotationGeneration
                        + "; previousFingerprint=" + previousCredentialFingerprint,
                eventWriter.writeJson(List.of())
        );
    }

    @Transactional
    void recordCredentialRevoked(
            UUID templateId,
            String groupCode,
            UUID credentialId,
            String credentialExternalId,
            String actorUsername,
            String actorSummary
    ) {
        eventWriter.persist(
                API_CREDENTIAL_REVOKED,
                templateId,
                groupCode,
                credentialId,
                null,
                null,
                eventWriter.writeJson(List.of()),
                false,
                null,
                actorUsername,
                actorSummary,
                eventWriter.fingerprint(credentialExternalId),
                "Credential revoked",
                eventWriter.writeJson(List.of())
        );
    }

    private String buildStatusSummary(List<String> changedAreas, PolicyUpdateAuditDetail detail) {
        if (detail.configDiffSummary().isEmpty()) {
            return "Policy updated: " + String.join(", ", changedAreas);
        }
        return "Policy updated: " + String.join("; ", detail.configDiffSummary());
    }
}
