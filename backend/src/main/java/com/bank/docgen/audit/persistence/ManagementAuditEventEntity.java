package com.bank.docgen.audit.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "management_audit_event")
public class ManagementAuditEventEntity {

    @Id
    private UUID id;

    @Column(name = "event_at", nullable = false)
    private Instant eventAt;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "group_code", length = 64)
    private String groupCode;

    @Column(name = "credential_id")
    private UUID credentialId;

    @Column(name = "previous_policy_version")
    private Integer previousPolicyVersion;

    @Column(name = "policy_version")
    private Integer policyVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changed_areas", nullable = false)
    private String changedAreasJson;

    @Column(nullable = false)
    private boolean rollback;

    @Column(name = "rollback_source_policy_version")
    private Integer rollbackSourcePolicyVersion;

    @Column(name = "actor_username", nullable = false, length = 8)
    private String actorUsername;

    @Column(name = "actor_summary", length = 256)
    private String actorSummary;

    @Column(name = "credential_fingerprint", length = 64)
    private String credentialFingerprint;

    @Column(name = "status_summary", length = 512)
    private String statusSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "warning_codes", nullable = false)
    private String warningCodesJson;

    protected ManagementAuditEventEntity() {
    }

    public ManagementAuditEventEntity(
            UUID id,
            Instant eventAt,
            String eventType,
            UUID templateId,
            String groupCode,
            UUID credentialId,
            Integer previousPolicyVersion,
            Integer policyVersion,
            String changedAreasJson,
            boolean rollback,
            Integer rollbackSourcePolicyVersion,
            String actorUsername,
            String actorSummary,
            String credentialFingerprint,
            String statusSummary,
            String warningCodesJson
    ) {
        this.id = id;
        this.eventAt = eventAt;
        this.eventType = eventType;
        this.templateId = templateId;
        this.groupCode = groupCode;
        this.credentialId = credentialId;
        this.previousPolicyVersion = previousPolicyVersion;
        this.policyVersion = policyVersion;
        this.changedAreasJson = changedAreasJson;
        this.rollback = rollback;
        this.rollbackSourcePolicyVersion = rollbackSourcePolicyVersion;
        this.actorUsername = actorUsername;
        this.actorSummary = actorSummary;
        this.credentialFingerprint = credentialFingerprint;
        this.statusSummary = statusSummary;
        this.warningCodesJson = warningCodesJson;
    }

    public UUID getId() {
        return id;
    }

    public Instant getEventAt() {
        return eventAt;
    }

    public String getEventType() {
        return eventType;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public UUID getCredentialId() {
        return credentialId;
    }

    public Integer getPreviousPolicyVersion() {
        return previousPolicyVersion;
    }

    public Integer getPolicyVersion() {
        return policyVersion;
    }

    public String getChangedAreasJson() {
        return changedAreasJson;
    }

    public boolean isRollback() {
        return rollback;
    }

    public Integer getRollbackSourcePolicyVersion() {
        return rollbackSourcePolicyVersion;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public String getActorSummary() {
        return actorSummary;
    }

    public String getCredentialFingerprint() {
        return credentialFingerprint;
    }

    public String getStatusSummary() {
        return statusSummary;
    }

    public String getWarningCodesJson() {
        return warningCodesJson;
    }
}
