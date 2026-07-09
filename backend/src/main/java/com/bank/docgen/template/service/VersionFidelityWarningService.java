package com.bank.docgen.template.service;

import com.bank.docgen.authoring.structured.FidelityValidationService;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Publish-time snapshot and runtime resolution of fidelity warning codes (CORE-FORTRESS F2-B1).
 */
@Service
public class VersionFidelityWarningService {

    private final FidelityValidationService fidelityValidationService;
    private final ObjectMapper objectMapper;

    public VersionFidelityWarningService(
            FidelityValidationService fidelityValidationService,
            ObjectMapper objectMapper
    ) {
        this.fidelityValidationService = fidelityValidationService;
        this.objectMapper = objectMapper;
    }

    public void snapshotOnPublish(TemplateVersionEntity version, UUID masterId) {
        List<String> codes = fidelityValidationService.collectWarningCodesForVersion(version.getId(), masterId);
        version.setFidelityWarningCodesJson(writeCodes(codes));
    }

    public List<String> resolveWarningCodes(TemplateVersionEntity version, UUID masterId) {
        if (hasCachedCodes(version) && isImmutableLifecycle(version.getLifecycleStatus())) {
            return readCodes(version.getFidelityWarningCodesJson());
        }
        return fidelityValidationService.collectWarningCodesForVersion(version.getId(), masterId);
    }

    private static boolean hasCachedCodes(TemplateVersionEntity version) {
        String json = version.getFidelityWarningCodesJson();
        return json != null && !json.isBlank();
    }

    private static boolean isImmutableLifecycle(TemplateLifecycleStatus status) {
        return status == TemplateLifecycleStatus.PUBLISHED
                || status == TemplateLifecycleStatus.STOPPED
                || status == TemplateLifecycleStatus.DEPRECATED;
    }

    private String writeCodes(List<String> codes) {
        try {
            return objectMapper.writeValueAsString(codes);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize fidelity warning codes", ex);
        }
    }

    private List<String> readCodes(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to deserialize fidelity warning codes", ex);
        }
    }
}
