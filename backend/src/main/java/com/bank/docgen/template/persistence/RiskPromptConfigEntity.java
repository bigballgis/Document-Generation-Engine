package com.bank.docgen.template.persistence;

import com.bank.docgen.template.domain.RiskPromptScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "risk_prompt_config")
public class RiskPromptConfigEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 16)
    private RiskPromptScope scopeType;

    @Column(name = "group_code", length = 64)
    private String groupCode;

    @Column(name = "reason_categories_json", nullable = false)
    private String reasonCategoriesJson;

    @Column(name = "risk_prompt_copy_json", nullable = false)
    private String riskPromptCopyJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RiskPromptConfigEntity() {
    }

    public RiskPromptConfigEntity(
            UUID id,
            RiskPromptScope scopeType,
            String groupCode,
            String reasonCategoriesJson,
            String riskPromptCopyJson
    ) {
        this.id = id;
        this.scopeType = scopeType;
        this.groupCode = groupCode;
        this.reasonCategoriesJson = reasonCategoriesJson;
        this.riskPromptCopyJson = riskPromptCopyJson;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public RiskPromptScope getScopeType() {
        return scopeType;
    }

    public String getGroupCode() {
        return groupCode;
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
