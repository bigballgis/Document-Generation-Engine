package com.bank.docgen.template.persistence;

import com.bank.docgen.template.domain.VariableType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "variable_schema")
public class VariableSchemaEntity {

    @Id
    private UUID id;

    @Column(name = "template_version_id", nullable = false)
    private UUID templateVersionId;

    @Column(name = "variable_key", nullable = false, length = 128)
    private String variableKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "variable_type", nullable = false, length = 32)
    private VariableType variableType;

    @Column(name = "required_flag", nullable = false)
    private boolean required;

    @Column(name = "default_value")
    private String defaultValue;

    @Column(name = "enum_values")
    private String enumValues;

    @Column(length = 512)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VariableSchemaEntity() {
    }

    public VariableSchemaEntity(
            UUID id,
            UUID templateVersionId,
            String variableKey,
            VariableType variableType,
            boolean required,
            String defaultValue,
            String enumValues,
            String description
    ) {
        this.id = id;
        this.templateVersionId = templateVersionId;
        this.variableKey = variableKey;
        this.variableType = variableType;
        this.required = required;
        this.defaultValue = defaultValue;
        this.enumValues = enumValues;
        this.description = description;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTemplateVersionId() {
        return templateVersionId;
    }

    public String getVariableKey() {
        return variableKey;
    }

    public VariableType getVariableType() {
        return variableType;
    }

    public boolean isRequired() {
        return required;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getEnumValues() {
        return enumValues;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(
            VariableType variableType,
            boolean required,
            String defaultValue,
            String enumValues,
            String description
    ) {
        this.variableType = variableType;
        this.required = required;
        this.defaultValue = defaultValue;
        this.enumValues = enumValues;
        this.description = description;
        this.updatedAt = Instant.now();
    }
}
