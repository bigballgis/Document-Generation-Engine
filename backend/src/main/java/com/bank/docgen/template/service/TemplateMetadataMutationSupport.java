package com.bank.docgen.template.service;

import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.service.MasterNotFoundException;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.locale.LocaleLanguageCompatibility;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.CreateTemplateRequest;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.api.UpdateTemplateRequest;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.mapping.TemplateViewMapper;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/**
 * Package-private create / metadata-update bodies for TemplateService.
 */
final class TemplateMetadataMutationSupport {

    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final MasterDocumentRepository masterDocumentRepository;
    private final GroupAccessService groupAccessService;
    private final TemplateViewMapper templateViewMapper;
    private final TemplateAccessGuardSupport access;

    TemplateMetadataMutationSupport(
            TemplateRepository templateRepository,
            TemplateVersionRepository templateVersionRepository,
            MasterDocumentRepository masterDocumentRepository,
            GroupAccessService groupAccessService,
            TemplateViewMapper templateViewMapper,
            TemplateAccessGuardSupport access
    ) {
        this.templateRepository = templateRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.masterDocumentRepository = masterDocumentRepository;
        this.groupAccessService = groupAccessService;
        this.templateViewMapper = templateViewMapper;
        this.access = access;
    }

    TemplateDetailView create(CreateTemplateRequest request, ManagementSessionClaims session) {
        access.assertCanAuthorTemplates(session);
        if (!groupAccessService.canAccessGroup(session, request.groupCode())) {
            throw new TemplateAccessDeniedException();
        }
        String locale = requireValidLocale(request.locale());
        UUID familyId = request.localeVariantFamilyId();
        assertFamilyLocaleUnique(request.groupCode(), familyId, locale, null);
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
        template.setLocale(locale);
        template.setLocaleVariantFamilyId(familyId);
        templateRepository.save(template);
        TemplateVersionEntity version = new TemplateVersionEntity(UUID.randomUUID(), templateId, session.username());
        templateVersionRepository.save(version);
        return templateViewMapper.toDetail(template);
    }

    TemplateDetailView updateMetadata(
            UUID templateId,
            UpdateTemplateRequest request,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = access.requireWritable(templateId, session);
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
        boolean localeOrFamilyChanged = false;
        if (request.locale() != null) {
            template.setLocale(requireValidLocale(request.locale()));
            localeOrFamilyChanged = true;
        }
        if (request.localeVariantFamilyId() != null) {
            template.setLocaleVariantFamilyId(request.localeVariantFamilyId());
            localeOrFamilyChanged = true;
        }
        if (localeOrFamilyChanged) {
            assertFamilyLocaleUnique(
                    template.getGroupCode(),
                    template.getLocaleVariantFamilyId(),
                    template.getLocale(),
                    template.getId()
            );
        }
        template.setUpdatedBy(session.username());
        templateRepository.save(template);
        return templateViewMapper.toDetail(template);
    }

    private static String requireValidLocale(String locale) {
        try {
            return LocaleLanguageCompatibility.requireValidTag(locale);
        } catch (IllegalArgumentException ex) {
            throw new TemplateValidationException("api.error.template.localeRequired");
        }
    }

    private void assertFamilyLocaleUnique(
            String groupCode,
            UUID familyId,
            String locale,
            UUID excludingTemplateId
    ) {
        if (familyId == null) {
            return;
        }
        boolean conflict = excludingTemplateId == null
                ? templateRepository.existsByGroupCodeAndLocaleVariantFamilyIdAndLocaleAndDeletedAtIsNull(
                        groupCode, familyId, locale)
                : templateRepository
                        .existsByGroupCodeAndLocaleVariantFamilyIdAndLocaleAndDeletedAtIsNullAndIdNot(
                                groupCode, familyId, locale, excludingTemplateId);
        if (conflict) {
            throw new TemplateGovernanceException(
                    ApiErrorCodes.LOCALE_VARIANT_CONFLICT,
                    "api.error.template.localeVariantConflict",
                    HttpStatus.CONFLICT
            );
        }
    }
}
