package com.bank.docgen.library.persistence;

import com.bank.docgen.library.domain.AssetLibraryAssetClass;
import com.bank.docgen.library.domain.AssetLibraryAssetStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "library_asset")
public class LibraryAssetEntity {

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

    protected LibraryAssetEntity() {
    }

    public LibraryAssetEntity(
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
    }

    public void markDisabled(Instant at) {
        this.status = AssetLibraryAssetStatus.DISABLED;
        this.updatedAt = at;
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
}
