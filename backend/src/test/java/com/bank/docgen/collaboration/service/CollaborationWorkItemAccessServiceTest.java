package com.bank.docgen.collaboration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CollaborationWorkItemAccessServiceTest {

    @Mock
    private GroupAccessService groupAccessService;

    private CollaborationWorkItemAccessService accessSupport;

    @BeforeEach
    void setUp() {
        accessSupport = new CollaborationWorkItemAccessService(groupAccessService);
    }

    @Test
    void visibleQueues_forTesterIncludesTestQueueOnly() {
        ManagementSessionClaims tester = session(List.of("TEMPLATE_TESTER"));
        when(groupAccessService.canViewCollaborationWorkItems(tester)).thenReturn(true);
        when(groupAccessService.hasCollaborationWorkItemAdminVisibility(tester)).thenReturn(false);

        assertThat(accessSupport.visibleQueues(tester)).containsExactly(CollaborationWorkItemQueue.TEST);
    }

    @Test
    void visibleQueues_forAuthorIncludesRemediationAndPendingRelease() {
        ManagementSessionClaims author = session(List.of("DOCUMENT_AUTHOR"));
        when(groupAccessService.canViewCollaborationWorkItems(author)).thenReturn(true);
        when(groupAccessService.hasCollaborationWorkItemAdminVisibility(author)).thenReturn(false);

        assertThat(accessSupport.visibleQueues(author))
                .containsExactlyInAnyOrder(
                        CollaborationWorkItemQueue.REMEDIATION,
                        CollaborationWorkItemQueue.PENDING_RELEASE
                );
    }

    @Test
    void visibleQueues_forGroupAdminIncludesAllQueues() {
        ManagementSessionClaims groupAdmin = session(List.of("GROUP_ADMIN"));
        when(groupAccessService.canViewCollaborationWorkItems(groupAdmin)).thenReturn(true);
        when(groupAccessService.hasCollaborationWorkItemAdminVisibility(groupAdmin)).thenReturn(true);

        assertThat(accessSupport.visibleQueues(groupAdmin))
                .containsExactlyInAnyOrder(CollaborationWorkItemQueue.values());
    }

    @Test
    void requireViewer_deniesUnauthorizedRoles() {
        ManagementSessionClaims auditAdmin = session(List.of("AUDIT_ADMIN"));
        when(groupAccessService.canViewCollaborationWorkItems(auditAdmin)).thenReturn(false);

        assertThatThrownBy(() -> accessSupport.requireViewer(auditAdmin))
                .isInstanceOf(CollaborationWorkItemAccessDeniedException.class);
    }

    @Test
    void requireVisibleQueue_deniesQueueOutsideRoleScope() {
        ManagementSessionClaims tester = session(List.of("TEMPLATE_TESTER"));
        when(groupAccessService.canViewCollaborationWorkItems(tester)).thenReturn(true);
        when(groupAccessService.hasCollaborationWorkItemAdminVisibility(tester)).thenReturn(false);

        assertThatThrownBy(() -> accessSupport.requireVisibleQueue(tester, CollaborationWorkItemQueue.APPROVAL))
                .isInstanceOf(CollaborationWorkItemAccessDeniedException.class);
    }

    @Test
    void requireVisibleQueue_allowsVisibleQueue() {
        ManagementSessionClaims tester = session(List.of("TEMPLATE_TESTER"));
        when(groupAccessService.canViewCollaborationWorkItems(tester)).thenReturn(true);
        when(groupAccessService.hasCollaborationWorkItemAdminVisibility(tester)).thenReturn(false);

        Set<CollaborationWorkItemQueue> visible = accessSupport.visibleQueues(tester);
        assertThat(visible).contains(CollaborationWorkItemQueue.TEST);
        accessSupport.requireVisibleQueue(tester, CollaborationWorkItemQueue.TEST);
    }

    private ManagementSessionClaims session(List<String> roles) {
        return new ManagementSessionClaims(
                "10000001",
                "User",
                "user@example.com",
                AuthSource.LOCAL,
                roles,
                List.of("RETAIL"),
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                Instant.now().plusSeconds(3600)
        );
    }
}
