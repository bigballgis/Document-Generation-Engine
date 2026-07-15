package com.bank.docgen.template.api;

/**
 * CE-E01 clause body/structure snapshot embedded in a v2 export bundle.
 *
 * <p>{@code semanticVersion} preserves full semver fidelity (e.g. {@code 1.2.3}).
 * {@code sourceModuleId} is the export-time module UUID used to remap
 * {@code contentModuleReferences} on a fresh target without treating source UUIDs
 * as target identities.
 */
public record TemplateExportClauseSnapshotView(
        String moduleCode,
        String moduleVersionId,
        int versionNumber,
        String contentStructureJson,
        Boolean locked,
        String jurisdiction,
        String effectiveFrom,
        String effectiveTo,
        String legalReviewRef,
        String semanticVersion,
        String sourceModuleId
) {
}
