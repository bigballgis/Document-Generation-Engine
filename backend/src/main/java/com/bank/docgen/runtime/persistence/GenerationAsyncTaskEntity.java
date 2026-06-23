package com.bank.docgen.runtime.persistence;

import com.bank.docgen.runtime.domain.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "generation_async_task")
public class GenerationAsyncTaskEntity {

    @Id
    private UUID id;

    @Column(name = "task_external_id", unique = true, length = 64)
    private String taskExternalId;

    @Column(name = "batch_external_id", nullable = false, length = 64)
    private String batchExternalId;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TaskStatus status;

    @Column(name = "route_type", nullable = false, length = 32)
    private String routeType;

    @Column(name = "release_version", length = 32)
    private String releaseVersion;

    @Column(name = "request_id", nullable = false, length = 256)
    private String requestId;

    @Column(name = "idempotency_key", nullable = false, length = 256)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 128)
    private String requestHash;

    @Column(name = "request_payload_json", nullable = false, columnDefinition = "TEXT")
    private String requestPayloadJson;

    @Column(name = "batch_result_json", columnDefinition = "TEXT")
    private String batchResultJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected GenerationAsyncTaskEntity() {
    }

    public GenerationAsyncTaskEntity(
            UUID id,
            String taskExternalId,
            String batchExternalId,
            UUID templateId,
            TaskStatus status,
            String routeType,
            String releaseVersion,
            String requestId,
            String idempotencyKey,
            String requestHash,
            String requestPayloadJson,
            Instant expiresAt
    ) {
        this.id = id;
        this.taskExternalId = taskExternalId;
        this.batchExternalId = batchExternalId;
        this.templateId = templateId;
        this.status = status;
        this.routeType = routeType;
        this.releaseVersion = releaseVersion;
        this.requestId = requestId;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.requestPayloadJson = requestPayloadJson;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public String getTaskExternalId() {
        return taskExternalId;
    }

    public String getBatchExternalId() {
        return batchExternalId;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getRouteType() {
        return routeType;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getRequestPayloadJson() {
        return requestPayloadJson;
    }

    public String getBatchResultJson() {
        return batchResultJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void markProcessing() {
        this.status = TaskStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    public void markSucceeded(String batchResultJson) {
        this.status = TaskStatus.SUCCEEDED;
        this.batchResultJson = batchResultJson;
        this.updatedAt = Instant.now();
    }

    public void markCancelled() {
        this.status = TaskStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }
}
