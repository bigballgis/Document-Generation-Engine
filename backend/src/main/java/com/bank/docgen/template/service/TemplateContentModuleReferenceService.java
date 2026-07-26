package com.bank.docgen.template.service;

import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.contentmodule.api.ContentModuleNestingPublishSummaryView;
import com.bank.docgen.contentmodule.service.ContentModuleAccessService;
import com.bank.docgen.contentmodule.service.ContentModuleNestingService;
import com.bank.docgen.sharedkernel.locale.LocaleLanguageCompatibility;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.ContentModuleEffectiveExpirySummaryView;
import com.bank.docgen.template.api.ContentModuleEffectiveNotStartedSummaryView;
import com.bank.docgen.template.api.ContentModuleLocaleMismatchSummaryView;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateContentModuleReferenceService {

    private final TemplateService templateService;
    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final TemplateContentModuleReferenceRepository referenceRepository;
    private final ContentModuleRepository contentModuleRepository;
    private final ContentModuleVersionRepository contentModuleVersionRepository;
    private final TemplateCurrentVersionResolver templateVersionSupport;
    private final TemplateContentModuleReferenceSupport referenceSupport;
    private final GroupAccessService groupAccessService;
    private final ContentModuleNestingService nestingService;

    public TemplateContentModuleReferenceService(
            TemplateService templateService,
            TemplateRepository templateRepository,
            TemplateVersionRepository templateVersionRepository,
            TemplateContentModuleReferenceRepository referenceRepository,
            ContentModuleRepository contentModuleRepository,
            ContentModuleVersionRepository contentModuleVersionRepository,
            ContentModuleAccessService ContentModuleAccessService,
            TemplateCurrentVersionResolver templateVersionSupport,
            GroupAccessService groupAccessService,
            ContentModuleNestingService nestingService
    ) {
        this.templateService = templateService;
        this.templateRepository = templateRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.referenceRepository = referenceRepository;
        this.contentModuleRepository = contentModuleRepository;
        this.contentModuleVersionRepository = contentModuleVersionRepository;
        this.templateVersionSupport = templateVersionSupport;
        this.groupAccessService = groupAccessService;
        this.nestingService = nestingService;
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

    /**
     * IBL-E5: evaluate referenced content-module versions whose effectiveFrom is strictly after utcNow.
     * Orthogonal to {@link #evaluateEffectiveExpiry} and CONTENT_MODULE_REFERENCES validity.
     */
    @Transactional(readOnly = true)
    public ContentModuleEffectiveNotStartedSummaryView evaluateEffectiveNotStarted(UUID templateVersionId) {
        return evaluateEffectiveNotStarted(templateVersionId, Instant.now());
    }

    @Transactional(readOnly = true)
    public ContentModuleEffectiveNotStartedSummaryView evaluateEffectiveNotStarted(
            UUID templateVersionId,
            Instant utcNow
    ) {
        List<TemplateContentModuleReferenceEntity> references =
                referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(templateVersionId);
        List<String> notStartedDetails = new ArrayList<>();
        int resolved = 0;
        for (TemplateContentModuleReferenceEntity reference : references) {
            ContentModuleVersionEntity version = contentModuleVersionRepository
                    .findById(reference.getContentModuleVersionId())
                    .orElse(null);
            if (version == null) {
                continue;
            }
            resolved++;
            if (!version.isEffectiveNotStarted(utcNow)) {
                continue;
            }
            notStartedDetails.add(referenceSupport.formatNotStartedEffectiveDetail(reference, version));
        }
        return new ContentModuleEffectiveNotStartedSummaryView(
                !notStartedDetails.isEmpty(),
                notStartedDetails.size(),
                resolved,
                notStartedDetails
        );
    }

    /**
     * IBL-E1: referenced content-module locale must be language-compatible with the template locale.
     */
    @Transactional(readOnly = true)
    public ContentModuleLocaleMismatchSummaryView evaluateLocaleMismatch(UUID templateVersionId) {
        TemplateVersionEntity templateVersion = templateVersionRepository.findById(templateVersionId).orElse(null);
        if (templateVersion == null) {
            return new ContentModuleLocaleMismatchSummaryView(false, 0, 0, List.of());
        }
        TemplateEntity template = templateRepository.findByIdAndDeletedAtIsNull(templateVersion.getTemplateId())
                .orElse(null);
        if (template == null || LocaleLanguageCompatibility.isBlankOrMissing(template.getLocale())) {
            return new ContentModuleLocaleMismatchSummaryView(false, 0, 0, List.of());
        }
        String templateLocale = template.getLocale();
        List<TemplateContentModuleReferenceEntity> references =
                referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(templateVersionId);
        List<String> mismatchDetails = new ArrayList<>();
        int resolved = 0;
        for (TemplateContentModuleReferenceEntity reference : references) {
            ContentModuleVersionEntity version = contentModuleVersionRepository
                    .findById(reference.getContentModuleVersionId())
                    .orElse(null);
            if (version == null) {
                continue;
            }
            ContentModuleEntity module = contentModuleRepository.findByIdAndDeletedAtIsNull(version.getModuleId())
                    .orElse(null);
            if (module == null) {
                continue;
            }
            resolved++;
            if (LocaleLanguageCompatibility.areCompatible(templateLocale, module.getLocale())) {
                continue;
            }
            mismatchDetails.add(
                    module.getModuleCode()
                            + "@"
                            + version.getSemanticVersion()
                            + " locale="
                            + module.getLocale()
            );
        }
        return new ContentModuleLocaleMismatchSummaryView(
                !mismatchDetails.isEmpty(),
                mismatchDetails.size(),
                resolved,
                mismatchDetails
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
            // FOS-W7-1: missing pinned clause versions fail closed (no silent omission).
            ContentModuleVersionEntity version = contentModuleVersionRepository
                    .findById(reference.getContentModuleVersionId())
                    .orElseThrow(() -> new TemplateValidationException(
                            "api.error.validation.contentModuleStructureMissing"
                    ));
            pinnedStructures.put(reference.getReferenceKey(), version.getContentStructureJson());
        }
        return pinnedStructures;
    }

    /**
     * IBL-E6 / ADR-0067 — nesting cycle / depth / unpinned summary for publish-gate.
     */
    @Transactional(readOnly = true)
    public ContentModuleNestingPublishSummaryView evaluateNestingClosure(UUID templateVersionId) {
        return nestingService.evaluatePublishClosure(resolvePinnedContentStructures(templateVersionId));
    }

    @Transactional(readOnly = true)
    public Set<String> listReferenceKeys(UUID templateVersionId) {
        Set<String> keys = new LinkedHashSet<>();
        for (TemplateContentModuleReferenceEntity reference
                : referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(templateVersionId)) {
            keys.add(reference.getReferenceKey());
        }
        return Set.copyOf(keys);
    }

    /**
     * Pinned CM jurisdictions keyed by referenceKey (for ADR-0063 E2-C10 mismatch check).
     */
    @Transactional(readOnly = true)
    public Map<String, String> resolvePinnedJurisdictions(UUID templateVersionId) {
        Map<String, String> jurisdictions = new LinkedHashMap<>();
        for (TemplateContentModuleReferenceEntity reference
                : referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(templateVersionId)) {
            contentModuleVersionRepository.findById(reference.getContentModuleVersionId())
                    .ifPresent(version -> jurisdictions.put(
                            reference.getReferenceKey(),
                            version.getJurisdiction()
                    ));
        }
        return jurisdictions;
    }

    private void assertDraft(TemplateEntity template) {
        if (template.getLifecycleStatus() != TemplateLifecycleStatus.DRAFT) {
            throw new TemplateValidationException("api.error.template.invalidState");
        }
    }
}
