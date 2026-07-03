package com.bank.docgen.rendering.persistence;

import com.bank.docgen.rendering.domain.BatchTestRunStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private BatchTestRunStatus status;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    @Column(name = "template_version_id")
    private UUID templateVersionId;

    @Column(name = "sample_results_json", columnDefinition = "TEXT")
    private String sampleResultsJson;

    @Column(name = "persistent_artifacts_json", columnDefinition = "TEXT")
    private String persistentArtifactsJson;

    @Column(name = "anchor_coverage_pct", precision = 5, scale = 2)
    private BigDecimal anchorCoveragePct;

    @Column(name = "variable_coverage_pct", precision = 5, scale = 2)
    private BigDecimal variableCoveragePct;

    @Column(name = "sample_coverage_pct", precision = 5, scale = 2)
    private BigDecimal sampleCoveragePct;

    @Column(name = "all_samples_succeeded")
    private Boolean allSamplesSucceeded;

    @Column(name = "gate_passed")
    private Boolean gatePassed;

    @Column(name = "hidden", nullable = false)
    private boolean hidden;

    protected BatchTestRunEntity() {
    }

    /**
     * Legacy constructor used by BatchTestGenerationService (synchronous batch).
     */
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
        this.status = BatchTestRunStatus.COMPLETED;
        this.createdAt = Instant.now();
        this.completedAt = Instant.now();
        this.hidden = false;
    }

    /**
     * Factory for async SSE batch runs; starts in RUNNING state.
     */
    public static BatchTestRunEntity startNew(
            UUID id,
            UUID templateId,
            UUID templateVersionId,
            String createdBy,
            int totalSamples
    ) {
        BatchTestRunEntity entity = new BatchTestRunEntity();
        entity.id = id;
        entity.templateId = templateId;
        entity.templateVersionId = templateVersionId;
        entity.createdBy = createdBy;
        entity.totalSamples = totalSamples;
        entity.succeededCount = 0;
        entity.failedCount = 0;
        entity.warningCount = 0;
        entity.blockerCount = 0;
        entity.summaryJson = "[]";
        entity.status = BatchTestRunStatus.RUNNING;
        entity.createdAt = Instant.now();
        entity.hidden = false;
        return entity;
    }

    public void completeRun(
            int succeededCount,
            int failedCount,
            int warningCount,
            int blockerCount,
            String sampleResultsJson,
            BigDecimal anchorCoveragePct,
            BigDecimal variableCoveragePct,
            BigDecimal sampleCoveragePct,
            boolean allSamplesSucceeded,
            boolean gatePassed
    ) {
        this.succeededCount = succeededCount;
        this.failedCount = failedCount;
        this.warningCount = warningCount;
        this.blockerCount = blockerCount;
        this.summaryJson = sampleResultsJson;
        this.sampleResultsJson = sampleResultsJson;
        this.anchorCoveragePct = anchorCoveragePct;
        this.variableCoveragePct = variableCoveragePct;
        this.sampleCoveragePct = sampleCoveragePct;
        this.allSamplesSucceeded = allSamplesSucceeded;
        this.gatePassed = gatePassed;
        this.status = BatchTestRunStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void failRun() {
        this.status = BatchTestRunStatus.FAILED;
        this.completedAt = Instant.now();
    }

    public void invalidate() {
        this.invalidatedAt = Instant.now();
    }

    public void hide() {
        this.hidden = true;
    }

    public void updateSummary(
            int totalSamples,
            int succeededCount,
            int failedCount,
            int warningCount,
            int blockerCount,
            String summaryJson
    ) {
        this.totalSamples = totalSamples;
        this.succeededCount = succeededCount;
        this.failedCount = failedCount;
        this.warningCount = warningCount;
        this.blockerCount = blockerCount;
        this.summaryJson = summaryJson;
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

    public BatchTestRunStatus getStatus() {
        return status;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getInvalidatedAt() {
        return invalidatedAt;
    }

    public UUID getTemplateVersionId() {
        return templateVersionId;
    }

    public String getSampleResultsJson() {
        return sampleResultsJson;
    }

    public BigDecimal getAnchorCoveragePct() {
        return anchorCoveragePct;
    }

    public BigDecimal getVariableCoveragePct() {
        return variableCoveragePct;
    }

    public BigDecimal getSampleCoveragePct() {
        return sampleCoveragePct;
    }

    public Boolean getAllSamplesSucceeded() {
        return allSamplesSucceeded;
    }

    public Boolean getGatePassed() {
        return gatePassed;
    }

    public boolean isHidden() {
        return hidden;
    }
}
