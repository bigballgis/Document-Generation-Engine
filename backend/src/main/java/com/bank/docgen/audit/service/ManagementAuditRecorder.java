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
