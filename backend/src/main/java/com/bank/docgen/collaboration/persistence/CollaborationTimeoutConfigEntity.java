package com.bank.docgen.collaboration.persistence;

import com.bank.docgen.collaboration.domain.CollaborationTimeoutScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "collaboration_timeout_config")
public class CollaborationTimeoutConfigEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 16)
    private CollaborationTimeoutScope scopeType;

    @Column(name = "group_code", length = 64)
    private String groupCode;

    @Column(name = "test_threshold_hours", nullable = false)
    private int testThresholdHours;

    @Column(name = "approval_threshold_hours", nullable = false)
    private int approvalThresholdHours;

    @Column(name = "pending_release_threshold_hours", nullable = false)
    private int pendingReleaseThresholdHours;

    @Column(name = "remediation_threshold_hours", nullable = false)
    private int remediationThresholdHours;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CollaborationTimeoutConfigEntity() {
    }

    public CollaborationTimeoutConfigEntity(
            UUID id,
            CollaborationTimeoutScope scopeType,
            String groupCode,
            int testThresholdHours,
            int approvalThresholdHours,
            int pendingReleaseThresholdHours,
            int remediationThresholdHours
    ) {
        this.id = id;
        this.scopeType = scopeType;
        this.groupCode = groupCode;
        this.testThresholdHours = testThresholdHours;
        this.approvalThresholdHours = approvalThresholdHours;
        this.pendingReleaseThresholdHours = pendingReleaseThresholdHours;
        this.remediationThresholdHours = remediationThresholdHours;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public CollaborationTimeoutScope getScopeType() {
        return scopeType;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public int getTestThresholdHours() {
        return testThresholdHours;
    }

    public int getApprovalThresholdHours() {
        return approvalThresholdHours;
    }

    public int getPendingReleaseThresholdHours() {
        return pendingReleaseThresholdHours;
    }

    public int getRemediationThresholdHours() {
        return remediationThresholdHours;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(
            int testThresholdHours,
            int approvalThresholdHours,
            int pendingReleaseThresholdHours,
            int remediationThresholdHours
    ) {
        this.testThresholdHours = testThresholdHours;
        this.approvalThresholdHours = approvalThresholdHours;
        this.pendingReleaseThresholdHours = pendingReleaseThresholdHours;
        this.remediationThresholdHours = remediationThresholdHours;
        this.updatedAt = Instant.now();
    }
}
