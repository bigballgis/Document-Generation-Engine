package com.bank.docgen.contentmodule.persistence;

import com.bank.docgen.contentmodule.domain.ContentModuleReviewAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "content_module_review_record")
public class ContentModuleReviewRecordEntity {

    @Id
    private UUID id;

    @Column(name = "module_id", nullable = false)
    private UUID moduleId;

    @Column(name = "version_id")
    private UUID versionId;

    @Column(name = "semantic_version", length = 32)
    private String semanticVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ContentModuleReviewAction action;

    @Column(length = 32)
    private String decision;

    @Column(name = "change_summary", length = 2048)
    private String changeSummary;

    @Column(name = "comment_summary", length = 2048)
    private String commentSummary;

    @Column(name = "actor_username", nullable = false, length = 8)
    private String actorUsername;

    @Column(name = "self_approval_exception", nullable = false)
    private boolean selfApprovalException;

    @Column(name = "exception_reason", length = 2048)
    private String exceptionReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ContentModuleReviewRecordEntity() {
    }

    public ContentModuleReviewRecordEntity(
            UUID id,
            UUID moduleId,
            UUID versionId,
            String semanticVersion,
            ContentModuleReviewAction action,
            String decision,
            String changeSummary,
            String commentSummary,
            String actorUsername,
            boolean selfApprovalException,
            String exceptionReason
    ) {
        this.id = id;
        this.moduleId = moduleId;
        this.versionId = versionId;
        this.semanticVersion = semanticVersion;
        this.action = action;
        this.decision = decision;
        this.changeSummary = changeSummary;
        this.commentSummary = commentSummary;
        this.actorUsername = actorUsername;
        this.selfApprovalException = selfApprovalException;
        this.exceptionReason = exceptionReason;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getModuleId() {
        return moduleId;
    }

    public UUID getVersionId() {
        return versionId;
    }

    public String getSemanticVersion() {
        return semanticVersion;
    }

    public ContentModuleReviewAction getAction() {
        return action;
    }

    public String getDecision() {
        return decision;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public String getCommentSummary() {
        return commentSummary;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public boolean isSelfApprovalException() {
        return selfApprovalException;
    }

    public String getExceptionReason() {
        return exceptionReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
