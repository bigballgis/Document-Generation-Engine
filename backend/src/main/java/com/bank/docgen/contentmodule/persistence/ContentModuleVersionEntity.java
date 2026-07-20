package com.bank.docgen.contentmodule.persistence;

import com.bank.docgen.contentmodule.domain.ContentModuleLifecycleState;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "content_module_version",
        uniqueConstraints = @UniqueConstraint(columnNames = {"module_id", "semantic_version"})
)
public class ContentModuleVersionEntity {

    @Id
    private UUID id;

    @Column(name = "module_id", nullable = false)
    private UUID moduleId;

    @Column(name = "semantic_version", nullable = false, length = 32)
    private String semanticVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_state", nullable = false, length = 32)
    private ContentModuleReviewState reviewState;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_state", length = 32)
    private ContentModuleLifecycleState lifecycleState;

    @Column(name = "content_structure_json", nullable = false, columnDefinition = "TEXT")
    private String contentStructureJson;

    @Column(name = "change_description", length = 2048)
    private String changeDescription;

    @Column(name = "rejection_reason", length = 2048)
    private String rejectionReason;

    @Column(name = "jurisdiction", length = 128)
    private String jurisdiction;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "legal_review_ref", length = 128)
    private String legalReviewRef;

    @Column(name = "submitted_by", length = 8)
    private String submittedBy;

    @Column(name = "created_by", nullable = false, length = 8)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 8)
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ContentModuleVersionEntity() {
    }

    public ContentModuleVersionEntity(
            UUID id,
            UUID moduleId,
            String semanticVersion,
            String contentStructureJson,
            String changeDescription,
            String createdBy
    ) {
        this.id = id;
        this.moduleId = moduleId;
        this.semanticVersion = semanticVersion;
        this.reviewState = ContentModuleReviewState.DRAFT;
        this.contentStructureJson = contentStructureJson;
        this.changeDescription = changeDescription;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getModuleId() {
        return moduleId;
    }

    public String getSemanticVersion() {
        return semanticVersion;
    }

    public ContentModuleReviewState getReviewState() {
        return reviewState;
    }

    public ContentModuleLifecycleState getLifecycleState() {
        return lifecycleState;
    }

    public String getContentStructureJson() {
        return contentStructureJson;
    }

    public String getChangeDescription() {
        return changeDescription;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return effectiveTo;
    }

    public String getLegalReviewRef() {
        return legalReviewRef;
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

    public String getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
        this.updatedAt = Instant.now();
    }

    public boolean isReferencable() {
        return reviewState == ContentModuleReviewState.APPROVED
                && lifecycleState == ContentModuleLifecycleState.ACTIVE;
    }

    public void setReviewState(ContentModuleReviewState reviewState) {
        this.reviewState = reviewState;
        this.updatedAt = Instant.now();
    }

    public void setLifecycleState(ContentModuleLifecycleState lifecycleState) {
        this.lifecycleState = lifecycleState;
        this.updatedAt = Instant.now();
    }

    public void setContentStructureJson(String contentStructureJson) {
        this.contentStructureJson = contentStructureJson;
        this.updatedAt = Instant.now();
    }

    public void setChangeDescription(String changeDescription) {
        this.changeDescription = changeDescription;
        this.updatedAt = Instant.now();
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
        this.updatedAt = Instant.now();
    }

    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
        this.updatedAt = Instant.now();
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
        this.updatedAt = Instant.now();
    }

    public void setEffectiveTo(Instant effectiveTo) {
        this.effectiveTo = effectiveTo;
        this.updatedAt = Instant.now();
    }

    public void setLegalReviewRef(String legalReviewRef) {
        this.legalReviewRef = legalReviewRef;
        this.updatedAt = Instant.now();
    }

    public void setLegalMetadata(
            String jurisdiction,
            Instant effectiveFrom,
            Instant effectiveTo,
            String legalReviewRef
    ) {
        this.jurisdiction = jurisdiction;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.legalReviewRef = legalReviewRef;
        this.updatedAt = Instant.now();
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    /**
     * K08-C5: expired when effectiveTo is set and utcNow is strictly after it.
     */
    public boolean isEffectiveExpired(Instant utcNow) {
        return effectiveTo != null && utcNow.isAfter(effectiveTo);
    }

    /**
     * IBL-E5 / E5-C2: not started when effectiveFrom is set and utcNow is strictly before it.
     * Equal instant is already effective (same edge policy as {@link #isEffectiveExpired}).
     */
    public boolean isEffectiveNotStarted(Instant utcNow) {
        return effectiveFrom != null && utcNow.isBefore(effectiveFrom);
    }
}
