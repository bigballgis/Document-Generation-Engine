package com.bank.docgen.template.service;

import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.master.service.MasterNotFoundException;
import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.api.TemplateExportMasterPinView;
import com.bank.docgen.template.api.TemplateImportDependencyItemView;
import com.bank.docgen.template.domain.TemplateImportDependencySeverity;
import com.bank.docgen.template.domain.TemplateImportDependencyType;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Package-private MASTER_PIN dependency evaluation for {@link TemplateImportDependencyPrecheck}.
 */
final class TemplateImportMasterPinPrecheckSupport {

    private final MasterDocumentRepository masterDocumentRepository;
    private final MasterRevisionLineRepository masterRevisionLineRepository;
    private final ObjectStoragePort objectStoragePort;

    TemplateImportMasterPinPrecheckSupport(
            MasterDocumentRepository masterDocumentRepository,
            MasterRevisionLineRepository masterRevisionLineRepository,
            ObjectStoragePort objectStoragePort
    ) {
        this.masterDocumentRepository = masterDocumentRepository;
        this.masterRevisionLineRepository = masterRevisionLineRepository;
        this.objectStoragePort = objectStoragePort;
    }

    void evaluateMasterPin(
            TemplateImportDependencyPrecheck.PrecheckContext context,
            List<TemplateImportDependencyItemView> items
    ) {
        TemplateExportBundleView bundle = context.bundle();
        if (!TemplateExportV2Support.EXPORT_FORMAT_V2.equals(bundle.format())) {
            return;
        }
        TemplateExportMasterPinView pin = bundle.masterPin();
        if (pin == null || pin.masterFileHash() == null || pin.masterFileHash().isBlank()) {
            items.add(item(
                    TemplateImportDependencyType.MASTER_PIN,
                    TemplateImportDependencySeverity.MISSING,
                    "MASTER_PIN_ABSENT",
                    "api.error.template.dep.masterPinAbsent",
                    null
            ));
            return;
        }
        boolean hasDocx = context.embeddedMasterDocx() != null && context.embeddedMasterDocx().length > 0;
        if (!hasDocx) {
            items.add(item(
                    TemplateImportDependencyType.MASTER_PIN,
                    TemplateImportDependencySeverity.MISSING,
                    "MASTER_DOCX_ABSENT",
                    "api.error.template.dep.masterDocxAbsent",
                    context.zipCarrier() ? null : "JSON-only v2 carrier"
            ));
        } else {
            String embeddedHash = TemplateExportHashSupport.sha256Hex(context.embeddedMasterDocx());
            if (!embeddedHash.equalsIgnoreCase(pin.masterFileHash())) {
                items.add(item(
                        TemplateImportDependencyType.MASTER_PIN,
                        TemplateImportDependencySeverity.MISMATCH,
                        "MASTER_DOCX_HASH_MISMATCH",
                        "api.error.template.dep.masterDocxHashMismatch",
                        null
                ));
            }
        }

        String targetHash = resolveTargetMasterFileHash(context.targetMasterId());
        if (!targetHash.equalsIgnoreCase(pin.masterFileHash())) {
            if (context.promotionCarrier() && hasDocx) {
                items.add(item(
                        TemplateImportDependencyType.MASTER_PIN,
                        TemplateImportDependencySeverity.WILL_MATERIALIZE,
                        "MASTER_WILL_MATERIALIZE",
                        "api.error.template.dep.masterWillMaterialize",
                        null
                ));
            } else {
                items.add(item(
                        TemplateImportDependencyType.MASTER_PIN,
                        TemplateImportDependencySeverity.MISMATCH,
                        "MASTER_FINGERPRINT_MISMATCH",
                        "api.error.template.dep.masterFingerprintMismatch",
                        null
                ));
            }
        } else {
            items.add(item(
                    TemplateImportDependencyType.MASTER_PIN,
                    TemplateImportDependencySeverity.OK,
                    "MASTER_FINGERPRINT_OK",
                    "api.error.template.dep.masterFingerprintOk",
                    null
            ));
        }
    }

    String resolveTargetMasterFileHash(UUID targetMasterId) {
        MasterDocumentEntity master = masterDocumentRepository.findByIdAndDeletedAtIsNull(targetMasterId)
                .orElseThrow(MasterNotFoundException::new);
        UUID revisionId = master.getCurrentRevisionLineId();
        if (revisionId == null) {
            throw new TemplateValidationException("api.error.template.masterNotApproved");
        }
        MasterRevisionLineEntity revision = masterRevisionLineRepository
                .findByIdAndMasterIdAndDeletedAtIsNull(revisionId, targetMasterId)
                .orElseThrow(MasterNotFoundException::new);
        try (InputStream stream = objectStoragePort.get(revision.getStorageKey())) {
            return TemplateExportHashSupport.sha256Hex(stream.readAllBytes());
        } catch (IOException | RuntimeException ex) {
            throw new TemplateValidationException("api.error.template.masterNotApproved");
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
