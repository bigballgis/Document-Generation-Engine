package com.bank.docgen.template.service;

import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
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
import com.bank.docgen.template.mapping.TemplateViewMapper;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateService {

    private final GroupAccessService groupAccessService;
    private final TemplateViewMapper templateViewMapper;
    private final TemplateCatalogSupport catalogSupport;
    private final TemplateAccessGuardSupport access;
    private final TemplateMetadataMutationSupport metadataMutations;
    private final TemplateInFlightContentMutationSupport contentMutations;
    private final TemplateReleaseVersionListSupport releaseVersions;
    private final TemplateDisplayLookupSupport displayLookup;
    private final TemplateReadQuerySupport readQueries;

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
        this.groupAccessService = groupAccessService;
        this.templateViewMapper = templateViewMapper;
        var displayEnrichment = new TemplateDisplayEnrichmentSupport(managementUserDisplayService);
        this.access = new TemplateAccessGuardSupport(templateRepository, groupAccessService);
        this.catalogSupport = new TemplateCatalogSupport(
                templateRepository, groupAccessService, templateViewMapper, displayEnrichment);
        this.metadataMutations = new TemplateMetadataMutationSupport(
                templateRepository, templateVersionRepository, masterDocumentRepository,
                groupAccessService, templateViewMapper, access);
        this.contentMutations = new TemplateInFlightContentMutationSupport(
                this, templateVersionSupport, access, bindingConfigurationService, eventPublisher);
        this.releaseVersions = new TemplateReleaseVersionListSupport(
                templateVersionRepository, apiPolicyRepository, displayEnrichment, access);
        this.displayLookup = new TemplateDisplayLookupSupport(templateRepository);
        this.readQueries = new TemplateReadQuerySupport(
                access, templateVersionSupport, bindingConfigurationService, structuredAuthoringService);
    }

    @Transactional(readOnly = true)
    public PageView<TemplateSummaryView> list(ManagementSessionClaims session, Integer page, Integer size) {
        return list(session, page, size, null, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public PageView<TemplateSummaryView> list(
            ManagementSessionClaims session, Integer page, Integer size, String search, String groupCode,
            String lifecycleStatus, String approvalSubState, String sort
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
        return releaseVersions.listReleaseVersions(templateId, session);
    }

    @Transactional
    public TemplateDetailView create(CreateTemplateRequest request, ManagementSessionClaims session) {
        return metadataMutations.create(request, session);
    }

    @Transactional
    public TemplateDetailView updateMetadata(
            UUID templateId, UpdateTemplateRequest request, ManagementSessionClaims session
    ) {
        return metadataMutations.updateMetadata(templateId, request, session);
    }

    @Transactional
    public VariableSchemaView upsertVariable(
            UUID templateId, UpsertVariableSchemaRequest request, ManagementSessionClaims session
    ) {
        return contentMutations.upsertVariable(templateId, request, session);
    }

    @Transactional
    public void deleteVariable(UUID templateId, String variableKey, ManagementSessionClaims session) {
        contentMutations.deleteVariable(templateId, variableKey, session);
    }

    @Transactional
    public AnchorBindingView upsertBinding(
            UUID templateId, UpsertAnchorBindingRequest request, ManagementSessionClaims session
    ) {
        return contentMutations.upsertBinding(templateId, request, session);
    }

    @Transactional
    public List<CompositionRuleView> saveRules(
            UUID templateId, List<CompositionRuleView> rules, ManagementSessionClaims session
    ) {
        return contentMutations.saveRules(templateId, rules, session);
    }

    @Transactional(readOnly = true)
    public BindingValidationView validateBindings(UUID templateId, ManagementSessionClaims session) {
        return readQueries.validateBindings(templateId, session);
    }

    @Transactional(readOnly = true)
    public BindingValidationView validateBindingsForVersion(
            UUID templateId, TemplateVersionEntity version, ManagementSessionClaims session
    ) {
        return readQueries.validateBindingsForVersion(templateId, version, session);
    }

    @Transactional(readOnly = true)
    public MasterStyleCatalogView getMasterStyleCatalog(UUID templateId, ManagementSessionClaims session) {
        return readQueries.getMasterStyleCatalog(templateId, session);
    }

    @Transactional(readOnly = true)
    public PasteCleanResultView pasteClean(
            UUID templateId, PasteCleanRequest request, ManagementSessionClaims session
    ) {
        return readQueries.pasteClean(templateId, request, session);
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
        return displayLookup.lookupDisplayInfoByIds(templateIds);
    }

    TemplateDetailView toDetail(TemplateEntity template) {
        return templateViewMapper.toDetail(template);
    }

    List<CompositionRuleView> loadRules(TemplateVersionEntity version) {
        return templateViewMapper.loadRules(version);
    }
}
