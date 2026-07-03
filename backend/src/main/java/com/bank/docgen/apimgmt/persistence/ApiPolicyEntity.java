package com.bank.docgen.apimgmt.persistence;

import com.bank.docgen.apimgmt.domain.ApiPolicyPlatformDefaults;
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

    @Column(name = "batch_sync_max_items", nullable = false)
    private int batchSyncMaxItems = ApiPolicyPlatformDefaults.BATCH_SYNC_MAX_ITEMS;

    @Column(name = "batch_async_max_items", nullable = false)
    private int batchAsyncMaxItems = ApiPolicyPlatformDefaults.BATCH_ASYNC_MAX_ITEMS;

    @Column(name = "docx_encryption_enabled", nullable = false)
    private boolean docxEncryptionEnabled;

    @Column(name = "pdf_encryption_enabled", nullable = false)
    private boolean pdfEncryptionEnabled;

    @Column(name = "save_generated_documents", nullable = false)
    private boolean saveGeneratedDocuments = ApiPolicyPlatformDefaults.SAVE_GENERATED_DOCUMENTS;

    @Column(name = "invocation_record_retention_days", nullable = false)
    private int invocationRecordRetentionDays = ApiPolicyPlatformDefaults.INVOCATION_RECORD_RETENTION_DAYS;

    @Column(name = "document_retention_days", nullable = false)
    private int documentRetentionDays = ApiPolicyPlatformDefaults.DOCUMENT_RETENTION_DAYS;

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
        this.outputFormatsJson = ApiPolicyPlatformDefaults.OUTPUT_FORMATS_JSON;
        this.outputModesJson = ApiPolicyPlatformDefaults.OUTPUT_MODES_JSON;
        this.batchEnabled = ApiPolicyPlatformDefaults.BATCH_ENABLED;
        this.maxBatchSize = ApiPolicyPlatformDefaults.BATCH_SYNC_MAX_ITEMS;
        this.batchSyncMaxItems = ApiPolicyPlatformDefaults.BATCH_SYNC_MAX_ITEMS;
        this.batchAsyncMaxItems = ApiPolicyPlatformDefaults.BATCH_ASYNC_MAX_ITEMS;
        this.docxEncryptionEnabled = ApiPolicyPlatformDefaults.DOCX_ENCRYPTION_ENABLED;
        this.pdfEncryptionEnabled = ApiPolicyPlatformDefaults.PDF_ENCRYPTION_ENABLED;
        this.saveGeneratedDocuments = ApiPolicyPlatformDefaults.SAVE_GENERATED_DOCUMENTS;
        this.invocationRecordRetentionDays = ApiPolicyPlatformDefaults.INVOCATION_RECORD_RETENTION_DAYS;
        this.documentRetentionDays = ApiPolicyPlatformDefaults.DOCUMENT_RETENTION_DAYS;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static ApiPolicyEntity createSkeleton(UUID templateId, String actor) {
        return new ApiPolicyEntity(
                UUID.randomUUID(),
                templateId,
                ApiPolicyPlatformDefaults.ALLOWED_AD_GROUPS_JSON,
                actor
        );
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

    public int getBatchSyncMaxItems() {
        return batchSyncMaxItems;
    }

    public int getBatchAsyncMaxItems() {
        return batchAsyncMaxItems;
    }

    public boolean isDocxEncryptionEnabled() {
        return docxEncryptionEnabled;
    }

    public boolean isPdfEncryptionEnabled() {
        return pdfEncryptionEnabled;
    }

    public boolean isSaveGeneratedDocuments() {
        return saveGeneratedDocuments;
    }

    public int getInvocationRecordRetentionDays() {
        return invocationRecordRetentionDays;
    }

    public int getDocumentRetentionDays() {
        return documentRetentionDays;
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

    public void materializeDefaultRouteOnFirstPublish(String releaseVersion, String actor) {
        if (defaultRouteReleaseVersion != null && !defaultRouteReleaseVersion.isBlank()) {
            return;
        }
        this.defaultRouteReleaseVersion = releaseVersion;
        this.updatedBy = actor;
        this.updatedAt = Instant.now();
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
        if (defaultRouteReleaseVersion != null) {
            this.defaultRouteReleaseVersion = defaultRouteReleaseVersion;
        }
        this.outputFormatsJson = outputFormatsJson;
        this.outputModesJson = outputModesJson;
        this.batchEnabled = batchEnabled;
        this.maxBatchSize = maxBatchSize;
        this.batchSyncMaxItems = maxBatchSize;
        this.docxEncryptionEnabled = docxEncryptionEnabled;
        this.pdfEncryptionEnabled = pdfEncryptionEnabled;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    public void updateAdGroupsDomain(String allowedAdGroupsJson, String updatedBy) {
        this.allowedAdGroupsJson = allowedAdGroupsJson;
        bumpVersion(updatedBy);
    }

    public void updateOutputDomain(String outputFormatsJson, String outputModesJson, String updatedBy) {
        this.outputFormatsJson = outputFormatsJson;
        this.outputModesJson = outputModesJson;
        bumpVersion(updatedBy);
    }

    public void updateBatchLimitsDomain(
            boolean batchEnabled,
            int syncMaxItems,
            int asyncMaxItems,
            String updatedBy
    ) {
        this.batchEnabled = batchEnabled;
        this.batchSyncMaxItems = syncMaxItems;
        this.batchAsyncMaxItems = asyncMaxItems;
        this.maxBatchSize = syncMaxItems;
        bumpVersion(updatedBy);
    }

    public void updateEncryptionDomain(
            boolean docxEncryptionEnabled,
            boolean pdfEncryptionEnabled,
            String updatedBy
    ) {
        this.docxEncryptionEnabled = docxEncryptionEnabled;
        this.pdfEncryptionEnabled = pdfEncryptionEnabled;
        bumpVersion(updatedBy);
    }

    public void updateDefaultRouteDomain(String defaultRouteReleaseVersion, String updatedBy) {
        this.defaultRouteReleaseVersion = defaultRouteReleaseVersion;
        bumpVersion(updatedBy);
    }

    public void updateRetentionDomain(
            boolean saveGeneratedDocuments,
            int invocationRecordRetentionDays,
            int documentRetentionDays,
            String updatedBy
    ) {
        this.saveGeneratedDocuments = saveGeneratedDocuments;
        this.invocationRecordRetentionDays = invocationRecordRetentionDays;
        this.documentRetentionDays = documentRetentionDays;
        bumpVersion(updatedBy);
    }

    public void applyRollbackConfiguration(
            String allowedAdGroupsJson,
            String defaultRouteReleaseVersion,
            String outputFormatsJson,
            String outputModesJson,
            boolean batchEnabled,
            int batchSyncMaxItems,
            int batchAsyncMaxItems,
            boolean docxEncryptionEnabled,
            boolean pdfEncryptionEnabled,
            String updatedBy
    ) {
        this.allowedAdGroupsJson = allowedAdGroupsJson;
        this.defaultRouteReleaseVersion = defaultRouteReleaseVersion;
        this.outputFormatsJson = outputFormatsJson;
        this.outputModesJson = outputModesJson;
        this.batchEnabled = batchEnabled;
        this.batchSyncMaxItems = batchSyncMaxItems;
        this.batchAsyncMaxItems = batchAsyncMaxItems;
        this.maxBatchSize = batchSyncMaxItems;
        this.docxEncryptionEnabled = docxEncryptionEnabled;
        this.pdfEncryptionEnabled = pdfEncryptionEnabled;
        bumpVersion(updatedBy);
    }

    private void bumpVersion(String updatedBy) {
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
        this.policyVersion += 1;
    }
}
