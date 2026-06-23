package com.bank.docgen.audit.service;

import com.bank.docgen.audit.persistence.ManagementAuditEventEntity;
import com.bank.docgen.audit.persistence.ManagementAuditEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
                false,
                null,
                actorUsername,
                actorSummary,
                null,
                "Policy updated",
                writeJson(List.of())
        ));
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
