package com.bank.docgen.template.service;

import com.bank.docgen.authoring.structured.MasterStyleCatalogService;
import com.bank.docgen.authoring.structured.NodeMatrixValidationService;
import com.bank.docgen.authoring.structured.NumberingService;
import com.bank.docgen.authoring.structured.ReferenceNodeService;
import com.bank.docgen.authoring.structured.StructuredContentSchemaException;
import com.bank.docgen.authoring.structured.StructuredContentSchemaValidator;
import com.bank.docgen.authoring.structured.TableComponentService;
import com.bank.docgen.template.api.UpsertVariableSchemaRequest;
import com.bank.docgen.template.domain.AnchorContentType;
import com.bank.docgen.template.domain.BindingValidationStatus;
import com.bank.docgen.template.domain.VariableType;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Package-private binding status / variable request / structured-content validation.
 */
final class TemplateBindingStatusSupport {

    private static final Set<VariableType> SUPPORTED_TYPES = Set.of(VariableType.values());

    private final VariableSchemaRepository variableSchemaRepository;
    private final ObjectMapper objectMapper;
    private final StructuredContentSchemaValidator structuredContentSchemaValidator;
    private final NodeMatrixValidationService nodeMatrixValidationService;
    private final MasterStyleCatalogService masterStyleCatalogService;
    private final TableComponentService tableComponentService;
    private final ReferenceNodeService referenceNodeService;
    private final NumberingService numberingService;

    TemplateBindingStatusSupport(
            VariableSchemaRepository variableSchemaRepository,
            ObjectMapper objectMapper,
            StructuredContentSchemaValidator structuredContentSchemaValidator,
            NodeMatrixValidationService nodeMatrixValidationService,
            MasterStyleCatalogService masterStyleCatalogService,
            TableComponentService tableComponentService,
            ReferenceNodeService referenceNodeService,
            NumberingService numberingService
    ) {
        this.variableSchemaRepository = variableSchemaRepository;
        this.objectMapper = objectMapper;
        this.structuredContentSchemaValidator = structuredContentSchemaValidator;
        this.nodeMatrixValidationService = nodeMatrixValidationService;
        this.masterStyleCatalogService = masterStyleCatalogService;
        this.tableComponentService = tableComponentService;
        this.referenceNodeService = referenceNodeService;
        this.numberingService = numberingService;
    }

    BindingValidationStatus computeBindingStatus(
            String anchorId,
            AnchorContentType declaredContentType,
            Set<String> masterAnchors,
            List<String> allAnchorIds,
            String structuredContentJson,
            Set<String> declaredVariableKeys,
            UUID masterId,
            String pasteCleaningEvidenceJson
    ) {
        if (!masterAnchors.contains(anchorId)) {
            return BindingValidationStatus.MISSING_ANCHOR;
        }
        long count = allAnchorIds.stream().filter(id -> id.equals(anchorId)).count();
        if (count > 1) {
            return BindingValidationStatus.DUPLICATE_BINDING;
        }
        if (declaredContentType == AnchorContentType.IMAGE && anchorId.contains("TEXT")) {
            return BindingValidationStatus.INCOMPATIBLE_CONTENT_TYPE;
        }
        if (PasteCleaningEvidenceSupport.hasUnresolvedPasteBlockers(pasteCleaningEvidenceJson, objectMapper)) {
            return BindingValidationStatus.INCOMPATIBLE_CONTENT_TYPE;
        }
        if (structuredContentJson != null && !structuredContentJson.isBlank()) {
            var fidelity = nodeMatrixValidationService.validate(structuredContentJson, declaredVariableKeys);
            var styleCatalog = masterStyleCatalogService.loadForMaster(masterId);
            var styleFidelity = masterStyleCatalogService.validate(structuredContentJson, styleCatalog);
            var tableFidelity = tableComponentService.validateStructuredContent(structuredContentJson, declaredVariableKeys);
            var referenceFidelity = referenceNodeService.validateStructuredContent(structuredContentJson);
            var numberingFidelity = numberingService.validateStructuredContent(structuredContentJson);
            if (fidelity.hasBlockers()
                    || styleFidelity.hasBlockers()
                    || tableFidelity.hasBlockers()
                    || referenceFidelity.fidelity().hasBlockers()
                    || numberingFidelity.fidelity().hasBlockers()) {
                return BindingValidationStatus.INCOMPATIBLE_CONTENT_TYPE;
            }
        }
        return BindingValidationStatus.VALID;
    }

    Set<String> loadDeclaredVariableKeys(UUID templateVersionId) {
        return variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(templateVersionId).stream()
                .map(VariableSchemaEntity::getVariableKey)
                .collect(Collectors.toSet());
    }

    void validateVariableRequest(UpsertVariableSchemaRequest request) {
        if (request.variableType() == null || !SUPPORTED_TYPES.contains(request.variableType())) {
            throw new TemplateValidationException("api.error.template.variableTypeUnsupported");
        }
        if (request.variableType() == VariableType.ENUM
                && (request.enumValues() == null || request.enumValues().isBlank())) {
            throw new TemplateValidationException("api.error.template.enumValuesRequired");
        }
    }

    void validateStructuredContent(String json) {
        try {
            structuredContentSchemaValidator.validate(json);
        } catch (StructuredContentSchemaException ex) {
            throw new TemplateValidationException(ex.messageKey());
        }
    }
}
