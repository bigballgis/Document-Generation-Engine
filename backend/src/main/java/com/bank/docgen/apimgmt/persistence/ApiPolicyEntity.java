package com.bank.docgen.apimgmt.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_policy")
public class ApiPolicyEntity {

    @Id
    private UUID id;

    @Column(name = "template_id", nullable = false, unique = true)
    private UUID templateId;

    @Column(name = "policy_version", nullable = false)
    private int policyVersion;

    @Column(name = "allowed_ad_groups", nullable = false)
    private String allowedAdGroupsJson;

    @Column(name = "default_route_release_version", length = 32)
    private String defaultRouteReleaseVersion;

    @Column(name = "output_formats", nullable = false)
    private String outputFormatsJson;

    @Column(name = "output_modes", nullable = false)
    private String outputModesJson;

    @Column(name = "batch_enabled", nullable = false)
    private boolean batchEnabled;

    @Column(name = "max_batch_size", nullable = false)
    private int maxBatchSize;

    @Column(name = "docx_encryption_enabled", nullable = false)
    private boolean docxEncryptionEnabled;

    @Column(name = "pdf_encryption_enabled", nullable = false)
    private boolean pdfEncryptionEnabled;

    @Column(name = "created_by", nullable = false, length = 8)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 8)
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ApiPolicyEntity() {
    }

    public ApiPolicyEntity(UUID id, UUID templateId, String allowedAdGroupsJson, String createdBy) {
        this.id = id;
        this.templateId = templateId;
        this.policyVersion = 1;
        this.allowedAdGroupsJson = allowedAdGroupsJson;
        this.outputFormatsJson = "[\"DOCX\"]";
        this.outputModesJson = "[\"SYNC_STREAM\"]";
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
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

    public int getPolicyVersion() {
        return policyVersion;
    }

    public String getAllowedAdGroupsJson() {
        return allowedAdGroupsJson;
    }

    public String getDefaultRouteReleaseVersion() {
        return defaultRouteReleaseVersion;
    }

    public String getOutputFormatsJson() {
        return outputFormatsJson;
    }

    public String getOutputModesJson() {
        return outputModesJson;
    }

    public boolean isBatchEnabled() {
        return batchEnabled;
    }

    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    public boolean isDocxEncryptionEnabled() {
        return docxEncryptionEnabled;
    }

    public boolean isPdfEncryptionEnabled() {
        return pdfEncryptionEnabled;
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

    public void update(
            String allowedAdGroupsJson,
            String defaultRouteReleaseVersion,
            String outputFormatsJson,
            String outputModesJson,
            boolean batchEnabled,
            int maxBatchSize,
            boolean docxEncryptionEnabled,
            boolean pdfEncryptionEnabled,
            String updatedBy
    ) {
        replaceConfiguration(
                allowedAdGroupsJson,
                defaultRouteReleaseVersion,
                outputFormatsJson,
                outputModesJson,
                batchEnabled,
                maxBatchSize,
                docxEncryptionEnabled,
                pdfEncryptionEnabled,
                updatedBy
        );
        this.policyVersion += 1;
    }

    public void replaceConfiguration(
            String allowedAdGroupsJson,
            String defaultRouteReleaseVersion,
            String outputFormatsJson,
            String outputModesJson,
            boolean batchEnabled,
            int maxBatchSize,
            boolean docxEncryptionEnabled,
            boolean pdfEncryptionEnabled,
            String updatedBy
    ) {
        this.allowedAdGroupsJson = allowedAdGroupsJson;
        this.defaultRouteReleaseVersion = defaultRouteReleaseVersion;
        this.outputFormatsJson = outputFormatsJson;
        this.outputModesJson = outputModesJson;
        this.batchEnabled = batchEnabled;
        this.maxBatchSize = maxBatchSize;
        this.docxEncryptionEnabled = docxEncryptionEnabled;
        this.pdfEncryptionEnabled = pdfEncryptionEnabled;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }
}
