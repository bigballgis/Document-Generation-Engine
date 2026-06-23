package com.bank.docgen.template.persistence;

import com.bank.docgen.template.domain.AnchorContentType;
import com.bank.docgen.template.domain.BindingValidationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "anchor_binding")
public class AnchorBindingEntity {

    @Id
    private UUID id;

    @Column(name = "template_version_id", nullable = false)
    private UUID templateVersionId;

    @Column(name = "anchor_id", nullable = false, length = 128)
    private String anchorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "declared_content_type", nullable = false, length = 64)
    private AnchorContentType declaredContentType;

    @Column(name = "structured_content_json", nullable = false)
    private String structuredContentJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 64)
    private BindingValidationStatus validationStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AnchorBindingEntity() {
    }

    public AnchorBindingEntity(
            UUID id,
            UUID templateVersionId,
            String anchorId,
            AnchorContentType declaredContentType,
            String structuredContentJson,
            BindingValidationStatus validationStatus
    ) {
        this.id = id;
        this.templateVersionId = templateVersionId;
        this.anchorId = anchorId;
        this.declaredContentType = declaredContentType;
        this.structuredContentJson = structuredContentJson;
        this.validationStatus = validationStatus;
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

    public String getAnchorId() {
        return anchorId;
    }

    public AnchorContentType getDeclaredContentType() {
        return declaredContentType;
    }

    public String getStructuredContentJson() {
        return structuredContentJson;
    }

    public BindingValidationStatus getValidationStatus() {
        return validationStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(
            AnchorContentType declaredContentType,
            String structuredContentJson,
            BindingValidationStatus validationStatus
    ) {
        this.declaredContentType = declaredContentType;
        this.structuredContentJson = structuredContentJson;
        this.validationStatus = validationStatus;
        this.updatedAt = Instant.now();
    }
}
