package com.bank.docgen.template.service;

import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.contentmodule.service.ContentModuleAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.ContentModuleEffectiveExpirySummaryView;
import com.bank.docgen.template.api.ContentModuleReferenceValidationSummaryView;
import com.bank.docgen.template.api.ContentModuleReferenceView;
import com.bank.docgen.template.api.OutdatedClauseReferenceAuthorTaskView;
import com.bank.docgen.template.api.UpsertContentModuleReferenceRequest;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceEntity;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateContentModuleReferenceService {

    private final TemplateService templateService;
    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final TemplateContentModuleReferenceRepository referenceRepository;
    private final ContentModuleVersionRepository contentModuleVersionRepository;
    private final TemplateCurrentVersionResolver templateVersionSupport;
    private final TemplateContentModuleReferenceSupport referenceSupport;
    private final GroupAccessService groupAccessService;

    public TemplateContentModuleReferenceService(
            TemplateService templateService,
            TemplateRepository templateRepository,
            TemplateVersionRepository templateVersionRepository,
            TemplateContentModuleReferenceRepository referenceRepository,
            ContentModuleRepository contentModuleRepository,
            ContentModuleVersionRepository contentModuleVersionRepository,
            ContentModuleAccessService ContentModuleAccessService,
            TemplateCurrentVersionResolver templateVersionSupport,
            GroupAccessService groupAccessService
    ) {
        this.templateService = templateService;
        this.templateRepository = templateRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.referenceRepository = referenceRepository;
        this.contentModuleVersionRepository = contentModuleVersionRepository;
        this.templateVersionSupport = templateVersionSupport;
        this.groupAccessService = groupAccessService;
        this.referenceSupport = new TemplateContentModuleReferenceSupport(
                contentModuleRepository,
                contentModuleVersionRepository,
                ContentModuleAccessService
        );
    }

    @Transactional(readOnly = true)
    public List<OutdatedClauseReferenceAuthorTaskView> listOutdatedClauseReferenceAuthorTasks(
            ManagementSessionClaims session
    ) {
        if (!groupAccessService.canAuthorTemplates(session)) {
            return List.of();
        }
        List<String> groupCodes = groupAccessService.accessibleGroupCodes(session);
        if (groupCodes.isEmpty()) {
            return List.of();
        }
        List<TemplateEntity> draftTemplates;
        if (groupCodes.contains("*")) {
            draftTemplates = templateRepository.findByDeletedAtIsNullAndLifecycleStatusOrderByUpdatedAtDesc(
                    TemplateLifecycleStatus.DRAFT
            );
        } else {
            draftTemplates = templateRepository
                    .findByDeletedAtIsNullAndGroupCodeInAndLifecycleStatusOrderByUpdatedAtDesc(
                            groupCodes,
                            TemplateLifecycleStatus.DRAFT
                    );
        }
        List<OutdatedClauseReferenceAuthorTaskView> tasks = new ArrayList<>();
        for (TemplateEntity template : draftTemplates) {
            TemplateVersionEntity devVersion = templateVersionSupport.findInFlightDevVersion(template.getId())
                    .orElse(null);
            if (devVersion == null) {
                continue;
            }
            List<TemplateContentModuleReferenceEntity> references =
                    referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(devVersion.getId());
            int outdatedCount = referenceSupport.countOutdatedUnlockedReferences(references);
            if (outdatedCount <= 0) {
                continue;
            }
            tasks.add(new OutdatedClauseReferenceAuthorTaskView(
                    template.getId().toString(),
                    template.getExternalId(),
                    template.getGroupCode(),
                    template.getName(),
                    devVersion.getId().toString(),
                    outdatedCount,
                    template.getUpdatedAt()
            ));
        }
        return tasks;
    }

    @Transactional(readOnly = true)
    public List<ContentModuleReferenceView> listReferences(UUID templateId, ManagementSessionClaims session) {
        templateService.requireReadableTemplate(templateId, session);
        TemplateVersionEntity version = templateVersionSupport.requireExportableVersion(templateId);
        return referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(version.getId()).stream()
                .map(referenceSupport::toView)
                .toList();
    }

    @Transactional
    public ContentModuleReferenceView upsertReference(
            UUID templateId,
            UpsertContentModuleReferenceRequest request,
            ManagementSessionClaims session
    ) {
        return upsertReferenceInternal(templateId, request, session, false);
    }

    /**
     * CE-E01: wire content-module references during template import, including DRAFT
     * versions materialized in the same transaction (import-time referencable seam).
     */
    @Transactional
    public ContentModuleReferenceView upsertReferenceForImport(
            UUID templateId,
            UpsertContentModuleReferenceRequest request,
            ManagementSessionClaims session
    ) {
        return upsertReferenceInternal(templateId, request, session, true);
    }

    private ContentModuleReferenceView upsertReferenceInternal(
            UUID templateId,
            UpsertContentModuleReferenceRequest request,
            ManagementSessionClaims session,
            boolean importTimeDraftSeam
    ) {
        TemplateEntity template = templateService.requireWritableTemplate(templateId, session);
        TemplateVersionEntity version = templateVersionSupport.requireMutableInFlightDevVersion(templateId);
        assertDraft(template);
        String referenceKey = referenceSupport.normalizeReferenceKey(request.referenceKey());
        var existing = referenceRepository.findByTemplateVersionIdAndReferenceKey(version.getId(), referenceKey);
        if (existing.isPresent() && existing.get().isLockedFlag()) {
            throw new TemplateValidationException("api.error.template.contentModuleReferenceLocked");
        }
        ContentModuleVersionEntity moduleVersion = importTimeDraftSeam
                ? referenceSupport.resolveVersionForImport(
                        template, request.moduleId(), request.semanticVersion(), session)
                : referenceSupport.resolveReferencableVersion(
                        template, request.moduleId(), request.semanticVersion(), session);
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
        return referenceSupport.toView(entity);
    }

    @Transactional(readOnly = true)
    public ContentModuleReferenceValidationSummaryView validateReferences(UUID templateVersionId) {
        List<TemplateContentModuleReferenceEntity> references =
                referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(templateVersionId);
        int invalid = 0;
        for (TemplateContentModuleReferenceEntity reference : references) {
            if (!referenceSupport.isReferenceValidForPublish(reference)) {
                invalid++;
            }
        }
        return new ContentModuleReferenceValidationSummaryView(
                invalid > 0,
                references.size(),
                invalid
        );
    }

    /**
     * CE-K08: evaluate referenced content-module versions whose effectiveTo is strictly past utcNow.
     * Only resolved version entities are considered (orthogonal to CONTENT_MODULE_REFERENCES validity).
     */
    @Transactional(readOnly = true)
    public ContentModuleEffectiveExpirySummaryView evaluateEffectiveExpiry(UUID templateVersionId) {
        return evaluateEffectiveExpiry(templateVersionId, Instant.now());
    }

    @Transactional(readOnly = true)
    public ContentModuleEffectiveExpirySummaryView evaluateEffectiveExpiry(UUID templateVersionId, Instant utcNow) {
        List<TemplateContentModuleReferenceEntity> references =
                referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(templateVersionId);
        List<String> expiredDetails = new ArrayList<>();
        int resolved = 0;
        for (TemplateContentModuleReferenceEntity reference : references) {
            ContentModuleVersionEntity version = contentModuleVersionRepository
                    .findById(reference.getContentModuleVersionId())
                    .orElse(null);
            if (version == null) {
                continue;
            }
            resolved++;
            if (!version.isEffectiveExpired(utcNow)) {
                continue;
            }
            expiredDetails.add(referenceSupport.formatExpiredEffectiveDetail(version));
        }
        return new ContentModuleEffectiveExpirySummaryView(
                !expiredDetails.isEmpty(),
                expiredDetails.size(),
                resolved,
                expiredDetails
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

    private void assertDraft(TemplateEntity template) {
        if (template.getLifecycleStatus() != TemplateLifecycleStatus.DRAFT) {
            throw new TemplateValidationException("api.error.template.invalidState");
        }
    }
}
