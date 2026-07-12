package com.bank.docgen.template.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.api.TemplateDevVersionCreatedView;
import com.bank.docgen.template.api.TemplateVersionLineDetailView;
import com.bank.docgen.template.api.TemplateVersionLineSummaryView;
import com.bank.docgen.template.domain.ApprovalSubState;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.domain.TemplateVersionLineKind;
import com.bank.docgen.template.mapping.TemplateViewMapper;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceRepository;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateVersionLineService {
    private final TemplateService templateService;
    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final TemplateCurrentVersionResolver templateCurrentVersionResolver;
    private final VariableSchemaRepository variableSchemaRepository;
    private final AnchorBindingRepository anchorBindingRepository;
    private final TemplateViewMapper templateViewMapper;
    private final ApprovalSubStateResolver approvalSubStateResolver;
    private final GroupAccessService groupAccessService;
    private final ManagementUserDisplayService managementUserDisplayService;
    private final ApiPolicyRepository apiPolicyRepository;
    private final TemplateVersionLineCloneSupport cloneSupport;

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
            ManagementUserDisplayService managementUserDisplayService
    ) {
        this.templateService = templateService;
        this.templateRepository = templateRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.templateCurrentVersionResolver = templateCurrentVersionResolver;
        this.variableSchemaRepository = variableSchemaRepository;
        this.anchorBindingRepository = anchorBindingRepository;
        this.templateViewMapper = templateViewMapper;
        this.approvalSubStateResolver = approvalSubStateResolver;
        this.groupAccessService = groupAccessService;
        this.managementUserDisplayService = managementUserDisplayService;
        this.apiPolicyRepository = apiPolicyRepository;
        this.cloneSupport = new TemplateVersionLineCloneSupport(
                variableSchemaRepository,
                anchorBindingRepository,
                contentModuleReferenceRepository,
                lifecycleRecordRepository,
                messageResolver
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

        var rows = enrichSummaries(templateCurrentVersionResolver.listVersionLinesOrdered(templateId).stream()
                .map(version -> toSummary(version, template, defaultRoute, canAuthor, hasInFlight))
                .toList());

        return PageView.of(rows, page, size);
    }

    private List<TemplateVersionLineSummaryView> enrichSummaries(List<TemplateVersionLineSummaryView> summaries) {
        if (summaries.isEmpty()) {
            return summaries;
        }
        Set<String> usernames = summaries.stream()
                .map(TemplateVersionLineSummaryView::updatedBy)
                .filter(username -> username != null && !username.isBlank())
                .collect(Collectors.toSet());
        Map<String, String> displayNames = managementUserDisplayService.lookupDisplayNames(usernames);
        return summaries.stream()
                .map(summary -> new TemplateVersionLineSummaryView(
                        summary.devVersionId(),
                        summary.devVersionNumber(),
                        summary.releaseVersion(),
                        summary.lifecycleStatus(),
                        summary.approvalSubState(),
                        summary.lineKind(),
                        summary.updatedAt(),
                        summary.updatedBy(),
                        summary.defaultRouteTarget(),
                        summary.cloneable(),
                        summary.updatedBy() == null ? null : displayNames.get(summary.updatedBy())
                ))
                .toList();
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
        TemplateVersionLineSummaryView summary = toSummary(
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
                templateViewMapper.loadRules(version)
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
        String updatedBy = version.getCreatedBy();
        String updatedByDisplayName = updatedBy == null || updatedBy.isBlank()
                ? null
                : managementUserDisplayService.lookupDisplayNames(Set.of(updatedBy)).get(updatedBy);
        return new TemplateDetailView(
                detail.id(),
                detail.externalId(),
                detail.groupCode(),
                detail.name(),
                detail.description(),
                detail.masterId(),
                detail.lifecycleStatus(),
                detail.approvalSubState(),
                detail.releaseVersion(),
                detail.devVersionId(),
                detail.devVersionNumber(),
                detail.variables(),
                detail.bindings(),
                detail.rules(),
                detail.createdAt(),
                detail.updatedAt(),
                updatedBy,
                updatedByDisplayName,
                detail.readOnly()
        );
    }

    @Transactional
    public TemplateDevVersionCreatedView cloneReleaseVersion(
            UUID templateId,
            String releaseVersion,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = templateService.requireWritableTemplate(templateId, session);

        if (templateCurrentVersionResolver.hasInFlightDevVersion(templateId)) {
            throw new TemplateGovernanceException(
                    ApiErrorCodes.TEMPLATE_DEV_LINE_IN_FLIGHT,
                    "api.error.template.devLineInFlight",
                    HttpStatus.CONFLICT
            );
        }
        TemplateVersionEntity source = templateVersionRepository
                .findByTemplateIdAndReleaseVersion(templateId, releaseVersion)
                .orElseThrow(TemplateNotFoundException::new);

        if (source.getReleaseVersion() == null || source.getReleaseVersion().isBlank()) {
            throw new TemplateNotFoundException();
        }
        TemplateVersionEntity target = new TemplateVersionEntity(UUID.randomUUID(), templateId, session.username());
        target.setDevVersionNumber(templateCurrentVersionResolver.maxDevVersionNumber(templateId) + 1);
        target.setMasterCatalogVersion(source.getMasterCatalogVersion());
        target.setRulesJson(source.getRulesJson());
        target.setRenderProfileVersion(source.getRenderProfileVersion());
        target.setRenderProfileJson(source.getRenderProfileJson());
        templateVersionRepository.save(target);

        cloneSupport.copyVersionGraph(source, target);
        TemplateLifecycleStatus fromStatus = template.getLifecycleStatus();
        template.setLifecycleStatus(TemplateLifecycleStatus.DRAFT);
        template.setUpdatedBy(session.username());
        templateRepository.save(template);

        cloneSupport.recordCloneLifecycle(
                template,
                fromStatus,
                releaseVersion,
                target.getId(),
                target.getDevVersionNumber(),
                session
        );

        return new TemplateDevVersionCreatedView(
                target.getId().toString(),
                target.getDevVersionNumber()
        );
    }

    @Transactional
    public TemplateDetailView abandonInFlightDev(
            UUID templateId,
            UUID devVersionId,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = templateService.requireWritableTemplate(templateId, session);
        TemplateVersionEntity version = requireActiveDevVersionLine(templateId, devVersionId);

        if (!templateCurrentVersionResolver.isInFlight(version)) {
            throw new TemplateGovernanceException(
                    ApiErrorCodes.TEMPLATE_VERSION_IMMUTABLE,
                    "api.error.template.versionImmutable",
                    HttpStatus.FORBIDDEN
            );
        }
        TemplateLifecycleStatus fromStatus = template.getLifecycleStatus();

        version.setDeletedAt(Instant.now());
        templateVersionRepository.save(version);
        TemplateLifecycleStatus toStatus = templateCurrentVersionResolver.findLatestPublishedVersion(templateId)
                .map(TemplateVersionEntity::getLifecycleStatus)
                .orElse(TemplateLifecycleStatus.DRAFT);
        template.setLifecycleStatus(toStatus);
        template.setUpdatedBy(session.username());
        templateRepository.save(template);

        cloneSupport.recordAbandonLifecycle(
                template,
                fromStatus,
                toStatus,
                version.getDevVersionNumber(),
                devVersionId,
                session
        );

        return templateService.toDetail(template);
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

    private TemplateVersionLineSummaryView toSummary(
            TemplateVersionEntity version,
            TemplateEntity template,
            String defaultRouteReleaseVersion,
            boolean canAuthor,
            boolean hasInFlight
    ) {
        boolean inFlight = templateCurrentVersionResolver.isInFlight(version);
        TemplateVersionLineKind lineKind = inFlight ? TemplateVersionLineKind.IN_FLIGHT : TemplateVersionLineKind.PUBLISHED;
        Boolean defaultRouteTarget = null;
        if (!inFlight && version.getReleaseVersion() != null) {
            defaultRouteTarget = version.getReleaseVersion().equals(defaultRouteReleaseVersion);
        }
        ApprovalSubState approvalSubState = inFlight ? approvalSubStateResolver.resolve(template) : null;
        boolean cloneable = !inFlight && canAuthor && !hasInFlight;

        return new TemplateVersionLineSummaryView(
                version.getId().toString(),
                version.getDevVersionNumber(),
                version.getReleaseVersion(),
                resolveLifecycleStatus(version, template, inFlight),
                approvalSubState,
                lineKind,
                version.getUpdatedAt(),
                template.getUpdatedBy(),
                defaultRouteTarget,
                cloneable,
                null
        );
    }

    private TemplateLifecycleStatus resolveLifecycleStatus(
            TemplateVersionEntity version,
            TemplateEntity template,
            boolean inFlight
    ) {
        if (inFlight) {
            return template.getLifecycleStatus();
        }
        return version.getLifecycleStatus();
    }
}
