package com.bank.docgen.contentmodule.service;

import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.api.ContentModuleWorkflowTaskView;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewState;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Content-module workflow inbox projection.
 * PRR-A03: SUBMITTED candidates loaded via Pageable / LIMIT with page scan + return cap.
 */
@Service
public class ContentModuleWorkflowService {

    public static final String KIND_PENDING_REVIEW = "PENDING_REVIEW";
    public static final String KIND_REWORK = "REWORK";

    public static final int DEFAULT_PENDING_REVIEW_INBOX_LIMIT = 500;
    public static final int DEFAULT_SUBMITTED_SCAN_PAGE_SIZE = 1000;
    public static final int MIN_SUBMITTED_SCAN_PAGE_SIZE = 100;
    public static final int MAX_SUBMITTED_SCAN_PAGE_SIZE = 2000;

    private static final Logger LOG = LoggerFactory.getLogger(ContentModuleWorkflowService.class);

    private final ContentModuleRepository moduleRepository;
    private final ContentModuleVersionRepository versionRepository;
    private final GroupAccessService groupAccessService;
    private final ContentModuleAccessService accessSupport;
    private final int pendingReviewInboxLimit;
    private final int submittedScanPageSize;

    public ContentModuleWorkflowService(
            ContentModuleRepository moduleRepository,
            ContentModuleVersionRepository versionRepository,
            GroupAccessService groupAccessService,
            ContentModuleAccessService accessSupport,
            @Value("${docgen.content-module.pending-review-inbox-limit:500}") int pendingReviewInboxLimit,
            @Value("${docgen.content-module.submitted-scan-page-size:1000}") int submittedScanPageSize
    ) {
        this.moduleRepository = moduleRepository;
        this.versionRepository = versionRepository;
        this.groupAccessService = groupAccessService;
        this.accessSupport = accessSupport;
        if (pendingReviewInboxLimit < 1) {
            throw new IllegalStateException(
                    "docgen.content-module.pending-review-inbox-limit must be >= 1; got "
                            + pendingReviewInboxLimit
            );
        }
        if (submittedScanPageSize < MIN_SUBMITTED_SCAN_PAGE_SIZE
                || submittedScanPageSize > MAX_SUBMITTED_SCAN_PAGE_SIZE) {
            throw new IllegalStateException(
                    "docgen.content-module.submitted-scan-page-size must be between "
                            + MIN_SUBMITTED_SCAN_PAGE_SIZE + " and " + MAX_SUBMITTED_SCAN_PAGE_SIZE
                            + " (inclusive); got " + submittedScanPageSize
            );
        }
        this.pendingReviewInboxLimit = pendingReviewInboxLimit;
        this.submittedScanPageSize = submittedScanPageSize;
    }

    @Transactional(readOnly = true)
    public List<ContentModuleWorkflowTaskView> listWorkflowTasks(ManagementSessionClaims session) {
        if (!groupAccessService.canBrowseContentModuleCatalog(session)) {
            throw new ContentModuleAccessDeniedException();
        }

        List<ContentModuleWorkflowTaskView> tasks = new ArrayList<>();
        if (groupAccessService.canDecideContentModuleReviews(session)) {
            tasks.addAll(projectPendingReviewTasks(session));
        }
        if (groupAccessService.canAuthorContentModules(session)) {
            tasks.addAll(projectReworkTasks(session));
        }
        return tasks;
    }

    private List<ContentModuleWorkflowTaskView> projectPendingReviewTasks(ManagementSessionClaims session) {
        Map<UUID, ContentModuleWorkflowTaskView> byModule = new LinkedHashMap<>();
        int page = 0;
        int scanned = 0;
        boolean truncated = false;
        while (byModule.size() < pendingReviewInboxLimit) {
            Pageable pageable = PageRequest.of(page, submittedScanPageSize);
            List<ContentModuleVersionEntity> submitted =
                    versionRepository.findByReviewStateOrderByUpdatedAtDesc(
                            ContentModuleReviewState.SUBMITTED,
                            pageable
                    );
            if (submitted.isEmpty()) {
                break;
            }
            scanned += submitted.size();
            projectInto(session, submitted, KIND_PENDING_REVIEW, false, byModule, pendingReviewInboxLimit);
            if (byModule.size() >= pendingReviewInboxLimit) {
                truncated = true;
                break;
            }
            if (submitted.size() < submittedScanPageSize) {
                break;
            }
            page++;
        }
        if (truncated) {
            LOG.warn(
                    "Content-module PENDING_REVIEW inbox truncated: returned={} scannedCandidates={} "
                            + "inboxLimit={} scanPageSize={}",
                    byModule.size(),
                    scanned,
                    pendingReviewInboxLimit,
                    submittedScanPageSize
            );
        }
        return List.copyOf(byModule.values());
    }

    private List<ContentModuleWorkflowTaskView> projectReworkTasks(ManagementSessionClaims session) {
        List<ContentModuleVersionEntity> reworkVersions =
                versionRepository.findDraftVersionsWithRejectionReason();
        Map<UUID, ContentModuleWorkflowTaskView> byModule = new LinkedHashMap<>();
        projectInto(session, reworkVersions, KIND_REWORK, true, byModule, Integer.MAX_VALUE);
        return List.copyOf(byModule.values());
    }

    private void projectInto(
            ManagementSessionClaims session,
            List<ContentModuleVersionEntity> versions,
            String kind,
            boolean includeRejectionReason,
            Map<UUID, ContentModuleWorkflowTaskView> byModule,
            int maxTasks
    ) {
        if (versions.isEmpty() || byModule.size() >= maxTasks) {
            return;
        }

        Set<UUID> moduleIds = versions.stream()
                .map(ContentModuleVersionEntity::getModuleId)
                .collect(Collectors.toCollection(HashSet::new));
        Map<UUID, ContentModuleEntity> modulesById = moduleRepository.findAllById(moduleIds).stream()
                .filter(module -> module.getDeletedAt() == null)
                .collect(Collectors.toMap(ContentModuleEntity::getId, Function.identity()));

        // One task per module (latest matching version wins — versions already sorted desc by updatedAt).
        for (ContentModuleVersionEntity version : versions) {
            if (byModule.size() >= maxTasks) {
                return;
            }
            if (byModule.containsKey(version.getModuleId())) {
                continue;
            }
            ContentModuleEntity module = modulesById.get(version.getModuleId());
            if (module == null || !canAccessModule(session, module)) {
                continue;
            }
            byModule.put(
                    version.getModuleId(),
                    new ContentModuleWorkflowTaskView(
                            accessSupport.publicModuleId(module),
                            module.getModuleCode(),
                            module.getName(),
                            module.getGroupCode(),
                            kind,
                            version.getSemanticVersion(),
                            includeRejectionReason ? version.getRejectionReason() : null,
                            version.getUpdatedAt()
                    )
            );
        }
    }

    private boolean canAccessModule(ManagementSessionClaims session, ContentModuleEntity module) {
        if (groupAccessService.canAccessGroup(session, module.getGroupCode())) {
            return true;
        }
        return accessSupport.readSharedGroupCodes(module).stream()
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .anyMatch(code -> groupAccessService.canAccessGroup(session, code));
    }
}
