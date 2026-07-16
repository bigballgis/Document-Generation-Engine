package com.bank.docgen.audit.service;

import static com.bank.docgen.audit.service.ManagementAuditEventTypes.ASSET_LIBRARY_DISABLE;
import static com.bank.docgen.audit.service.ManagementAuditEventTypes.ASSET_LIBRARY_REUPLOAD;
import static com.bank.docgen.audit.service.ManagementAuditEventTypes.ASSET_LIBRARY_UPLOAD;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class AssetLibraryAuditRecorder {

    private final ManagementAuditEventWriter eventWriter;

    AssetLibraryAuditRecorder(ManagementAuditEventWriter eventWriter) {
        this.eventWriter = eventWriter;
    }

    @Transactional
    void recordUpload(
            String assetKey,
            String assetClass,
            String actorUsername,
            String actorSummary,
            String contentSha256
    ) {
        persist(ASSET_LIBRARY_UPLOAD, assetKey, assetClass, actorUsername, actorSummary, contentSha256);
    }

    @Transactional
    void recordDisable(
            String assetKey,
            String assetClass,
            String actorUsername,
            String actorSummary,
            String contentSha256
    ) {
        persist(ASSET_LIBRARY_DISABLE, assetKey, assetClass, actorUsername, actorSummary, contentSha256);
    }

    @Transactional
    void recordReupload(
            String assetKey,
            String assetClass,
            String actorUsername,
            String actorSummary,
            String contentSha256
    ) {
        persist(ASSET_LIBRARY_REUPLOAD, assetKey, assetClass, actorUsername, actorSummary, contentSha256);
    }

    private void persist(
            String eventType,
            String assetKey,
            String assetClass,
            String actorUsername,
            String actorSummary,
            String contentSha256
    ) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("assetKey", assetKey);
        detail.put("assetClass", assetClass);
        detail.put("contentSha256", contentSha256);
        eventWriter.persist(
                eventType,
                null,
                null,
                null,
                null,
                null,
                eventWriter.writeJsonMap(detail),
                false,
                null,
                actorUsername,
                actorSummary,
                null,
                eventWriter.truncate(eventType + " " + assetKey + " " + assetClass),
                eventWriter.writeJson(List.of())
        );
    }
}
