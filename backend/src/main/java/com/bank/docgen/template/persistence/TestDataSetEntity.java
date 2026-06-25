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

    @Column(nullable = false)
    private boolean required;

    @Column(name = "scenario_name", length = 256)
    private String scenarioName;

    @Column(name = "coverage_tags_json", nullable = false, columnDefinition = "TEXT")
    private String coverageTagsJson;

    @Column(name = "dataset_version", nullable = false)
    private int datasetVersion;

    @Column(nullable = false)
    private boolean locked;

    @Column(name = "derived_from_id")
    private UUID derivedFromId;

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
            String variablesJson,
            boolean required,
            String scenarioName,
            String coverageTagsJson,
            int datasetVersion,
            boolean locked,
            UUID derivedFromId
    ) {
        this.id = id;
        this.templateId = templateId;
        this.externalId = externalId;
        this.name = name;
        this.description = description;
        this.variablesJson = variablesJson;
        this.required = required;
        this.scenarioName = scenarioName;
        this.coverageTagsJson = coverageTagsJson;
        this.datasetVersion = datasetVersion;
        this.locked = locked;
        this.derivedFromId = derivedFromId;
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

    public boolean isRequired() {
        return required;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public String getCoverageTagsJson() {
        return coverageTagsJson;
    }

    public int getDatasetVersion() {
        return datasetVersion;
    }

    public boolean isLocked() {
        return locked;
    }

    public UUID getDerivedFromId() {
        return derivedFromId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(
            String name,
            String description,
            String variablesJson,
            boolean required,
            String scenarioName,
            String coverageTagsJson
    ) {
        this.name = name;
        this.description = description;
        this.variablesJson = variablesJson;
        this.required = required;
        this.scenarioName = scenarioName;
        this.coverageTagsJson = coverageTagsJson;
        this.updatedAt = Instant.now();
    }

    public void lockForEvidence() {
        this.locked = true;
        this.updatedAt = Instant.now();
    }
}
