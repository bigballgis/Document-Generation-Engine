package com.bank.docgen.contentmodule.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "content_module_nesting_edge",
        uniqueConstraints = @UniqueConstraint(columnNames = {"parent_version_id", "target_module_id"})
)
public class ContentModuleNestingEdgeEntity {

    @Id
    private UUID id;

    @Column(name = "parent_version_id", nullable = false)
    private UUID parentVersionId;

    @Column(name = "target_module_id", nullable = false)
    private UUID targetModuleId;

    @Column(name = "reference_key", nullable = false, length = 128)
    private String referenceKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ContentModuleNestingEdgeEntity() {
    }

    public ContentModuleNestingEdgeEntity(
            UUID id,
            UUID parentVersionId,
            UUID targetModuleId,
            String referenceKey
    ) {
        this.id = id;
        this.parentVersionId = parentVersionId;
        this.targetModuleId = targetModuleId;
        this.referenceKey = referenceKey;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getParentVersionId() {
        return parentVersionId;
    }

    public UUID getTargetModuleId() {
        return targetModuleId;
    }

    public String getReferenceKey() {
        return referenceKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
