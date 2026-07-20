package com.bank.docgen.documentbrand.persistence;

import com.bank.docgen.documentbrand.domain.DocumentBrandStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "legal_entity")
public class LegalEntityEntity {

    @Id
    private UUID id;

    @Column(name = "group_code", nullable = false, length = 64, updatable = false)
    private String groupCode;

    @Column(name = "legal_entity_code", nullable = false, length = 64, updatable = false)
    private String legalEntityCode;

    @Column(name = "display_name", nullable = false, length = 256)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DocumentBrandStatus status;

    @Column(name = "document_brand_code", nullable = false, length = 64)
    private String documentBrandCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected LegalEntityEntity() {
    }

    public LegalEntityEntity(
            UUID id,
            String groupCode,
            String legalEntityCode,
            String displayName,
            DocumentBrandStatus status,
            String documentBrandCode
    ) {
        this.id = id;
        this.groupCode = groupCode;
        this.legalEntityCode = legalEntityCode;
        this.displayName = displayName;
        this.status = status == null ? DocumentBrandStatus.ACTIVE : status;
        this.documentBrandCode = documentBrandCode;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String displayName, DocumentBrandStatus status, String documentBrandCode) {
        if (displayName != null) {
            this.displayName = displayName;
        }
        if (status != null) {
            this.status = status;
        }
        if (documentBrandCode != null) {
            this.documentBrandCode = documentBrandCode;
        }
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public String getLegalEntityCode() {
        return legalEntityCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public DocumentBrandStatus getStatus() {
        return status;
    }

    public String getDocumentBrandCode() {
        return documentBrandCode;
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
}
