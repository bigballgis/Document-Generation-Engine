package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import com.bank.docgen.apimgmt.api.ApiPolicyView;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TemplateExportBundleView(
        String format,
        TemplateExportMetadataView metadata,
        List<VariableSchemaView> variables,
        List<AnchorBindingView> bindings,
        List<CompositionRuleView> rules,
        List<ContentModuleReferenceView> contentModuleReferences,
        ApiPolicyView policySnapshot,
        TemplateExportMasterPinView masterPin,
        List<TemplateExportClauseSnapshotView> clauseSnapshots,
        TemplateExportRenderProfileView renderProfile,
        List<TemplateExportAssetKeyManifestItemView> assetKeyManifest
) {
    public TemplateExportBundleView {
        variables = DefensiveCopies.copyList(variables);
        bindings = DefensiveCopies.copyList(bindings);
        rules = DefensiveCopies.copyList(rules);
        contentModuleReferences = DefensiveCopies.copyList(contentModuleReferences);
        clauseSnapshots = DefensiveCopies.copyList(clauseSnapshots);
        assetKeyManifest = DefensiveCopies.copyList(assetKeyManifest);
    }

    /** v1-compatible constructor (no CE-E01 v2 fields). */
    public TemplateExportBundleView(
            String format,
            TemplateExportMetadataView metadata,
            List<VariableSchemaView> variables,
            List<AnchorBindingView> bindings,
            List<CompositionRuleView> rules,
            List<ContentModuleReferenceView> contentModuleReferences,
            ApiPolicyView policySnapshot
    ) {
        this(
                format,
                metadata,
                variables,
                bindings,
                rules,
                contentModuleReferences,
                policySnapshot,
                null,
                null,
                null,
                null
        );
    }
}
