package com.bank.docgen.template.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.service.GroupAccessService;
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
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.util.List;
import java.util.UUID;
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

    public TemplateService(
            TemplateRepository templateRepository,
            TemplateVersionRepository templateVersionRepository,
            MasterDocumentRepository masterDocumentRepository,
            ApiPolicyRepository apiPolicyRepository,
            GroupAccessService groupAccessService,
            TemplateStructuredAuthoringService structuredAuthoringService,
            TemplateBindingConfigurationService bindingConfigurationService,
            TemplateViewMapper templateViewMapper,
            TemplateCurrentVersionResolver templateVersionSupport
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
        return templates.stream().map(templateViewMapper::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public TemplateDetailView get(UUID templateId, ManagementSessionClaims session) {
        TemplateEntity template = requireReadableTemplate(templateId, session);
        return templateViewMapper.toDetail(template);
    }

    @Transactional(readOnly = true)
    public List<TemplateReleaseVersionView> listReleaseVersions(UUID templateId, ManagementSessionClaims session) {
        TemplateEntity template = requireReadableTemplate(templateId, session);
        String defaultRoute = apiPolicyRepository.findByTemplateId(templateId)
                .map(ApiPolicyEntity::getDefaultRouteReleaseVersion)
                .orElse(null);
        return templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(template.getId()).stream()
                .filter(version -> version.getReleaseVersion() != null && !version.getReleaseVersion().isBlank())
                .map(version -> new TemplateReleaseVersionView(
                        version.getReleaseVersion(),
                        version.getDevVersionNumber(),
                        version.getLifecycleStatus(),
                        version.getUpdatedAt(),
                        version.getCreatedBy(),
                        defaultRoute != null && defaultRoute.equals(version.getReleaseVersion())
                ))
                .toList();
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
        assertDraft(template);
        return bindingConfigurationService.upsertVariable(version, request);
    }

    @Transactional
    public void deleteVariable(UUID templateId, String variableKey, ManagementSessionClaims session) {
        TemplateEntity template = requireWritableTemplate(templateId, session);
        TemplateVersionEntity version = templateVersionSupport.requireMutableInFlightDevVersion(templateId);
        assertDraft(template);
        bindingConfigurationService.deleteVariable(version.getId(), variableKey);
    }

    @Transactional
    public AnchorBindingView upsertBinding(
            UUID templateId,
            UpsertAnchorBindingRequest request,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = requireWritableTemplate(templateId, session);
        TemplateVersionEntity version = templateVersionSupport.requireMutableInFlightDevVersion(templateId);
        assertDraft(template);
        return bindingConfigurationService.upsertBinding(template.getMasterId(), version, request);
    }

    @Transactional
    public List<CompositionRuleView> saveRules(
            UUID templateId,
            List<CompositionRuleView> rules,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = requireWritableTemplate(templateId, session);
        TemplateVersionEntity version = templateVersionSupport.requireMutableInFlightDevVersion(templateId);
        assertDraft(template);
        return bindingConfigurationService.saveRules(version, rules);
    }

    @Transactional(readOnly = true)
    public BindingValidationView validateBindings(UUID templateId, ManagementSessionClaims session) {
        TemplateEntity template = requireReadableTemplate(templateId, session);
        TemplateVersionEntity version = templateVersionSupport.requireInFlightDevVersion(templateId);
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

    TemplateDetailView toDetail(TemplateEntity template) {
        return templateViewMapper.toDetail(template);
    }

    List<CompositionRuleView> loadRules(TemplateVersionEntity version) {
        return templateViewMapper.loadRules(version);
    }
}
