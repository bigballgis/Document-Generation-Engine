package com.bank.docgen.template.service;

import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.service.MasterNotFoundException;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.AnchorBindingView;
import com.bank.docgen.template.api.BindingValidationSummaryView;
import com.bank.docgen.template.api.BindingValidationView;
import com.bank.docgen.template.api.CreateTemplateRequest;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.api.TemplateSummaryView;
import com.bank.docgen.template.api.UpsertAnchorBindingRequest;
import com.bank.docgen.template.api.UpsertVariableSchemaRequest;
import com.bank.docgen.template.api.VariableSchemaView;
import com.bank.docgen.template.domain.AnchorContentType;
import com.bank.docgen.template.domain.BindingValidationStatus;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.domain.VariableType;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.fasterxml.jackson.databind.JsonNode;
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
public class TemplateService {

    private static final Set<VariableType> SUPPORTED_TYPES = Set.of(VariableType.values());

    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final VariableSchemaRepository variableSchemaRepository;
    private final AnchorBindingRepository anchorBindingRepository;
    private final MasterDocumentRepository masterDocumentRepository;
    private final GroupAccessService groupAccessService;
    private final ObjectMapper objectMapper;

    public TemplateService(
            TemplateRepository templateRepository,
            TemplateVersionRepository templateVersionRepository,
            VariableSchemaRepository variableSchemaRepository,
            AnchorBindingRepository anchorBindingRepository,
            MasterDocumentRepository masterDocumentRepository,
            GroupAccessService groupAccessService,
            ObjectMapper objectMapper
    ) {
        this.templateRepository = templateRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.variableSchemaRepository = variableSchemaRepository;
        this.anchorBindingRepository = anchorBindingRepository;
        this.masterDocumentRepository = masterDocumentRepository;
        this.groupAccessService = groupAccessService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<TemplateSummaryView> list(ManagementSessionClaims session) {
        List<String> groupCodes = groupAccessService.accessibleGroupCodes(session);
        List<TemplateEntity> templates;
        if (groupCodes.contains("*")) {
            templates = templateRepository.findByDeletedAtIsNullOrderByUpdatedAtDesc();
        } else if (groupCodes.isEmpty()) {
            return List.of();
        } else {
            templates = templateRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(groupCodes);
        }
        return templates.stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public TemplateDetailView get(UUID templateId, ManagementSessionClaims session) {
        TemplateEntity template = requireReadableTemplate(templateId, session);
        return toDetail(template);
    }

    @Transactional
    public TemplateDetailView create(CreateTemplateRequest request, ManagementSessionClaims session) {
        assertCanAuthorTemplates(session);
        if (!groupAccessService.canAccessGroup(session, request.groupCode())) {
            throw new TemplateAccessDeniedException();
        }
        UUID masterId = UUID.fromString(request.masterId());
        MasterDocumentEntity master = masterDocumentRepository.findByIdAndDeletedAtIsNull(masterId)
                .orElseThrow(MasterNotFoundException::new);
        if (master.getStatus() != MasterDocumentStatus.APPROVED) {
            throw new TemplateValidationException("api.error.template.masterNotApproved");
        }
        if (!master.getGroupCode().equals(request.groupCode())) {
            throw new TemplateValidationException("api.error.template.masterGroupMismatch");
        }
        if (templateRepository.findByExternalIdAndDeletedAtIsNull(request.externalId()).isPresent()) {
            throw new TemplateValidationException("api.error.template.externalIdExists");
        }
        UUID templateId = UUID.randomUUID();
        TemplateEntity template = new TemplateEntity(
                templateId,
                request.externalId(),
                request.groupCode(),
                request.name(),
                request.description(),
                masterId,
                session.username()
        );
        templateRepository.save(template);
        TemplateVersionEntity version = new TemplateVersionEntity(UUID.randomUUID(), templateId, session.username());
        templateVersionRepository.save(version);
        return toDetail(template);
    }

    @Transactional
    public VariableSchemaView upsertVariable(
            UUID templateId,
            UpsertVariableSchemaRequest request,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = requireWritableTemplate(templateId, session);
        assertDraft(template);
        validateVariableRequest(request);
        TemplateVersionEntity version = currentDevVersion(templateId);
        var existing = variableSchemaRepository.findByTemplateVersionIdAndVariableKey(version.getId(), request.variableKey());
        VariableSchemaEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.update(
                    request.variableType(),
                    request.required(),
                    request.defaultValue(),
                    request.enumValues(),
                    request.description()
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
                    request.description()
            );
        }
        variableSchemaRepository.save(entity);
        return toVariableView(entity);
    }

    @Transactional
    public void deleteVariable(UUID templateId, String variableKey, ManagementSessionClaims session) {
        TemplateEntity template = requireWritableTemplate(templateId, session);
        assertDraft(template);
        TemplateVersionEntity version = currentDevVersion(templateId);
        variableSchemaRepository.findByTemplateVersionIdAndVariableKey(version.getId(), variableKey)
                .ifPresent(variableSchemaRepository::delete);
    }

    @Transactional
    public AnchorBindingView upsertBinding(
            UUID templateId,
            UpsertAnchorBindingRequest request,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = requireWritableTemplate(templateId, session);
        assertDraft(template);
        validateStructuredContent(request.structuredContentJson());
        TemplateVersionEntity version = currentDevVersion(templateId);
        MasterDocumentEntity master = masterDocumentRepository.findByIdAndDeletedAtIsNull(template.getMasterId())
                .orElseThrow(MasterNotFoundException::new);
        Set<String> masterAnchors = new HashSet<>();
        master.getAnchors().forEach(anchor -> masterAnchors.add(anchor.getAnchorId()));
        BindingValidationStatus status = computeBindingStatus(
                request.anchorId(),
                request.declaredContentType(),
                masterAnchors,
                List.of()
        );
        var existing = anchorBindingRepository.findByTemplateVersionIdAndAnchorId(version.getId(), request.anchorId());
        AnchorBindingEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.update(request.declaredContentType(), request.structuredContentJson(), status);
        } else {
            entity = new AnchorBindingEntity(
                    UUID.randomUUID(),
                    version.getId(),
                    request.anchorId(),
                    request.declaredContentType(),
                    request.structuredContentJson(),
                    status
            );
        }
        anchorBindingRepository.save(entity);
        return toBindingView(entity);
    }

