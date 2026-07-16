package com.bank.docgen.audit.service;

import static com.bank.docgen.audit.service.ManagementAuditEventTypes.LEGAL_HOLD_CREATED;
import static com.bank.docgen.audit.service.ManagementAuditEventTypes.LEGAL_HOLD_RELEASED;

import com.bank.docgen.legalhold.domain.LegalHoldScopeType;
import com.bank.docgen.legalhold.persistence.LegalHoldEntity;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * CE-G04: legal hold create/release audit — no variables / credentials / full request body.
 */
@Component
class LegalHoldAuditRecorder {

    private final ManagementAuditEventWriter eventWriter;

    LegalHoldAuditRecorder(ManagementAuditEventWriter eventWriter) {
        this.eventWriter = eventWriter;
    }

    @Transactional
    void recordCreated(LegalHoldEntity hold, String actorUsername, String actorSummary) {
        persist(LEGAL_HOLD_CREATED, hold, actorUsername, actorSummary);
    }

    @Transactional
    void recordReleased(LegalHoldEntity hold, String actorUsername, String actorSummary) {
        persist(LEGAL_HOLD_RELEASED, hold, actorUsername, actorSummary);
    }

    private void persist(
            String eventType,
            LegalHoldEntity hold,
            String actorUsername,
            String actorSummary
    ) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("holdId", hold.getId().toString());
        detail.put("holdExternalId", hold.getHoldExternalId());
        detail.put("scopeType", hold.getScopeType().name());
        detail.put("status", hold.getStatus().name());
        if (hold.getTemplateId() != null) {
            detail.put("templateId", hold.getTemplateId().toString());
        }
        if (hold.getScopeType() == LegalHoldScopeType.INVOCATION_SET) {
            detail.put("invocationCount", hold.getInvocationExternalIds().size());
        }
        if (hold.getReason() != null) {
            detail.put("reason", hold.getReason());
        }
        detail.put("actorUsername", actorUsername);
        eventWriter.persist(
                eventType,
                hold.getTemplateId(),
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
                eventWriter.truncate(eventType + " " + hold.getHoldExternalId() + " " + hold.getScopeType()),
                eventWriter.writeJson(List.of())
        );
    }
}
