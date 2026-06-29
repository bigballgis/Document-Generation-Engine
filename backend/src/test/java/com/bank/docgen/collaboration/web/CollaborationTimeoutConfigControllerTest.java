package com.bank.docgen.collaboration.web;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.web.ManagementAuthentication;
import com.bank.docgen.collaboration.domain.CollaborationTimeoutScope;
import com.bank.docgen.collaboration.persistence.CollaborationTimeoutConfigEntity;
import com.bank.docgen.collaboration.persistence.CollaborationTimeoutConfigRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CollaborationTimeoutConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CollaborationTimeoutConfigRepository repository;

    private ManagementSessionClaims globalAdmin;
    private ManagementSessionClaims groupAdmin;
    private ManagementSessionClaims tester;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.save(new CollaborationTimeoutConfigEntity(
                UUID.fromString("00000000-0000-4000-8000-000000000029"),
                CollaborationTimeoutScope.GLOBAL,
                null,
                72,
                72,
                48,
                168
        ));
        globalAdmin = session("10000001", List.of("GLOBAL_ADMIN"), List.of("*"));
        groupAdmin = session("10000002", List.of("GROUP_ADMIN"), List.of("RETAIL"));
        tester = session("10000003", List.of("TEMPLATE_TESTER"), List.of("RETAIL"));
    }

    @Test
    void getGlobal_returnsDefaultThresholdsForGlobalAdmin() throws Exception {
        mockMvc.perform(get("/api/management/v1/collaboration-timeout-config")
                        .with(authentication(new ManagementAuthentication(globalAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.scopeType").value("GLOBAL"))
                .andExpect(jsonPath("$.result.testThresholdHours").value(72))
                .andExpect(jsonPath("$.result.approvalThresholdHours").value(72))
                .andExpect(jsonPath("$.result.pendingReleaseThresholdHours").value(48))
                .andExpect(jsonPath("$.result.remediationThresholdHours").value(168));
    }

    @Test
    void getGlobal_deniesGroupAdmin() throws Exception {
        mockMvc.perform(get("/api/management/v1/collaboration-timeout-config")
                        .with(authentication(new ManagementAuthentication(groupAdmin))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void putGroupOverride_persistsAndReturnsGroupScope() throws Exception {
        String body = """
                {
                  "scopeType": "GROUP",
                  "groupCode": "RETAIL",
                  "testThresholdHours": 24,
                  "approvalThresholdHours": 24,
                  "pendingReleaseThresholdHours": 12,
                  "remediationThresholdHours": 96
                }
                """;

        mockMvc.perform(put("/api/management/v1/collaboration-timeout-config")
                        .contentType(APPLICATION_JSON)
                        .content(body)
                        .with(authentication(new ManagementAuthentication(groupAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.scopeType").value("GROUP"))
                .andExpect(jsonPath("$.result.groupCode").value("RETAIL"))
                .andExpect(jsonPath("$.result.testThresholdHours").value(24));

        mockMvc.perform(get("/api/management/v1/collaboration-timeout-config")
                        .param("groupCode", "RETAIL")
                        .with(authentication(new ManagementAuthentication(groupAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.scopeType").value("GROUP"))
                .andExpect(jsonPath("$.result.testThresholdHours").value(24));
    }

    @Test
    void put_deniesTester() throws Exception {
        String body = """
                {
                  "scopeType": "GROUP",
                  "groupCode": "RETAIL",
                  "testThresholdHours": 24,
                  "approvalThresholdHours": 24,
                  "pendingReleaseThresholdHours": 12,
                  "remediationThresholdHours": 96
                }
                """;

        mockMvc.perform(put("/api/management/v1/collaboration-timeout-config")
                        .contentType(APPLICATION_JSON)
                        .content(body)
                        .with(authentication(new ManagementAuthentication(tester))))
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
