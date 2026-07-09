package com.bank.docgen.authoring.structured;

import com.bank.docgen.rendering.api.FidelityWarningView;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Aggregates fidelity warnings (not blockers) from the structured authoring validation engine (P18-T09).
 */
@Service
public class FidelityValidationService {

    private final AnchorBindingRepository anchorBindingRepository;
    private final VariableSchemaRepository variableSchemaRepository;
    private final NodeMatrixValidationService nodeMatrixValidationService;
    private final MasterStyleCatalogService masterStyleCatalogService;
    private final TableComponentService tableComponentService;
    private final ReferenceNodeService referenceNodeService;
    private final NumberingService numberingService;

    public FidelityValidationService(
            AnchorBindingRepository anchorBindingRepository,
            VariableSchemaRepository variableSchemaRepository,
            NodeMatrixValidationService nodeMatrixValidationService,
            MasterStyleCatalogService masterStyleCatalogService,
            TableComponentService tableComponentService,
            ReferenceNodeService referenceNodeService,
            NumberingService numberingService
    ) {
        this.anchorBindingRepository = anchorBindingRepository;
        this.variableSchemaRepository = variableSchemaRepository;
        this.nodeMatrixValidationService = nodeMatrixValidationService;
        this.masterStyleCatalogService = masterStyleCatalogService;
        this.tableComponentService = tableComponentService;
        this.referenceNodeService = referenceNodeService;
        this.numberingService = numberingService;
    }

    public List<FidelityWarningView> collectWarningsForVersion(UUID templateVersionId, UUID masterId) {
        Set<String> declaredVariableKeys = loadDeclaredVariableKeys(templateVersionId);
        MasterStyleCatalog styleCatalog = masterStyleCatalogService.loadForMaster(masterId);
        List<AnchorBindingEntity> bindings =
                anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(templateVersionId);

        LinkedHashMap<String, FidelityWarningView> deduped = new LinkedHashMap<>();
        for (AnchorBindingEntity binding : bindings) {
            String structuredContentJson = binding.getStructuredContentJson();
            if (structuredContentJson == null || structuredContentJson.isBlank()) {
                continue;
            }
            collectStructuredContentWarnings(
                    binding.getAnchorId(),
                    structuredContentJson,
                    declaredVariableKeys,
                    styleCatalog,
                    deduped
            );
        }
        return List.copyOf(deduped.values());
    }

    public List<String> collectWarningCodesForVersion(UUID templateVersionId, UUID masterId) {
        return collectWarningsForVersion(templateVersionId, masterId).stream()
                .map(FidelityWarningView::code)
                .toList();
    }

    private void collectStructuredContentWarnings(
            String anchorId,
            String structuredContentJson,
            Set<String> declaredVariableKeys,
            MasterStyleCatalog styleCatalog,
            LinkedHashMap<String, FidelityWarningView> deduped
    ) {
        addWarnings(deduped, anchorId, nodeMatrixValidationService.validate(structuredContentJson, declaredVariableKeys)
                .warnings());
        addWarnings(deduped, anchorId, masterStyleCatalogService.validate(structuredContentJson, styleCatalog)
                .warnings());
        addWarnings(deduped, anchorId, tableComponentService.validateStructuredContent(structuredContentJson, declaredVariableKeys)
                .warnings());
        addWarnings(deduped, anchorId, referenceNodeService.validateStructuredContent(structuredContentJson)
                .fidelity()
                .warnings());
        addWarnings(deduped, anchorId, numberingService.validateStructuredContent(structuredContentJson)
                .fidelity()
                .warnings());
    }

    private void addWarnings(
            LinkedHashMap<String, FidelityWarningView> deduped,
            String anchorId,
            List<StructuredContentFidelityIssue> warnings
    ) {
        if (warnings == null || warnings.isEmpty()) {
            return;
        }
        for (StructuredContentFidelityIssue issue : warnings) {
            if (issue.severity() != StructuredContentFidelitySeverity.WARNING) {
                continue;
            }
            String dedupeKey = anchorId + "|" + issue.code().name() + "|" + issue.messageKey() + "|" + issue.location();
            deduped.putIfAbsent(dedupeKey, toView(issue, anchorId));
        }
    }

    private FidelityWarningView toView(StructuredContentFidelityIssue issue, String anchorId) {
        String location = issue.location();
        if (location == null || location.isBlank()) {
            location = anchorId;
        }
        return new FidelityWarningView(
                issue.code().name(),
                issue.messageKey(),
                location,
                anchorId,
                Boolean.FALSE
        );
    }

    private Set<String> loadDeclaredVariableKeys(UUID templateVersionId) {
        return variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(templateVersionId).stream()
                .map(VariableSchemaEntity::getVariableKey)
                .collect(Collectors.toSet());
    }
}
