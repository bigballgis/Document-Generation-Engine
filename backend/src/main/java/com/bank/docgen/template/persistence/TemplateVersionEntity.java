package com.bank.docgen.template.persistence;

import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "template_version")
public class TemplateVersionEntity {

    @Id
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "dev_version_number", nullable = false)
    private int devVersionNumber;

    @Column(name = "release_version", length = 32)
    private String releaseVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false, length = 32)
    private TemplateLifecycleStatus lifecycleStatus;

    @Column(name = "master_catalog_version", nullable = false, length = 32)
    private String masterCatalogVersion;

    @Column(name = "rules_json")
    private String rulesJson;

    @Column(name = "render_profile_version", length = 32)
    private String renderProfileVersion;

    @Column(name = "render_profile_json", columnDefinition = "TEXT")
    private String renderProfileJson;

    @Column(name = "fidelity_warning_codes_json", columnDefinition = "TEXT")
    private String fidelityWarningCodesJson;

    /**
     * ADR-0042: Microsoft Word authoring page count declared by the template author.
     * Never backfilled from LibreOffice / Docker PDF page counts.
     */
    @Column(name = "author_word_page_count")
    private Integer authorWordPageCount;

    // CE-K01: immutable release-bundle pinning. Populated on publish or by the
    // retroactive backfill service. Nullable for non-PUBLISHED / pre-migration rows.
    @Column(name = "master_revision_id")
    private UUID masterRevisionId;

    @Column(name = "master_file_hash", length = 64)
    private String masterFileHash;

    @Column(name = "pin_metadata_json", columnDefinition = "TEXT")
    private String pinMetadataJson;

    @Column(name = "created_by", nullable = false, length = 8)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected TemplateVersionEntity() {
    }

    public TemplateVersionEntity(UUID id, UUID templateId, String createdBy) {
        this.id = id;
        this.templateId = templateId;
        this.devVersionNumber = 1;
        this.lifecycleStatus = TemplateLifecycleStatus.DRAFT;
        this.masterCatalogVersion = "v1";
        this.createdBy = createdBy;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public int getDevVersionNumber() {
        return devVersionNumber;
    }

    public void setDevVersionNumber(int devVersionNumber) {
        this.devVersionNumber = devVersionNumber;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    public TemplateLifecycleStatus getLifecycleStatus() {
        return lifecycleStatus;
    }

    public String getMasterCatalogVersion() {
        return masterCatalogVersion;
    }

    public void setMasterCatalogVersion(String masterCatalogVersion) {
        this.masterCatalogVersion = masterCatalogVersion;
        this.updatedAt = Instant.now();
    }

    public String getRulesJson() {
        return rulesJson;
    }

    public void setRulesJson(String rulesJson) {
        this.rulesJson = rulesJson;
        this.updatedAt = Instant.now();
    }

    public String getRenderProfileVersion() {
        return renderProfileVersion;
    }

    public String getRenderProfileJson() {
        return renderProfileJson;
    }

    public void setRenderProfileVersion(String renderProfileVersion) {
        this.renderProfileVersion = renderProfileVersion;
        this.updatedAt = Instant.now();
    }

    public void setRenderProfileJson(String renderProfileJson) {
        this.renderProfileJson = renderProfileJson;
        this.updatedAt = Instant.now();
    }

    public String getFidelityWarningCodesJson() {
        return fidelityWarningCodesJson;
    }

    public void setFidelityWarningCodesJson(String fidelityWarningCodesJson) {
        this.fidelityWarningCodesJson = fidelityWarningCodesJson;
        this.updatedAt = Instant.now();
    }

    public Integer getAuthorWordPageCount() {
        return authorWordPageCount;
    }

    public void setAuthorWordPageCount(Integer authorWordPageCount) {
        this.authorWordPageCount = authorWordPageCount;
        this.updatedAt = Instant.now();
    }

    public String getCreatedBy() {
        return createdBy;
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

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
        this.updatedAt = Instant.now();
    }

    public void setReleaseVersion(String releaseVersion) {
        this.releaseVersion = releaseVersion;
    }

    public void setLifecycleStatus(TemplateLifecycleStatus lifecycleStatus) {
        this.lifecycleStatus = lifecycleStatus;
        this.updatedAt = Instant.now();
    }

    public UUID getMasterRevisionId() {
        return masterRevisionId;
    }

    public void setMasterRevisionId(UUID masterRevisionId) {
        this.masterRevisionId = masterRevisionId;
        this.updatedAt = Instant.now();
    }

    public String getMasterFileHash() {
        return masterFileHash;
    }

    public void setMasterFileHash(String masterFileHash) {
        this.masterFileHash = masterFileHash;
        this.updatedAt = Instant.now();
    }

    public String getPinMetadataJson() {
        return pinMetadataJson;
    }

    public void setPinMetadataJson(String pinMetadataJson) {
        this.pinMetadataJson = pinMetadataJson;
        this.updatedAt = Instant.now();
    }
}
