package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import com.bank.docgen.template.domain.ApprovalSubState;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.domain.TemplateVersionLineKind;
import java.time.Instant;
import java.util.List;

public record TemplateVersionLineDetailView(
        String devVersionId,
        int devVersionNumber,
        String releaseVersion,
        TemplateLifecycleStatus lifecycleStatus,
        ApprovalSubState approvalSubState,
        TemplateVersionLineKind lineKind,
        Instant updatedAt,
        String updatedBy,
        String updatedByDisplayName,
        Boolean defaultRouteTarget,
        boolean cloneable,
        List<VariableSchemaView> variables,
        List<AnchorBindingView> bindings,
        List<CompositionRuleView> rules
) {
    public TemplateVersionLineDetailView {
        variables = DefensiveCopies.copyList(variables);
        bindings = DefensiveCopies.copyList(bindings);
        rules = DefensiveCopies.copyList(rules);
    }

}
