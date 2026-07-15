package com.bank.docgen.runtime.persistence;

import com.bank.docgen.runtime.domain.InvocationKind;
import com.bank.docgen.runtime.domain.InvocationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_invocation_record")
public class ApiInvocationRecordEntity {

    @Id
    private UUID id;

    @Column(name = "invocation_external_id", nullable = false, unique = true, length = 64)
    private String invocationExternalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "invocation_kind", nullable = false, length = 32)
    private InvocationKind invocationKind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private InvocationStatus status;

    @Column(nullable = false, length = 32)
    private String environment;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "template_external_id", nullable = false, length = 64)
    private String templateExternalId;

    @Column(name = "credential_id", nullable = false)
    private UUID credentialId;

    @Column(name = "access_account", nullable = false, length = 64)
    private String accessAccount;

    @Column(name = "request_id", nullable = false, length = 256)
    private String requestId;

    @Column(name = "idempotency_key", nullable = false, length = 256)
    private String idempotencyKey;

    @Column(name = "route_type", length = 32)
    private String routeType;

    @Column(name = "requested_release_version", length = 32)
    private String requestedReleaseVersion;

    @Column(name = "resolved_release_version", length = 32)
    private String resolvedReleaseVersion;

    @Column(name = "output_format", length = 16)
    private String outputFormat;

    @Column(name = "output_mode", length = 32)
    private String outputMode;

    @Column(length = 32)
    private String outcome;

    @Column(name = "duration_ms")
    private Long durationMs;

    /**
     * ADR-0057: retention-scoped sanitized variables (passwords stripped). Purged with row TTL.
     * Encryption-at-rest deferred (ADR-0045); do not invent ad-hoc column crypto.
     */
    @Column(name = "parameters_storage", nullable = false, columnDefinition = "TEXT")
    private String parametersStorage;

    @Column(name = "document_id", length = 128)
    private String documentId;

    @Column(name = "artifact_storage_key", length = 512)
    private String artifactStorageKey;

    @Column(name = "artifact_saved", nullable = false)
    private boolean artifactSaved;

    @Column(name = "record_expires_at", nullable = false)
    private Instant recordExpiresAt;

    @Column(name = "document_expires_at")
    private Instant documentExpiresAt;

    @Column(name = "batch_external_id", length = 64)
    private String batchExternalId;

    @Column(name = "parent_invocation_external_id", length = 64)
    private String parentInvocationExternalId;

    @Column(name = "item_id", length = 128)
    private String itemId;

    @Column(name = "task_external_id", length = 64)
    private String taskExternalId;

    @Column(name = "idempotency_record_id")
    private UUID idempotencyRecordId;

    @Column(name = "audit_id", length = 64)
    private String auditId;

    @Column(name = "is_batch", nullable = false)
    private boolean batch;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "error_code", length = 128)
    private String errorCode;

    @Column(name = "error_category", length = 64)
    private String errorCategory;

    @Column(name = "error_message_key", length = 256)
    private String errorMessageKey;

    @Column(name = "error_retryable")
    private Boolean errorRetryable;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    /** CE-G06: PUBLISHED template_version.id captured at generation time. */
    @Column(name = "release_bundle_snapshot_id")
    private UUID releaseBundleSnapshotId;

    /** CE-G06: copy of template_version.master_file_hash (64-char lowercase hex). */
    @Column(name = "release_bundle_hash", length = 64)
    private String releaseBundleHash;

    protected ApiInvocationRecordEntity() {
    }

    public ApiInvocationRecordEntity(
            UUID id,
            String invocationExternalId,
            InvocationKind invocationKind,
            InvocationStatus status,
            String environment,
            UUID templateId,
            String templateExternalId,
            UUID credentialId,
            String accessAccount,
            String requestId,
            String idempotencyKey,
            String routeType,
            String requestedReleaseVersion,
            String resolvedReleaseVersion,
            String outputFormat,
            String outputMode,
            String outcome,
            Long durationMs,
            String parametersStorage,
            String documentId,
            String artifactStorageKey,
            boolean artifactSaved,
            Instant recordExpiresAt,
            Instant documentExpiresAt,
            String batchExternalId,
            String parentInvocationExternalId,
            String itemId,
            String taskExternalId,
            UUID idempotencyRecordId,
            String auditId,
            boolean batch,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.invocationExternalId = invocationExternalId;
        this.invocationKind = invocationKind;
        this.status = status;
        this.environment = environment;
        this.templateId = templateId;
        this.templateExternalId = templateExternalId;
        this.credentialId = credentialId;
        this.accessAccount = accessAccount;
        this.requestId = requestId;
        this.idempotencyKey = idempotencyKey;
        this.routeType = routeType;
        this.requestedReleaseVersion = requestedReleaseVersion;
        this.resolvedReleaseVersion = resolvedReleaseVersion;
        this.outputFormat = outputFormat;
        this.outputMode = outputMode;
        this.outcome = outcome;
        this.durationMs = durationMs;
        this.parametersStorage = parametersStorage;
        this.documentId = documentId;
        this.artifactStorageKey = artifactStorageKey;
        this.artifactSaved = artifactSaved;
        this.recordExpiresAt = recordExpiresAt;
        this.documentExpiresAt = documentExpiresAt;
        this.batchExternalId = batchExternalId;
        this.parentInvocationExternalId = parentInvocationExternalId;
        this.itemId = itemId;
        this.taskExternalId = taskExternalId;
        this.idempotencyRecordId = idempotencyRecordId;
        this.auditId = auditId;
        this.batch = batch;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getInvocationExternalId() {
        return invocationExternalId;
    }

    public InvocationKind getInvocationKind() {
        return invocationKind;
    }

    public InvocationStatus getStatus() {
        return status;
    }

    public String getEnvironment() {
        return environment;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public String getTemplateExternalId() {
        return templateExternalId;
    }

    public UUID getCredentialId() {
        return credentialId;
    }

    public String getAccessAccount() {
        return accessAccount;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRouteType() {
        return routeType;
    }

    public String getRequestedReleaseVersion() {
        return requestedReleaseVersion;
    }

    public String getResolvedReleaseVersion() {
        return resolvedReleaseVersion;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public String getOutputMode() {
        return outputMode;
    }

    public String getOutcome() {
        return outcome;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public String getParametersStorage() {
        return parametersStorage;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getArtifactStorageKey() {
        return artifactStorageKey;
    }

    public boolean isArtifactSaved() {
        return artifactSaved;
    }

    public Instant getRecordExpiresAt() {
        return recordExpiresAt;
    }

    public Instant getDocumentExpiresAt() {
        return documentExpiresAt;
    }

    public String getBatchExternalId() {
        return batchExternalId;
    }

    public String getParentInvocationExternalId() {
        return parentInvocationExternalId;
    }

    public String getItemId() {
        return itemId;
    }

    public String getTaskExternalId() {
        return taskExternalId;
    }

    public UUID getIdempotencyRecordId() {
        return idempotencyRecordId;
    }

    public String getAuditId() {
        return auditId;
    }

    public boolean isBatch() {
        return batch;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorCategory() {
        return errorCategory;
    }

    public String getErrorMessageKey() {
        return errorMessageKey;
    }

    public Boolean getErrorRetryable() {
        return errorRetryable;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public UUID getReleaseBundleSnapshotId() {
        return releaseBundleSnapshotId;
    }

    public String getReleaseBundleHash() {
        return releaseBundleHash;
    }

    /**
     * CE-G06: attach release-bundle fingerprint when a PUBLISHED release was resolved.
     * Passing nulls clears the fingerprint (pre-G06 / unresolved-release rows).
     */
    public void applyReleaseBundleFingerprint(UUID releaseBundleSnapshotId, String releaseBundleHash) {
        this.releaseBundleSnapshotId = releaseBundleSnapshotId;
        this.releaseBundleHash = releaseBundleHash;
    }

    public void applyErrorEnvelope(
            String errorCode,
            String errorCategory,
            String errorMessageKey,
            Boolean errorRetryable,
            String errorMessage
    ) {
        this.errorCode = errorCode;
        this.errorCategory = errorCategory;
        this.errorMessageKey = errorMessageKey;
        this.errorRetryable = errorRetryable;
        this.errorMessage = errorMessage;
    }

    public void updateTerminalStatus(InvocationStatus status, String outcome, Instant updatedAt) {
        this.status = status;
        this.outcome = outcome;
        this.updatedAt = updatedAt;
    }

    public void updateItemOutcome(
            InvocationStatus status,
            String outcome,
            String documentId,
            String artifactStorageKey,
            boolean artifactSaved,
            Instant documentExpiresAt,
            Instant updatedAt
    ) {
        this.status = status;
        this.outcome = outcome;
        this.documentId = documentId;
        this.artifactStorageKey = artifactStorageKey;
        this.artifactSaved = artifactSaved;
        this.documentExpiresAt = documentExpiresAt;
        this.updatedAt = updatedAt;
    }

    public void markDocumentArtifactCleaned(Instant updatedAt) {
        this.artifactStorageKey = null;
        this.updatedAt = updatedAt;
    }
}
