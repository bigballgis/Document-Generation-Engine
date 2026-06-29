package com.bank.docgen.collaboration.service;

import com.bank.docgen.authorization.management.service.GroupAccessService;
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
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CollaborationWorkItemService {

    private final CollaborationWorkItemRepository workItemRepository;
    private final GroupAccessService groupAccessService;
    private final CollaborationWorkItemAccessSupport accessSupport;

    public CollaborationWorkItemService(
            CollaborationWorkItemRepository workItemRepository,
            GroupAccessService groupAccessService,
            CollaborationWorkItemAccessSupport accessSupport
    ) {
        this.workItemRepository = workItemRepository;
        this.groupAccessService = groupAccessService;
        this.accessSupport = accessSupport;
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

        return workItemRepository.findOpenByQueuesAndGroups(requestedQueues, groupCodes).stream()
                .map(this::toSummary)
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
                entity.getTemplateExternalId(),
                entity.getTemplateName(),
                entity.getGroupCode(),
                entity.getQueue(),
                entity.getTriggerType(),
                entity.getSubmitterUserId(),
                entity.getSummaryText(),
                entity.getCreatedAt(),
                ageSeconds
        );
    }
}
