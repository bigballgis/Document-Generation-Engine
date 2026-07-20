package com.bank.docgen.template.service;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.contentmodule.service.ContentModuleAccessService;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.BulkRepinContentModuleReferencesRequest;
import com.bank.docgen.template.api.BulkRepinContentModuleReferencesResultView;
import com.bank.docgen.template.api.BulkRepinItemStatus;
import com.bank.docgen.template.api.BulkRepinItemView;
import com.bank.docgen.template.api.BulkRepinSummaryView;
import com.bank.docgen.template.api.UpsertContentModuleReferenceRequest;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceEntity;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * IBL-E5 / ADR-0066 — group-scoped bulk re-pin of draft template content-module references.
 */
@Service
public class BulkRepinContentModuleReferencesService {

    private final GroupAccessService groupAccessService;
    private final ContentModuleAccessService contentModuleAccessService;
    private final ContentModuleVersionRepository contentModuleVersionRepository;
    private final TemplateRepository templateRepository;
    private final TemplateContentModuleReferenceRepository referenceRepository;
    private final TemplateCurrentVersionResolver templateVersionSupport;
    private final TemplateContentModuleReferenceService referenceService;
    private final TemplateContentModuleReferenceSupport referenceSupport;
    private final ManagementAuditRecorder auditRecorder;

    public BulkRepinContentModuleReferencesService(
            GroupAccessService groupAccessService,
            ContentModuleAccessService contentModuleAccessService,
            ContentModuleRepository contentModuleRepository,
            ContentModuleVersionRepository contentModuleVersionRepository,
            TemplateRepository templateRepository,
            TemplateContentModuleReferenceRepository referenceRepository,
            TemplateCurrentVersionResolver templateVersionSupport,
            TemplateContentModuleReferenceService referenceService,
            ManagementAuditRecorder auditRecorder
    ) {
        this.groupAccessService = groupAccessService;
        this.contentModuleAccessService = contentModuleAccessService;
        this.contentModuleVersionRepository = contentModuleVersionRepository;
        this.templateRepository = templateRepository;
        this.referenceRepository = referenceRepository;
        this.templateVersionSupport = templateVersionSupport;
        this.referenceService = referenceService;
        this.referenceSupport = new TemplateContentModuleReferenceSupport(
                contentModuleRepository,
                contentModuleVersionRepository,
                contentModuleAccessService
        );
        this.auditRecorder = auditRecorder;
    }

