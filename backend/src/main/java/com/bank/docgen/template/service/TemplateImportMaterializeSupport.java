package com.bank.docgen.template.service;

import com.bank.docgen.contentmodule.api.ContentModuleDetailView;
import com.bank.docgen.contentmodule.api.CreateContentModuleRequest;
import com.bank.docgen.contentmodule.api.CreateContentModuleVersionRequest;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.contentmodule.service.ContentModuleService;
import com.bank.docgen.contentmodule.service.ContentModuleValidationException;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.ContentModuleReferenceView;
import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.api.TemplateExportClauseSnapshotView;
import com.bank.docgen.template.api.TemplateExportMetadataView;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Package-private clause snapshot materialization / reference remap for
 * {@link TemplateImportService}.
 */
final class TemplateImportMaterializeSupport {

    private final ContentModuleService contentModuleService;
    private final ContentModuleRepository contentModuleRepository;
    private final ContentModuleVersionRepository contentModuleVersionRepository;

    TemplateImportMaterializeSupport(
            ContentModuleService contentModuleService,
            ContentModuleRepository contentModuleRepository,
            ContentModuleVersionRepository contentModuleVersionRepository
    ) {
        this.contentModuleService = contentModuleService;
        this.contentModuleRepository = contentModuleRepository;
        this.contentModuleVersionRepository = contentModuleVersionRepository;
    }

    MaterializeResult materializeClauses(
            TemplateExportBundleView bundle,
            TemplateExportMetadataView metadata,
            ManagementSessionClaims session
    ) {
        List<TemplateExportClauseSnapshotView> snapshots =
                bundle.clauseSnapshots() == null ? List.of() : bundle.clauseSnapshots();
        Map<String, RemappedClause> remappedBySourceModuleId = new LinkedHashMap<>();
        int count = 0;
        for (TemplateExportClauseSnapshotView snapshot : snapshots) {
            if (snapshot == null || snapshot.moduleCode() == null || snapshot.moduleCode().isBlank()) {
                continue;
            }
            String moduleCode = snapshot.moduleCode().trim().toUpperCase(Locale.ROOT);
            String semanticVersion = resolveSemanticVersion(snapshot);
            String targetModuleId;
            Optional<ContentModuleEntity> existing =
                    contentModuleRepository.findByModuleCodeAndDeletedAtIsNull(moduleCode);
            if (existing.isEmpty()) {
                ContentModuleDetailView created = contentModuleService.create(
                        new CreateContentModuleRequest(
                                moduleCode,
                                metadata.groupCode(),
                                moduleCode,
                                "Imported clause snapshot",
                                List.of(),
                                semanticVersion,
                                snapshot.contentStructureJson(),
                                "Materialized from template export bundle v2",
                                metadata.locale() == null || metadata.locale().isBlank()
                                        ? "zh-CN"
                                        : metadata.locale(),
                                null
                        ),
                        session
                );
                targetModuleId = created.moduleId();
                count++;
            } else {
                ContentModuleEntity module = existing.get();
                targetModuleId = module.getId().toString();
                if (!contentModuleVersionRepository.existsByModuleIdAndSemanticVersion(
                        module.getId(), semanticVersion)) {
                    try {
                        contentModuleService.createVersion(
                                module.getId().toString(),
                                new CreateContentModuleVersionRequest(
                                        semanticVersion,
                                        snapshot.contentStructureJson(),
                                        "Materialized from template export bundle v2",
                                        snapshot.jurisdiction(),
                                        null,
                                        null,
                                        snapshot.legalReviewRef()
                                ),
                                session
                        );
                        count++;
                    } catch (ContentModuleValidationException ex) {
                        if (!"api.error.contentModule.versionExists".equals(ex.messageKey())) {
                            throw ex;
                        }
                    }
                }
            }
            if (snapshot.sourceModuleId() != null && !snapshot.sourceModuleId().isBlank()) {
                remappedBySourceModuleId.put(
                        snapshot.sourceModuleId().trim(),
                        new RemappedClause(targetModuleId, semanticVersion)
                );
            }
        }
        return new MaterializeResult(count, remappedBySourceModuleId);
    }

    TemplateExportBundleView remapContentModuleReferences(
            TemplateExportBundleView bundle,
            Map<String, RemappedClause> remappedBySourceModuleId
    ) {
        List<ContentModuleReferenceView> sourceRefs =
                bundle.contentModuleReferences() == null ? List.of() : bundle.contentModuleReferences();
        if (sourceRefs.isEmpty() || remappedBySourceModuleId.isEmpty()) {
            return bundle;
        }
        List<ContentModuleReferenceView> remapped = new ArrayList<>(sourceRefs.size());
        for (ContentModuleReferenceView reference : sourceRefs) {
            if (reference == null) {
                continue;
            }
            RemappedClause mapped = null;
            if (reference.moduleId() != null && !reference.moduleId().isBlank()) {
                mapped = remappedBySourceModuleId.get(reference.moduleId().trim());
            }
            if (mapped == null) {
                remapped.add(reference);
                continue;
            }
            remapped.add(new ContentModuleReferenceView(
                    reference.referenceKey(),
                    mapped.targetModuleId(),
                    mapped.semanticVersion(),
                    reference.locked(),
                    false,
                    null
            ));
        }
        return new TemplateExportBundleView(
                bundle.format(),
                bundle.metadata(),
                bundle.variables(),
                bundle.bindings(),
                bundle.rules(),
                remapped,
                bundle.policySnapshot(),
                bundle.masterPin(),
                bundle.clauseSnapshots(),
                bundle.renderProfile(),
                bundle.assetKeyManifest(),
                bundle.compositionInclusionRules(),
                bundle.clauseNestingGraph(),
                bundle.dependencyClosure()
        );
    }

    private static String resolveSemanticVersion(TemplateExportClauseSnapshotView snapshot) {
        if (snapshot.semanticVersion() != null && !snapshot.semanticVersion().isBlank()) {
            return snapshot.semanticVersion().trim();
        }
        return Math.max(1, snapshot.versionNumber()) + ".0.0";
    }

    record RemappedClause(String targetModuleId, String semanticVersion) {
    }

    record MaterializeResult(int materializedCount, Map<String, RemappedClause> remappedBySourceModuleId) {
        MaterializeResult {
            remappedBySourceModuleId = remappedBySourceModuleId == null
                    ? Map.of()
                    : Map.copyOf(remappedBySourceModuleId);
        }
    }
}
