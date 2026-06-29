package com.bank.docgen.apimgmt.service;

import com.bank.docgen.apimgmt.api.SaveAdGroupsRequest;
import com.bank.docgen.apimgmt.api.SaveBatchLimitsRequest;
import com.bank.docgen.apimgmt.api.SaveDefaultRouteRequest;
import com.bank.docgen.apimgmt.api.SaveEncryptionPolicyRequest;
import com.bank.docgen.apimgmt.api.SaveOutputPolicyRequest;
import com.bank.docgen.apimgmt.api.UpsertApiPolicyRequest;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

final class ApiPolicyCandidateBuilder {

    private ApiPolicyCandidateBuilder() {
    }

    static UpsertApiPolicyRequest fromEntity(ApiPolicyEntity policy, ObjectMapper objectMapper) {
        return new UpsertApiPolicyRequest(
                readStringList(policy.getAllowedAdGroupsJson(), objectMapper),
                policy.getDefaultRouteReleaseVersion(),
                readStringList(policy.getOutputFormatsJson(), objectMapper),
                readStringList(policy.getOutputModesJson(), objectMapper),
                policy.isBatchEnabled(),
                policy.getBatchSyncMaxItems(),
                policy.isDocxEncryptionEnabled(),
                policy.isPdfEncryptionEnabled()
        );
    }

    static UpsertApiPolicyRequest withAdGroups(
            ApiPolicyEntity policy,
            SaveAdGroupsRequest request,
            ObjectMapper objectMapper
    ) {
        UpsertApiPolicyRequest base = fromEntity(policy, objectMapper);
        return new UpsertApiPolicyRequest(
                request.allowedAdGroups(),
                base.defaultRouteReleaseVersion(),
                base.outputFormats(),
                base.outputModes(),
                base.batchEnabled(),
                base.maxBatchSize(),
                base.docxEncryptionEnabled(),
                base.pdfEncryptionEnabled()
        );
    }

    static UpsertApiPolicyRequest withOutput(
            ApiPolicyEntity policy,
            SaveOutputPolicyRequest request,
            ObjectMapper objectMapper
    ) {
        UpsertApiPolicyRequest base = fromEntity(policy, objectMapper);
        return new UpsertApiPolicyRequest(
                base.allowedAdGroups(),
                base.defaultRouteReleaseVersion(),
                request.outputFormats(),
                request.outputModes(),
                base.batchEnabled(),
                base.maxBatchSize(),
                base.docxEncryptionEnabled(),
                base.pdfEncryptionEnabled()
        );
    }

    static UpsertApiPolicyRequest withBatchLimits(
            ApiPolicyEntity policy,
            SaveBatchLimitsRequest request,
            ObjectMapper objectMapper
    ) {
        UpsertApiPolicyRequest base = fromEntity(policy, objectMapper);
        return new UpsertApiPolicyRequest(
                base.allowedAdGroups(),
                base.defaultRouteReleaseVersion(),
                base.outputFormats(),
                base.outputModes(),
                request.batchEnabled(),
                request.syncMaxItems(),
                base.docxEncryptionEnabled(),
                base.pdfEncryptionEnabled()
        );
    }

    static UpsertApiPolicyRequest withEncryption(
            ApiPolicyEntity policy,
            SaveEncryptionPolicyRequest request,
            ObjectMapper objectMapper
    ) {
        UpsertApiPolicyRequest base = fromEntity(policy, objectMapper);
        return new UpsertApiPolicyRequest(
                base.allowedAdGroups(),
                base.defaultRouteReleaseVersion(),
                base.outputFormats(),
                base.outputModes(),
                base.batchEnabled(),
                base.maxBatchSize(),
                request.docxEncryptionEnabled(),
                request.pdfEncryptionEnabled()
        );
    }

    static UpsertApiPolicyRequest withDefaultRoute(
            ApiPolicyEntity policy,
            SaveDefaultRouteRequest request,
            ObjectMapper objectMapper
    ) {
        UpsertApiPolicyRequest base = fromEntity(policy, objectMapper);
        return new UpsertApiPolicyRequest(
                base.allowedAdGroups(),
                request.defaultRouteReleaseVersion(),
                base.outputFormats(),
                base.outputModes(),
                base.batchEnabled(),
                base.maxBatchSize(),
                base.docxEncryptionEnabled(),
                base.pdfEncryptionEnabled()
        );
    }

    private static List<String> readStringList(String json, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }
}