    public BulkRepinContentModuleReferencesResultView bulkRepin(
            BulkRepinContentModuleReferencesRequest request,
            ManagementSessionClaims session
    ) {
        if (!groupAccessService.canAuthorTemplates(session)) {
            throw new TemplateAccessDeniedException();
        }
        if (request.dryRun() == null) {
            throw new TemplateGovernanceException(
                    ApiErrorCodes.REQUEST_BODY_INVALID,
                    "api.error.template.bulkRepin.dryRunRequired",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }
        boolean dryRun = request.dryRun();
        String groupCode = resolveGroupCode(session, request.groupCode());
        ContentModuleEntity module = contentModuleAccessService.requireReadableModule(
                request.contentModuleId(),
                session
        );
        TargetResolution target = resolveTarget(module, request);
        List<TemplateEntity> candidates = listCandidateTemplates(session, groupCode, request.templateIds());
        List<BulkRepinItemView> items = new ArrayList<>();
        for (TemplateEntity template : candidates) {
            items.addAll(evaluateTemplate(
                    template, module, target, request.fromSemanticVersion(), dryRun, session));
        }
        BulkRepinSummaryView summary = summarize(dryRun, items);
        BulkRepinContentModuleReferencesResultView result = new BulkRepinContentModuleReferencesResultView(
                module.getId().toString(),
                groupCode,
                blankToNull(request.fromSemanticVersion()),
                target.resolvedSemanticVersion(),
                target.useLatestApproved(),
                summary,
                items
        );
        auditRecorder.recordContentModuleBulkRepin(
                module.getId(),
                groupCode,
                session.username(),
                contentModuleAccessService.actorSummary(session),
                dryRun,
                blankToNull(request.fromSemanticVersion()),
                target.resolvedSemanticVersion(),
                target.useLatestApproved(),
                summary,
                items
        );
        return result;
    }

    private String resolveGroupCode(ManagementSessionClaims session, String requestedGroup) {
        if (requestedGroup != null && !requestedGroup.isBlank()) {
            String group = requestedGroup.trim();
            if (!groupAccessService.canAccessGroup(session, group)) {
                throw new TemplateAccessDeniedException();
            }
            return group;
        }
        List<String> accessible = groupAccessService.accessibleGroupCodes(session);
        if (accessible.contains("*") || accessible.size() != 1) {
            throw new TemplateGovernanceException(
                    ApiErrorCodes.REQUEST_BODY_INVALID,
                    "api.error.template.bulkRepin.groupCodeRequired",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }
        return accessible.getFirst();
    }

    private TargetResolution resolveTarget(
            ContentModuleEntity module,
            BulkRepinContentModuleReferencesRequest request
    ) {
        boolean hasTo = request.toSemanticVersion() != null && !request.toSemanticVersion().isBlank();
        boolean useLatest = Boolean.TRUE.equals(request.useLatestApproved());
        if (hasTo == useLatest) {
            throw new TemplateGovernanceException(
                    ApiErrorCodes.REQUEST_BODY_INVALID,
                    "api.error.template.bulkRepin.targetXor",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }
        if (useLatest) {
            Optional<ContentModuleVersionEntity> latest =
                    referenceSupport.findLatestReferencableApprovedVersion(module.getId());
            return latest
                    .map(version -> new TargetResolution(version.getSemanticVersion(), true, null))
                    .orElseGet(() -> new TargetResolution(null, true, ApiErrorCodes.BULK_REPIN_TARGET_INVALID));
        }
        String semantic = request.toSemanticVersion().trim();
        ContentModuleVersionEntity version = contentModuleVersionRepository
                .findByModuleIdAndSemanticVersion(module.getId(), semantic)
                .orElse(null);
        if (version == null || !version.isReferencable()) {
            return new TargetResolution(semantic, false, ApiErrorCodes.BULK_REPIN_TARGET_INVALID);
        }
        return new TargetResolution(version.getSemanticVersion(), false, null);
    }

    private List<TemplateEntity> listCandidateTemplates(
            ManagementSessionClaims session,
            String groupCode,
            List<String> templateIds
    ) {
        Set<UUID> filterIds = parseTemplateIds(templateIds);
        List<TemplateEntity> drafts = templateRepository
                .findByDeletedAtIsNullAndGroupCodeInAndLifecycleStatusOrderByUpdatedAtDesc(
                        List.of(groupCode),
                        TemplateLifecycleStatus.DRAFT
                );
        if (filterIds.isEmpty()) {
            return drafts;
        }
        List<TemplateEntity> filtered = new ArrayList<>();
        for (TemplateEntity draft : drafts) {
            if (filterIds.contains(draft.getId())) {
                filtered.add(draft);
            }
        }
        for (UUID templateId : filterIds) {
            boolean already = filtered.stream().anyMatch(t -> t.getId().equals(templateId));
            if (already) {
                continue;
            }
            templateRepository.findByIdAndDeletedAtIsNull(templateId).ifPresent(template -> {
                if (template.getGroupCode().equalsIgnoreCase(groupCode)
                        && groupAccessService.canAccessGroup(session, template.getGroupCode())) {
                    filtered.add(template);
                }
            });
        }
        return filtered;
    }

    private Set<UUID> parseTemplateIds(List<String> templateIds) {
        if (templateIds == null || templateIds.isEmpty()) {
            return Set.of();
        }
        Set<UUID> ids = new HashSet<>();
        for (String raw : templateIds) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                ids.add(UUID.fromString(raw.trim()));
            } catch (IllegalArgumentException ignored) {
                // malformed ids yield no matches
            }
        }
        return ids;
    }

    private List<BulkRepinItemView> evaluateTemplate(
            TemplateEntity template,
            ContentModuleEntity module,
            TargetResolution target,
            String fromSemanticVersion,
            boolean dryRun,
            ManagementSessionClaims session
    ) {
        Optional<TemplateVersionEntity> versionOpt = templateVersionSupport.findInFlightDevVersion(template.getId());
        if (template.getLifecycleStatus() != TemplateLifecycleStatus.DRAFT || versionOpt.isEmpty()) {
            return evaluateLockedPublished(template, module, fromSemanticVersion, target);
        }
        TemplateVersionEntity version = versionOpt.get();
        List<TemplateContentModuleReferenceEntity> references =
                referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(version.getId());
        List<BulkRepinItemView> items = new ArrayList<>();
        boolean anyModulePin = false;
        for (TemplateContentModuleReferenceEntity reference : references) {
            ContentModuleVersionEntity pinned = contentModuleVersionRepository
                    .findById(reference.getContentModuleVersionId())
                    .orElse(null);
            if (pinned == null || !pinned.getModuleId().equals(module.getId())) {
                continue;
            }
            anyModulePin = true;
            items.add(classifyDraftPin(
                    template, version, reference, pinned, module, target, fromSemanticVersion, dryRun, session));
        }
        if (!anyModulePin) {
            items.add(item(
                    template,
                    version,
                    null,
                    null,
                    target.resolvedSemanticVersion(),
                    BulkRepinItemStatus.SKIPPED_NO_MATCH,
                    null
            ));
        }
        return items;
    }

    private BulkRepinItemView classifyDraftPin(
            TemplateEntity template,
            TemplateVersionEntity version,
            TemplateContentModuleReferenceEntity reference,
            ContentModuleVersionEntity pinned,
            ContentModuleEntity module,
            TargetResolution target,
            String fromSemanticVersion,
            boolean dryRun,
            ManagementSessionClaims session
    ) {
        if (fromSemanticVersion != null
                && !fromSemanticVersion.isBlank()
                && !fromSemanticVersion.trim().equals(pinned.getSemanticVersion())) {
            return item(
                    template,
                    version,
                    reference.getReferenceKey(),
                    pinned.getSemanticVersion(),
                    target.resolvedSemanticVersion(),
                    BulkRepinItemStatus.SKIPPED_NO_MATCH,
                    null
            );
        }
        if (reference.isLockedFlag()) {
            return item(
                    template,
                    version,
                    reference.getReferenceKey(),
                    pinned.getSemanticVersion(),
                    target.resolvedSemanticVersion(),
                    BulkRepinItemStatus.SKIPPED_LOCKED,
                    null
            );
        }
        if (target.errorCode() != null) {
            return item(
                    template,
                    version,
                    reference.getReferenceKey(),
                    pinned.getSemanticVersion(),
                    target.resolvedSemanticVersion(),
                    BulkRepinItemStatus.FAILED,
                    target.errorCode()
            );
        }
        if (pinned.getSemanticVersion().equals(target.resolvedSemanticVersion())) {
            return item(
                    template,
                    version,
                    reference.getReferenceKey(),
                    pinned.getSemanticVersion(),
                    target.resolvedSemanticVersion(),
                    BulkRepinItemStatus.SKIPPED_ALREADY_AT_TARGET,
                    null
            );
        }
        if (dryRun) {
            return item(
                    template,
                    version,
                    reference.getReferenceKey(),
                    pinned.getSemanticVersion(),
                    target.resolvedSemanticVersion(),
                    BulkRepinItemStatus.WOULD_APPLY,
                    null
            );
        }
        return applyRepin(template, version, reference, pinned, module, target, session);
    }

    private List<BulkRepinItemView> evaluateLockedPublished(
            TemplateEntity template,
            ContentModuleEntity module,
            String fromSemanticVersion,
            TargetResolution target
    ) {
        Optional<TemplateVersionEntity> published = templateVersionSupport.findLatestPublishedVersion(template.getId());
        if (published.isEmpty()) {
            return List.of(item(
                    template,
                    null,
                    null,
                    null,
                    target.resolvedSemanticVersion(),
                    BulkRepinItemStatus.SKIPPED_LOCKED,
                    null
            ));
        }
        TemplateVersionEntity version = published.get();
        List<BulkRepinItemView> items = new ArrayList<>();
        for (TemplateContentModuleReferenceEntity reference
                : referenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(version.getId())) {
            ContentModuleVersionEntity pinned = contentModuleVersionRepository
                    .findById(reference.getContentModuleVersionId())
                    .orElse(null);
            if (pinned == null || !pinned.getModuleId().equals(module.getId())) {
                continue;
            }
            if (fromSemanticVersion != null
                    && !fromSemanticVersion.isBlank()
                    && !fromSemanticVersion.trim().equals(pinned.getSemanticVersion())) {
                continue;
            }
            items.add(item(
                    template,
                    version,
                    reference.getReferenceKey(),
                    pinned.getSemanticVersion(),
                    target.resolvedSemanticVersion(),
                    BulkRepinItemStatus.SKIPPED_LOCKED,
                    null
            ));
        }
        if (items.isEmpty()) {
            items.add(item(
                    template,
                    version,
                    null,
                    null,
                    target.resolvedSemanticVersion(),
                    BulkRepinItemStatus.SKIPPED_LOCKED,
                    null
            ));
        }
        return items;
    }

    private BulkRepinItemView applyRepin(
            TemplateEntity template,
            TemplateVersionEntity version,
            TemplateContentModuleReferenceEntity reference,
            ContentModuleVersionEntity pinned,
            ContentModuleEntity module,
            TargetResolution target,
            ManagementSessionClaims session
    ) {
        try {
            referenceService.upsertReference(
                    template.getId(),
                    new UpsertContentModuleReferenceRequest(
                            reference.getReferenceKey(),
                            contentModuleAccessService.publicModuleId(module),
                            target.resolvedSemanticVersion()
                    ),
                    session
            );
            return item(
                    template,
                    version,
                    reference.getReferenceKey(),
                    pinned.getSemanticVersion(),
                    target.resolvedSemanticVersion(),
                    BulkRepinItemStatus.APPLIED,
                    null
            );
        } catch (RuntimeException ex) {
            return item(
                    template,
                    version,
                    reference.getReferenceKey(),
                    pinned.getSemanticVersion(),
                    target.resolvedSemanticVersion(),
                    BulkRepinItemStatus.FAILED,
                    ApiErrorCodes.BULK_REPIN_TARGET_INVALID
            );
        }
    }

    private static BulkRepinItemView item(
            TemplateEntity template,
            TemplateVersionEntity version,
            String referenceKey,
            String before,
            String after,
            BulkRepinItemStatus status,
            String errorCode
    ) {
        return new BulkRepinItemView(
                template.getId().toString(),
                version == null ? null : version.getId().toString(),
                referenceKey,
                before,
                after,
                status,
                errorCode
        );
    }

    private static BulkRepinSummaryView summarize(boolean dryRun, List<BulkRepinItemView> items) {
        int wouldApply = 0;
        int applied = 0;
        int skippedLocked = 0;
        int skippedAlready = 0;
        int skippedNoMatch = 0;
        int failed = 0;
        for (BulkRepinItemView item : items) {
            switch (item.status()) {
                case WOULD_APPLY -> wouldApply++;
                case APPLIED -> applied++;
                case SKIPPED_LOCKED -> skippedLocked++;
                case SKIPPED_ALREADY_AT_TARGET -> skippedAlready++;
                case SKIPPED_NO_MATCH -> skippedNoMatch++;
                case FAILED -> failed++;
                default -> {
                }
            }
        }
        return new BulkRepinSummaryView(
                dryRun, wouldApply, applied, skippedLocked, skippedAlready, skippedNoMatch, failed
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record TargetResolution(
            String resolvedSemanticVersion,
            boolean useLatestApproved,
            String errorCode
    ) {
    }
}
