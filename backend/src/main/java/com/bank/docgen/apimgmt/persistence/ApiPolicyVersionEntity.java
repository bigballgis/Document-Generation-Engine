package com.bank.docgen.apimgmt.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_policy_version")
public class ApiPolicyVersionEntity {

    @Id
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "policy_version", nullable = false)
    private int policyVersion;

    @Column(name = "changed_areas", nullable = false, columnDefinition = "TEXT")
    private String changedAreasJson;

    @Column(name = "config_snapshot", nullable = false, columnDefinition = "TEXT")
    private String configSnapshotJson;

    @Column(name = "updated_by", nullable = false, length = 8)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ApiPolicyVersionEntity() {
    }

    public ApiPolicyVersionEntity(
            UUID id,
            UUID templateId,
            int policyVersion,
            String changedAreasJson,
            String configSnapshotJson,
            String updatedBy,
            Instant updatedAt
    ) {
        this.id = id;
        this.templateId = templateId;
        this.policyVersion = policyVersion;
        this.changedAreasJson = changedAreasJson;
        this.configSnapshotJson = configSnapshotJson;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public int getPolicyVersion() {
        return policyVersion;
    }

    public String getChangedAreasJson() {
        return changedAreasJson;
    }

    public String getConfigSnapshotJson() {
        return configSnapshotJson;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
