package com.bank.docgen.master.persistence;

import com.bank.docgen.master.domain.MasterReviewAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "master_review_record")
public class MasterReviewRecordEntity {

    @Id
    private UUID id;

    @Column(name = "master_id", nullable = false)
    private UUID masterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MasterReviewAction action;

    @Column(length = 32)
    private String decision;

    @Column(name = "change_summary", length = 2048)
    private String changeSummary;

    @Column(name = "comment_summary", length = 2048)
    private String commentSummary;

    @Column(name = "actor_username", nullable = false, length = 8)
    private String actorUsername;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MasterReviewRecordEntity() {
    }

    public MasterReviewRecordEntity(
            UUID id,
            UUID masterId,
            MasterReviewAction action,
            String decision,
            String changeSummary,
            String commentSummary,
            String actorUsername
    ) {
        this.id = id;
        this.masterId = masterId;
        this.action = action;
        this.decision = decision;
        this.changeSummary = changeSummary;
        this.commentSummary = commentSummary;
        this.actorUsername = actorUsername;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getMasterId() {
        return masterId;
    }

    public MasterReviewAction getAction() {
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

    public Instant getCreatedAt() {
        return createdAt;
    }
}
