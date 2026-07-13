package com.bank.docgen.collaboration.service;

import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.collaboration.api.CollaborationNotificationItemView;
import com.bank.docgen.collaboration.api.CollaborationNotificationUnreadCountView;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemEntity;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemReadMarkerEntity;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemReadMarkerRepository;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CollaborationNotificationService {

    private final CollaborationWorkItemRepository workItemRepository;
    private final CollaborationWorkItemReadMarkerRepository readMarkerRepository;
    private final GroupAccessService groupAccessService;
    private final CollaborationWorkItemAccessService accessSupport;
    private final int listLimit;

    public CollaborationNotificationService(
            CollaborationWorkItemRepository workItemRepository,
            CollaborationWorkItemReadMarkerRepository readMarkerRepository,
            GroupAccessService groupAccessService,
            CollaborationWorkItemAccessService accessSupport,
            @Value("${docgen.collaboration.notification-list-size:20}") int listLimit
    ) {
        this.workItemRepository = workItemRepository;
        this.readMarkerRepository = readMarkerRepository;
        this.groupAccessService = groupAccessService;
        this.accessSupport = accessSupport;
        this.listLimit = listLimit > 0 ? listLimit : 20;
    }

    @Transactional(readOnly = true)
    public CollaborationNotificationUnreadCountView unreadCount(ManagementSessionClaims session) {
        Scope scope = resolveScope(session);
        if (scope.groupCodes().isEmpty()) {
            return new CollaborationNotificationUnreadCountView(0);
        }
        long count = workItemRepository.countOpenUnreadByQueuesAndGroups(
                scope.queues(),
                scope.groupCodes(),
                session.username()
        );
        return new CollaborationNotificationUnreadCountView(count);
    }

    @Transactional(readOnly = true)
    public List<CollaborationNotificationItemView> list(ManagementSessionClaims session) {
        Scope scope = resolveScope(session);
        if (scope.groupCodes().isEmpty()) {
            return List.of();
        }
        List<CollaborationWorkItemEntity> items = workItemRepository.findOpenByQueuesAndGroupsNewestFirst(
                scope.queues(),
                scope.groupCodes(),
                PageRequest.of(0, listLimit)
        );
        if (items.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = items.stream().map(CollaborationWorkItemEntity::getId).toList();
        Set<UUID> readIds = readMarkerRepository.findWorkItemIdsByUserIdAndWorkItemIdIn(
                session.username(),
                ids
        );
        Instant now = Instant.now();
        return items.stream()
                .map(item -> toView(item, readIds.contains(item.getId()), now))
                .toList();
    }

    @Transactional
    public CollaborationNotificationUnreadCountView markRead(
            ManagementSessionClaims session,
            UUID workItemId
    ) {
        Scope scope = resolveScope(session);
        if (scope.groupCodes().isEmpty()) {
            throw new CollaborationWorkItemNotFoundException();
        }
        CollaborationWorkItemEntity item = workItemRepository.findVisibleOpenById(
                        workItemId,
                        scope.queues(),
                        scope.groupCodes()
                )
                .orElseThrow(CollaborationWorkItemNotFoundException::new);
        if (!readMarkerRepository.existsByUserIdAndWorkItemId(session.username(), item.getId())) {
            readMarkerRepository.save(new CollaborationWorkItemReadMarkerEntity(
                    UUID.randomUUID(),
                    session.username(),
                    item.getId()
            ));
        }
        return unreadCountAfterMutation(session, scope);
    }

    @Transactional
    public CollaborationNotificationUnreadCountView markAllRead(ManagementSessionClaims session) {
        Scope scope = resolveScope(session);
        if (scope.groupCodes().isEmpty()) {
            return new CollaborationNotificationUnreadCountView(0);
        }
        List<UUID> unreadIds = workItemRepository.findOpenUnreadIdsByQueuesAndGroups(
                scope.queues(),
                scope.groupCodes(),
                session.username()
        );
        if (!unreadIds.isEmpty()) {
            List<CollaborationWorkItemReadMarkerEntity> markers = new ArrayList<>(unreadIds.size());
            for (UUID workItemId : unreadIds) {
                markers.add(new CollaborationWorkItemReadMarkerEntity(
                        UUID.randomUUID(),
                        session.username(),
                        workItemId
                ));
            }
            readMarkerRepository.saveAll(markers);
        }
        return unreadCountAfterMutation(session, scope);
    }

    private CollaborationNotificationUnreadCountView unreadCountAfterMutation(
            ManagementSessionClaims session,
            Scope scope
    ) {
        if (scope.groupCodes().isEmpty()) {
            return new CollaborationNotificationUnreadCountView(0);
        }
        long count = workItemRepository.countOpenUnreadByQueuesAndGroups(
                scope.queues(),
                scope.groupCodes(),
                session.username()
        );
        return new CollaborationNotificationUnreadCountView(count);
    }

    private Scope resolveScope(ManagementSessionClaims session) {
        accessSupport.requireViewer(session);
        Set<CollaborationWorkItemQueue> visibleQueues = accessSupport.visibleQueues(session);
        if (visibleQueues.isEmpty()) {
            throw new CollaborationWorkItemAccessDeniedException();
        }
        List<String> groupCodes = resolveGroupScope(session);
        if (groupCodes.isEmpty()) {
            return new Scope(List.copyOf(visibleQueues), List.of());
        }
        return new Scope(List.copyOf(visibleQueues), groupCodes);
    }

    private List<String> resolveGroupScope(ManagementSessionClaims session) {
        List<String> accessible = groupAccessService.accessibleGroupCodes(session);
        if (accessible.contains("*")) {
            return List.of("*");
        }
        return accessible.stream()
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .toList();
    }

    private CollaborationNotificationItemView toView(
            CollaborationWorkItemEntity entity,
            boolean read,
            Instant now
    ) {
        long ageSeconds = Math.max(0, Duration.between(entity.getCreatedAt(), now).getSeconds());
        return new CollaborationNotificationItemView(
                entity.getId().toString(),
                entity.getTemplateId().toString(),
                entity.getTemplateName(),
                entity.getGroupCode(),
                entity.getQueue(),
                entity.getTriggerType(),
                entity.getSummaryText(),
                entity.getCreatedAt(),
                ageSeconds,
                read
        );
    }

    private record Scope(List<CollaborationWorkItemQueue> queues, List<String> groupCodes) {
    }
}
