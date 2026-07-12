package com.bank.docgen.audit.service;

import com.bank.docgen.audit.api.ContentModuleLifecycleAuditDetail;
import com.bank.docgen.audit.api.PolicyUpdateAuditDetail;
import com.bank.docgen.audit.persistence.ManagementAuditEventEntity;
import com.bank.docgen.audit.persistence.ManagementAuditEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Package-private write helper for management audit entity construction and JSON payloads.
 */
final class ManagementAuditEventWriter {

    private static final int STATUS_SUMMARY_MAX = 512;

    private final ManagementAuditEventRepository repository;
    private final ObjectMapper objectMapper;

    ManagementAuditEventWriter(ManagementAuditEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    void persist(
            String eventType,
            UUID templateId,
            String groupCode,
            UUID credentialId,
            Integer previousPolicyVersion,
            Integer policyVersion,
            String changedAreasJson,
            boolean rollback,
            Integer rollbackSourcePolicyVersion,
            String actorUsername,
            String actorSummary,
            String credentialFingerprint,
            String statusSummary,
            String warningCodesJson
    ) {
        repository.save(new ManagementAuditEventEntity(
                UUID.randomUUID(),
                Instant.now(),
                eventType,
                templateId,
                groupCode,
                credentialId,
                previousPolicyVersion,
                policyVersion,
                changedAreasJson,
                rollback,
                rollbackSourcePolicyVersion,
                actorUsername,
                actorSummary,
                credentialFingerprint,
                statusSummary,
                warningCodesJson
        ));
    }

    void persistIdentity(
            String eventType,
            String groupCode,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        persist(
                eventType,
                null,
                groupCode,
                null,
                null,
                null,
                emptyJsonArray(),
                false,
                null,
                actorUsername,
                actorSummary,
                null,
                truncate(statusSummary),
                emptyJsonArray()
        );
    }

    void persistContentModule(
            String eventType,
            UUID moduleId,
            String groupCode,
            String statusSummary,
            String actorUsername,
            String actorSummary,
            String auditPayloadJson
    ) {
        persist(
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
        );
    }

    void persistCredential(
            String eventType,
            UUID templateId,
            String groupCode,
            UUID credentialId,
            String credentialExternalId,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        persist(
                eventType,
                templateId,
                groupCode,
                credentialId,
                null,
                null,
                emptyJsonArray(),
                false,
                null,
                actorUsername,
                actorSummary,
                fingerprint(credentialExternalId),
                statusSummary,
                emptyJsonArray()
        );
    }

    String buildPolicyStatusSummary(List<String> changedAreas, PolicyUpdateAuditDetail detail) {
        if (detail.configDiffSummary().isEmpty()) {
            return "Policy updated: " + String.join(", ", changedAreas);
        }
        return "Policy updated: " + String.join("; ", detail.configDiffSummary());
    }

    String writePolicyPayload(PolicyUpdateAuditDetail detail) {
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

    String writeContentModuleLifecyclePayload(ContentModuleLifecycleAuditDetail detail) {
        if (detail == null) {
            return emptyJsonArray();
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
            return emptyJsonArray();
        }
    }

    String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= STATUS_SUMMARY_MAX ? value : value.substring(0, STATUS_SUMMARY_MAX);
    }

    String fingerprint(String externalId) {
        return externalId == null ? null : "fp-" + externalId;
    }

    String emptyJsonArray() {
        return writeJson(List.of());
    }

    String writeJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }
}
