package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.ApprovalSubState;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import java.time.Instant;
import java.util.List;

public record TemplateDetailView(
        String id,
        String externalId,
        String groupCode,
        String name,
        String description,
        String masterId,
        TemplateLifecycleStatus lifecycleStatus,
        ApprovalSubState approvalSubState,
        String releaseVersion,
        String devVersionId,
        int devVersionNumber,
        List<VariableSchemaView> variables,
        List<AnchorBindingView> bindings,
        List<CompositionRuleView> rules,
        Instant createdAt,
        Instant updatedAt
) {
}
