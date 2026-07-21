package com.bank.docgen.collaboration.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.web.ManagementAuthentication;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemStatus;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemTriggerType;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemEntity;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemReadMarkerRepository;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CollaborationNotificationControllerTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID WORK_ITEM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ESCALATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CORP_ITEM_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID RESOLVED_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CollaborationWorkItemRepository workItemRepository;

    @Autowired
    private CollaborationWorkItemReadMarkerRepository readMarkerRepository;

    private ManagementSessionClaims tester;
    private ManagementSessionClaims groupAdmin;
    private ManagementSessionClaims auditAdmin;

    @BeforeEach
    void setUp() {
        readMarkerRepository.deleteAll();
        workItemRepository.deleteAll();
        tester = session("10000006", List.of("TEMPLATE_TESTER"), List.of("RETAIL"));
        groupAdmin = session("10000002", List.of("GROUP_ADMIN"), List.of("RETAIL"));
        auditAdmin = session("10000004", List.of("AUDIT_ADMIN"), List.of());

        workItemRepository.save(openItem(
                WORK_ITEM_ID,
                "RETAIL",
                CollaborationWorkItemQueue.TEST,
                CollaborationWorkItemTriggerType.SUBMIT_FOR_TEST,
                "Template submitted for testing"
        ));
        workItemRepository.save(openItem(
                ESCALATION_ID,
                "RETAIL",
                CollaborationWorkItemQueue.ESCALATION,
                CollaborationWorkItemTriggerType.TIMEOUT_ESCALATION,
                "Timeout escalation"
        ));
        workItemRepository.save(openItem(
                CORP_ITEM_ID,
                "CORP",
                CollaborationWorkItemQueue.TEST,
                CollaborationWorkItemTriggerType.SUBMIT_FOR_TEST,
                "Corp template submitted for testing"
        ));
        CollaborationWorkItemEntity resolved = openItem(
                RESOLVED_ID,
                "RETAIL",
                CollaborationWorkItemQueue.TEST,
                CollaborationWorkItemTriggerType.SUBMIT_FOR_TEST,
                "Already resolved"
        );
        resolved.setStatus(CollaborationWorkItemStatus.RESOLVED);
        resolved.setResolvedAt(Instant.now());
        workItemRepository.save(resolved);
    }

    @Test
    void unreadCount_returnsVisibleUnreadOnly() throws Exception {
        mockMvc.perform(get("/api/management/v1/collaboration-notifications/unread-count")
                        .with(authentication(new ManagementAuthentication(tester))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.unreadCount").value(1));
    }

    @Test
    void list_returnsOpenVisibleItemsWithReadFalse() throws Exception {
        mockMvc.perform(get("/api/management/v1/collaboration-notifications")
                        .with(authentication(new ManagementAuthentication(tester))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(1))
                .andExpect(jsonPath("$.result[0].workItemId").value(WORK_ITEM_ID.toString()))
                .andExpect(jsonPath("$.result[0].queue").value("TEST"))
                .andExpect(jsonPath("$.result[0].read").value(false))
                .andExpect(jsonPath("$.result[0].summaryText").value("Template submitted for testing"));
    }

    @Test
    void markRead_thenUnreadCountDropsAndListShowsRead() throws Exception {
        mockMvc.perform(post("/api/management/v1/collaboration-notifications/"
                        + WORK_ITEM_ID + "/read")
                        .with(authentication(new ManagementAuthentication(tester))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.unreadCount").value(0));

        mockMvc.perform(get("/api/management/v1/collaboration-notifications/unread-count")
                        .with(authentication(new ManagementAuthentication(tester))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.unreadCount").value(0));

        mockMvc.perform(get("/api/management/v1/collaboration-notifications")
                        .with(authentication(new ManagementAuthentication(tester))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].read").value(true));
    }

    @Test
    void markAllRead_clearsUnreadCount() throws Exception {
        UUID second = UUID.fromString("55555555-5555-5555-5555-555555555555");
        workItemRepository.save(openItem(
                second,
                "RETAIL",
                CollaborationWorkItemQueue.TEST,
                CollaborationWorkItemTriggerType.SUBMIT_FOR_TEST,
                "Second open item"
        ));

        mockMvc.perform(post("/api/management/v1/collaboration-notifications/read-all")
                        .with(authentication(new ManagementAuthentication(tester))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.unreadCount").value(0));

        mockMvc.perform(get("/api/management/v1/collaboration-notifications/unread-count")
                        .with(authentication(new ManagementAuthentication(tester))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.unreadCount").value(0));
    }

    @Test
    void markRead_returns404ForOtherGroupItem() throws Exception {
        mockMvc.perform(post("/api/management/v1/collaboration-notifications/"
                        + CORP_ITEM_ID + "/read")
                        .with(authentication(new ManagementAuthentication(tester))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WORK_ITEM_NOT_FOUND"));
    }

    @Test
    void markRead_returns404ForEscalationWhenTesterCannotSeeQueue() throws Exception {
        mockMvc.perform(post("/api/management/v1/collaboration-notifications/"
                        + ESCALATION_ID + "/read")
                        .with(authentication(new ManagementAuthentication(tester))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("WORK_ITEM_NOT_FOUND"));
    }

    @Test
    void groupAdmin_seesEscalationInListAndUnread() throws Exception {
        mockMvc.perform(get("/api/management/v1/collaboration-notifications/unread-count")
                        .with(authentication(new ManagementAuthentication(groupAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.unreadCount").value(2));

        mockMvc.perform(get("/api/management/v1/collaboration-notifications")
                        .with(authentication(new ManagementAuthentication(groupAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(2));
    }

    @Test
    void deniesUnauthorizedRole() throws Exception {
        mockMvc.perform(get("/api/management/v1/collaboration-notifications/unread-count")
                        .with(authentication(new ManagementAuthentication(auditAdmin))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    private CollaborationWorkItemEntity openItem(
            UUID id,
            String groupCode,
            CollaborationWorkItemQueue queue,
            CollaborationWorkItemTriggerType triggerType,
            String summary
    ) {
        return new CollaborationWorkItemEntity(
                id,
                TEMPLATE_ID,
                "TPL-LOAN-NOTICE",
                "Loan Notice Template",
                groupCode,
                queue,
                triggerType,
                CollaborationWorkItemStatus.OPEN,
                "10000003",
                summary
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
