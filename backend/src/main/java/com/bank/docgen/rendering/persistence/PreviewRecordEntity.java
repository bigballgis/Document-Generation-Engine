package com.bank.docgen.rendering.persistence;

import com.bank.docgen.rendering.domain.PreviewStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "preview_record")
public class PreviewRecordEntity {

    @Id
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "template_version_id", nullable = false)
    private UUID templateVersionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PreviewStatus status;

    @Column(name = "output_format", nullable = false, length = 16)
    private String outputFormat;

    @Column(name = "variables_hash", length = 128)
    private String variablesHash;

    @Column(name = "artifact_storage_key", length = 512)
    private String artifactStorageKey;

    @Column(name = "pdf_artifact_storage_key", length = 512)
    private String pdfArtifactStorageKey;

    @Column(name = "fidelity_warnings_json")
    private String fidelityWarningsJson;

    @Column(name = "test_data_set_external_id", length = 64)
    private String testDataSetExternalId;

    @Column(name = "batch_test_run_id")
    private UUID batchTestRunId;

    @Column(name = "render_profile_version", length = 32)
    private String renderProfileVersion;

    @Column(name = "created_by", nullable = false, length = 8)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "temp_storage_key", length = 512)
    private String tempStorageKey;

    @Column(name = "temp_artifact_cleaned", nullable = false)
    private boolean tempArtifactCleaned;

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    protected PreviewRecordEntity() {
    }

    public PreviewRecordEntity(
            UUID id,
            UUID templateId,
            UUID templateVersionId,
            String outputFormat,
            String variablesHash,
            String createdBy,
            String testDataSetExternalId,
            UUID batchTestRunId
    ) {
        this.id = id;
        this.templateId = templateId;
        this.templateVersionId = templateVersionId;
        this.status = PreviewStatus.ACCEPTED;
        this.outputFormat = outputFormat;
        this.variablesHash = variablesHash;
        this.testDataSetExternalId = testDataSetExternalId;
        this.batchTestRunId = batchTestRunId;
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

    public UUID getTemplateVersionId() {
        return templateVersionId;
    }

    public PreviewStatus getStatus() {
        return status;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public String getVariablesHash() {
        return variablesHash;
    }

    public String getArtifactStorageKey() {
        return artifactStorageKey;
    }

    public String getPdfArtifactStorageKey() {
        return pdfArtifactStorageKey;
    }

    public String getFidelityWarningsJson() {
        return fidelityWarningsJson;
    }

    public String getTestDataSetExternalId() {
        return testDataSetExternalId;
    }

    public UUID getBatchTestRunId() {
        return batchTestRunId;
    }

    public String getRenderProfileVersion() {
        return renderProfileVersion;
    }

    public void setRenderProfileVersion(String renderProfileVersion) {
        this.renderProfileVersion = renderProfileVersion;
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

    public void markProcessing() {
        this.status = PreviewStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    public void markSucceeded(String artifactStorageKey, String pdfArtifactStorageKey, String fidelityWarningsJson) {
        this.status = PreviewStatus.SUCCEEDED;
        this.artifactStorageKey = artifactStorageKey;
        this.pdfArtifactStorageKey = pdfArtifactStorageKey;
        this.fidelityWarningsJson = fidelityWarningsJson;
        this.updatedAt = Instant.now();
    }

    public void setPdfArtifactStorageKey(String pdfArtifactStorageKey) {
        this.pdfArtifactStorageKey = pdfArtifactStorageKey;
        this.updatedAt = Instant.now();
    }

    public void markFailed() {
        this.status = PreviewStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String errorDetails) {
        this.status = PreviewStatus.FAILED;
        this.errorDetails = errorDetails;
        this.updatedAt = Instant.now();
    }

    public void markExpired() {
        this.status = PreviewStatus.EXPIRED;
        this.updatedAt = Instant.now();
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
        this.updatedAt = Instant.now();
    }

    public void setTempStorageKey(String tempStorageKey) {
        this.tempStorageKey = tempStorageKey;
        this.updatedAt = Instant.now();
    }

    public void markTempArtifactCleaned() {
        this.tempArtifactCleaned = true;
        this.updatedAt = Instant.now();
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getTempStorageKey() {
        return tempStorageKey;
    }

    public boolean isTempArtifactCleaned() {
        return tempArtifactCleaned;
    }

    public String getErrorDetails() {
        return errorDetails;
    }
}
