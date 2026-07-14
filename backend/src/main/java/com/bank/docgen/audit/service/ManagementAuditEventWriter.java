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
import org.springframework.stereotype.Component;

/**
 * Shared persistence and JSON helpers for {@link ManagementAuditRecorder}.
 */
@Component
class ManagementAuditEventWriter {

    static final int STATUS_SUMMARY_MAX = 512;

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

    String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= STATUS_SUMMARY_MAX ? value : value.substring(0, STATUS_SUMMARY_MAX);
    }

    String fingerprint(String externalId) {
        return externalId == null ? null : "fp-" + externalId;
    }

    String writeJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    String writeJsonMap(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
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
}
