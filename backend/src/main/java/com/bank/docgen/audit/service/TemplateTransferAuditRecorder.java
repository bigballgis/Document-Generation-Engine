package com.bank.docgen.audit.service;

import static com.bank.docgen.audit.service.ManagementAuditEventTypes.TEMPLATE_EXPORTED;
import static com.bank.docgen.audit.service.ManagementAuditEventTypes.TEMPLATE_IMPORTED;
import static com.bank.docgen.audit.service.ManagementAuditEventTypes.TEMPLATE_IMPORT_DRY_RUN;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class TemplateTransferAuditRecorder {

    private final ManagementAuditEventWriter eventWriter;

    TemplateTransferAuditRecorder(ManagementAuditEventWriter eventWriter) {
        this.eventWriter = eventWriter;
    }

    @Transactional
    void recordTemplateExported(
            UUID templateId,
            String groupCode,
            String externalId,
            String actorUsername,
            String actorSummary
    ) {
        eventWriter.persist(
                TEMPLATE_EXPORTED,
                templateId,
                groupCode,
                null,
                null,
                null,
                null,
                false,
                null,
                actorUsername,
                actorSummary,
                null,
                eventWriter.truncate("Template exported: " + externalId),
                eventWriter.writeJson(List.of())
        );
    }

    @Transactional
    void recordTemplateImported(
            UUID templateId,
            String groupCode,
            String externalId,
            String importBatchId,
            int developmentVersion,
            String actorUsername,
            String actorSummary,
            String bundleFormat,
            Integer materializedClauseCount
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("importBatchId", importBatchId);
        details.put("developmentVersion", developmentVersion);
        if (bundleFormat != null) {
            details.put("bundleFormat", bundleFormat);
        }
        if (materializedClauseCount != null) {
            details.put("materializedClauseCount", materializedClauseCount);
        }
        eventWriter.persist(
                TEMPLATE_IMPORTED,
                templateId,
                groupCode,
                null,
                null,
                null,
                eventWriter.writeJsonMap(details),
                false,
                null,
                actorUsername,
                actorSummary,
                null,
                eventWriter.truncate("Template imported: " + externalId + " batch=" + importBatchId + " dev=" + developmentVersion),
                eventWriter.writeJson(List.of())
        );
    }

    @Transactional
    void recordTemplateImportDryRun(
            String groupCode,
            String externalId,
            boolean readyToCommit,
            int blockingCount,
            String bundleFormat,
            String actorUsername,
            String actorSummary
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("readyToCommit", readyToCommit);
        details.put("blockingCount", blockingCount);
        details.put("bundleFormat", bundleFormat);
        details.put("externalId", externalId);
        eventWriter.persist(
                TEMPLATE_IMPORT_DRY_RUN,
                null,
                groupCode,
                null,
                null,
                null,
                eventWriter.writeJsonMap(details),
                false,
                null,
                actorUsername,
                actorSummary,
                null,
                eventWriter.truncate(
                        "Template import dry-run: " + externalId
                                + " ready=" + readyToCommit
                                + " blocking=" + blockingCount
                ),
                eventWriter.writeJson(List.of())
        );
    }
}
