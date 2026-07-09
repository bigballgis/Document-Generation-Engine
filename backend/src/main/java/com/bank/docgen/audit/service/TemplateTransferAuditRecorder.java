package com.bank.docgen.audit.service;

import static com.bank.docgen.audit.service.ManagementAuditEventTypes.TEMPLATE_EXPORTED;
import static com.bank.docgen.audit.service.ManagementAuditEventTypes.TEMPLATE_IMPORTED;

import java.util.List;
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
            String actorSummary
    ) {
        eventWriter.persist(
                TEMPLATE_IMPORTED,
                templateId,
                groupCode,
                null,
                null,
                null,
                eventWriter.writeJson(List.of()),
                false,
                null,
                actorUsername,
                actorSummary,
                null,
                eventWriter.truncate("Template imported: " + externalId + " batch=" + importBatchId + " dev=" + developmentVersion),
                eventWriter.writeJson(List.of())
        );
    }
}
