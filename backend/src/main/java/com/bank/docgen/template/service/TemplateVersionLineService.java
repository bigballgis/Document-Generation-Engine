package com.bank.docgen.template.service;



import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;

import com.bank.docgen.authorization.management.api.PageView;

import com.bank.docgen.authorization.management.service.GroupAccessService;

import com.bank.docgen.infrastructure.i18n.MessageResolver;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;

import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;

import com.bank.docgen.template.api.TemplateDetailView;

import com.bank.docgen.template.api.TemplateDevVersionCreatedView;

import com.bank.docgen.template.api.TemplateVersionLineDetailView;

import com.bank.docgen.template.api.TemplateVersionLineSummaryView;

import com.bank.docgen.template.domain.ApprovalSubState;

import com.bank.docgen.template.domain.LifecycleAction;

import com.bank.docgen.template.domain.TemplateLifecycleStatus;

import com.bank.docgen.template.domain.TemplateVersionLineKind;

import com.bank.docgen.template.mapping.TemplateViewMapper;

import com.bank.docgen.template.persistence.AnchorBindingEntity;

import com.bank.docgen.template.persistence.AnchorBindingRepository;

import com.bank.docgen.template.persistence.TemplateContentModuleReferenceEntity;

import com.bank.docgen.template.persistence.TemplateContentModuleReferenceRepository;

import com.bank.docgen.template.persistence.TemplateEntity;

import com.bank.docgen.template.persistence.TemplateLifecycleRecordEntity;

import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;

import com.bank.docgen.template.persistence.TemplateRepository;

import com.bank.docgen.template.persistence.TemplateVersionEntity;

import com.bank.docgen.template.persistence.TemplateVersionRepository;

import com.bank.docgen.template.persistence.VariableSchemaEntity;

import com.bank.docgen.template.persistence.VariableSchemaRepository;

import java.util.UUID;

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

    private final TemplateContentModuleReferenceRepository contentModuleReferenceRepository;

    private final TemplateLifecycleRecordRepository lifecycleRecordRepository;

    private final ApiPolicyRepository apiPolicyRepository;

    private final TemplateViewMapper templateViewMapper;

    private final ApprovalSubStateResolver approvalSubStateResolver;

    private final GroupAccessService groupAccessService;

    private final MessageResolver messageResolver;



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

            MessageResolver messageResolver

    ) {

        this.templateService = templateService;

        this.templateRepository = templateRepository;

        this.templateVersionRepository = templateVersionRepository;

        this.templateCurrentVersionResolver = templateCurrentVersionResolver;

        this.variableSchemaRepository = variableSchemaRepository;

        this.anchorBindingRepository = anchorBindingRepository;

        this.contentModuleReferenceRepository = contentModuleReferenceRepository;

        this.lifecycleRecordRepository = lifecycleRecordRepository;

        this.apiPolicyRepository = apiPolicyRepository;

        this.templateViewMapper = templateViewMapper;

        this.approvalSubStateResolver = approvalSubStateResolver;

        this.groupAccessService = groupAccessService;

        this.messageResolver = messageResolver;

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

        var rows = templateCurrentVersionResolver.listVersionLinesOrdered(templateId).stream()

                .map(version -> toSummary(version, template, defaultRoute, canAuthor, hasInFlight))

                .toList();

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

        return templateViewMapper.toDetailForVersion(template, version, true);

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

        copyVariables(source, target);

        copyBindings(source, target);

        copyContentModuleReferences(source, target);



        TemplateLifecycleStatus fromStatus = template.getLifecycleStatus();

        template.setLifecycleStatus(TemplateLifecycleStatus.DRAFT);

        template.setUpdatedBy(session.username());

        templateRepository.save(template);



        recordCloneLifecycle(

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



    private void copyVariables(TemplateVersionEntity source, TemplateVersionEntity target) {

        for (VariableSchemaEntity variable : variableSchemaRepository

                .findByTemplateVersionIdOrderByVariableKeyAsc(source.getId())) {

            variableSchemaRepository.save(new VariableSchemaEntity(

                    UUID.randomUUID(),

                    target.getId(),

                    variable.getVariableKey(),

                    variable.getVariableType(),

                    variable.isRequired(),

                    variable.getDefaultValue(),

                    variable.getEnumValues(),

                    variable.getDescription()

            ));

        }

    }



    private void copyBindings(TemplateVersionEntity source, TemplateVersionEntity target) {

        for (AnchorBindingEntity binding : anchorBindingRepository

                .findByTemplateVersionIdOrderByAnchorIdAsc(source.getId())) {

            anchorBindingRepository.save(new AnchorBindingEntity(

                    UUID.randomUUID(),

                    target.getId(),

                    binding.getAnchorId(),

                    binding.getDeclaredContentType(),

                    binding.getStructuredContentJson(),

                    binding.getValidationStatus()

            ));

        }

    }



    private void copyContentModuleReferences(TemplateVersionEntity source, TemplateVersionEntity target) {

        for (TemplateContentModuleReferenceEntity reference : contentModuleReferenceRepository

                .findByTemplateVersionIdOrderByReferenceKeyAsc(source.getId())) {

            TemplateContentModuleReferenceEntity copied = new TemplateContentModuleReferenceEntity(

                    UUID.randomUUID(),

                    target.getId(),

                    reference.getReferenceKey(),

                    reference.getContentModuleVersionId()

            );

            contentModuleReferenceRepository.save(copied);

        }

    }



    private void recordCloneLifecycle(

            TemplateEntity template,

            TemplateLifecycleStatus fromStatus,

            String sourceReleaseVersion,

            UUID newDevVersionId,

            int newDevVersionNumber,

            ManagementSessionClaims session

    ) {

        String comment = messageResolver.resolve(

                "api.audit.lifecycle.clonedRelease",

                sourceReleaseVersion,

                newDevVersionNumber,

                newDevVersionId

        );

        lifecycleRecordRepository.save(new TemplateLifecycleRecordEntity(

                UUID.randomUUID(),

                template.getId(),

                LifecycleAction.CLONE_FROM_RELEASE,

                fromStatus,

                TemplateLifecycleStatus.DRAFT,

                null,

                comment,

                sourceReleaseVersion,

                session.username()

        ));

    }



    private TemplateVersionEntity requireDevVersionLine(UUID templateId, UUID devVersionId) {

        TemplateVersionEntity version = templateVersionRepository.findById(devVersionId)

                .orElseThrow(TemplateNotFoundException::new);

        if (!version.getTemplateId().equals(templateId)) {

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

                cloneable

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


