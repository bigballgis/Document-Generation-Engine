package com.bank.docgen.rendering.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "template_batch_test_run")
public class BatchTestRunEntity {

    @Id
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "created_by", nullable = false, length = 8)
    private String createdBy;

    @Column(name = "total_samples", nullable = false)
    private int totalSamples;

    @Column(name = "succeeded_count", nullable = false)
    private int succeededCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "warning_count", nullable = false)
    private int warningCount;

    @Column(name = "blocker_count", nullable = false)
    private int blockerCount;

    @Column(name = "summary_json", nullable = false, columnDefinition = "TEXT")
    private String summaryJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected BatchTestRunEntity() {
    }

    public BatchTestRunEntity(
            UUID id,
            UUID templateId,
            String createdBy,
            int totalSamples,
            int succeededCount,
            int failedCount,
            int warningCount,
            int blockerCount,
            String summaryJson
    ) {
        this.id = id;
        this.templateId = templateId;
        this.createdBy = createdBy;
        this.totalSamples = totalSamples;
        this.succeededCount = succeededCount;
        this.failedCount = failedCount;
        this.warningCount = warningCount;
        this.blockerCount = blockerCount;
        this.summaryJson = summaryJson;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public int getTotalSamples() {
        return totalSamples;
    }

    public int getSucceededCount() {
        return succeededCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public int getBlockerCount() {
        return blockerCount;
    }

    public String getSummaryJson() {
        return summaryJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
