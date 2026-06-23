package com.bank.docgen.apimgmt.persistence;

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

    public void revoke() {
        this.status = ApiCredentialStatus.REVOKED;
        this.revokedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void rotateSecret(String newSecretHash) {
        this.secretHash = newSecretHash;
        this.updatedAt = Instant.now();
    }
}
