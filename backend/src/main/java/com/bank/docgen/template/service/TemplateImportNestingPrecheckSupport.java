package com.bank.docgen.template.service;

import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.api.TemplateExportClauseNestingGraphEdgeView;
import com.bank.docgen.template.api.TemplateExportClauseNestingGraphView;
import com.bank.docgen.template.api.TemplateExportClauseSnapshotView;
import com.bank.docgen.template.api.TemplateImportDependencyItemView;
import com.bank.docgen.template.domain.TemplateImportDependencySeverity;
import com.bank.docgen.template.domain.TemplateImportDependencyType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Package-private CLAUSE_NESTING dependency evaluation for
 * {@link TemplateImportDependencyPrecheck}.
 */
final class TemplateImportNestingPrecheckSupport {

    private final ContentModuleRepository contentModuleRepository;

    TemplateImportNestingPrecheckSupport(ContentModuleRepository contentModuleRepository) {
        this.contentModuleRepository = contentModuleRepository;
    }

    void evaluateNesting(TemplateExportBundleView bundle, List<TemplateImportDependencyItemView> items) {
        if (!TemplateExportV2Support.EXPORT_FORMAT_V2.equals(bundle.format())) {
            return;
        }
        TemplateExportClauseNestingGraphView graph = bundle.clauseNestingGraph();
        if (graph == null || graph.edges() == null || graph.edges().isEmpty()) {
            return;
        }
        Map<String, TemplateExportClauseSnapshotView> snapshotsByCode = new LinkedHashMap<>();
        List<TemplateExportClauseSnapshotView> snapshots =
                bundle.clauseSnapshots() == null ? List.of() : bundle.clauseSnapshots();
        for (TemplateExportClauseSnapshotView snapshot : snapshots) {
            if (snapshot == null || snapshot.moduleCode() == null || snapshot.moduleCode().isBlank()) {
                continue;
            }
            snapshotsByCode.put(snapshot.moduleCode().trim().toUpperCase(Locale.ROOT), snapshot);
        }
        for (TemplateExportClauseNestingGraphEdgeView edge : graph.edges()) {
            if (edge == null) {
                continue;
            }
            String parent = edge.parentModuleCode() == null
                    ? ""
                    : edge.parentModuleCode().trim().toUpperCase(Locale.ROOT);
            String child = edge.childModuleCode() == null
                    ? ""
                    : edge.childModuleCode().trim().toUpperCase(Locale.ROOT);
            boolean parentOnTarget = !parent.isBlank()
                    && contentModuleRepository.findByModuleCodeAndDeletedAtIsNull(parent).isPresent();
            boolean childOnTarget = !child.isBlank()
                    && contentModuleRepository.findByModuleCodeAndDeletedAtIsNull(child).isPresent();
            boolean parentSnapshot = snapshotsByCode.containsKey(parent);
            boolean childSnapshot = snapshotsByCode.containsKey(child);
            if (parentOnTarget && childOnTarget) {
                items.add(item(
                        TemplateImportDependencyType.CLAUSE_NESTING,
                        TemplateImportDependencySeverity.OK,
                        "CLAUSE_NESTING_OK",
                        "api.error.template.dep.clauseNestingOk",
                        parent + ">" + child
                ));
            } else if ((parentOnTarget || parentSnapshot) && (childOnTarget || childSnapshot)) {
                items.add(item(
                        TemplateImportDependencyType.CLAUSE_NESTING,
                        TemplateImportDependencySeverity.WILL_MATERIALIZE,
                        "CLAUSE_NESTING_WILL_MATERIALIZE",
                        "api.error.template.dep.clauseNestingWillMaterialize",
                        parent + ">" + child
                ));
            } else {
                items.add(item(
                        TemplateImportDependencyType.CLAUSE_NESTING,
                        TemplateImportDependencySeverity.MISSING,
                        "CLAUSE_NESTING_MISSING",
                        "api.error.template.dep.clauseNestingMissing",
                        parent + ">" + child
                ));
            }
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
