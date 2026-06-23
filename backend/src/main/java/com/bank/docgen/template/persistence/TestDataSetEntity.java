package com.bank.docgen.template.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "template_test_data_set")
public class TestDataSetEntity {

    @Id
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId;

    @Column(nullable = false, length = 256)
    private String name;

    @Column(length = 512)
    private String description;

    @Column(name = "variables_json", nullable = false, columnDefinition = "TEXT")
    private String variablesJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TestDataSetEntity() {
    }

    public TestDataSetEntity(
            UUID id,
            UUID templateId,
            String externalId,
            String name,
            String description,
            String variablesJson
    ) {
        this.id = id;
        this.templateId = templateId;
        this.externalId = externalId;
        this.name = name;
        this.description = description;
        this.variablesJson = variablesJson;
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

    public String getExternalId() {
        return externalId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getVariablesJson() {
        return variablesJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(String name, String description, String variablesJson) {
        this.name = name;
        this.description = description;
        this.variablesJson = variablesJson;
        this.updatedAt = Instant.now();
    }
}