    @Transactional(readOnly = true)
    public BindingValidationView validateBindings(UUID templateId, ManagementSessionClaims session) {
        TemplateEntity template = requireReadableTemplate(templateId, session);
        TemplateVersionEntity version = currentDevVersion(templateId);
        MasterDocumentEntity master = masterDocumentRepository.findByIdAndDeletedAtIsNull(template.getMasterId())
                .orElseThrow(MasterNotFoundException::new);
        Set<String> masterAnchors = new HashSet<>();
        master.getAnchors().forEach(anchor -> masterAnchors.add(anchor.getAnchorId()));
        List<AnchorBindingEntity> bindings = anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(version.getId());
        Map<String, Integer> anchorCounts = new HashMap<>();
        bindings.forEach(binding -> anchorCounts.merge(binding.getAnchorId(), 1, Integer::sum));

        List<AnchorBindingView> views = new ArrayList<>();
        int valid = 0;
        int missing = 0;
        int duplicate = 0;
        int incompatible = 0;
        for (AnchorBindingEntity binding : bindings) {
            BindingValidationStatus status = computeBindingStatus(
                    binding.getAnchorId(),
                    binding.getDeclaredContentType(),
                    masterAnchors,
                    bindings.stream().map(AnchorBindingEntity::getAnchorId).toList()
            );
            if (status != binding.getValidationStatus()) {
                binding.update(binding.getDeclaredContentType(), binding.getStructuredContentJson(), status);
                anchorBindingRepository.save(binding);
            }
            views.add(toBindingView(binding));
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

    private BindingValidationStatus computeBindingStatus(
            String anchorId,
            AnchorContentType declaredContentType,
            Set<String> masterAnchors,
            List<String> allAnchorIds
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
        return BindingValidationStatus.VALID;
    }

    private void validateVariableRequest(UpsertVariableSchemaRequest request) {
        if (!SUPPORTED_TYPES.contains(request.variableType())) {
            throw new TemplateValidationException("api.error.template.variableTypeUnsupported");
        }
        if (request.variableType() == VariableType.ENUM
                && (request.enumValues() == null || request.enumValues().isBlank())) {
            throw new TemplateValidationException("api.error.template.enumValuesRequired");
        }
    }

    private void validateStructuredContent(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.has("nodes")) {
                throw new TemplateValidationException("api.error.template.structuredContentInvalid");
            }
        } catch (TemplateValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TemplateValidationException("api.error.template.structuredContentInvalid");
        }
    }

