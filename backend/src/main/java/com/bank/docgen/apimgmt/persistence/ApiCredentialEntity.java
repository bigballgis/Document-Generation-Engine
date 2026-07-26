package com.bank.docgen.apimgmt.persistence;

import com.bank.docgen.apimgmt.domain.ApiCredentialLifecycleSupport;
import com.bank.docgen.apimgmt.domain.ApiCredentialStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_credential")
public class ApiCredentialEntity {

    @Id
    private UUID id;

    @Column(name = "external_id", nullable = false, unique = true, length = 128)
    private String externalId;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "secret_hash", nullable = false, length = 256)
    private String secretHash;

    @Column(name = "previous_secret_hash", length = 256)
    private String previousSecretHash;

    @Column(name = "rotation_grace_period_ends_at")
    private Instant rotationGracePeriodEndsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApiCredentialStatus status;

    @Column(name = "created_by", nullable = false, length = 8)
    private String createdBy;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "rotation_generation", nullable = false)
    private int rotationGeneration;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected ApiCredentialEntity() {
    }

    public ApiCredentialEntity(UUID id, String externalId, UUID templateId, String secretHash, String createdBy) {
        this.id = id;
        this.externalId = externalId;
        this.templateId = templateId;
        this.secretHash = secretHash;
        this.status = ApiCredentialStatus.ACTIVE;
        this.createdBy = createdBy;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.rotationGeneration = 0;
        this.expiresAt = ApiCredentialLifecycleSupport.defaultExpiresAt(now);
    }

    public UUID getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public String getSecretHash() {
        return secretHash;
    }

    public String getPreviousSecretHash() {
        return previousSecretHash;
    }

    public Instant getRotationGracePeriodEndsAt() {
        return rotationGracePeriodEndsAt;
    }

    public ApiCredentialStatus getStatus() {
        return status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public int getRotationGeneration() {
        return rotationGeneration;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
        this.updatedAt = Instant.now();
    }

    public void revoke() {
        this.status = ApiCredentialStatus.REVOKED;
        this.revokedAt = Instant.now();
        this.updatedAt = Instant.now();
        this.previousSecretHash = null;
        this.rotationGracePeriodEndsAt = null;
    }

    /**
     * FOS-W10-1/W10-2: keep prior hash for 28-day grace; rebase {@code expiresAt} from {@code now}.
     */
    public void rotateSecret(String newSecretHash, Instant now) {
        this.previousSecretHash = this.secretHash;
        this.secretHash = newSecretHash;
        this.rotationGracePeriodEndsAt = ApiCredentialLifecycleSupport.rotationGracePeriodEndsAt(now);
        this.rotationGeneration++;
        this.expiresAt = ApiCredentialLifecycleSupport.defaultExpiresAt(now);
        this.updatedAt = now;
    }
}
