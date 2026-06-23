package com.bank.docgen.runtime.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "generation_idempotency")
public class GenerationIdempotencyEntity {

    @Id
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, length = 256)
    private String idempotencyKey;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "request_hash", nullable = false, length = 128)
    private String requestHash;

    @Column(name = "response_storage_key", length = 512)
    private String responseStorageKey;

    @Column(name = "document_id", length = 128)
    private String documentId;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "download_expires_at")
    private Instant downloadExpiresAt;

    protected GenerationIdempotencyEntity() {
    }

    public GenerationIdempotencyEntity(
            UUID id,
            String idempotencyKey,
            UUID templateId,
            String requestHash,
            String status,
            Instant expiresAt
    ) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.templateId = templateId;
        this.requestHash = requestHash;
        this.status = status;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getResponseStorageKey() {
        return responseStorageKey;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getDownloadExpiresAt() {
        return downloadExpiresAt;
    }

    public void complete(String responseStorageKey, String documentId) {
        this.responseStorageKey = responseStorageKey;
        this.documentId = documentId;
        this.status = "COMPLETED";
        this.downloadExpiresAt = Instant.now().plusSeconds(900);
    }

    public void markDownloadExpired(Instant expiredAt) {
        this.downloadExpiresAt = expiredAt;
    }
}
