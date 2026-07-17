package com.bank.docgen.template.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.AuthorWordPageCountView;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.api.TemplateDevVersionCreatedView;
import com.bank.docgen.template.api.TemplateExportMasterPinView;
import com.bank.docgen.template.api.TemplateVersionLineDetailView;
import com.bank.docgen.template.api.TemplateVersionLineSummaryView;
import com.bank.docgen.template.api.UpdateAuthorWordPageCountRequest;
import com.bank.docgen.template.mapping.TemplateMasterPinMapper;
import com.bank.docgen.template.mapping.TemplateViewMapper;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceRepository;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateVersionLineService {
    private final TemplateService templateService;
    private final TemplateVersionRepository templateVersionRepository;
    private final TemplateCurrentVersionResolver templateCurrentVersionResolver;
    private final VariableSchemaRepository variableSchemaRepository;
    private final AnchorBindingRepository anchorBindingRepository;
    private final TemplateViewMapper templateViewMapper;
    private final GroupAccessService groupAccessService;
    private final ApiPolicyRepository apiPolicyRepository;
    private final TemplateVersionLineViewSupport viewSupport;
    private final TemplateVersionLineMutationSupport mutations;
    private final TemplateMasterPinMapper templateMasterPinMapper;

    public TemplateVersionLineService(
            TemplateService templateService,
            TemplateRepository templateRepository,
            TemplateVersionRepository templateVersionRepository,
            TemplateCurrentVersionResolver templateCurrentVersionResolver,
            VariableSchemaRepository variableSchemaRepository,
            AnchorBindingRepository anchorBindingRepository,
            TemplateContentModuleReferenceRepository contentModuleReferenceRepository,
            TemplateLifecycleRecordRepository lifecycleRecordRepository,
            ApiPolicyRepository apiPolicyRepository,
            TemplateViewMapper templateViewMapper,
            ApprovalSubStateResolver approvalSubStateResolver,
            GroupAccessService groupAccessService,
            MessageResolver messageResolver,
            ManagementUserDisplayService managementUserDisplayService,
            TemplateMasterPinMapper templateMasterPinMapper
    ) {
        this.templateService = templateService;
        this.templateVersionRepository = templateVersionRepository;
        this.templateCurrentVersionResolver = templateCurrentVersionResolver;
        this.variableSchemaRepository = variableSchemaRepository;
        this.anchorBindingRepository = anchorBindingRepository;
        this.templateViewMapper = templateViewMapper;
        this.groupAccessService = groupAccessService;
        this.apiPolicyRepository = apiPolicyRepository;
        this.templateMasterPinMapper = templateMasterPinMapper;
        TemplateVersionLineCloneSupport cloneSupport = new TemplateVersionLineCloneSupport(
                variableSchemaRepository,
                anchorBindingRepository,
                contentModuleReferenceRepository,
                lifecycleRecordRepository,
                messageResolver
        );
        this.viewSupport = new TemplateVersionLineViewSupport(
                templateCurrentVersionResolver,
                approvalSubStateResolver,
                managementUserDisplayService
        );
        this.mutations = new TemplateVersionLineMutationSupport(
                templateRepository,
                templateVersionRepository,
                templateCurrentVersionResolver,
                cloneSupport
        );
    }

    @Transactional(readOnly = true)
    public PageView<TemplateVersionLineSummaryView> list(
            UUID templateId,
            int page,
            int size,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = templateService.requireReadableTemplate(templateId, session);
        String defaultRoute = defaultRouteReleaseVersion(templateId);
        boolean canAuthor = groupAccessService.canAuthorTemplates(session);
        boolean hasInFlight = templateCurrentVersionResolver.hasInFlightDevVersion(templateId);

        var rows = viewSupport.enrichSummaries(templateCurrentVersionResolver.listVersionLinesOrdered(templateId).stream()
                .map(version -> viewSupport.toSummary(version, template, defaultRoute, canAuthor, hasInFlight))
                .toList());

        return PageView.of(rows, page, size);
    }

    @Transactional(readOnly = true)
    public TemplateVersionLineDetailView get(
            UUID templateId,
            UUID versionLineId,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = templateService.requireReadableTemplate(templateId, session);
        TemplateVersionEntity version = requireDevVersionLine(templateId, versionLineId);
        String defaultRoute = defaultRouteReleaseVersion(templateId);
        boolean canAuthor = groupAccessService.canAuthorTemplates(session);
        boolean hasInFlight = templateCurrentVersionResolver.hasInFlightDevVersion(templateId);
        TemplateVersionLineSummaryView summary = viewSupport.toSummary(
                version, template, defaultRoute, canAuthor, hasInFlight
        );

        return new TemplateVersionLineDetailView(
                summary.devVersionId(),
                summary.devVersionNumber(),
                summary.releaseVersion(),
                summary.lifecycleStatus(),
                summary.approvalSubState(),
                summary.lineKind(),
                summary.updatedAt(),
                summary.updatedBy(),
                summary.updatedByDisplayName(),
                summary.defaultRouteTarget(),
                summary.cloneable(),
                variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(version.getId()).stream()
                        .map(templateViewMapper::toVariableView)
                        .toList(),
                anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(version.getId()).stream()
                        .map(templateViewMapper::toBindingView)
                        .toList(),
                templateViewMapper.loadRules(version),
                templateMasterPinMapper.toView(version)
        );
    }

    @Transactional(readOnly = true)
    public TemplateDetailView getDevDetail(
            UUID templateId,
            UUID devVersionId,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = templateService.requireReadableTemplate(templateId, session);
        TemplateVersionEntity version = requireDevVersionLine(templateId, devVersionId);

        if (!templateCurrentVersionResolver.isInFlight(version)) {
            throw new TemplateGovernanceException(
                    ApiErrorCodes.TEMPLATE_VERSION_IMMUTABLE,
                    "api.error.template.versionImmutable",
                    HttpStatus.FORBIDDEN
            );
        }

        return templateViewMapper.toDetailForVersion(template, version, false);
    }

    @Transactional(readOnly = true)
    public TemplateDetailView getReleaseDetail(
            UUID templateId,
            String releaseVersion,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = templateService.requireReadableTemplate(templateId, session);
        TemplateVersionEntity version = templateVersionRepository
                .findByTemplateIdAndReleaseVersion(templateId, releaseVersion)
                .orElseThrow(TemplateNotFoundException::new);

        TemplateDetailView detail = templateViewMapper.toDetailForVersion(template, version, true);
        TemplateExportMasterPinView masterPin = templateMasterPinMapper.toView(version);
        return viewSupport.overlayReleaseDetailUpdatedBy(detail, version, masterPin);
    }

    @Transactional
    public TemplateDevVersionCreatedView cloneReleaseVersion(
            UUID templateId,
            String releaseVersion,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = templateService.requireWritableTemplate(templateId, session);
        return mutations.cloneReleaseVersion(template, releaseVersion, session);
    }

    @Transactional
    public TemplateDetailView abandonInFlightDev(
            UUID templateId,
            UUID devVersionId,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = templateService.requireWritableTemplate(templateId, session);
        TemplateVersionEntity version = requireActiveDevVersionLine(templateId, devVersionId);
        return mutations.abandonInFlightDev(
                template,
                templateId,
                devVersionId,
                version,
                session,
                (entity, ignored) -> templateService.toDetail(entity)
        );
    }

    @Transactional(readOnly = true)
    public AuthorWordPageCountView getAuthorWordPageCount(UUID templateId, ManagementSessionClaims session) {
        templateService.requireReadableTemplate(templateId, session);
        TemplateVersionEntity version = templateCurrentVersionResolver.requireInFlightDevVersion(templateId);
        return new AuthorWordPageCountView(
                templateId.toString(),
                version.getId().toString(),
                version.getAuthorWordPageCount()
        );
    }

    @Transactional
    public AuthorWordPageCountView updateAuthorWordPageCount(
            UUID templateId,
            UpdateAuthorWordPageCountRequest request,
            ManagementSessionClaims session
    ) {
        templateService.requireWritableTemplate(templateId, session);
        TemplateVersionEntity version = templateCurrentVersionResolver.requireInFlightDevVersion(templateId);
        if (!templateCurrentVersionResolver.isInFlight(version)) {
            throw new TemplateGovernanceException(
                    ApiErrorCodes.TEMPLATE_VERSION_IMMUTABLE,
                    "api.error.template.versionImmutable",
                    HttpStatus.FORBIDDEN
            );
        }
        Integer pageCount = request == null ? null : request.authorWordPageCount();
        version.setAuthorWordPageCount(pageCount);
        templateVersionRepository.save(version);
        return new AuthorWordPageCountView(
                templateId.toString(),
                version.getId().toString(),
                version.getAuthorWordPageCount()
        );
    }

    private TemplateVersionEntity requireDevVersionLine(UUID templateId, UUID devVersionId) {
        TemplateVersionEntity version = templateVersionRepository.findById(devVersionId)
                .orElseThrow(TemplateNotFoundException::new);
        if (!version.getTemplateId().equals(templateId)) {
            throw new TemplateNotFoundException();
        }
        return version;
    }

    private TemplateVersionEntity requireActiveDevVersionLine(UUID templateId, UUID devVersionId) {
        TemplateVersionEntity version = requireDevVersionLine(templateId, devVersionId);
        if (version.isDeleted()) {
            throw new TemplateNotFoundException();
        }
        return version;
    }

    private String defaultRouteReleaseVersion(UUID templateId) {
        return apiPolicyRepository.findByTemplateId(templateId)
                .map(ApiPolicyEntity::getDefaultRouteReleaseVersion)
                .orElse(null);
    }
}
