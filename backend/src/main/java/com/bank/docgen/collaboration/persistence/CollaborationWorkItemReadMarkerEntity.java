package com.bank.docgen.collaboration.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "collaboration_work_item_read_marker")
public class CollaborationWorkItemReadMarkerEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 8)
    private String userId;

    @Column(name = "work_item_id", nullable = false)
    private UUID workItemId;

    @Column(name = "read_at", nullable = false)
    private Instant readAt;

    protected CollaborationWorkItemReadMarkerEntity() {
    }

    public CollaborationWorkItemReadMarkerEntity(UUID id, String userId, UUID workItemId) {
        this.id = id;
        this.userId = userId;
        this.workItemId = workItemId;
        this.readAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public UUID getWorkItemId() {
        return workItemId;
    }

    public Instant getReadAt() {
        return readAt;
    }
}
