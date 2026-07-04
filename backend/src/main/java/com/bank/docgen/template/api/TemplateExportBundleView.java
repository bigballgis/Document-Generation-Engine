package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import com.bank.docgen.apimgmt.api.ApiPolicyView;
import java.util.List;

public record TemplateExportBundleView(
        String format,
        TemplateExportMetadataView metadata,
        List<VariableSchemaView> variables,
        List<AnchorBindingView> bindings,
        List<CompositionRuleView> rules,
        List<ContentModuleReferenceView> contentModuleReferences,
        ApiPolicyView policySnapshot
) {
    public TemplateExportBundleView {
        variables = DefensiveCopies.copyList(variables);
        bindings = DefensiveCopies.copyList(bindings);
        rules = DefensiveCopies.copyList(rules);
        contentModuleReferences = DefensiveCopies.copyList(contentModuleReferences);
    }

}
