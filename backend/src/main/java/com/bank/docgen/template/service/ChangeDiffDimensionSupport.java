package com.bank.docgen.template.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.template.api.ChangeDiffDimensionView;
import com.bank.docgen.template.api.ChangeDiffHumanReadableEntry;
import com.bank.docgen.template.api.ChangeDiffModificationView;
import com.bank.docgen.template.api.CompositionRuleView;
import com.bank.docgen.template.domain.ChangeDiffDimension;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceEntity;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Package-private dimension diffs for change-diff computation.
 */
final class ChangeDiffDimensionSupport {

    private final ApiPolicyRepository apiPolicyRepository;
    private final TemplateContentModuleReferenceRepository contentModuleReferenceRepository;
    private final ContentModuleVersionRepository contentModuleVersionRepository;
    private final ChangeDiffDimensionHelperSupport helpers;
    private final SemanticContentDiffEngine semanticContentDiffEngine;

    ChangeDiffDimensionSupport(
            VariableSchemaRepository variableSchemaRepository,
            AnchorBindingRepository anchorBindingRepository,
            ApiPolicyRepository apiPolicyRepository,
            TemplateContentModuleReferenceRepository contentModuleReferenceRepository,
            ContentModuleVersionRepository contentModuleVersionRepository,
            ObjectMapper objectMapper
    ) {
        this.apiPolicyRepository = apiPolicyRepository;
        this.contentModuleReferenceRepository = contentModuleReferenceRepository;
        this.contentModuleVersionRepository = contentModuleVersionRepository;
        this.helpers = new ChangeDiffDimensionHelperSupport(
                variableSchemaRepository, anchorBindingRepository, objectMapper);
        this.semanticContentDiffEngine = new SemanticContentDiffEngine(objectMapper);
    }

    DimensionBuildResult buildDimensions(
            UUID templateId, TemplateVersionEntity candidate, TemplateVersionEntity baseline) {
        List<ChangeDiffDimensionView> dimensions = new ArrayList<>();
        ContentDiffResult content = diffContent(candidate, baseline);
        dimensions.add(content.dimension());
        dimensions.add(diffAnchors(candidate.getId(), baseline == null ? null : baseline.getId()));
        dimensions.add(diffVariables(candidate.getId(), baseline == null ? null : baseline.getId()));
        dimensions.add(diffRules(candidate, baseline));
        dimensions.add(diffContractSummary(templateId, baseline));
        return new DimensionBuildResult(dimensions, content.humanReadableEntries());
    }

    private ContentDiffResult diffContent(TemplateVersionEntity candidate, TemplateVersionEntity baseline) {
        if (baseline == null) {
            return new ContentDiffResult(
                    helpers.emptyDimension(ChangeDiffDimension.CONTENT),
                    List.of()
            );
        }
        Map<String, String> baselineTrees = loadAnchorTrees(baseline.getId());
        Map<String, String> candidateTrees = loadAnchorTrees(candidate.getId());
        SemanticContentDiffEngine.Result semantic = semanticContentDiffEngine.diffAnchors(
                baselineTrees, candidateTrees);

        List<String> added = new ArrayList<>(semantic.added());
        List<String> removed = new ArrayList<>(semantic.removed());
        List<ChangeDiffModificationView> modified = new ArrayList<>(semantic.modified());
        List<ChangeDiffHumanReadableEntry> entries = new ArrayList<>(semantic.entries());

        appendClauseReferenceDiffs(baseline.getId(), candidate.getId(), added, removed, modified, entries);

        if (!Objects.equals(candidate.getMasterCatalogVersion(), baseline.getMasterCatalogVersion())) {
            String summary = "Master catalog version '" + baseline.getMasterCatalogVersion()
                    + "' → '" + candidate.getMasterCatalogVersion() + "'";
            modified.add(new ChangeDiffModificationView("masterCatalogVersion", "MODIFIED", summary));
            entries.add(new ChangeDiffHumanReadableEntry("MODIFIED", "masterCatalogVersion", summary));
        }

        return new ContentDiffResult(
                new ChangeDiffDimensionView(ChangeDiffDimension.CONTENT, added, removed, modified),
                List.copyOf(entries)
        );
    }

