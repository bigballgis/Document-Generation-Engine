package com.bank.docgen.runtime.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "runtime_generation_audit_event")
public class RuntimeGenerationAuditEventEntity {

    @Id
    private UUID id;

    @Column(name = "event_at", nullable = false)
    private Instant eventAt;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, length = 32)
    private String environment;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "group_code", length = 64)
    private String groupCode;

    @Column(name = "credential_id")
    private UUID credentialId;

    @Column(name = "credential_fingerprint", length = 64)
    private String credentialFingerprint;

    @Column(name = "access_account", length = 64)
    private String accessAccount;

    @Column(name = "release_version", length = 32)
    private String releaseVersion;

    @Column(name = "resolved_release_version", length = 32)
    private String resolvedReleaseVersion;

    @Column(name = "route_type", length = 32)
    private String routeType;

    @Column(name = "output_format", length = 16)
    private String outputFormat;

    @Column(name = "output_mode", length = 32)
    private String outputMode;

    @Column(name = "request_id", length = 256)
    private String requestId;

    @Column(name = "idempotency_key_hash", length = 128)
    private String idempotencyKeyHash;

    @Column(name = "idempotency_status", length = 32)
    private String idempotencyStatus;

    @Column(name = "task_external_id", length = 64)
    private String taskExternalId;

    @Column(name = "batch_external_id", length = 64)
    private String batchExternalId;

    @Column(name = "document_id", length = 128)
    private String documentId;

    @Column(nullable = false, length = 32)
    private String outcome;

    @Column(name = "result_summary", length = 512)
    private String resultSummary;

    @Column(name = "error_summary", length = 512)
    private String errorSummary;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "audit_id", nullable = false, length = 64)
    private String auditId;

    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;

    protected RuntimeGenerationAuditEventEntity() {
    }

    public RuntimeGenerationAuditEventEntity(
            UUID id,
            Instant eventAt,
            String eventType,
            String environment,
            UUID templateId,
            String groupCode,
            UUID credentialId,
            String credentialFingerprint,
            String accessAccount,
            String releaseVersion,
            String resolvedReleaseVersion,
            String routeType,
            String outputFormat,
            String outputMode,
            String requestId,
            String idempotencyKeyHash,
            String idempotencyStatus,
            String taskExternalId,
            String batchExternalId,
            String documentId,
            String outcome,
            String resultSummary,
            String errorSummary,
            Long durationMs,
            String auditId,
            String traceId
    ) {
        this.id = id;
        this.eventAt = eventAt;
        this.eventType = eventType;
        this.environment = environment;
        this.templateId = templateId;
        this.groupCode = groupCode;
        this.credentialId = credentialId;
        this.credentialFingerprint = credentialFingerprint;
        this.accessAccount = accessAccount;
        this.releaseVersion = releaseVersion;
        this.resolvedReleaseVersion = resolvedReleaseVersion;
        this.routeType = routeType;
        this.outputFormat = outputFormat;
        this.outputMode = outputMode;
        this.requestId = requestId;
        this.idempotencyKeyHash = idempotencyKeyHash;
        this.idempotencyStatus = idempotencyStatus;
        this.taskExternalId = taskExternalId;
        this.batchExternalId = batchExternalId;
        this.documentId = documentId;
        this.outcome = outcome;
        this.resultSummary = resultSummary;
        this.errorSummary = errorSummary;
        this.durationMs = durationMs;
        this.auditId = auditId;
        this.traceId = traceId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEnvironment() {
        return environment;
    }
}
