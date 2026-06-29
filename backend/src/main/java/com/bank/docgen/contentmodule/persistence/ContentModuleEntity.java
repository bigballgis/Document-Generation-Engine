package com.bank.docgen.contentmodule.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "content_module")
public class ContentModuleEntity {

    @Id
    private UUID id;

    @Column(name = "module_code", nullable = false, length = 128)
    private String moduleCode;

    @Column(name = "group_code", nullable = false, length = 64)
    private String groupCode;

    @Column(nullable = false, length = 256)
    private String name;

    @Column(length = 1024)
    private String description;

    @Column(name = "shared_group_codes_json", nullable = false)
    private String sharedGroupCodesJson;

    @Column(name = "created_by", nullable = false, length = 8)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 8)
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected ContentModuleEntity() {
    }

    public ContentModuleEntity(
            UUID id,
            String moduleCode,
            String groupCode,
            String name,
            String description,
            String sharedGroupCodesJson,
            String createdBy
    ) {
        this.id = id;
        this.moduleCode = moduleCode;
        this.groupCode = groupCode;
        this.name = name;
        this.description = description;
        this.sharedGroupCodesJson = sharedGroupCodesJson;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSharedGroupCodesJson() {
        return sharedGroupCodesJson;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
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

    public void setName(String name) {
        this.name = name;
        this.updatedAt = Instant.now();
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public void setSharedGroupCodesJson(String sharedGroupCodesJson) {
        this.sharedGroupCodesJson = sharedGroupCodesJson;
        this.updatedAt = Instant.now();
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
        this.updatedAt = Instant.now();
    }
}