    private void appendClauseReferenceDiffs(
            UUID baselineVersionId,
            UUID candidateVersionId,
            List<String> added,
            List<String> removed,
            List<ChangeDiffModificationView> modified,
            List<ChangeDiffHumanReadableEntry> entries
    ) {
        Map<String, String> baselineRefs = loadClauseVersions(baselineVersionId);
        Map<String, String> candidateRefs = loadClauseVersions(candidateVersionId);
        Set<String> keys = new TreeSetUnion(baselineRefs.keySet(), candidateRefs.keySet());
        for (String key : keys) {
            String baselineVersion = baselineRefs.get(key);
            String candidateVersion = candidateRefs.get(key);
            String path = "contentModuleRef:" + key;
            if (baselineVersion == null) {
                String summary = key + ": content-module reference added (" + candidateVersion + ")";
                added.add(path);
                entries.add(new ChangeDiffHumanReadableEntry("ADDED", path, summary));
                continue;
            }
            if (candidateVersion == null) {
                String summary = key + ": content-module reference removed (" + baselineVersion + ")";
                removed.add(path);
                entries.add(new ChangeDiffHumanReadableEntry("REMOVED", path, summary));
                continue;
            }
            if (!baselineVersion.equals(candidateVersion)) {
                String summary = key + ": content-module reference version "
                        + baselineVersion + " → " + candidateVersion;
                modified.add(new ChangeDiffModificationView(path, "MODIFIED", summary));
                entries.add(new ChangeDiffHumanReadableEntry("MODIFIED", path, summary));
            }
        }
    }

    private Map<String, String> loadClauseVersions(UUID templateVersionId) {
        Map<String, String> versions = new LinkedHashMap<>();
        for (TemplateContentModuleReferenceEntity reference
                : contentModuleReferenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(templateVersionId)) {
            ContentModuleVersionEntity moduleVersion = contentModuleVersionRepository
                    .findById(reference.getContentModuleVersionId())
                    .orElse(null);
            if (moduleVersion != null) {
                versions.put(reference.getReferenceKey(), moduleVersion.getSemanticVersion());
            }
        }
        return versions;
    }

    private Map<String, String> loadAnchorTrees(UUID versionId) {
        Map<String, String> trees = new LinkedHashMap<>();
        for (AnchorBindingEntity binding
                : helpers.anchorBindings(versionId)) {
            trees.put(binding.getAnchorId(), binding.getStructuredContentJson());
        }
        return trees;
    }

    private ChangeDiffDimensionView diffAnchors(UUID candidateVersionId, UUID baselineVersionId) {
        if (baselineVersionId == null) {
            List<String> added = helpers.anchorHashes(candidateVersionId).keySet().stream().sorted().toList();
            return new ChangeDiffDimensionView(ChangeDiffDimension.ANCHORS, added, List.of(), List.of());
        }
        Map<String, String> candidateHashes = helpers.anchorHashes(candidateVersionId);
        Map<String, String> baselineHashes = helpers.anchorHashes(baselineVersionId);
        Set<String> candidateIds = candidateHashes.keySet();
        Set<String> baselineIds = baselineHashes.keySet();
        List<String> added = candidateIds.stream().filter(id -> !baselineIds.contains(id)).sorted().toList();
        List<String> removed = baselineIds.stream().filter(id -> !candidateIds.contains(id)).sorted().toList();
        List<ChangeDiffModificationView> modified = new ArrayList<>();
        for (String anchorId : candidateIds) {
            if (baselineIds.contains(anchorId) && !candidateHashes.get(anchorId).equals(baselineHashes.get(anchorId))) {
                modified.add(new ChangeDiffModificationView(
                        anchorId, "MODIFIED", "bindingHash=" + candidateHashes.get(anchorId)));
            }
        }
        return new ChangeDiffDimensionView(ChangeDiffDimension.ANCHORS, added, removed, modified);
    }

