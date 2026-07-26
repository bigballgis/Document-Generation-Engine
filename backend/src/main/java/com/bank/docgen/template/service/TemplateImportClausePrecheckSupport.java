package com.bank.docgen.template.service;

import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.template.api.ContentModuleReferenceView;
import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.api.TemplateExportClauseSnapshotView;
import com.bank.docgen.template.api.TemplateImportDependencyItemView;
import com.bank.docgen.template.domain.TemplateImportDependencySeverity;
import com.bank.docgen.template.domain.TemplateImportDependencyType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Package-private CLAUSE dependency evaluation for {@link TemplateImportDependencyPrecheck}.
 */
final class TemplateImportClausePrecheckSupport {

    private final ContentModuleRepository contentModuleRepository;
    private final ContentModuleVersionRepository contentModuleVersionRepository;

    TemplateImportClausePrecheckSupport(
            ContentModuleRepository contentModuleRepository,
            ContentModuleVersionRepository contentModuleVersionRepository
    ) {
        this.contentModuleRepository = contentModuleRepository;
        this.contentModuleVersionRepository = contentModuleVersionRepository;
    }

    void evaluateClauses(TemplateExportBundleView bundle, List<TemplateImportDependencyItemView> items) {
        if (!TemplateExportV2Support.EXPORT_FORMAT_V2.equals(bundle.format())) {
            return;
        }
        List<TemplateExportClauseSnapshotView> snapshots =
                bundle.clauseSnapshots() == null ? List.of() : bundle.clauseSnapshots();
        Map<String, TemplateExportClauseSnapshotView> snapshotsByCode = new LinkedHashMap<>();
        Map<String, TemplateExportClauseSnapshotView> snapshotsBySourceModuleId = new LinkedHashMap<>();
        for (TemplateExportClauseSnapshotView snapshot : snapshots) {
            if (snapshot == null || snapshot.moduleCode() == null || snapshot.moduleCode().isBlank()) {
                continue;
            }
            String code = snapshot.moduleCode().trim().toUpperCase(Locale.ROOT);
            snapshotsByCode.put(code, snapshot);
            if (snapshot.sourceModuleId() != null && !snapshot.sourceModuleId().isBlank()) {
                snapshotsBySourceModuleId.put(snapshot.sourceModuleId().trim(), snapshot);
            }
        }

        for (Map.Entry<String, TemplateExportClauseSnapshotView> entry : snapshotsByCode.entrySet()) {
            String moduleCode = entry.getKey();
            TemplateExportClauseSnapshotView snapshot = entry.getValue();
            Optional<ContentModuleEntity> existing =
                    contentModuleRepository.findByModuleCodeAndDeletedAtIsNull(moduleCode);
            if (existing.isPresent()
                    && hasCompatibleVersion(existing.get().getId(), snapshot)) {
                items.add(item(
                        TemplateImportDependencyType.CLAUSE,
                        TemplateImportDependencySeverity.OK,
                        "CLAUSE_PRESENT",
                        "api.error.template.dep.clausePresent",
                        moduleCode
                ));
            } else {
                items.add(item(
                        TemplateImportDependencyType.CLAUSE,
                        TemplateImportDependencySeverity.WILL_MATERIALIZE,
                        "CLAUSE_WILL_MATERIALIZE",
                        "api.error.template.dep.clauseWillMaterialize",
                        moduleCode
                ));
            }
        }

        List<ContentModuleReferenceView> references =
                bundle.contentModuleReferences() == null ? List.of() : bundle.contentModuleReferences();
        for (ContentModuleReferenceView reference : references) {
            if (reference == null) {
                continue;
            }
            String moduleCode = resolveModuleCode(reference, snapshotsBySourceModuleId);
            if (moduleCode != null && snapshotsByCode.containsKey(moduleCode)) {
                continue;
            }
            if (moduleCode != null) {
                Optional<ContentModuleEntity> existing =
                        contentModuleRepository.findByModuleCodeAndDeletedAtIsNull(moduleCode);
                if (existing.isPresent()) {
                    items.add(item(
                            TemplateImportDependencyType.CLAUSE,
                            TemplateImportDependencySeverity.OK,
                            "CLAUSE_PRESENT",
                            "api.error.template.dep.clausePresent",
                            moduleCode
                    ));
                    continue;
                }
            }
            items.add(item(
                    TemplateImportDependencyType.CLAUSE,
                    TemplateImportDependencySeverity.MISSING,
                    "CLAUSE_MISSING",
                    "api.error.template.dep.clauseMissing",
                    moduleCode == null ? reference.moduleId() : moduleCode
            ));
        }
    }

    private String resolveModuleCode(
            ContentModuleReferenceView reference,
            Map<String, TemplateExportClauseSnapshotView> snapshotsBySourceModuleId
    ) {
        if (reference.moduleId() != null && !reference.moduleId().isBlank()) {
            TemplateExportClauseSnapshotView fromSnapshot =
                    snapshotsBySourceModuleId.get(reference.moduleId().trim());
            if (fromSnapshot != null && fromSnapshot.moduleCode() != null) {
                return fromSnapshot.moduleCode().trim().toUpperCase(Locale.ROOT);
            }
        }
        if (reference.moduleId() == null || reference.moduleId().isBlank()) {
            return null;
        }
        try {
            return contentModuleRepository.findByIdAndDeletedAtIsNull(UUID.fromString(reference.moduleId()))
                    .map(ContentModuleEntity::getModuleCode)
                    .map(code -> code.toUpperCase(Locale.ROOT))
                    .orElse(null);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean hasCompatibleVersion(UUID moduleId, TemplateExportClauseSnapshotView snapshot) {
        String full = snapshot.semanticVersion();
        if (full != null && !full.isBlank()) {
            return contentModuleVersionRepository
                    .findByModuleIdAndSemanticVersion(moduleId, full.trim())
                    .isPresent();
        }
        return contentModuleVersionRepository.findByModuleIdOrderBySemanticVersionDesc(moduleId).stream()
                .anyMatch(version -> compatibleVersion(version.getSemanticVersion(), snapshot.versionNumber()));
    }

    private static boolean compatibleVersion(String semanticVersion, int versionNumber) {
        if (semanticVersion == null || semanticVersion.isBlank()) {
            return false;
        }
        String major = semanticVersion.split("\\.")[0].replaceAll("[^0-9]", "");
        if (major.isBlank()) {
            return false;
        }
        try {
            return Integer.parseInt(major) == versionNumber;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static TemplateImportDependencyItemView item(
            TemplateImportDependencyType type,
            TemplateImportDependencySeverity severity,
            String code,
            String messageKey,
            String detail
    ) {
        return new TemplateImportDependencyItemView(type, severity, code, messageKey, detail);
    }
}
