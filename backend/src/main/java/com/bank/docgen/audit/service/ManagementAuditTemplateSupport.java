package com.bank.docgen.audit.service;

import java.util.UUID;

/**
 * Package-private template import/export management-audit record helpers.
 */
final class ManagementAuditTemplateSupport {

    private final ManagementAuditEventWriter writer;

    ManagementAuditTemplateSupport(ManagementAuditEventWriter writer) {
        this.writer = writer;
    }

    void recordTemplateExported(
            UUID templateId,
            String groupCode,
            String externalId,
            String actorUsername,
            String actorSummary
    ) {
        writer.persist(
                ManagementAuditRecorder.TEMPLATE_EXPORTED,
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
                writer.truncate("Template exported: " + externalId),
                writer.emptyJsonArray()
        );
    }

    void recordTemplateImported(
            UUID templateId,
            String groupCode,
            String externalId,
            String importBatchId,
            int developmentVersion,
            String actorUsername,
            String actorSummary
    ) {
        writer.persist(
                ManagementAuditRecorder.TEMPLATE_IMPORTED,
                templateId,
                groupCode,
                null,
                null,
                null,
                writer.emptyJsonArray(),
                false,
                null,
                actorUsername,
                actorSummary,
                null,
                writer.truncate("Template imported: " + externalId + " batch=" + importBatchId + " dev=" + developmentVersion),
                writer.emptyJsonArray()
        );
    }
}
