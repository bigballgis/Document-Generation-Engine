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
        List<TemplateExportAssetKeyManifestItemView> assetKeyManifest,
        List<CompositionInclusionRuleView> compositionInclusionRules,
        TemplateExportClauseNestingGraphView clauseNestingGraph,
        String dependencyClosure
) {
    public TemplateExportBundleView {
        variables = DefensiveCopies.copyList(variables);
        bindings = DefensiveCopies.copyList(bindings);
        rules = DefensiveCopies.copyList(rules);
        contentModuleReferences = DefensiveCopies.copyList(contentModuleReferences);
        clauseSnapshots = DefensiveCopies.copyList(clauseSnapshots);
        assetKeyManifest = DefensiveCopies.copyList(assetKeyManifest);
        compositionInclusionRules = DefensiveCopies.copyList(compositionInclusionRules);
    }

    /** v1-compatible constructor (no CE-E01 v2 fields / inclusion rules). */
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
                null,
                null,
                null,
                null
        );
    }

    /** CE-E01 v2 constructor without inclusion rules / nesting graph. */
    public TemplateExportBundleView(
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
        this(
                format,
                metadata,
                variables,
                bindings,
                rules,
                contentModuleReferences,
                policySnapshot,
                masterPin,
                clauseSnapshots,
                renderProfile,
                assetKeyManifest,
                null,
                null,
                null
        );
    }

    /** CE-E01 v2 + inclusion rules (pre-Wave-7). */
    public TemplateExportBundleView(
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
            List<TemplateExportAssetKeyManifestItemView> assetKeyManifest,
            List<CompositionInclusionRuleView> compositionInclusionRules
    ) {
        this(
                format,
                metadata,
                variables,
                bindings,
                rules,
                contentModuleReferences,
                policySnapshot,
                masterPin,
                clauseSnapshots,
                renderProfile,
                assetKeyManifest,
                compositionInclusionRules,
                null,
                null
        );
    }

    /** Wave 7: nesting graph without explicit dependencyClosure (legacy / tests). */
    public TemplateExportBundleView(
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
            List<TemplateExportAssetKeyManifestItemView> assetKeyManifest,
            List<CompositionInclusionRuleView> compositionInclusionRules,
            TemplateExportClauseNestingGraphView clauseNestingGraph
    ) {
        this(
                format,
                metadata,
                variables,
                bindings,
                rules,
                contentModuleReferences,
                policySnapshot,
                masterPin,
                clauseSnapshots,
                renderProfile,
                assetKeyManifest,
                compositionInclusionRules,
                clauseNestingGraph,
                null
        );
    }
}