    private TemplateVersionEntity currentDevVersion(UUID templateId) {
        return templateVersionRepository.findByTemplateIdAndDevVersionNumber(templateId, 1)
                .orElseThrow(TemplateNotFoundException::new);
    }

    private void assertDraft(TemplateEntity template) {
        if (template.getLifecycleStatus() != TemplateLifecycleStatus.DRAFT) {
            throw new TemplateValidationException("api.error.template.invalidState");
        }
    }

    private void assertCanAuthorTemplates(ManagementSessionClaims session) {
        if (!groupAccessService.canAuthorTemplates(session)) {
            throw new TemplateAccessDeniedException();
        }
    }

    public TemplateEntity requireReadableTemplate(UUID templateId, ManagementSessionClaims session) {
        TemplateEntity template = templateRepository.findByIdAndDeletedAtIsNull(templateId)
                .orElseThrow(TemplateNotFoundException::new);
        if (!groupAccessService.canAccessGroup(session, template.getGroupCode())) {
            throw new TemplateAccessDeniedException();
        }
        return template;
    }

    TemplateEntity requireWritableTemplate(UUID templateId, ManagementSessionClaims session) {
        TemplateEntity template = requireReadableTemplate(templateId, session);
        assertCanAuthorTemplates(session);
        return template;
    }

    public TemplateEntity requireTemplateByExternalId(String externalId) {
        return templateRepository.findByExternalIdAndDeletedAtIsNull(externalId)
                .orElseThrow(TemplateNotFoundException::new);
    }

    private TemplateSummaryView toSummary(TemplateEntity template) {
        return new TemplateSummaryView(
                template.getId().toString(),
                template.getExternalId(),
                template.getGroupCode(),
                template.getName(),
                template.getLifecycleStatus(),
                template.getReleaseVersion(),
                template.getMasterId().toString(),
                template.getUpdatedAt()
        );
    }

    TemplateDetailView toDetail(TemplateEntity template) {
        TemplateVersionEntity version = currentDevVersion(template.getId());
        List<VariableSchemaView> variables = variableSchemaRepository
                .findByTemplateVersionIdOrderByVariableKeyAsc(version.getId())
                .stream()
                .map(this::toVariableView)
                .toList();
        List<AnchorBindingView> bindings = anchorBindingRepository
                .findByTemplateVersionIdOrderByAnchorIdAsc(version.getId())
                .stream()
                .map(this::toBindingView)
                .toList();
        return new TemplateDetailView(
                template.getId().toString(),
                template.getExternalId(),
                template.getGroupCode(),
                template.getName(),
                template.getDescription(),
                template.getMasterId().toString(),
                template.getLifecycleStatus(),
                template.getReleaseVersion(),
                version.getId().toString(),
                version.getDevVersionNumber(),
                variables,
                bindings,
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }

    private VariableSchemaView toVariableView(VariableSchemaEntity entity) {
        return new VariableSchemaView(
                entity.getId().toString(),
                entity.getVariableKey(),
                entity.getVariableType(),
                entity.isRequired(),
                entity.getDefaultValue(),
                entity.getEnumValues(),
                entity.getDescription()
        );
    }

    private AnchorBindingView toBindingView(AnchorBindingEntity entity) {
        return new AnchorBindingView(
                entity.getId().toString(),
                entity.getAnchorId(),
                entity.getDeclaredContentType().name(),
                entity.getStructuredContentJson(),
                entity.getValidationStatus()
        );
    }
}
