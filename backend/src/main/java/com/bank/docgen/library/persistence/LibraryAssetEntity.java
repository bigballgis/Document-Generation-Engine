package com.bank.docgen.library.persistence;

import com.bank.docgen.library.domain.AssetLibraryAssetClass;
import com.bank.docgen.library.domain.AssetLibraryAssetStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "library_asset")
@IdClass(LibraryAssetId.class)
public class LibraryAssetEntity {

    @Id
    @Column(name = "group_code", nullable = false, length = 64)
    private String groupCode;

    @Id
    @Column(name = "asset_key", nullable = false, length = 128)
    private String assetKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class", nullable = false, length = 16)
    private AssetLibraryAssetClass assetClass;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AssetLibraryAssetStatus status;

    @Column(name = "content_type", nullable = false, length = 64)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "content_sha256", nullable = false, length = 64)
    private String contentSha256;

    @Column(name = "original_file_name", nullable = false, length = 512)
    private String originalFileName;

    @Column(name = "uploaded_by", nullable = false, length = 128)
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** Set by ALGI-M1 for legacy rows pending/after quarantine; null for new uploads. */
    @Column(name = "migrated_quarantine_at")
    private Instant migratedQuarantineAt;

    /** Set after ALGI-M1 object purge + audit for a quarantined row. */
    @Column(name = "object_purge_completed_at")
    private Instant objectPurgeCompletedAt;

    protected LibraryAssetEntity() {
    }

    public LibraryAssetEntity(
            String groupCode,
            String assetKey,
            AssetLibraryAssetClass assetClass,
            AssetLibraryAssetStatus status,
            String contentType,
            long sizeBytes,
            String contentSha256,
            String originalFileName,
            String uploadedBy,
            Instant uploadedAt
    ) {
        this.groupCode = groupCode;
        this.assetKey = assetKey;
        this.assetClass = assetClass;
        this.status = status;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.contentSha256 = contentSha256;
        this.originalFileName = originalFileName;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = uploadedAt;
        this.updatedAt = uploadedAt;
    }

    public void reactivate(
            AssetLibraryAssetClass assetClass,
            String contentType,
            long sizeBytes,
            String contentSha256,
            String originalFileName,
            String uploadedBy,
            Instant uploadedAt
    ) {
        this.assetClass = assetClass;
        this.status = AssetLibraryAssetStatus.ACTIVE;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.contentSha256 = contentSha256;
        this.originalFileName = originalFileName;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = uploadedAt;
        this.updatedAt = uploadedAt;
        this.deletedAt = null;
        this.migratedQuarantineAt = null;
        this.objectPurgeCompletedAt = null;
    }

    public void markDisabled(Instant at) {
        this.status = AssetLibraryAssetStatus.DISABLED;
        this.updatedAt = at;
    }

    public void markObjectPurgeCompleted(Instant at) {
        this.objectPurgeCompletedAt = at;
        this.updatedAt = at;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public String getAssetKey() {
        return assetKey;
    }

    public AssetLibraryAssetClass getAssetClass() {
        return assetClass;
    }

    public AssetLibraryAssetStatus getStatus() {
        return status;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getContentSha256() {
        return contentSha256;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Instant getMigratedQuarantineAt() {
        return migratedQuarantineAt;
    }

    public Instant getObjectPurgeCompletedAt() {
        return objectPurgeCompletedAt;
    }
}
