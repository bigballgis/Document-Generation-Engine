package com.bank.docgen.template.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "template_risk_prompt_override")
public class TemplateRiskPromptOverrideEntity {

    @Id
    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "reason_categories_json", nullable = false)
    private String reasonCategoriesJson;

    @Column(name = "risk_prompt_copy_json", nullable = false)
    private String riskPromptCopyJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TemplateRiskPromptOverrideEntity() {
    }

    public TemplateRiskPromptOverrideEntity(
            UUID templateId,
            String reasonCategoriesJson,
            String riskPromptCopyJson
    ) {
        this.templateId = templateId;
        this.reasonCategoriesJson = reasonCategoriesJson;
        this.riskPromptCopyJson = riskPromptCopyJson;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public String getReasonCategoriesJson() {
        return reasonCategoriesJson;
    }

    public String getRiskPromptCopyJson() {
        return riskPromptCopyJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(String reasonCategoriesJson, String riskPromptCopyJson) {
        this.reasonCategoriesJson = reasonCategoriesJson;
        this.riskPromptCopyJson = riskPromptCopyJson;
        this.updatedAt = Instant.now();
    }
}
