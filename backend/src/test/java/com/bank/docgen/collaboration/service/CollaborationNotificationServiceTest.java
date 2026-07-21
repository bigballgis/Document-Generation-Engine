package com.bank.docgen.collaboration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.collaboration.api.CollaborationNotificationItemView;
import com.bank.docgen.collaboration.api.CollaborationNotificationUnreadCountView;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemStatus;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemTriggerType;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemEntity;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemReadMarkerEntity;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemReadMarkerRepository;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CollaborationNotificationServiceTest {

    private static final UUID WORK_ITEM_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TEMPLATE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID ESCALATION_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID OTHER_GROUP_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Mock
    private CollaborationWorkItemRepository workItemRepository;
    @Mock
    private CollaborationWorkItemReadMarkerRepository readMarkerRepository;
    @Mock
    private GroupAccessService groupAccessService;

    private CollaborationWorkItemAccessService accessSupport;
    private CollaborationNotificationService service;
    private ManagementSessionClaims tester;
    private ManagementSessionClaims groupAdmin;
    private ManagementSessionClaims auditAdmin;

    @BeforeEach
    void setUp() {
        accessSupport = new CollaborationWorkItemAccessService(groupAccessService);
        service = new CollaborationNotificationService(
                workItemRepository,
                readMarkerRepository,
                groupAccessService,
                accessSupport,
                20
        );
        tester = session("10000006", List.of("TEMPLATE_TESTER"), List.of("RETAIL"));
        groupAdmin = session("10000002", List.of("GROUP_ADMIN"), List.of("RETAIL"));
        auditAdmin = session("10000004", List.of("AUDIT_ADMIN"), List.of());
    }

    @Test
    void unreadCount_countsOpenVisibleItemsWithoutReadMarker() {
        when(groupAccessService.canViewCollaborationWorkItems(tester)).thenReturn(true);
        when(groupAccessService.hasCollaborationWorkItemAdminVisibility(tester)).thenReturn(false);
        when(groupAccessService.accessibleGroupCodes(tester)).thenReturn(List.of("RETAIL"));
        when(workItemRepository.countOpenUnreadByQueuesAndGroups(
                eq(List.of(CollaborationWorkItemQueue.TEST)),
                eq(List.of("RETAIL")),
                eq("10000006")
        )).thenReturn(2L);

        CollaborationNotificationUnreadCountView view = service.unreadCount(tester);

        assertThat(view.unreadCount()).isEqualTo(2);
    }

    @Test
    void unreadCount_deniesUnauthorizedRole() {
        when(groupAccessService.canViewCollaborationWorkItems(auditAdmin)).thenReturn(false);

        assertThatThrownBy(() -> service.unreadCount(auditAdmin))
                .isInstanceOf(CollaborationWorkItemAccessDeniedException.class);
    }

    @Test
    void list_returnsNewestFirstWithReadFlagAndCapsAtTwenty() {
        when(groupAccessService.canViewCollaborationWorkItems(tester)).thenReturn(true);
        when(groupAccessService.hasCollaborationWorkItemAdminVisibility(tester)).thenReturn(false);
        when(groupAccessService.accessibleGroupCodes(tester)).thenReturn(List.of("RETAIL"));
        List<CollaborationWorkItemEntity> items = IntStream.range(0, 20)
                .mapToObj(i -> workItem(
                        UUID.fromString(String.format("11111111-1111-1111-1111-%012d", i)),
                        CollaborationWorkItemQueue.TEST,
                        "RETAIL"
                ))
                .toList();
        when(workItemRepository.findOpenByQueuesAndGroupsNewestFirst(
                eq(List.of(CollaborationWorkItemQueue.TEST)),
                eq(List.of("RETAIL")),
                any(Pageable.class)
        )).thenReturn(items);
        UUID firstId = items.getFirst().getId();
        when(readMarkerRepository.findWorkItemIdsByUserIdAndWorkItemIdIn(
                eq("10000006"),
                eq(items.stream().map(CollaborationWorkItemEntity::getId).toList())
        )).thenReturn(Set.of(firstId));

        List<CollaborationNotificationItemView> views = service.list(tester);

        assertThat(views).hasSize(20);
        assertThat(views.getFirst().workItemId()).isEqualTo(firstId.toString());
        assertThat(views.getFirst().read()).isTrue();
        assertThat(views.get(1).read()).isFalse();
        assertThat(views.getFirst().queue()).isEqualTo(CollaborationWorkItemQueue.TEST);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(workItemRepository).findOpenByQueuesAndGroupsNewestFirst(
                eq(List.of(CollaborationWorkItemQueue.TEST)),
                eq(List.of("RETAIL")),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void list_excludesEscalationForTester() {
        when(groupAccessService.canViewCollaborationWorkItems(tester)).thenReturn(true);
        when(groupAccessService.hasCollaborationWorkItemAdminVisibility(tester)).thenReturn(false);
        when(groupAccessService.accessibleGroupCodes(tester)).thenReturn(List.of("RETAIL"));
        when(workItemRepository.findOpenByQueuesAndGroupsNewestFirst(
                eq(List.of(CollaborationWorkItemQueue.TEST)),
                eq(List.of("RETAIL")),
                any(Pageable.class)
        )).thenReturn(List.of(workItem(WORK_ITEM_ID, CollaborationWorkItemQueue.TEST, "RETAIL")));
        when(readMarkerRepository.findWorkItemIdsByUserIdAndWorkItemIdIn(eq("10000006"), any()))
                .thenReturn(Set.of());

        List<CollaborationNotificationItemView> views = service.list(tester);

        assertThat(views).extracting(CollaborationNotificationItemView::queue)
                .containsExactly(CollaborationWorkItemQueue.TEST);
        verify(workItemRepository).findOpenByQueuesAndGroupsNewestFirst(
                eq(List.of(CollaborationWorkItemQueue.TEST)),
                eq(List.of("RETAIL")),
                any(Pageable.class)
        );
    }

    @Test
    void list_includesEscalationForGroupAdmin() {
        when(groupAccessService.canViewCollaborationWorkItems(groupAdmin)).thenReturn(true);
        when(groupAccessService.hasCollaborationWorkItemAdminVisibility(groupAdmin)).thenReturn(true);
        when(groupAccessService.accessibleGroupCodes(groupAdmin)).thenReturn(List.of("RETAIL"));
        when(workItemRepository.findOpenByQueuesAndGroupsNewestFirst(
                any(),
                eq(List.of("RETAIL")),
                any(Pageable.class)
        )).thenReturn(List.of(workItem(ESCALATION_ID, CollaborationWorkItemQueue.ESCALATION, "RETAIL")));
        when(readMarkerRepository.findWorkItemIdsByUserIdAndWorkItemIdIn(eq("10000002"), any()))
                .thenReturn(Set.of());

        List<CollaborationNotificationItemView> views = service.list(groupAdmin);

        assertThat(views).hasSize(1);
        assertThat(views.getFirst().queue()).isEqualTo(CollaborationWorkItemQueue.ESCALATION);
        assertThat(views.getFirst().read()).isFalse();
    }

    @Test
    void markRead_persistsMarkerIdempotently() {
        when(groupAccessService.canViewCollaborationWorkItems(tester)).thenReturn(true);
        when(groupAccessService.hasCollaborationWorkItemAdminVisibility(tester)).thenReturn(false);
        when(groupAccessService.accessibleGroupCodes(tester)).thenReturn(List.of("RETAIL"));
        when(workItemRepository.findVisibleOpenById(
                eq(WORK_ITEM_ID),
                eq(List.of(CollaborationWorkItemQueue.TEST)),
                eq(List.of("RETAIL"))
        )).thenReturn(Optional.of(workItem(WORK_ITEM_ID, CollaborationWorkItemQueue.TEST, "RETAIL")));
        when(readMarkerRepository.existsByUserIdAndWorkItemId("10000006", WORK_ITEM_ID)).thenReturn(false);
        when(workItemRepository.countOpenUnreadByQueuesAndGroups(
                eq(List.of(CollaborationWorkItemQueue.TEST)),
                eq(List.of("RETAIL")),
                eq("10000006")
        )).thenReturn(0L);

        CollaborationNotificationUnreadCountView result = service.markRead(tester, WORK_ITEM_ID);

        assertThat(result.unreadCount()).isZero();
        ArgumentCaptor<CollaborationWorkItemReadMarkerEntity> captor =
                ArgumentCaptor.forClass(CollaborationWorkItemReadMarkerEntity.class);
        verify(readMarkerRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo("10000006");
        assertThat(captor.getValue().getWorkItemId()).isEqualTo(WORK_ITEM_ID);
    }

    @Test
    void markRead_isIdempotentWhenMarkerExists() {
        when(groupAccessService.canViewCollaborationWorkItems(tester)).thenReturn(true);
        when(groupAccessService.hasCollaborationWorkItemAdminVisibility(tester)).thenReturn(false);
        when(groupAccessService.accessibleGroupCodes(tester)).thenReturn(List.of("RETAIL"));
        when(workItemRepository.findVisibleOpenById(
                eq(WORK_ITEM_ID),
                eq(List.of(CollaborationWorkItemQueue.TEST)),
                eq(List.of("RETAIL"))
        )).thenReturn(Optional.of(workItem(WORK_ITEM_ID, CollaborationWorkItemQueue.TEST, "RETAIL")));
        when(readMarkerRepository.existsByUserIdAndWorkItemId("10000006", WORK_ITEM_ID)).thenReturn(true);
        when(workItemRepository.countOpenUnreadByQueuesAndGroups(
                eq(List.of(CollaborationWorkItemQueue.TEST)),
                eq(List.of("RETAIL")),
                eq("10000006")
        )).thenReturn(0L);

        service.markRead(tester, WORK_ITEM_ID);

        verify(readMarkerRepository, never()).save(any());
    }

    @Test
    void markRead_returnsNotFoundForInvisibleWorkItem() {
        when(groupAccessService.canViewCollaborationWorkItems(tester)).thenReturn(true);
        when(groupAccessService.hasCollaborationWorkItemAdminVisibility(tester)).thenReturn(false);
        when(groupAccessService.accessibleGroupCodes(tester)).thenReturn(List.of("RETAIL"));
        when(workItemRepository.findVisibleOpenById(
                eq(OTHER_GROUP_ID),
                eq(List.of(CollaborationWorkItemQueue.TEST)),
                eq(List.of("RETAIL"))
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(tester, OTHER_GROUP_ID))
                .isInstanceOf(CollaborationWorkItemNotFoundException.class);
        verify(readMarkerRepository, never()).save(any());
    }

    @Test
    void markAllRead_marksEveryVisibleUnreadOpenItem() {
        when(groupAccessService.canViewCollaborationWorkItems(tester)).thenReturn(true);
        when(groupAccessService.hasCollaborationWorkItemAdminVisibility(tester)).thenReturn(false);
        when(groupAccessService.accessibleGroupCodes(tester)).thenReturn(List.of("RETAIL"));
        UUID secondId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        when(workItemRepository.findOpenUnreadIdsByQueuesAndGroups(
                eq(List.of(CollaborationWorkItemQueue.TEST)),
                eq(List.of("RETAIL")),
                eq("10000006")
        )).thenReturn(List.of(WORK_ITEM_ID, secondId));
        when(workItemRepository.countOpenUnreadByQueuesAndGroups(
                eq(List.of(CollaborationWorkItemQueue.TEST)),
                eq(List.of("RETAIL")),
                eq("10000006")
        )).thenReturn(0L);

        CollaborationNotificationUnreadCountView result = service.markAllRead(tester);

        assertThat(result.unreadCount()).isZero();
        verify(readMarkerRepository).saveAll(any());
    }

    private CollaborationWorkItemEntity workItem(
            UUID id,
            CollaborationWorkItemQueue queue,
            String groupCode
    ) {
        CollaborationWorkItemEntity entity = new CollaborationWorkItemEntity(
                id,
                TEMPLATE_ID,
                "TPL-LOAN-NOTICE",
                "Loan Notice Template",
                groupCode,
                queue,
                queue == CollaborationWorkItemQueue.ESCALATION
                        ? CollaborationWorkItemTriggerType.TIMEOUT_ESCALATION
                        : CollaborationWorkItemTriggerType.SUBMIT_FOR_TEST,
                CollaborationWorkItemStatus.OPEN,
                "10000003",
                "Template submitted for testing"
        );
        entity.setCreatedAt(Instant.parse("2026-07-11T10:00:00Z"));
        return entity;
    }

    private ManagementSessionClaims session(String username, List<String> roles, List<String> groups) {
        return new ManagementSessionClaims(
                username,
                username,
                username + "@example.com",
                AuthSource.LOCAL,
                roles,
                groups,
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                Instant.now().plusSeconds(3600)
        );
    }
}
