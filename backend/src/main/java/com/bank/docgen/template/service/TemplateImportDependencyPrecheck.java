package com.bank.docgen.template.service;

import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.master.service.MasterNotFoundException;
import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.api.TemplateExportClauseNestingGraphView;
import com.bank.docgen.template.api.TemplateImportDependencyItemView;
import com.bank.docgen.template.api.TemplateImportDependencyReportView;
import com.bank.docgen.template.domain.TemplateDependencyClosure;
import com.bank.docgen.template.domain.TemplateImportDependencySeverity;
import com.bank.docgen.template.domain.TemplateImportDependencyType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * CE-E01 / Wave 7: builds import dependency pre-check reports for dry-run and commit gates.
 */
@Component
public class TemplateImportDependencyPrecheck {

    private final MasterDocumentRepository masterDocumentRepository;
    private final TemplateImportMasterPinPrecheckSupport masterPinSupport;
    private final TemplateImportClausePrecheckSupport clauseSupport;
    private final TemplateImportNestingPrecheckSupport nestingSupport;
    private final TemplateImportAssetPrecheckSupport assetSupport;

    public TemplateImportDependencyPrecheck(
            MasterDocumentRepository masterDocumentRepository,
            MasterRevisionLineRepository masterRevisionLineRepository,
            ObjectStoragePort objectStoragePort,
            ContentModuleRepository contentModuleRepository,
            ContentModuleVersionRepository contentModuleVersionRepository
    ) {
        this.masterDocumentRepository = masterDocumentRepository;
        this.masterPinSupport = new TemplateImportMasterPinPrecheckSupport(
                masterDocumentRepository,
                masterRevisionLineRepository,
                objectStoragePort
        );
        this.clauseSupport = new TemplateImportClausePrecheckSupport(
                contentModuleRepository,
                contentModuleVersionRepository
        );
        this.nestingSupport = new TemplateImportNestingPrecheckSupport(contentModuleRepository);
        this.assetSupport = new TemplateImportAssetPrecheckSupport(objectStoragePort);
    }

    public record PrecheckContext(
            TemplateExportBundleView bundle,
            UUID targetMasterId,
            byte[] embeddedMasterDocx,
            boolean zipCarrier,
            boolean requireMasterDocxForCommit,
            Map<String, byte[]> embeddedAssetBinaries
    ) {
        public PrecheckContext {
            embeddedAssetBinaries = embeddedAssetBinaries == null ? Map.of() : Map.copyOf(embeddedAssetBinaries);
        }

        /** Backward-compatible CE-E01 constructor (no embedded assets). */
        public PrecheckContext(
                TemplateExportBundleView bundle,
                UUID targetMasterId,
                byte[] embeddedMasterDocx,
                boolean zipCarrier,
                boolean requireMasterDocxForCommit
        ) {
            this(bundle, targetMasterId, embeddedMasterDocx, zipCarrier, requireMasterDocxForCommit, Map.of());
        }

        /**
         * Prefer explicit bundle {@code dependencyClosure=PROMOTION} (Wave 7 export marker).
         * Fall back to embedded-asset / nesting-graph heuristics for legacy packs without the field.
         * Unknown explicit values fail closed (not treated as promotion).
         */
        public boolean promotionCarrier() {
            if (bundle != null
                    && bundle.dependencyClosure() != null
                    && !bundle.dependencyClosure().isBlank()) {
                try {
                    return TemplateDependencyClosure.parseOptional(bundle.dependencyClosure())
                            == TemplateDependencyClosure.PROMOTION;
                } catch (IllegalArgumentException ex) {
                    return false;
                }
            }
            if (embeddedAssetBinaries != null && !embeddedAssetBinaries.isEmpty()) {
                return true;
            }
            TemplateExportClauseNestingGraphView graph = bundle == null ? null : bundle.clauseNestingGraph();
            return graph != null && graph.edges() != null && !graph.edges().isEmpty();
        }
    }

    public TemplateImportDependencyReportView evaluate(PrecheckContext context) {
        List<TemplateImportDependencyItemView> items = new ArrayList<>();
        TemplateExportBundleView bundle = context.bundle();
        String format = bundle.format();

        items.add(item(
                TemplateImportDependencyType.BUNDLE_FORMAT,
                TemplateImportDependencySeverity.OK,
                "BUNDLE_FORMAT_OK",
                "api.error.template.dep.bundleFormatOk",
                format
        ));

        masterPinSupport.evaluateMasterPin(context, items);
        clauseSupport.evaluateClauses(bundle, items);
        nestingSupport.evaluateNesting(bundle, items);
        assetSupport.evaluateAssets(context, items);
        assetSupport.evaluateRenderProfile(bundle, items);

        int blocking = 0;
        int warning = 0;
        int info = 0;
        for (TemplateImportDependencyItemView entry : items) {
            if (entry.severity() == TemplateImportDependencySeverity.MISSING
                    || entry.severity() == TemplateImportDependencySeverity.MISMATCH) {
                blocking++;
            } else if (entry.severity() == TemplateImportDependencySeverity.INFO) {
                info++;
            } else if (entry.severity() == TemplateImportDependencySeverity.WILL_MATERIALIZE) {
                warning++;
            }
        }
        return new TemplateImportDependencyReportView(
                items,
                blocking,
                warning,
                info,
                blocking == 0,
                format
        );
    }

    public void assertMasterGate(UUID targetMasterId, String expectedGroupCode) {
        MasterDocumentEntity master = masterDocumentRepository.findByIdAndDeletedAtIsNull(targetMasterId)
                .orElseThrow(MasterNotFoundException::new);
        if (master.getStatus() != MasterDocumentStatus.APPROVED) {
            throw new TemplateValidationException("api.error.template.masterNotApproved");
        }
        if (!master.getGroupCode().equals(expectedGroupCode)) {
            throw new TemplateValidationException("api.error.template.masterGroupMismatch");
        }
    }

    public String resolveTargetMasterFileHash(UUID targetMasterId) {
        return masterPinSupport.resolveTargetMasterFileHash(targetMasterId);
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
