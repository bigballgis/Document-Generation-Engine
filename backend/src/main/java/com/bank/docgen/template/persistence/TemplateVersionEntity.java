package com.bank.docgen.template.persistence;

import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "template_version")
public class TemplateVersionEntity {

    @Id
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "dev_version_number", nullable = false)
    private int devVersionNumber;

    @Column(name = "release_version", length = 32)
    private String releaseVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false, length = 32)
    private TemplateLifecycleStatus lifecycleStatus;

    @Column(name = "master_catalog_version", nullable = false, length = 32)
    private String masterCatalogVersion;

    @Column(name = "rules_json")
    private String rulesJson;

    @Column(name = "created_by", nullable = false, length = 8)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TemplateVersionEntity() {
    }

    public TemplateVersionEntity(UUID id, UUID templateId, String createdBy) {
        this.id = id;
        this.templateId = templateId;
        this.devVersionNumber = 1;
        this.lifecycleStatus = TemplateLifecycleStatus.DRAFT;
        this.masterCatalogVersion = "v1";
        this.createdBy = createdBy;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public int getDevVersionNumber() {
        return devVersionNumber;
    }

    public void setDevVersionNumber(int devVersionNumber) {
        this.devVersionNumber = devVersionNumber;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    public TemplateLifecycleStatus getLifecycleStatus() {
        return lifecycleStatus;
    }

    public String getMasterCatalogVersion() {
        return masterCatalogVersion;
    }

    public String getRulesJson() {
        return rulesJson;
    }

    public void setRulesJson(String rulesJson) {
        this.rulesJson = rulesJson;
        this.updatedAt = Instant.now();
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setReleaseVersion(String releaseVersion) {
        this.releaseVersion = releaseVersion;
    }

    public void setLifecycleStatus(TemplateLifecycleStatus lifecycleStatus) {
        this.lifecycleStatus = lifecycleStatus;
        this.updatedAt = Instant.now();
    }
}
