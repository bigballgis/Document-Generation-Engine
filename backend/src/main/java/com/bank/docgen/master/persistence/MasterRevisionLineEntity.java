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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "master_revision_line")
public class MasterRevisionLineEntity {

    @Id
    private UUID id;

    @Column(name = "master_id", nullable = false)
    private UUID masterId;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "original_filename", nullable = false, length = 256)
    private String originalFilename;

    @Column(name = "anchor_count", nullable = false)
    private int anchorCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_snapshot", nullable = false, length = 32)
    private MasterDocumentStatus statusSnapshot;

    @Column(name = "revision_sequence", nullable = false)
    private int revisionSequence;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    @Column(name = "change_summary", length = 2048)
    private String changeSummary;

    /**
     * CE-K02: JSON-serialized {@link com.bank.docgen.sharedkernel.document.style.MasterStyleCatalog}
     * for this revision (styles.xml + theme + docDefaults).
     */
    @Column(name = "style_catalog_json", columnDefinition = "TEXT")
    private String styleCatalogJson;

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

    @OneToMany(mappedBy = "revisionLine", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("documentSequence ASC")
    private List<MasterRevisionLineAnchorEntity> anchors = new ArrayList<>();

    protected MasterRevisionLineEntity() {
    }

    public MasterRevisionLineEntity(
            UUID id,
            UUID masterId,
            String storageKey,
            String originalFilename,
            int anchorCount,
            MasterDocumentStatus statusSnapshot,
            int revisionSequence,
            boolean current,
            String changeSummary,
            String actor
    ) {
        this.id = id;
        this.masterId = masterId;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.anchorCount = anchorCount;
        this.statusSnapshot = statusSnapshot;
        this.revisionSequence = revisionSequence;
        this.current = current;
        this.changeSummary = changeSummary;
        this.createdBy = actor;
        this.updatedBy = actor;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMasterId() {
        return masterId;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public int getAnchorCount() {
        return anchorCount;
    }

    public MasterDocumentStatus getStatusSnapshot() {
        return statusSnapshot;
    }

    public int getRevisionSequence() {
        return revisionSequence;
    }

    public boolean isCurrent() {
        return current;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public String getStyleCatalogJson() {
        return styleCatalogJson;
    }

    public void setStyleCatalogJson(String styleCatalogJson) {
        this.styleCatalogJson = styleCatalogJson;
        this.updatedAt = Instant.now();
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

    public List<MasterRevisionLineAnchorEntity> getAnchors() {
        return List.copyOf(anchors);
    }

    /**
     * CE-U06: mutate managed snapshot anchor displayLabel (documentSequence / anchorId unchanged).
     *
     * @return true when the anchor existed on this line
     */
    public boolean updateAnchorDisplayLabel(String anchorId, String displayLabel) {
        for (MasterRevisionLineAnchorEntity anchor : anchors) {
            if (anchor.getAnchorId().equals(anchorId)) {
                anchor.setDisplayLabel(displayLabel);
                this.updatedAt = Instant.now();
                return true;
            }
        }
        return false;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    public void markSuperseded() {
        this.current = false;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
        this.updatedAt = Instant.now();
    }

    public void replaceAnchors(List<MasterRevisionLineAnchorEntity> snapshotAnchors) {
        anchors.clear();
        anchors.addAll(snapshotAnchors);
    }
}
