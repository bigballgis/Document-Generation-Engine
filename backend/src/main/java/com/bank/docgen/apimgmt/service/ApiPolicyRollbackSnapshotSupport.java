package com.bank.docgen.apimgmt.service;

import com.bank.docgen.apimgmt.api.UpsertApiPolicyRequest;
import com.bank.docgen.apimgmt.persistence.ApiPolicyVersionEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyVersionRepository;
import com.bank.docgen.template.service.TemplateValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;

/**
 * Package-private snapshot JSON helpers for API policy rollback.
 */
final class ApiPolicyRollbackSnapshotSupport {

    private final ApiPolicyVersionRepository apiPolicyVersionRepository;
    private final ObjectMapper objectMapper;

    ApiPolicyRollbackSnapshotSupport(
            ApiPolicyVersionRepository apiPolicyVersionRepository,
            ObjectMapper objectMapper
    ) {
        this.apiPolicyVersionRepository = apiPolicyVersionRepository;
        this.objectMapper = objectMapper;
    }

    UpsertApiPolicyRequest loadCandidateRequest(UUID templateId, int targetPolicyVersion) {
        JsonNode snapshot = loadSnapshotNode(templateId, targetPolicyVersion);
        return new UpsertApiPolicyRequest(
                readStringList(snapshot, "allowedAdGroups"),
                textOrNull(snapshot, "defaultRouteReleaseVersion"),
                readStringList(snapshot, "outputFormats"),
                readStringList(snapshot, "outputModes"),
                snapshot.path("batchEnabled").asBoolean(false),
                batchSyncMaxItems(snapshot),
                snapshot.path("docxEncryptionEnabled").asBoolean(false),
                snapshot.path("pdfEncryptionEnabled").asBoolean(false)
        );
    }

    JsonNode loadSnapshotNode(UUID templateId, int targetPolicyVersion) {
        ApiPolicyVersionEntity history = apiPolicyVersionRepository
                .findByTemplateIdAndPolicyVersion(templateId, targetPolicyVersion)
                .orElseThrow(() -> new TemplateValidationException("api.error.apimgmt.policyVersionNotFound"));
        try {
            return objectMapper.readTree(history.getConfigSnapshotJson());
        } catch (JsonProcessingException ex) {
            throw new TemplateValidationException("api.error.apimgmt.policyVersionNotFound");
        }
    }

    int batchSyncMaxItems(JsonNode snapshot) {
        if (snapshot.has("batchSyncMaxItems")) {
            return snapshot.path("batchSyncMaxItems").asInt(100);
        }
        return snapshot.path("maxBatchSize").asInt(100);
    }

    int batchAsyncMaxItems(JsonNode snapshot) {
        if (snapshot.has("batchAsyncMaxItems")) {
            return snapshot.path("batchAsyncMaxItems").asInt(10000);
        }
        return snapshot.path("maxBatchSize").asInt(10000);
    }

    String textOrNull(JsonNode snapshot, String field) {
        JsonNode node = snapshot.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }

    List<String> readStringList(JsonNode snapshot, String field) {
        JsonNode node = snapshot.get(field);
        if (node == null || node.isNull()) {
            return List.of();
        }
        try {
            return objectMapper.convertValue(node, new TypeReference<List<String>>() {
            });
        } catch (IllegalArgumentException ex) {
            return List.of();
        }
    }

    String writeJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }
}
