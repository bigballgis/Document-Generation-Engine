package com.bank.docgen.apimgmt.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyVersionEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyVersionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ApiPolicyVersionSnapshotService {

    private final ApiPolicyVersionRepository apiPolicyVersionRepository;
    private final ObjectMapper objectMapper;

    public ApiPolicyVersionSnapshotService(
            ApiPolicyVersionRepository apiPolicyVersionRepository,
            ObjectMapper objectMapper
    ) {
        this.apiPolicyVersionRepository = apiPolicyVersionRepository;
        this.objectMapper = objectMapper;
    }

    public void snapshot(ApiPolicyEntity policy, List<String> changedAreas) {
        apiPolicyVersionRepository.save(new ApiPolicyVersionEntity(
                UUID.randomUUID(),
                policy.getTemplateId(),
                policy.getPolicyVersion(),
                writeJson(changedAreas),
                writeConfigSnapshot(policy),
                policy.getUpdatedBy(),
                policy.getUpdatedAt()
        ));
    }

    private String writeConfigSnapshot(ApiPolicyEntity policy) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("allowedAdGroups", readStringList(policy.getAllowedAdGroupsJson()));
        snapshot.put("defaultRouteReleaseVersion", policy.getDefaultRouteReleaseVersion());
        snapshot.put("outputFormats", readStringList(policy.getOutputFormatsJson()));
        snapshot.put("outputModes", readStringList(policy.getOutputModesJson()));
        snapshot.put("batchEnabled", policy.isBatchEnabled());
        snapshot.put("maxBatchSize", policy.getMaxBatchSize());
        snapshot.put("batchSyncMaxItems", policy.getBatchSyncMaxItems());
        snapshot.put("batchAsyncMaxItems", policy.getBatchAsyncMaxItems());
        snapshot.put("docxEncryptionEnabled", policy.isDocxEncryptionEnabled());
        snapshot.put("pdfEncryptionEnabled", policy.isPdfEncryptionEnabled());
        return writeJson(snapshot);
    }

    private List<String> readStringList(String json) {
        try {
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }
}
