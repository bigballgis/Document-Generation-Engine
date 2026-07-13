package com.bank.docgen.template.service;

import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.service.MasterNotFoundException;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.AnchorBindingView;
import com.bank.docgen.template.api.BindingValidationView;
import com.bank.docgen.template.api.CompositionRuleView;
import com.bank.docgen.template.api.CreateTemplateRequest;
import com.bank.docgen.template.api.MasterStyleCatalogView;
import com.bank.docgen.template.api.PasteCleanRequest;
import com.bank.docgen.template.api.PasteCleanResultView;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.api.TemplateReleaseVersionView;
import com.bank.docgen.template.api.TemplateSummaryView;
import com.bank.docgen.template.api.UpdateTemplateRequest;
import com.bank.docgen.template.api.UpsertAnchorBindingRequest;
import com.bank.docgen.template.api.UpsertVariableSchemaRequest;
import com.bank.docgen.template.api.VariableSchemaView;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.mapping.TemplateViewMapper;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.event.TemplateContentChangedEvent;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final MasterDocumentRepository masterDocumentRepository;
    private final ApiPolicyRepository apiPolicyRepository;
    private final GroupAccessService groupAccessService;
    private final TemplateStructuredAuthoringService structuredAuthoringService;
    private final TemplateBindingConfigurationService bindingConfigurationService;
    private final TemplateViewMapper templateViewMapper;
    private final TemplateCurrentVersionResolver templateVersionSupport;
    private final ApplicationEventPublisher eventPublisher;
    private final TemplateCatalogSupport catalogSupport;
    private final TemplateDisplayEnrichmentSupport displayEnrichment;
    private final TemplateAccessGuardSupport access;

    public TemplateService(
            TemplateRepository templateRepository,
            TemplateVersionRepository templateVersionRepository,
            MasterDocumentRepository masterDocumentRepository,
            ApiPolicyRepository apiPolicyRepository,
            GroupAccessService groupAccessService,
            TemplateStructuredAuthoringService structuredAuthoringService,
            TemplateBindingConfigurationService bindingConfigurationService,
            TemplateViewMapper templateViewMapper,
            TemplateCurrentVersionResolver templateVersionSupport,
            ApplicationEventPublisher eventPublisher,
            ManagementUserDisplayService managementUserDisplayService
    ) {
        this.templateRepository = templateRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.masterDocumentRepository = masterDocumentRepository;
        this.apiPolicyRepository = apiPolicyRepository;
        this.groupAccessService = groupAccessService;
        this.structuredAuthoringService = structuredAuthoringService;
        this.bindingConfigurationService = bindingConfigurationService;
        this.templateViewMapper = templateViewMapper;
        this.templateVersionSupport = templateVersionSupport;
        this.eventPublisher = eventPublisher;
        this.displayEnrichment = new TemplateDisplayEnrichmentSupport(managementUserDisplayService);
        this.catalogSupport = new TemplateCatalogSupport(
                templateRepository,
                groupAccessService,
                templateViewMapper,
                displayEnrichment
        );
        this.access = new TemplateAccessGuardSupport(templateRepository, groupAccessService);
    }

    @Transactional(readOnly = true)
    public PageView<TemplateSummaryView> list(ManagementSessionClaims session, Integer page, Integer size) {
        return list(session, page, size, null, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public PageView<TemplateSummaryView> list(
            ManagementSessionClaims session,
            Integer page,
            Integer size,
            String search,
            String groupCode,
            String lifecycleStatus,
            String approvalSubState,
            String sort
    ) {
        return catalogSupport.list(session, page, size, search, groupCode, lifecycleStatus, approvalSubState, sort);
    }

    @Transactional(readOnly = true)
    public List<TemplateSummaryView> listAll(ManagementSessionClaims session) {
        return catalogSupport.listAll(session);
    }

    @Transactional(readOnly = true)
    public TemplateDetailView get(UUID templateId, ManagementSessionClaims session) {
        return templateViewMapper.toDetail(requireReadableTemplate(templateId, session));
    }

    @Transactional(readOnly = true)
    public List<TemplateReleaseVersionView> listReleaseVersions(UUID templateId, ManagementSessionClaims session) {
        TemplateEntity template = requireReadableTemplate(templateId, session);
        String defaultRoute = apiPolicyRepository.findByTemplateId(templateId)
                .map(ApiPolicyEntity::getDefaultRouteReleaseVersion)
                .orElse(null);
        return displayEnrichment.enrichReleaseVersions(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(template.getId()).stream()
                .filter(version -> version.getReleaseVersion() != null && !version.getReleaseVersion().isBlank())
                .map(version -> new TemplateReleaseVersionView(
                        version.getReleaseVersion(),
                        version.getDevVersionNumber(),
                        version.getLifecycleStatus(),
                        version.getUpdatedAt(),
                        version.getCreatedBy(),
                        null,
                        defaultRoute != null && defaultRoute.equals(version.getReleaseVersion())
                ))
                .toList());
    }

    @Transactional
    public TemplateDetailView create(CreateTemplateRequest request, ManagementSessionClaims session) {
        access.assertCanAuthorTemplates(session);
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
        return templateViewMapper.toDetail(template);
    }

    @Transactional
    public TemplateDetailView updateMetadata(
            UUID templateId,
            UpdateTemplateRequest request,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = requireWritableTemplate(templateId, session);
        if (template.getLifecycleStatus() == TemplateLifecycleStatus.PUBLISHED
                || template.getLifecycleStatus() == TemplateLifecycleStatus.STOPPED
                || template.getLifecycleStatus() == TemplateLifecycleStatus.DEPRECATED) {
            throw new TemplateValidationException("api.error.template.invalidState");
        }
        if (request.name() != null && !request.name().isBlank()) {
            template.setName(request.name());
        }
        if (request.description() != null) {
            template.setDescription(request.description());
        }
        template.setUpdatedBy(session.username());
        templateRepository.save(template);
        return templateViewMapper.toDetail(template);
    }

    @Transactional
    public VariableSchemaView upsertVariable(
            UUID templateId,
            UpsertVariableSchemaRequest request,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = requireWritableTemplate(templateId, session);
        TemplateVersionEntity version = templateVersionSupport.requireMutableInFlightDevVersion(templateId);
        access.assertDraft(template);
        VariableSchemaView result = bindingConfigurationService.upsertVariable(version, request);
        eventPublisher.publishEvent(new TemplateContentChangedEvent(this, templateId));
        return result;
    }

    @Transactional
    public void deleteVariable(UUID templateId, String variableKey, ManagementSessionClaims session) {
        TemplateEntity template = requireWritableTemplate(templateId, session);
        TemplateVersionEntity version = templateVersionSupport.requireMutableInFlightDevVersion(templateId);
        access.assertDraft(template);
        bindingConfigurationService.deleteVariable(version.getId(), variableKey);
        eventPublisher.publishEvent(new TemplateContentChangedEvent(this, templateId));
    }

    @Transactional
    public AnchorBindingView upsertBinding(
            UUID templateId,
            UpsertAnchorBindingRequest request,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = requireWritableTemplate(templateId, session);
        TemplateVersionEntity version = templateVersionSupport.requireMutableInFlightDevVersion(templateId);
        access.assertDraft(template);
        AnchorBindingView result = bindingConfigurationService.upsertBinding(template.getMasterId(), version, request);
        eventPublisher.publishEvent(new TemplateContentChangedEvent(this, templateId));
        return result;
    }

    @Transactional
    public List<CompositionRuleView> saveRules(
            UUID templateId,
            List<CompositionRuleView> rules,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = requireWritableTemplate(templateId, session);
        TemplateVersionEntity version = templateVersionSupport.requireMutableInFlightDevVersion(templateId);
        access.assertDraft(template);
        List<CompositionRuleView> result = bindingConfigurationService.saveRules(version, rules);
        eventPublisher.publishEvent(new TemplateContentChangedEvent(this, templateId));
        return result;
    }

    @Transactional(readOnly = true)
    public BindingValidationView validateBindings(UUID templateId, ManagementSessionClaims session) {
        TemplateEntity template = requireReadableTemplate(templateId, session);
        TemplateVersionEntity version = templateVersionSupport.requireInFlightDevVersion(templateId);
        return bindingConfigurationService.validateBindings(template.getMasterId(), version);
    }

    @Transactional(readOnly = true)
    public BindingValidationView validateBindingsForVersion(
            UUID templateId,
            TemplateVersionEntity version,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = requireReadableTemplate(templateId, session);
        return bindingConfigurationService.validateBindings(template.getMasterId(), version);
    }

    @Transactional(readOnly = true)
    public MasterStyleCatalogView getMasterStyleCatalog(UUID templateId, ManagementSessionClaims session) {
        TemplateEntity template = requireReadableTemplate(templateId, session);
        return structuredAuthoringService.getMasterStyleCatalog(template.getMasterId());
    }

    @Transactional(readOnly = true)
    public PasteCleanResultView pasteClean(
            UUID templateId,
            PasteCleanRequest request,
            ManagementSessionClaims session
    ) {
        requireWritableTemplate(templateId, session);
        return structuredAuthoringService.pasteClean(request);
    }

    public TemplateEntity requireReadableTemplate(UUID templateId, ManagementSessionClaims session) {
        return access.requireReadable(templateId, session);
    }

    TemplateEntity requireWritableTemplate(UUID templateId, ManagementSessionClaims session) {
        return access.requireWritable(templateId, session);
    }

    public TemplateEntity requireTemplateByExternalId(String externalId) {
        return access.requireByExternalId(externalId);
    }

    public record TemplateDisplayInfo(String name, String externalId) {
    }

    @Transactional(readOnly = true)
    public Map<UUID, TemplateDisplayInfo> lookupDisplayInfoByIds(Set<UUID> templateIds) {
        if (templateIds == null || templateIds.isEmpty()) {
            return Map.of();
        }
        List<UUID> distinctIds = templateIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        return templateRepository.findByIdInAndDeletedAtIsNull(distinctIds).stream()
                .collect(Collectors.toMap(
                        TemplateEntity::getId,
                        template -> new TemplateDisplayInfo(template.getName(), template.getExternalId())
                ));
    }

    TemplateDetailView toDetail(TemplateEntity template) {
        return templateViewMapper.toDetail(template);
    }

    List<CompositionRuleView> loadRules(TemplateVersionEntity version) {
        return templateViewMapper.loadRules(version);
    }
}
