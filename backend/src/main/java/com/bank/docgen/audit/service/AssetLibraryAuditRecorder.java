package com.bank.docgen.audit.service;

import static com.bank.docgen.audit.service.ManagementAuditEventTypes.ASSET_LIBRARY_DISABLE;
import static com.bank.docgen.audit.service.ManagementAuditEventTypes.ASSET_LIBRARY_MIGRATE_QUARANTINE;
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
            String groupCode,
            String assetKey,
            String assetClass,
            String actorUsername,
            String actorSummary,
            String contentSha256
    ) {
        persist(ASSET_LIBRARY_UPLOAD, groupCode, assetKey, assetClass, actorUsername, actorSummary, contentSha256);
    }

    @Transactional
    void recordDisable(
            String groupCode,
            String assetKey,
            String assetClass,
            String actorUsername,
            String actorSummary,
            String contentSha256
    ) {
        persist(ASSET_LIBRARY_DISABLE, groupCode, assetKey, assetClass, actorUsername, actorSummary, contentSha256);
    }

    @Transactional
    void recordReupload(
            String groupCode,
            String assetKey,
            String assetClass,
            String actorUsername,
            String actorSummary,
            String contentSha256
    ) {
        persist(ASSET_LIBRARY_REUPLOAD, groupCode, assetKey, assetClass, actorUsername, actorSummary, contentSha256);
    }

    @Transactional
    void recordMigrateQuarantine(String groupCode, String assetKey, String migrationId) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("assetKey", assetKey);
        detail.put("groupCode", groupCode);
        detail.put("migrationId", migrationId);
        eventWriter.persist(
                ASSET_LIBRARY_MIGRATE_QUARANTINE,
                null,
                groupCode,
                null,
                null,
                null,
                eventWriter.writeJsonMap(detail),
                false,
                null,
                "SYSTEM",
                "SYSTEM",
                null,
                eventWriter.truncate(ASSET_LIBRARY_MIGRATE_QUARANTINE + " " + groupCode + " " + assetKey),
                eventWriter.writeJson(List.of())
        );
    }

    private void persist(
            String eventType,
            String groupCode,
            String assetKey,
            String assetClass,
            String actorUsername,
            String actorSummary,
            String contentSha256
    ) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("groupCode", groupCode);
        detail.put("assetKey", assetKey);
        detail.put("assetClass", assetClass);
        detail.put("contentSha256", contentSha256);
        eventWriter.persist(
                eventType,
                null,
                groupCode,
                null,
                null,
                null,
                eventWriter.writeJsonMap(detail),
                false,
                null,
                actorUsername,
                actorSummary,
                null,
                eventWriter.truncate(eventType + " " + groupCode + " " + assetKey + " " + assetClass),
                eventWriter.writeJson(List.of())
        );
    }
}
