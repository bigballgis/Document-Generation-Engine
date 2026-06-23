package com.bank.docgen.template.persistence;

import com.bank.docgen.template.domain.LifecycleAction;
import com.bank.docgen.template.domain.LifecycleDecision;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "template_lifecycle_record")
public class TemplateLifecycleRecordEntity {

    @Id
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private LifecycleAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 32)
    private TemplateLifecycleStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 32)
    private TemplateLifecycleStatus toStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private LifecycleDecision decision;

    @Column(name = "comment_summary", length = 2048)
    private String commentSummary;

    @Column(name = "release_version", length = 32)
    private String releaseVersion;

    @Column(name = "actor_username", nullable = false, length = 8)
    private String actorUsername;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TemplateLifecycleRecordEntity() {
    }

    public TemplateLifecycleRecordEntity(
            UUID id,
            UUID templateId,
            LifecycleAction action,
            TemplateLifecycleStatus fromStatus,
            TemplateLifecycleStatus toStatus,
            LifecycleDecision decision,
            String commentSummary,
            String releaseVersion,
            String actorUsername
    ) {
        this.id = id;
        this.templateId = templateId;
        this.action = action;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.decision = decision;
        this.commentSummary = commentSummary;
        this.releaseVersion = releaseVersion;
        this.actorUsername = actorUsername;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public LifecycleAction getAction() {
        return action;
    }

    public TemplateLifecycleStatus getFromStatus() {
        return fromStatus;
    }

    public TemplateLifecycleStatus getToStatus() {
        return toStatus;
    }

    public LifecycleDecision getDecision() {
        return decision;
    }

    public String getCommentSummary() {
        return commentSummary;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
