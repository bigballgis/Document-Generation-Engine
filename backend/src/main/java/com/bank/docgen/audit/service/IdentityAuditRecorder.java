package com.bank.docgen.audit.service;

import static com.bank.docgen.audit.service.ManagementAuditEventTypes.COLLABORATION_TIMEOUT_CONFIG_UPDATED;
import static com.bank.docgen.audit.service.ManagementAuditEventTypes.IDENTITY_ESCALATION_DENIED;
import static com.bank.docgen.audit.service.ManagementAuditEventTypes.RISK_PROMPT_CONFIG_UPDATED;

import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class IdentityAuditRecorder {

    private final ManagementAuditEventWriter eventWriter;

    IdentityAuditRecorder(ManagementAuditEventWriter eventWriter) {
        this.eventWriter = eventWriter;
    }

    @Transactional
    void recordUserEvent(
            String eventType,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        recordIdentityEvent(eventType, null, actorUsername, actorSummary, statusSummary);
    }

    @Transactional
    void recordGroupEvent(
            String eventType,
            String groupCode,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        recordIdentityEvent(eventType, groupCode, actorUsername, actorSummary, statusSummary);
    }

    @Transactional
    void recordRiskPromptConfigUpdated(
            String scopeType,
            String groupCode,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        recordIdentityEvent(
                RISK_PROMPT_CONFIG_UPDATED,
                groupCode,
                actorUsername,
                actorSummary,
                scopeType + ": " + statusSummary
        );
    }

    @Transactional
    void recordCollaborationTimeoutConfigUpdated(
            String scopeType,
            String groupCode,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        recordIdentityEvent(
                COLLABORATION_TIMEOUT_CONFIG_UPDATED,
                groupCode,
                actorUsername,
                actorSummary,
                scopeType + ": " + statusSummary
        );
    }

    @Transactional
    void recordEscalationDenied(
            String reasonCode,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        recordIdentityEvent(
                IDENTITY_ESCALATION_DENIED,
                null,
                actorUsername,
                actorSummary,
                reasonCode + ": " + statusSummary
        );
    }

    private void recordIdentityEvent(
            String eventType,
            String groupCode,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        eventWriter.persist(
                eventType,
                null,
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
                eventWriter.truncate(statusSummary),
                eventWriter.writeJson(List.of())
        );
    }
}
