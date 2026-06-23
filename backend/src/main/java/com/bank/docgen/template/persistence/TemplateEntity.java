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
@Table(name = "template")
public class TemplateEntity {

    @Id
    private UUID id;

    @Column(name = "external_id", nullable = false, length = 128, unique = true)
    private String externalId;

    @Column(name = "group_code", nullable = false, length = 64)
    private String groupCode;

    @Column(nullable = false, length = 256)
    private String name;

    @Column(length = 1024)
    private String description;

    @Column(name = "master_id", nullable = false)
    private UUID masterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false, length = 32)
    private TemplateLifecycleStatus lifecycleStatus;

    @Column(name = "release_version", length = 32)
    private String releaseVersion;

    @Column(name = "created_by", nullable = false, length = 8)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 8)
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected TemplateEntity() {
    }

    public TemplateEntity(
            UUID id,
            String externalId,
            String groupCode,
            String name,
            String description,
            UUID masterId,
            String createdBy
    ) {
        this.id = id;
        this.externalId = externalId;
        this.groupCode = groupCode;
        this.name = name;
        this.description = description;
        this.masterId = masterId;
        this.lifecycleStatus = TemplateLifecycleStatus.DRAFT;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public UUID getMasterId() {
        return masterId;
    }

    public TemplateLifecycleStatus getLifecycleStatus() {
        return lifecycleStatus;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLifecycleStatus(TemplateLifecycleStatus lifecycleStatus) {
        this.lifecycleStatus = lifecycleStatus;
    }

    public void setReleaseVersion(String releaseVersion) {
        this.releaseVersion = releaseVersion;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
