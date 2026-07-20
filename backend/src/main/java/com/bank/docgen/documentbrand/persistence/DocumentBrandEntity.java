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
@Table(name = "document_brand")
public class DocumentBrandEntity {

    @Id
    private UUID id;

    @Column(name = "group_code", nullable = false, length = 64, updatable = false)
    private String groupCode;

    @Column(name = "document_brand_code", nullable = false, length = 64, updatable = false)
    private String documentBrandCode;

    @Column(name = "display_name", nullable = false, length = 256)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DocumentBrandStatus status;

    @Column(name = "logo_object_ref", nullable = false, length = 256)
    private String logoObjectRef;

    @Column(name = "default_seal_object_ref", length = 256)
    private String defaultSealObjectRef;

    @Column(name = "letterhead_legal_name", length = 256)
    private String letterheadLegalName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected DocumentBrandEntity() {
    }

    public DocumentBrandEntity(
            UUID id,
            String groupCode,
            String documentBrandCode,
            String displayName,
            DocumentBrandStatus status,
            String logoObjectRef,
            String defaultSealObjectRef,
            String letterheadLegalName
    ) {
        this.id = id;
        this.groupCode = groupCode;
        this.documentBrandCode = documentBrandCode;
        this.displayName = displayName;
        this.status = status == null ? DocumentBrandStatus.ACTIVE : status;
        this.logoObjectRef = logoObjectRef;
        this.defaultSealObjectRef = defaultSealObjectRef;
        this.letterheadLegalName = letterheadLegalName;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(
            String displayName,
            DocumentBrandStatus status,
            String logoObjectRef,
            String defaultSealObjectRef,
            String letterheadLegalName
    ) {
        if (displayName != null) {
            this.displayName = displayName;
        }
        if (status != null) {
            this.status = status;
        }
        if (logoObjectRef != null) {
            this.logoObjectRef = logoObjectRef;
        }
        this.defaultSealObjectRef = defaultSealObjectRef;
        this.letterheadLegalName = letterheadLegalName;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public String getDocumentBrandCode() {
        return documentBrandCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public DocumentBrandStatus getStatus() {
        return status;
    }

    public String getLogoObjectRef() {
        return logoObjectRef;
    }

    public String getDefaultSealObjectRef() {
        return defaultSealObjectRef;
    }

    public String getLetterheadLegalName() {
        return letterheadLegalName;
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
