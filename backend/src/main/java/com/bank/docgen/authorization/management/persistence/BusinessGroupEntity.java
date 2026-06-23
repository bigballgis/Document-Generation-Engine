package com.bank.docgen.authorization.management.persistence;

import com.bank.docgen.authorization.management.domain.GroupDimension;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "business_group")
public class BusinessGroupEntity {

    @Id
    private UUID id;

    @Column(name = "group_code", nullable = false, length = 64, updatable = false)
    private String groupCode;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "dimension", nullable = false, length = 32, updatable = false)
    private GroupDimension dimension;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected BusinessGroupEntity() {
    }

    public BusinessGroupEntity(UUID id, String groupCode, String displayName, GroupDimension dimension) {
        this.id = id;
        this.groupCode = groupCode;
        this.displayName = displayName;
        this.dimension = dimension;
        this.enabled = true;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void rename(String newDisplayName) {
        this.displayName = newDisplayName;
        touch();
    }

    public void disable() {
        this.enabled = false;
        touch();
    }

    public void enable() {
        this.enabled = true;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public GroupDimension getDimension() {
        return dimension;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
