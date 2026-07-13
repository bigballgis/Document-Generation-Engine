package com.bank.docgen.collaboration.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.web.ManagementAuthentication;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemStatus;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemTriggerType;
import com.bank.docgen.collaboration.persistence.CollaborationWorkItemEntity;
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
class CollaborationWorkItemControllerTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CollaborationWorkItemRepository workItemRepository;

    private ManagementSessionClaims tester;
    private ManagementSessionClaims masterDesigner;

    @BeforeEach
    void setUp() {
        workItemRepository.deleteAll();
        tester = session("10000006", List.of("TEMPLATE_TESTER"), List.of("RETAIL"));
        masterDesigner = session("10000004", List.of("MASTER_DESIGNER"), List.of("RETAIL"));
        workItemRepository.save(new CollaborationWorkItemEntity(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                TEMPLATE_ID,
                "TPL-LOAN-NOTICE",
                "Loan Notice Template",
                "RETAIL",
                CollaborationWorkItemQueue.TEST,
                CollaborationWorkItemTriggerType.SUBMIT_FOR_TEST,
                CollaborationWorkItemStatus.OPEN,
                "10000003",
                "Template submitted for testing"
        ));
    }

    @Test
    void listQueue_returnsNonSensitiveSummaryForTester() throws Exception {
        mockMvc.perform(get("/api/management/v1/collaboration-work-items")
                        .with(authentication(new ManagementAuthentication(tester))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(1))
                .andExpect(jsonPath("$.result[0].templateName").value("Loan Notice Template"))
                .andExpect(jsonPath("$.result[0].groupCode").value("RETAIL"))
                .andExpect(jsonPath("$.result[0].submitterUserId").value("10000003"))
                .andExpect(jsonPath("$.result[0].queue").value("TEST"))
                .andExpect(jsonPath("$.result[0].ageSeconds").isNumber());
    }

    @Test
    void listQueue_deniesMasterDesigner() throws Exception {
        mockMvc.perform(get("/api/management/v1/collaboration-work-items")
                        .with(authentication(new ManagementAuthentication(masterDesigner))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
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
