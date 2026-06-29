package com.bank.docgen.template.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "template_content_module_reference",
        uniqueConstraints = @UniqueConstraint(columnNames = {"template_version_id", "reference_key"})
)
public class TemplateContentModuleReferenceEntity {

    @Id
    private UUID id;

    @Column(name = "template_version_id", nullable = false)
    private UUID templateVersionId;

    @Column(name = "reference_key", nullable = false, length = 128)
    private String referenceKey;

    @Column(name = "content_module_version_id", nullable = false)
    private UUID contentModuleVersionId;

    @Column(name = "locked_flag", nullable = false)
    private boolean lockedFlag;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TemplateContentModuleReferenceEntity() {
    }

    public TemplateContentModuleReferenceEntity(
            UUID id,
            UUID templateVersionId,
            String referenceKey,
            UUID contentModuleVersionId
    ) {
        this.id = id;
        this.templateVersionId = templateVersionId;
        this.referenceKey = referenceKey;
        this.contentModuleVersionId = contentModuleVersionId;
        this.lockedFlag = false;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTemplateVersionId() {
        return templateVersionId;
    }

    public String getReferenceKey() {
        return referenceKey;
    }

    public UUID getContentModuleVersionId() {
        return contentModuleVersionId;
    }

    public boolean isLockedFlag() {
        return lockedFlag;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateContentModuleVersion(UUID contentModuleVersionId) {
        this.contentModuleVersionId = contentModuleVersionId;
        this.updatedAt = Instant.now();
    }

    public void lock() {
        this.lockedFlag = true;
        this.updatedAt = Instant.now();
    }
}
