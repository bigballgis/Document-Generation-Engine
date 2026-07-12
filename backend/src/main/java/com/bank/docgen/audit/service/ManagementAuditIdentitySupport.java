package com.bank.docgen.audit.service;

/**
 * Package-private identity / group / escalation management-audit record helpers.
 */
final class ManagementAuditIdentitySupport {

    private final ManagementAuditEventWriter writer;

    ManagementAuditIdentitySupport(ManagementAuditEventWriter writer) {
        this.writer = writer;
    }

    void recordUserEvent(
            String eventType,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        writer.persistIdentity(eventType, null, actorUsername, actorSummary, statusSummary);
    }

    void recordGroupEvent(
            String eventType,
            String groupCode,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        writer.persistIdentity(eventType, groupCode, actorUsername, actorSummary, statusSummary);
    }

    void recordRiskPromptConfigUpdated(
            String scopeType,
            String groupCode,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        writer.persistIdentity(
                ManagementAuditRecorder.RISK_PROMPT_CONFIG_UPDATED,
                groupCode,
                actorUsername,
                actorSummary,
                scopeType + ": " + statusSummary
        );
    }

    void recordEscalationDenied(
            String reasonCode,
            String actorUsername,
            String actorSummary,
            String statusSummary
    ) {
        writer.persistIdentity(
                ManagementAuditRecorder.IDENTITY_ESCALATION_DENIED,
                null,
                actorUsername,
                actorSummary,
                reasonCode + ": " + statusSummary
        );
    }
}
