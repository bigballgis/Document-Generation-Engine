package com.bank.docgen.apimgmt.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invocation_regeneration")
public class InvocationRegenerationEntity {

    @Id
    private UUID id;

    @Column(name = "regeneration_external_id", nullable = false, unique = true, length = 64)
    private String regenerationExternalId;

    @Column(name = "source_invocation_external_id", nullable = false, length = 64)
    private String sourceInvocationExternalId;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "release_bundle_snapshot_id", nullable = false)
    private UUID releaseBundleSnapshotId;

    @Column(name = "release_bundle_hash", nullable = false, length = 64)
    private String releaseBundleHash;

    @Column(name = "output_format", nullable = false, length = 16)
    private String outputFormat;

    @Column(nullable = false, length = 32)
    private String outcome;

    @Column(name = "error_code", length = 128)
    private String errorCode;

    @Column(name = "artifact_storage_key", length = 512)
    private String artifactStorageKey;

    @Column(nullable = false)
    private boolean specimen;

    @Column(name = "encryption_reapplied", nullable = false)
    private boolean encryptionReapplied;

    @Column(name = "actor_username", nullable = false, length = 128)
    private String actorUsername;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InvocationRegenerationEntity() {
    }

    public InvocationRegenerationEntity(
            UUID id,
            String regenerationExternalId,
            String sourceInvocationExternalId,
            UUID templateId,
            UUID releaseBundleSnapshotId,
            String releaseBundleHash,
            String outputFormat,
            String outcome,
            String errorCode,
            String artifactStorageKey,
            boolean specimen,
            boolean encryptionReapplied,
            String actorUsername,
            Instant createdAt
    ) {
        this.id = id;
        this.regenerationExternalId = regenerationExternalId;
        this.sourceInvocationExternalId = sourceInvocationExternalId;
        this.templateId = templateId;
        this.releaseBundleSnapshotId = releaseBundleSnapshotId;
        this.releaseBundleHash = releaseBundleHash;
        this.outputFormat = outputFormat;
        this.outcome = outcome;
        this.errorCode = errorCode;
        this.artifactStorageKey = artifactStorageKey;
        this.specimen = specimen;
        this.encryptionReapplied = encryptionReapplied;
        this.actorUsername = actorUsername;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getRegenerationExternalId() {
        return regenerationExternalId;
    }

    public String getSourceInvocationExternalId() {
        return sourceInvocationExternalId;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public UUID getReleaseBundleSnapshotId() {
        return releaseBundleSnapshotId;
    }

    public String getReleaseBundleHash() {
        return releaseBundleHash;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getArtifactStorageKey() {
        return artifactStorageKey;
    }

    public boolean isSpecimen() {
        return specimen;
    }

    public boolean isEncryptionReapplied() {
        return encryptionReapplied;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
