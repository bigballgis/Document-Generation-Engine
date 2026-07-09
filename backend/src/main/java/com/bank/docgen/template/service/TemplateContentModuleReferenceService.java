package com.bank.docgen.template.service;

import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.contentmodule.service.ContentModuleAccessSupport;
import com.bank.docgen.contentmodule.service.ContentModuleNotFoundException;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.ContentModuleReferenceValidationSummaryView;
import com.bank.docgen.template.api.ContentModuleReferenceView;
import com.bank.docgen.template.api.UpsertContentModuleReferenceRequest;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceEntity;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateContentModuleReferenceService {

    private final TemplateService templateService;
    private final TemplateVersionRepository templateVersionRepository;
    private final TemplateContentModuleReferenceRepository referenceRepository;
    private final ContentModuleRepository contentModuleRepository;
    private final ContentModuleVersionRepository contentModuleVersionRepository;
    private final ContentModuleAccessSupport contentModuleAccessSupport;
    private final TemplateCurrentVersionResolver templateVersionSupport;

    public TemplateContentModuleReferenceService(
            TemplateService templateService,
            TemplateVersionRepository templateVersionRepository,
            TemplateContentModuleReferenceRepository referenceRepository,
            ContentModuleRepository contentModuleRepository,
            ContentModuleVersionRepository contentModuleVersionRepository,
            ContentModuleAccessSupport contentModuleAccessSupport,
            TemplateCurrentVersionResolver templateVersionSupport
    ) {
        this.templateService = templateService;
        this.templateVersionRepository = templateVersionRepository;
        this.referenceRepository = referenceRepository;
        this.contentModuleRepository = contentModuleRepository;
        this.contentModuleVersionRepository = contentModuleVersionRepository;
        this.contentModuleAccessSupport = contentModuleAccessSupport;
        this.templateVersionSupport = templateVersionSupport;
    }

    @Transactional(readOnly = true)
    public List<ContentModuleReferenceView> listReferences(UUID templateId, ManagementSessionClaims session) {
        templateService.requireReadableTemplate(templateId, session);
        TemplateVersionEntity version = templateVersionSupport.requireExportableVersion(templateId);
        return referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(version.getId()).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional
    public ContentModuleReferenceView upsertReference(
            UUID templateId,
            UpsertContentModuleReferenceRequest request,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = templateService.requireWritableTemplate(templateId, session);
        TemplateVersionEntity version = templateVersionSupport.requireMutableInFlightDevVersion(templateId);
        assertDraft(template);
        String referenceKey = normalizeReferenceKey(request.referenceKey());
        var existing = referenceRepository.findByTemplateVersionIdAndReferenceKey(version.getId(), referenceKey);
        if (existing.isPresent() && existing.get().isLockedFlag()) {
            throw new TemplateValidationException("api.error.template.contentModuleReferenceLocked");
        }
        ContentModuleVersionEntity moduleVersion = resolveReferencableVersion(
                template,
                request.moduleId(),
                request.semanticVersion(),
                session
        );
        TemplateContentModuleReferenceEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.updateContentModuleVersion(moduleVersion.getId());
        } else {
            entity = new TemplateContentModuleReferenceEntity(
                    UUID.randomUUID(),
                    version.getId(),
                    referenceKey,
                    moduleVersion.getId()
            );
        }
        referenceRepository.save(entity);
        return toView(entity);
    }

    @Transactional(readOnly = true)
    public ContentModuleReferenceValidationSummaryView validateReferences(UUID templateVersionId) {
        List<TemplateContentModuleReferenceEntity> references =
                referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(templateVersionId);
        int invalid = 0;
        for (TemplateContentModuleReferenceEntity reference : references) {
            if (!isReferenceValidForPublish(reference)) {
                invalid++;
            }
        }
        return new ContentModuleReferenceValidationSummaryView(
                invalid > 0,
                references.size(),
                invalid
        );
    }

    @Transactional
    public void lockReferencesForPublish(UUID templateVersionId) {
        List<TemplateContentModuleReferenceEntity> references =
                referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(templateVersionId);
        for (TemplateContentModuleReferenceEntity reference : references) {
            reference.lock();
        }
        referenceRepository.saveAll(references);
    }

    @Transactional(readOnly = true)
    public List<UUID> resolveLockedModuleVersionIds(UUID templateVersionId) {
        List<UUID> lockedVersionIds = new ArrayList<>();
        for (TemplateContentModuleReferenceEntity reference
                : referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(templateVersionId)) {
            if (reference.isLockedFlag()) {
                lockedVersionIds.add(reference.getContentModuleVersionId());
            }
        }
        return lockedVersionIds;
    }

    @Transactional(readOnly = true)
    public Map<String, String> resolvePinnedContentStructures(UUID templateVersionId) {
        Map<String, String> pinnedStructures = new LinkedHashMap<>();
        for (TemplateContentModuleReferenceEntity reference
                : referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(templateVersionId)) {
            contentModuleVersionRepository.findById(reference.getContentModuleVersionId())
                    .ifPresent(version -> pinnedStructures.put(
                            reference.getReferenceKey(),
                            version.getContentStructureJson()
                    ));
        }
        return pinnedStructures;
    }

    private boolean isReferenceValidForPublish(TemplateContentModuleReferenceEntity reference) {
        return contentModuleVersionRepository.findById(reference.getContentModuleVersionId())
                .map(version -> isReferenceVersionValidForPublish(reference, version))
                .orElse(false);
    }

    private boolean isReferenceVersionValidForPublish(
            TemplateContentModuleReferenceEntity reference,
            ContentModuleVersionEntity version
    ) {
        if (isPinnedStructureMissing(version)) {
            return false;
        }
        if (reference.isLockedFlag()) {
            return true;
        }
        return version.isReferencable();
    }

    private boolean isPinnedStructureMissing(ContentModuleVersionEntity version) {
        String structure = version.getContentStructureJson();
        return structure == null || structure.isBlank();
    }

    private ContentModuleVersionEntity resolveReferencableVersion(
            TemplateEntity template,
            String moduleId,
            String semanticVersion,
            ManagementSessionClaims session
    ) {
        ContentModuleEntity module = contentModuleAccessSupport.requireReadableModule(moduleId, session);
        assertTemplateCanReferenceModule(template, module);
        ContentModuleVersionEntity version = contentModuleVersionRepository
                .findByModuleIdAndSemanticVersion(module.getId(), semanticVersion.trim())
                .orElseThrow(() -> new TemplateValidationException("api.error.template.contentModuleReferenceMissing"));
        if (!version.isReferencable()) {
            throw new TemplateValidationException("api.error.template.contentModuleReferenceInvalid");
        }
        return version;
    }

    private void assertTemplateCanReferenceModule(TemplateEntity template, ContentModuleEntity module) {
        if (template.getGroupCode().equalsIgnoreCase(module.getGroupCode())) {
            return;
        }
        boolean shared = contentModuleAccessSupport.readSharedGroupCodes(module).stream()
                .anyMatch(code -> code.equalsIgnoreCase(template.getGroupCode()));
        if (!shared) {
            throw new TemplateValidationException("api.error.template.contentModuleReferenceInvalid");
        }
    }

    private ContentModuleReferenceView toView(TemplateContentModuleReferenceEntity reference) {
        ContentModuleVersionEntity version = contentModuleVersionRepository
                .findById(reference.getContentModuleVersionId())
                .orElseThrow(ContentModuleNotFoundException::new);
        ContentModuleEntity module = contentModuleRepository.findByIdAndDeletedAtIsNull(version.getModuleId())
                .orElseThrow(ContentModuleNotFoundException::new);
        return new ContentModuleReferenceView(
                reference.getReferenceKey(),
                contentModuleAccessSupport.publicModuleId(module),
                version.getSemanticVersion(),
                reference.isLockedFlag()
        );
    }

    private void assertDraft(TemplateEntity template) {
        if (template.getLifecycleStatus() != TemplateLifecycleStatus.DRAFT) {
            throw new TemplateValidationException("api.error.template.invalidState");
        }
    }

    private String normalizeReferenceKey(String referenceKey) {
        return referenceKey.trim().toUpperCase(Locale.ROOT);
    }
}
