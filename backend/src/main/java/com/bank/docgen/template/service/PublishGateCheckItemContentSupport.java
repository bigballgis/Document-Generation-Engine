package com.bank.docgen.template.service;

import com.bank.docgen.authoring.structured.NodeMatrixValidationService;
import com.bank.docgen.template.api.BindingValidationView;
import com.bank.docgen.template.api.CoverageSummaryView;
import com.bank.docgen.template.api.PublishGateItemView;
import com.bank.docgen.template.domain.PublishGateCheckCode;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.port.PreviewEvidencePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;

/**
 * Package-private builders for content-module / structured-node / paste publish-gate items.
 */
final class PublishGateCheckItemContentSupport {

    private final PreviewEvidencePort previewEvidencePort;
    private final TemplateContentModuleReferenceService contentModuleReferenceService;
    private final AnchorBindingRepository anchorBindingRepository;
    private final NodeMatrixValidationService nodeMatrixValidationService;
    private final ObjectMapper objectMapper;

    PublishGateCheckItemContentSupport(
            PreviewEvidencePort previewEvidencePort,
            TemplateContentModuleReferenceService contentModuleReferenceService,
            AnchorBindingRepository anchorBindingRepository,
            NodeMatrixValidationService nodeMatrixValidationService,
            ObjectMapper objectMapper
    ) {
        this.previewEvidencePort = previewEvidencePort;
        this.contentModuleReferenceService = contentModuleReferenceService;
        this.anchorBindingRepository = anchorBindingRepository;
        this.nodeMatrixValidationService = nodeMatrixValidationService;
        this.objectMapper = objectMapper;
    }

    PublishGateItemView contentModuleReferencesItem(UUID versionId) {
        var validation = contentModuleReferenceService.validateReferences(versionId);
        boolean blocking = validation.blocking();
        return new PublishGateItemView(
                PublishGateCheckCode.CONTENT_MODULE_REFERENCES,
                !blocking,
                blocking,
                blocking
                        ? "api.publishGate.contentModuleReferences.blocked"
                        : "api.publishGate.contentModuleReferences.ready",
                "invalidReferences=" + validation.invalidReferences()
                        + ",totalReferences=" + validation.totalReferences()
        );
    }

    PublishGateItemView contentModuleEffectiveExpiredItem(UUID versionId) {
        var expiry = contentModuleReferenceService.evaluateEffectiveExpiry(versionId);
        boolean blocking = expiry.blocking();
        String detail = blocking
                ? String.join(";", expiry.expiredDetails())
                : "expiredReferences=0";
        return new PublishGateItemView(
                PublishGateCheckCode.CONTENT_MODULE_EFFECTIVE_EXPIRED,
                !blocking,
                blocking,
                blocking
                        ? "api.publishGate.contentModuleEffectiveExpired.blocked"
                        : "api.publishGate.contentModuleEffectiveExpired.ready",
                detail
        );
    }

    PublishGateItemView contentModuleLocaleMismatchItem(UUID versionId) {
        var mismatch = contentModuleReferenceService.evaluateLocaleMismatch(versionId);
        boolean blocking = mismatch != null && mismatch.blocking();
        String detail = blocking
                ? String.join(";", mismatch.mismatchDetails())
                : "mismatchedReferences=0";
        return new PublishGateItemView(
                PublishGateCheckCode.CONTENT_MODULE_LOCALE_MISMATCH,
                !blocking,
                blocking,
                blocking
                        ? "api.publishGate.contentModuleLocaleMismatch.blocked"
                        : "api.publishGate.contentModuleLocaleMismatch.ready",
                detail
        );
    }

    PublishGateItemView unsupportedStructuredNodesItem(UUID versionId) {
        int unsupportedNodeCount = 0;
        for (AnchorBindingEntity binding : anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(versionId)) {
            unsupportedNodeCount += nodeMatrixValidationService.countUnsupportedNodeBlockers(
                    binding.getStructuredContentJson()
            );
        }
        boolean blocking = unsupportedNodeCount > 0;
        return new PublishGateItemView(
                PublishGateCheckCode.UNSUPPORTED_STRUCTURED_NODES,
                !blocking,
                blocking,
                blocking
                        ? "api.publishGate.unsupportedStructuredNodes.blocked"
                        : "api.publishGate.unsupportedStructuredNodes.ready",
                "unsupportedNodeCount=" + unsupportedNodeCount
        );
    }

    PublishGateItemView pasteCleaningBlockersItem(UUID versionId) {
        int unresolvedBindingCount = 0;
        for (AnchorBindingEntity binding : anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(versionId)) {
            if (PasteCleaningEvidenceSupport.hasUnresolvedPasteBlockers(
                    binding.getPasteCleaningEvidenceJson(),
                    objectMapper
            )) {
                unresolvedBindingCount++;
            }
        }
        boolean blocking = unresolvedBindingCount > 0;
        return new PublishGateItemView(
                PublishGateCheckCode.PASTE_CLEANING_BLOCKERS,
                !blocking,
                blocking,
                blocking
                        ? "api.publishGate.pasteCleaningBlockers.blocked"
                        : "api.publishGate.pasteCleaningBlockers.ready",
                "unresolvedPasteBindings=" + unresolvedBindingCount
        );
    }

    PublishGateItemView blockerStatusItem(
            UUID templateId,
            UUID versionId,
            BindingValidationView bindings,
            CoverageSummaryView coverage
    ) {
        int previewBlockers = previewEvidencePort.countFailedPreviews(templateId, versionId);
        boolean blocking = bindings.summary().blocking()
                || coverage.belowThreshold()
                || previewBlockers > 0;
        return new PublishGateItemView(
                PublishGateCheckCode.BLOCKER_STATUS,
                !blocking,
                blocking,
                blocking ? "api.publishGate.blockerStatus.blocked" : "api.publishGate.blockerStatus.ready",
                "previewFailures=" + previewBlockers
        );
    }
}
