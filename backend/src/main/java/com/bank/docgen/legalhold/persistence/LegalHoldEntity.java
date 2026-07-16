package com.bank.docgen.legalhold.persistence;

import com.bank.docgen.legalhold.domain.LegalHoldScopeType;
import com.bank.docgen.legalhold.domain.LegalHoldStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "legal_hold")
public class LegalHoldEntity {

    @Id
    private UUID id;

    @Column(name = "hold_external_id", nullable = false, unique = true, length = 64)
    private String holdExternalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 32)
    private LegalHoldScopeType scopeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LegalHoldStatus status;

    @Column(length = 512)
    private String reason;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "template_external_id", length = 128)
    private String templateExternalId;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by_username", nullable = false, length = 8)
    private String createdByUsername;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "released_by_username", length = 8)
    private String releasedByUsername;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "legal_hold_invocation",
            joinColumns = @JoinColumn(name = "hold_id")
    )
    @Column(name = "invocation_external_id", nullable = false, length = 64)
    private Set<String> invocationExternalIds = new HashSet<>();

    protected LegalHoldEntity() {
    }

    public LegalHoldEntity(
            UUID id,
            String holdExternalId,
            LegalHoldScopeType scopeType,
            LegalHoldStatus status,
            String reason,
            UUID templateId,
            String templateExternalId,
            Instant effectiveFrom,
            Instant effectiveTo,
            Instant createdAt,
            String createdByUsername,
            Set<String> invocationExternalIds
    ) {
        this.id = id;
        this.holdExternalId = holdExternalId;
        this.scopeType = scopeType;
        this.status = status;
        this.reason = reason;
        this.templateId = templateId;
        this.templateExternalId = templateExternalId;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdAt = createdAt;
        this.createdByUsername = createdByUsername;
        if (invocationExternalIds != null) {
            this.invocationExternalIds = new HashSet<>(invocationExternalIds);
        }
    }

    public void release(Instant releasedAt, String releasedByUsername) {
        this.status = LegalHoldStatus.RELEASED;
        this.releasedAt = releasedAt;
        this.releasedByUsername = releasedByUsername;
    }

    public UUID getId() {
        return id;
    }

    public String getHoldExternalId() {
        return holdExternalId;
    }

    public LegalHoldScopeType getScopeType() {
        return scopeType;
    }

    public LegalHoldStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public String getTemplateExternalId() {
        return templateExternalId;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return effectiveTo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedByUsername() {
        return createdByUsername;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public String getReleasedByUsername() {
        return releasedByUsername;
    }

    public Set<String> getInvocationExternalIds() {
        return Set.copyOf(invocationExternalIds);
    }

    public boolean containsInvocationExternalId(String invocationExternalId) {
        return invocationExternalIds.contains(invocationExternalId);
    }
}
