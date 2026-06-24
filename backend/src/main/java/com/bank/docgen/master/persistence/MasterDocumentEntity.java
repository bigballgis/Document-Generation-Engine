package com.bank.docgen.master.persistence;

import com.bank.docgen.master.domain.MasterDocumentStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "master_document")
public class MasterDocumentEntity {

    @Id
    private UUID id;

    @Column(name = "group_code", nullable = false, length = 64)
    private String groupCode;

    @Column(nullable = false, length = 256)
    private String name;

    @Column(length = 1024)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MasterDocumentStatus status;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "original_filename", nullable = false, length = 256)
    private String originalFilename;

    @Column(name = "change_summary", length = 2048)
    private String changeSummary;

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

    @OneToMany(mappedBy = "master", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MasterAnchorEntity> anchors = new ArrayList<>();

    protected MasterDocumentEntity() {
    }

    public MasterDocumentEntity(
            UUID id,
            String groupCode,
            String name,
            String description,
            String storageKey,
            String originalFilename,
            String createdBy
    ) {
        this.id = id;
        this.groupCode = groupCode;
        this.name = name;
        this.description = description;
        this.status = MasterDocumentStatus.DRAFT;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
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

    public MasterDocumentStatus getStatus() {
        return status;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getChangeSummary() {
        return changeSummary;
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

    public List<MasterAnchorEntity> getAnchors() {
        return anchors;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(MasterDocumentStatus status) {
        this.status = status;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    public void replaceAnchors(List<MasterAnchorEntity> newAnchors) {
        anchors.clear();
        anchors.addAll(newAnchors);
    }
}
