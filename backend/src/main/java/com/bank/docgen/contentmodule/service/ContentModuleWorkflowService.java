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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentModuleWorkflowService {

    public static final String KIND_PENDING_REVIEW = "PENDING_REVIEW";
    public static final String KIND_REWORK = "REWORK";

    private final ContentModuleRepository moduleRepository;
    private final ContentModuleVersionRepository versionRepository;
    private final GroupAccessService groupAccessService;
    private final ContentModuleAccessService accessSupport;

    public ContentModuleWorkflowService(
            ContentModuleRepository moduleRepository,
            ContentModuleVersionRepository versionRepository,
            GroupAccessService groupAccessService,
            ContentModuleAccessService accessSupport
    ) {
        this.moduleRepository = moduleRepository;
        this.versionRepository = versionRepository;
        this.groupAccessService = groupAccessService;
        this.accessSupport = accessSupport;
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
        List<ContentModuleVersionEntity> submitted =
                versionRepository.findByReviewStateOrderByUpdatedAtDesc(ContentModuleReviewState.SUBMITTED);
        return projectTasks(session, submitted, KIND_PENDING_REVIEW, false);
    }

    private List<ContentModuleWorkflowTaskView> projectReworkTasks(ManagementSessionClaims session) {
        List<ContentModuleVersionEntity> reworkVersions =
                versionRepository.findDraftVersionsWithRejectionReason();
        return projectTasks(session, reworkVersions, KIND_REWORK, true);
    }

    private List<ContentModuleWorkflowTaskView> projectTasks(
            ManagementSessionClaims session,
            List<ContentModuleVersionEntity> versions,
            String kind,
            boolean includeRejectionReason
    ) {
        if (versions.isEmpty()) {
            return List.of();
        }

        Set<UUID> moduleIds = versions.stream()
                .map(ContentModuleVersionEntity::getModuleId)
                .collect(Collectors.toCollection(HashSet::new));
        Map<UUID, ContentModuleEntity> modulesById = moduleRepository.findAllById(moduleIds).stream()
                .filter(module -> module.getDeletedAt() == null)
                .collect(Collectors.toMap(ContentModuleEntity::getId, Function.identity()));

        // One task per module (latest matching version wins — versions already sorted desc by updatedAt).
        Map<UUID, ContentModuleWorkflowTaskView> byModule = new LinkedHashMap<>();
        for (ContentModuleVersionEntity version : versions) {
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
        return List.copyOf(byModule.values());
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
