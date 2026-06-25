package com.bank.docgen.template.persistence;

import com.bank.docgen.template.domain.CoverageThresholdScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coverage_threshold_config")
public class CoverageThresholdConfigEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 16)
    private CoverageThresholdScope scopeType;

    @Column(name = "group_code", length = 64)
    private String groupCode;

    @Column(name = "min_required_variable_pct", nullable = false)
    private int minRequiredVariablePct;

    @Column(name = "min_required_sample_pct", nullable = false)
    private int minRequiredSamplePct;

    @Column(name = "min_anchor_binding_pct", nullable = false)
    private int minAnchorBindingPct;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CoverageThresholdConfigEntity() {
    }

    public CoverageThresholdConfigEntity(
            UUID id,
            CoverageThresholdScope scopeType,
            String groupCode,
            int minRequiredVariablePct,
            int minRequiredSamplePct,
            int minAnchorBindingPct
    ) {
        this.id = id;
        this.scopeType = scopeType;
        this.groupCode = groupCode;
        this.minRequiredVariablePct = minRequiredVariablePct;
        this.minRequiredSamplePct = minRequiredSamplePct;
        this.minAnchorBindingPct = minAnchorBindingPct;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public CoverageThresholdScope getScopeType() {
        return scopeType;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public int getMinRequiredVariablePct() {
        return minRequiredVariablePct;
    }

    public int getMinRequiredSamplePct() {
        return minRequiredSamplePct;
    }

    public int getMinAnchorBindingPct() {
        return minAnchorBindingPct;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
