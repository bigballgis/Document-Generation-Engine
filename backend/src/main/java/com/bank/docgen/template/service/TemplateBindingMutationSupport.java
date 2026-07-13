package com.bank.docgen.template.service;

import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.service.MasterNotFoundException;
import com.bank.docgen.template.api.AnchorBindingView;
import com.bank.docgen.template.api.CompositionRuleView;
import com.bank.docgen.template.api.UpsertAnchorBindingRequest;
import com.bank.docgen.template.api.UpsertVariableSchemaRequest;
import com.bank.docgen.template.api.VariableSchemaView;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Package-private variable / binding / rules mutation bodies.
 */
final class TemplateBindingMutationSupport {

    private final VariableSchemaRepository variableSchemaRepository;
    private final AnchorBindingRepository anchorBindingRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final MasterDocumentRepository masterDocumentRepository;
    private final ObjectMapper objectMapper;
    private final TemplateViewMapper templateViewMapper;
    private final TemplateBindingStatusSupport statusSupport;

    TemplateBindingMutationSupport(
            VariableSchemaRepository variableSchemaRepository,
            AnchorBindingRepository anchorBindingRepository,
            TemplateVersionRepository templateVersionRepository,
            MasterDocumentRepository masterDocumentRepository,
            ObjectMapper objectMapper,
            TemplateViewMapper templateViewMapper,
            TemplateBindingStatusSupport statusSupport
    ) {
        this.variableSchemaRepository = variableSchemaRepository;
        this.anchorBindingRepository = anchorBindingRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.masterDocumentRepository = masterDocumentRepository;
        this.objectMapper = objectMapper;
        this.templateViewMapper = templateViewMapper;
        this.statusSupport = statusSupport;
    }

    VariableSchemaView upsertVariable(TemplateVersionEntity version, UpsertVariableSchemaRequest request) {
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

    void deleteVariable(UUID templateVersionId, String variableKey) {
        variableSchemaRepository.findByTemplateVersionIdAndVariableKey(templateVersionId, variableKey)
                .ifPresent(variableSchemaRepository::delete);
    }

    AnchorBindingView upsertBinding(
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
        BindingValidationStatus status = statusSupport.computeBindingStatus(
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

    List<CompositionRuleView> saveRules(TemplateVersionEntity version, List<CompositionRuleView> rules) {
        try {
            version.setRulesJson(objectMapper.writeValueAsString(rules));
        } catch (JsonProcessingException exception) {
            throw new TemplateValidationException("api.error.template.invalidRulesJson");
        }
        templateVersionRepository.save(version);
        return templateViewMapper.loadRules(version);
    }
}
