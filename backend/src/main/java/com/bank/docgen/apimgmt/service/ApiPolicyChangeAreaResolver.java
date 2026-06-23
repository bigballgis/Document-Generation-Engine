package com.bank.docgen.apimgmt.service;

import com.bank.docgen.apimgmt.api.UpsertApiPolicyRequest;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class ApiPolicyChangeAreaResolver {

    private ApiPolicyChangeAreaResolver() {
    }

    static List<String> detectChangedAreas(
            ApiPolicyEntity existing,
            UpsertApiPolicyRequest request,
            String allowedJson,
            String outputFormatsJson,
            String outputModesJson
    ) {
        List<String> changedAreas = new ArrayList<>();
        if (!Objects.equals(existing.getAllowedAdGroupsJson(), allowedJson)) {
            changedAreas.add("AD_GROUP_AUTHORIZATION");
        }
        if (!Objects.equals(existing.getOutputFormatsJson(), outputFormatsJson)
                || !Objects.equals(existing.getOutputModesJson(), outputModesJson)) {
            changedAreas.add("OUTPUT_POLICY");
        }
        if (existing.isBatchEnabled() != request.batchEnabled() || existing.getMaxBatchSize() != request.maxBatchSize()) {
            changedAreas.add("BATCH_LIMIT");
        }
        if (existing.isDocxEncryptionEnabled() != request.docxEncryptionEnabled()
                || existing.isPdfEncryptionEnabled() != request.pdfEncryptionEnabled()) {
            changedAreas.add("ENCRYPTION_POLICY");
        }
        if (!Objects.equals(existing.getDefaultRouteReleaseVersion(), request.defaultRouteReleaseVersion())) {
            changedAreas.add("DEFAULT_ROUTE_TARGET");
        }
        if (changedAreas.isEmpty()) {
            changedAreas.add("OUTPUT_POLICY");
        }
        return changedAreas;
    }

    static List<String> initialChangedAreas() {
        return List.of(
                "AD_GROUP_AUTHORIZATION",
                "OUTPUT_POLICY",
                "BATCH_LIMIT",
                "DEFAULT_ROUTE_TARGET"
        );
    }
}
