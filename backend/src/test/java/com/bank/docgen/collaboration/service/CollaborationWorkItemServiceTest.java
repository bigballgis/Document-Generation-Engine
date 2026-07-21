package com.bank.docgen.collaboration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.collaboration.api.CollaborationWorkItemSummaryView;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemStatus;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemTriggerType;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemEntity;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CollaborationWorkItemServiceTest {

    private static final UUID WORK_ITEM_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TEMPLATE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Mock
    private CollaborationWorkItemRepository workItemRepository;
    @Mock
    private GroupAccessService groupAccessService;
    @Mock
    private ManagementUserDisplayService managementUserDisplayService;

    private CollaborationWorkItemAccessService accessSupport;
    private CollaborationWorkItemService service;
    private ManagementSessionClaims tester;
    private ManagementSessionClaims author;
    private ManagementSessionClaims auditAdmin;

    @BeforeEach
    void setUp() {
        accessSupport = new CollaborationWorkItemAccessService(groupAccessService);
        service = new CollaborationWorkItemService(
                workItemRepository, groupAccessService, accessSupport, managementUserDisplayService);
        tester = session("10000006", List.of("TEMPLATE_TESTER"), List.of("RETAIL"));
        author = session("10000003", List.of("DOCUMENT_AUTHOR"), List.of("RETAIL"));
        auditAdmin = session("10000004", List.of("AUDIT_ADMIN"), List.of());
    }

    @Test
    void listQueue_returnsTestItemsForTester() {
        when(groupAccessService.canViewCollaborationWorkItems(tester)).thenReturn(true);
        when(groupAccessService.hasCollaborationWorkItemAdminVisibility(tester)).thenReturn(false);
        when(groupAccessService.accessibleGroupCodes(tester)).thenReturn(List.of("RETAIL"));
        when(workItemRepository.findOpenByQueuesAndGroups(
                eq(List.of(CollaborationWorkItemQueue.TEST)),
                eq(List.of("RETAIL"))
        )).thenReturn(List.of(testWorkItem()));
        when(managementUserDisplayService.lookupDisplayNames(Set.of("10000003")))
                .thenReturn(Map.of("10000003", "Template Author (10000003)"));

        List<CollaborationWorkItemSummaryView> items = service.listQueue(tester, null, null);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).templateId()).isEqualTo(TEMPLATE_ID.toString());
        assertThat(items.get(0).templateName()).isEqualTo("Loan Notice Template");
        assertThat(items.get(0).queue()).isEqualTo(CollaborationWorkItemQueue.TEST);
        assertThat(items.get(0).submitterUserId()).isEqualTo("10000003");
        assertThat(items.get(0).submitterDisplayName()).isEqualTo("Template Author (10000003)");
        assertThat(items.get(0).ageSeconds()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void listQueue_filtersByRequestedQueueWhenVisible() {
        when(groupAccessService.canViewCollaborationWorkItems(author)).thenReturn(true);
        when(groupAccessService.hasCollaborationWorkItemAdminVisibility(author)).thenReturn(false);
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        when(workItemRepository.findOpenByQueuesAndGroups(
                eq(List.of(CollaborationWorkItemQueue.REMEDIATION)),
                eq(List.of("RETAIL"))
        )).thenReturn(List.of(remediationWorkItem()));

        List<CollaborationWorkItemSummaryView> items = service.listQueue(
                author,
                "RETAIL",
                CollaborationWorkItemQueue.REMEDIATION
        );

        assertThat(items).hasSize(1);
        assertThat(items.get(0).queue()).isEqualTo(CollaborationWorkItemQueue.REMEDIATION);
    }

    @Test
    void listQueue_deniesAuditAdmin() {
        when(groupAccessService.canViewCollaborationWorkItems(auditAdmin)).thenReturn(false);

        assertThatThrownBy(() -> service.listQueue(auditAdmin, null, null))
                .isInstanceOf(CollaborationWorkItemAccessDeniedException.class);
    }

    @Test
    void listQueue_deniesQueueOutsideRoleVisibility() {
        when(groupAccessService.canViewCollaborationWorkItems(tester)).thenReturn(true);
        when(groupAccessService.hasCollaborationWorkItemAdminVisibility(tester)).thenReturn(false);

        assertThatThrownBy(() -> service.listQueue(tester, null, CollaborationWorkItemQueue.APPROVAL))
                .isInstanceOf(CollaborationWorkItemAccessDeniedException.class);
    }

    @Test
    void listQueue_deniesGroupOutsideScope() {
        when(groupAccessService.canViewCollaborationWorkItems(tester)).thenReturn(true);
        when(groupAccessService.hasCollaborationWorkItemAdminVisibility(tester)).thenReturn(false);
        when(groupAccessService.canAccessGroup(tester, "CORP")).thenReturn(false);

        assertThatThrownBy(() -> service.listQueue(tester, "CORP", null))
                .isInstanceOf(CollaborationWorkItemAccessDeniedException.class);
    }

    @Test
    void listQueue_globalAdminUsesWildcardGroupScope() {
        ManagementSessionClaims globalAdmin = session("10000001", List.of("GLOBAL_ADMIN"), List.of());
        when(groupAccessService.canViewCollaborationWorkItems(globalAdmin)).thenReturn(true);
        when(groupAccessService.hasCollaborationWorkItemAdminVisibility(globalAdmin)).thenReturn(true);
        when(groupAccessService.accessibleGroupCodes(globalAdmin)).thenReturn(List.of("*"));
        when(workItemRepository.findOpenByQueuesAndGroups(any(), eq(List.of("*"))))
                .thenReturn(List.of(testWorkItem()));

        List<CollaborationWorkItemSummaryView> items = service.listQueue(globalAdmin, null, null);

        assertThat(items).hasSize(1);
    }

    private CollaborationWorkItemEntity testWorkItem() {
        return new CollaborationWorkItemEntity(
                WORK_ITEM_ID,
                TEMPLATE_ID,
                "TPL-LOAN-NOTICE",
                "Loan Notice Template",
                "RETAIL",
                CollaborationWorkItemQueue.TEST,
                CollaborationWorkItemTriggerType.SUBMIT_FOR_TEST,
                CollaborationWorkItemStatus.OPEN,
                "10000003",
                "Template submitted for testing"
        );
    }

    private CollaborationWorkItemEntity remediationWorkItem() {
        return new CollaborationWorkItemEntity(
                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                TEMPLATE_ID,
                "TPL-LOAN-NOTICE",
                "Loan Notice Template",
                "RETAIL",
                CollaborationWorkItemQueue.REMEDIATION,
                CollaborationWorkItemTriggerType.TEST_FAILURE_OR_RETURN_TO_DRAFT,
                CollaborationWorkItemStatus.OPEN,
                "10000006",
                "Test failed; remediation required"
        );
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
