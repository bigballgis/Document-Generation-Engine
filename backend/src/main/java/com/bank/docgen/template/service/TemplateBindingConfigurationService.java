package com.bank.docgen.template.service;

import com.bank.docgen.authoring.structured.MasterStyleCatalogService;
import com.bank.docgen.authoring.structured.NodeMatrixValidationService;
import com.bank.docgen.authoring.structured.NumberingService;
import com.bank.docgen.authoring.structured.ReferenceNodeService;
import com.bank.docgen.authoring.structured.StructuredContentSchemaValidator;
import com.bank.docgen.authoring.structured.TableComponentService;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.service.MasterNotFoundException;
import com.bank.docgen.template.api.AnchorBindingView;
import com.bank.docgen.template.api.BindingValidationSummaryView;
import com.bank.docgen.template.api.BindingValidationView;
import com.bank.docgen.template.api.CompositionRuleView;
import com.bank.docgen.template.api.UpsertAnchorBindingRequest;
import com.bank.docgen.template.api.UpsertVariableSchemaRequest;
import com.bank.docgen.template.api.VariableSchemaView;
import com.bank.docgen.template.domain.AnchorContentType;
import com.bank.docgen.template.domain.BindingValidationStatus;
import com.bank.docgen.template.mapping.TemplateViewMapper;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateBindingConfigurationService {

    private final VariableSchemaRepository variableSchemaRepository;
    private final AnchorBindingRepository anchorBindingRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final MasterDocumentRepository masterDocumentRepository;
    private final ObjectMapper objectMapper;
    private final TemplateViewMapper templateViewMapper;
    private final TemplateBindingStatusSupport statusSupport;

    public TemplateBindingConfigurationService(
            VariableSchemaRepository variableSchemaRepository,
            AnchorBindingRepository anchorBindingRepository,
            TemplateVersionRepository templateVersionRepository,
            MasterDocumentRepository masterDocumentRepository,
            ObjectMapper objectMapper,
            StructuredContentSchemaValidator structuredContentSchemaValidator,
            NodeMatrixValidationService nodeMatrixValidationService,
            MasterStyleCatalogService masterStyleCatalogService,
            TableComponentService tableComponentService,
            ReferenceNodeService referenceNodeService,
            NumberingService numberingService,
            TemplateViewMapper templateViewMapper
    ) {
        this.variableSchemaRepository = variableSchemaRepository;
        this.anchorBindingRepository = anchorBindingRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.masterDocumentRepository = masterDocumentRepository;
        this.objectMapper = objectMapper;
        this.templateViewMapper = templateViewMapper;
        this.statusSupport = new TemplateBindingStatusSupport(
                variableSchemaRepository,
                objectMapper,
                structuredContentSchemaValidator,
                nodeMatrixValidationService,
                masterStyleCatalogService,
                tableComponentService,
                referenceNodeService,
                numberingService
        );
    }

    @Transactional
    public VariableSchemaView upsertVariable(
            TemplateVersionEntity version,
            UpsertVariableSchemaRequest request
    ) {
        statusSupport.validateVariableRequest(request);
        var existing = variableSchemaRepository.findByTemplateVersionIdAndVariableKey(
                version.getId(),
                request.variableKey()
        );
        VariableSchemaEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.update(
                    request.variableType(),
                    request.required(),
                    request.defaultValue(),
                    request.enumValues(),
                    request.description(),
                    request.computeExpression()
            );
        } else {
            entity = new VariableSchemaEntity(
                    UUID.randomUUID(),
                    version.getId(),
                    request.variableKey(),
                    request.variableType(),
                    request.required(),
                    request.defaultValue(),
                    request.enumValues(),
                    request.description(),
                    request.computeExpression()
            );
        }
        variableSchemaRepository.save(entity);
        return templateViewMapper.toVariableView(entity);
    }

    @Transactional
    public void deleteVariable(UUID templateVersionId, String variableKey) {
        variableSchemaRepository.findByTemplateVersionIdAndVariableKey(templateVersionId, variableKey)
                .ifPresent(variableSchemaRepository::delete);
    }

    @Transactional
    public AnchorBindingView upsertBinding(
            UUID masterId,
            TemplateVersionEntity version,
            UpsertAnchorBindingRequest request
    ) {
        statusSupport.validateStructuredContent(request.structuredContentJson());
        MasterDocumentEntity master = masterDocumentRepository.findByIdAndDeletedAtIsNull(masterId)
                .orElseThrow(MasterNotFoundException::new);
        Set<String> masterAnchors = new HashSet<>();
        master.getAnchors().forEach(anchor -> masterAnchors.add(anchor.getAnchorId()));
        Set<String> declaredVariableKeys = statusSupport.loadDeclaredVariableKeys(version.getId());
        var existing = anchorBindingRepository.findByTemplateVersionIdAndAnchorId(version.getId(), request.anchorId());
        String existingEvidenceJson = existing.map(AnchorBindingEntity::getPasteCleaningEvidenceJson).orElse(null);
        String evidenceJson = PasteCleaningEvidenceSupport.resolveForUpsert(
                existingEvidenceJson,
                request.pasteCleaningEvidence(),
                request.clearPasteCleaningEvidence(),
                objectMapper
        );
        BindingValidationStatus status = computeBindingStatus(
                request.anchorId(),
                request.declaredContentType(),
                masterAnchors,
                List.of(),
                request.structuredContentJson(),
                declaredVariableKeys,
                masterId,
                evidenceJson
        );
        AnchorBindingEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.update(request.declaredContentType(), request.structuredContentJson(), status, evidenceJson);
        } else {
            entity = new AnchorBindingEntity(
                    UUID.randomUUID(),
                    version.getId(),
                    request.anchorId(),
                    request.declaredContentType(),
                    request.structuredContentJson(),
                    status
            );
            entity.setPasteCleaningEvidenceJson(evidenceJson);
        }
        anchorBindingRepository.save(entity);
        return templateViewMapper.toBindingView(entity);
    }

    @Transactional
    public List<CompositionRuleView> saveRules(
            TemplateVersionEntity version,
            List<CompositionRuleView> rules
    ) {
        try {
            version.setRulesJson(objectMapper.writeValueAsString(rules));
        } catch (JsonProcessingException exception) {
            throw new TemplateValidationException("api.error.template.invalidRulesJson");
        }
        templateVersionRepository.save(version);
        return templateViewMapper.loadRules(version);
    }

    @Transactional
    public BindingValidationView validateBindings(UUID masterId, TemplateVersionEntity version) {
        MasterDocumentEntity master = masterDocumentRepository.findByIdAndDeletedAtIsNull(masterId)
                .orElseThrow(MasterNotFoundException::new);
        Set<String> masterAnchors = new HashSet<>();
        master.getAnchors().forEach(anchor -> masterAnchors.add(anchor.getAnchorId()));
        List<AnchorBindingEntity> bindings = anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(
                version.getId()
        );
        Map<String, Integer> anchorCounts = new HashMap<>();
        bindings.forEach(binding -> anchorCounts.merge(binding.getAnchorId(), 1, Integer::sum));

        List<AnchorBindingView> views = new ArrayList<>();
        int valid = 0;
        int missing = 0;
        int duplicate = 0;
        int incompatible = 0;
        Set<String> declaredVariableKeys = statusSupport.loadDeclaredVariableKeys(version.getId());
        for (AnchorBindingEntity binding : bindings) {
            BindingValidationStatus status = computeBindingStatus(
                    binding.getAnchorId(),
                    binding.getDeclaredContentType(),
                    masterAnchors,
                    bindings.stream().map(AnchorBindingEntity::getAnchorId).toList(),
                    binding.getStructuredContentJson(),
                    declaredVariableKeys,
                    masterId,
                    binding.getPasteCleaningEvidenceJson()
            );
            if (status != binding.getValidationStatus()) {
                binding.update(
                        binding.getDeclaredContentType(),
                        binding.getStructuredContentJson(),
                        status,
                        binding.getPasteCleaningEvidenceJson()
                );
                anchorBindingRepository.save(binding);
            }
            views.add(templateViewMapper.toBindingView(binding));
            switch (status) {
                case VALID -> valid++;
                case MISSING_ANCHOR -> missing++;
                case DUPLICATE_BINDING -> duplicate++;
                case INCOMPATIBLE_CONTENT_TYPE -> incompatible++;
                default -> {
                }
            }
        }
        boolean blocking = missing > 0 || duplicate > 0 || incompatible > 0;
        BindingValidationSummaryView summary = new BindingValidationSummaryView(
                blocking,
                bindings.size(),
                valid,
                missing,
                duplicate,
                incompatible
        );
        return new BindingValidationView(views, summary);
    }

    BindingValidationStatus computeBindingStatus(
            String anchorId,
            AnchorContentType declaredContentType,
            Set<String> masterAnchors,
            List<String> allAnchorIds,
            String structuredContentJson,
            Set<String> declaredVariableKeys,
            UUID masterId
    ) {
        return computeBindingStatus(
                anchorId,
                declaredContentType,
                masterAnchors,
                allAnchorIds,
                structuredContentJson,
                declaredVariableKeys,
                masterId,
                null
        );
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
        return statusSupport.computeBindingStatus(
                anchorId,
                declaredContentType,
                masterAnchors,
                allAnchorIds,
                structuredContentJson,
                declaredVariableKeys,
                masterId,
                pasteCleaningEvidenceJson
        );
    }

    void validateVariableRequest(UpsertVariableSchemaRequest request) {
        statusSupport.validateVariableRequest(request);
    }
}