    private ChangeDiffDimensionView diffVariables(UUID candidateVersionId, UUID baselineVersionId) {
        Map<String, VariableSchemaEntity> candidateVars = helpers.variableMap(candidateVersionId);
        if (baselineVersionId == null) {
            return new ChangeDiffDimensionView(
                    ChangeDiffDimension.VARIABLES,
                    candidateVars.keySet().stream().sorted().toList(), List.of(), List.of());
        }
        Map<String, VariableSchemaEntity> baselineVars = helpers.variableMap(baselineVersionId);
        Set<String> candidateKeys = candidateVars.keySet();
        Set<String> baselineKeys = baselineVars.keySet();
        List<String> added = candidateKeys.stream().filter(key -> !baselineKeys.contains(key)).sorted().toList();
        List<String> removed = baselineKeys.stream().filter(key -> !candidateKeys.contains(key)).sorted().toList();
        List<ChangeDiffModificationView> modified = new ArrayList<>();
        for (String key : candidateKeys) {
            if (!baselineKeys.contains(key)) {
                continue;
            }
            VariableSchemaEntity candidateVar = candidateVars.get(key);
            VariableSchemaEntity baselineVar = baselineVars.get(key);
            if (candidateVar.isRequired() != baselineVar.isRequired()
                    || candidateVar.getVariableType() != baselineVar.getVariableType()) {
                modified.add(new ChangeDiffModificationView(key, "MODIFIED", "schemaChanged"));
            }
        }
        return new ChangeDiffDimensionView(ChangeDiffDimension.VARIABLES, added, removed, modified);
    }

    private ChangeDiffDimensionView diffRules(TemplateVersionEntity candidate, TemplateVersionEntity baseline) {
        List<CompositionRuleView> candidateRules = helpers.readRules(candidate.getRulesJson());
        if (baseline == null) {
            return new ChangeDiffDimensionView(
                    ChangeDiffDimension.RULES,
                    candidateRules.stream().map(CompositionRuleView::ruleId).sorted().toList(),
                    List.of(), List.of());
        }
        Map<String, CompositionRuleView> candidateMap = helpers.ruleMap(candidateRules);
        Map<String, CompositionRuleView> baselineMap = helpers.ruleMap(helpers.readRules(baseline.getRulesJson()));
        Set<String> candidateIds = candidateMap.keySet();
        Set<String> baselineIds = baselineMap.keySet();
        List<String> added = candidateIds.stream().filter(id -> !baselineIds.contains(id)).sorted().toList();
        List<String> removed = baselineIds.stream().filter(id -> !candidateIds.contains(id)).sorted().toList();
        List<ChangeDiffModificationView> modified = new ArrayList<>();
        for (String ruleId : candidateIds) {
            if (!baselineIds.contains(ruleId)) {
                continue;
            }
            if (!helpers.ruleSignature(candidateMap.get(ruleId)).equals(helpers.ruleSignature(baselineMap.get(ruleId)))) {
                modified.add(new ChangeDiffModificationView(ruleId, "MODIFIED", "ruleExpressionChanged"));
            }
        }
        return new ChangeDiffDimensionView(ChangeDiffDimension.RULES, added, removed, modified);
    }

    private ChangeDiffDimensionView diffContractSummary(UUID templateId, TemplateVersionEntity baseline) {
        ApiPolicyEntity policy = apiPolicyRepository.findByTemplateId(templateId).orElse(null);
        if (policy == null) {
            return helpers.emptyDimension(ChangeDiffDimension.CONTRACT_SUMMARY);
        }
        if (baseline == null) {
            return new ChangeDiffDimensionView(
                    ChangeDiffDimension.CONTRACT_SUMMARY, List.of("apiPolicyConfigured"), List.of(), List.of());
        }
        return helpers.emptyDimension(ChangeDiffDimension.CONTRACT_SUMMARY);
    }

    record DimensionBuildResult(
            List<ChangeDiffDimensionView> dimensions,
            List<ChangeDiffHumanReadableEntry> humanReadableEntries
    ) {
    }

    private record ContentDiffResult(
            ChangeDiffDimensionView dimension,
            List<ChangeDiffHumanReadableEntry> humanReadableEntries
    ) {
    }

    /** Tiny sorted union helper to avoid an extra import-heavy utility. */
    private static final class TreeSetUnion extends java.util.TreeSet<String> {
        private TreeSetUnion(Set<String> left, Set<String> right) {
            super(left);
            addAll(right);
        }
    }
}
