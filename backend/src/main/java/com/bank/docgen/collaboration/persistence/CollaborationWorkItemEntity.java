package com.bank.docgen.collaboration.persistence;

import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemStatus;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemTriggerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "collaboration_work_item")
public class CollaborationWorkItemEntity {

    @Id
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "template_external_id", nullable = false, length = 128)
    private String templateExternalId;

    @Column(name = "template_name", nullable = false, length = 256)
    private String templateName;

    @Column(name = "group_code", nullable = false, length = 64)
    private String groupCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "queue_type", nullable = false, length = 32)
    private CollaborationWorkItemQueue queue;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 64)
    private CollaborationWorkItemTriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CollaborationWorkItemStatus status;

    @Column(name = "submitter_user_id", nullable = false, length = 8)
    private String submitterUserId;

    @Column(name = "summary_text", nullable = false, length = 512)
    private String summaryText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "source_work_item_id")
    private UUID sourceWorkItemId;

    protected CollaborationWorkItemEntity() {
    }

    public CollaborationWorkItemEntity(
            UUID id,
            UUID templateId,
            String templateExternalId,
            String templateName,
            String groupCode,
            CollaborationWorkItemQueue queue,
            CollaborationWorkItemTriggerType triggerType,
            CollaborationWorkItemStatus status,
            String submitterUserId,
            String summaryText
    ) {
        this.id = id;
        this.templateId = templateId;
        this.templateExternalId = templateExternalId;
        this.templateName = templateName;
        this.groupCode = groupCode;
        this.queue = queue;
        this.triggerType = triggerType;
        this.status = status;
        this.submitterUserId = submitterUserId;
        this.summaryText = summaryText;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public String getTemplateExternalId() {
        return templateExternalId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public CollaborationWorkItemQueue getQueue() {
        return queue;
    }

    public CollaborationWorkItemTriggerType getTriggerType() {
        return triggerType;
    }

    public CollaborationWorkItemStatus getStatus() {
        return status;
    }

    public void setStatus(CollaborationWorkItemStatus status) {
        this.status = status;
    }

    public void setTriggerType(CollaborationWorkItemTriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public String getSubmitterUserId() {
        return submitterUserId;
    }

    public void setSubmitterUserId(String submitterUserId) {
        this.submitterUserId = submitterUserId;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public UUID getSourceWorkItemId() {
        return sourceWorkItemId;
    }

    public void setSourceWorkItemId(UUID sourceWorkItemId) {
        this.sourceWorkItemId = sourceWorkItemId;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
