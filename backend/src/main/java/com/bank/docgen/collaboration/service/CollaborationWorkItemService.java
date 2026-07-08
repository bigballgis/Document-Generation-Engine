package com.bank.docgen.collaboration.service;

import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.collaboration.api.CollaborationWorkItemSummaryView;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemEntity;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CollaborationWorkItemService {

    private final CollaborationWorkItemRepository workItemRepository;
    private final GroupAccessService groupAccessService;
    private final CollaborationWorkItemAccessSupport accessSupport;
    private final ManagementUserDisplayService managementUserDisplayService;

    public CollaborationWorkItemService(
            CollaborationWorkItemRepository workItemRepository,
            GroupAccessService groupAccessService,
            CollaborationWorkItemAccessSupport accessSupport,
            ManagementUserDisplayService managementUserDisplayService
    ) {
        this.workItemRepository = workItemRepository;
        this.groupAccessService = groupAccessService;
        this.accessSupport = accessSupport;
        this.managementUserDisplayService = managementUserDisplayService;
    }

    @Transactional(readOnly = true)
    public List<CollaborationWorkItemSummaryView> listQueue(
            ManagementSessionClaims session,
            String groupCode,
            CollaborationWorkItemQueue queue
    ) {
        accessSupport.requireViewer(session);
        Set<CollaborationWorkItemQueue> visibleQueues = accessSupport.visibleQueues(session);
        if (visibleQueues.isEmpty()) {
            throw new CollaborationWorkItemAccessDeniedException();
        }

        List<CollaborationWorkItemQueue> requestedQueues = new ArrayList<>(visibleQueues);
        if (queue != null) {
            accessSupport.requireVisibleQueue(session, queue);
            requestedQueues = List.of(queue);
        }

        List<String> groupCodes = resolveGroupScope(session, groupCode);
        if (groupCodes.isEmpty()) {
            return List.of();
        }

        return enrichSummaries(workItemRepository.findOpenByQueuesAndGroups(requestedQueues, groupCodes).stream()
                .map(this::toSummary)
                .toList());
    }

    private List<CollaborationWorkItemSummaryView> enrichSummaries(List<CollaborationWorkItemSummaryView> summaries) {
        if (summaries.isEmpty()) {
            return summaries;
        }
        Set<String> usernames = summaries.stream()
                .map(CollaborationWorkItemSummaryView::submitterUserId)
                .filter(username -> username != null && !username.isBlank())
                .collect(Collectors.toSet());
        Map<String, String> displayNames = managementUserDisplayService.lookupDisplayNames(usernames);
        return summaries.stream()
                .map(summary -> new CollaborationWorkItemSummaryView(
                        summary.workItemId(),
                        summary.templateId(),
                        summary.templateName(),
                        summary.groupCode(),
                        summary.queue(),
                        summary.triggerType(),
                        summary.submitterUserId(),
                        summary.summaryText(),
                        summary.createdAt(),
                        summary.ageSeconds(),
                        summary.submitterUserId() == null ? null : displayNames.get(summary.submitterUserId())
                ))
                .toList();
    }

    private List<String> resolveGroupScope(ManagementSessionClaims session, String groupCode) {
        if (groupCode != null && !groupCode.isBlank()) {
            String normalized = groupCode.trim().toUpperCase(Locale.ROOT);
            if (!groupAccessService.canAccessGroup(session, normalized)) {
                throw new CollaborationWorkItemAccessDeniedException();
            }
            return List.of(normalized);
        }
        List<String> accessible = groupAccessService.accessibleGroupCodes(session);
        if (accessible.contains("*")) {
            return List.of("*");
        }
        return accessible;
    }

    private CollaborationWorkItemSummaryView toSummary(CollaborationWorkItemEntity entity) {
        Instant now = Instant.now();
        long ageSeconds = Math.max(0, Duration.between(entity.getCreatedAt(), now).getSeconds());
        return new CollaborationWorkItemSummaryView(
                entity.getId().toString(),
                entity.getTemplateId().toString(),
                entity.getTemplateName(),
                entity.getGroupCode(),
                entity.getQueue(),
                entity.getTriggerType(),
                entity.getSubmitterUserId(),
                entity.getSummaryText(),
                entity.getCreatedAt(),
                ageSeconds,
                null
        );
    }
}
