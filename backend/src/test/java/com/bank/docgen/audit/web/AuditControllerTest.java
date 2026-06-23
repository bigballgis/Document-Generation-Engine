package com.bank.docgen.audit.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.web.ManagementAuthentication;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void globalAdminQueriesManagementAuditEvents() throws Exception {
        mockMvc.perform(get("/api/management/v1/admin/audit/management-events")
                        .param("actorRole", "GLOBAL_ADMIN")
                        .with(authentication(new ManagementAuthentication(globalAdmin()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.events").isArray());
    }

    @Test
    void groupAdminRequiresScopeForManagementAuditQuery() throws Exception {
        mockMvc.perform(get("/api/management/v1/admin/audit/management-events")
                        .param("actorRole", "GROUP_ADMIN")
                        .with(authentication(new ManagementAuthentication(groupAdmin()))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("AUDIT_SCOPE_REQUIRED"));
    }

    @Test
    void rejectsInvalidTimeWindow() throws Exception {
        mockMvc.perform(get("/api/management/v1/admin/audit/management-events")
                        .param("actorRole", "GLOBAL_ADMIN")
                        .param("eventAtFrom", Instant.parse("2026-06-23T12:00:00Z").toString())
                        .param("eventAtTo", Instant.parse("2026-06-23T10:00:00Z").toString())
                        .with(authentication(new ManagementAuthentication(globalAdmin()))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("INVALID_TIME_WINDOW"));
    }

    @Test
    void exportReturnsMaskedFormat() throws Exception {
        mockMvc.perform(get("/api/management/v1/admin/audit/management-events/export")
                        .param("actorRole", "GLOBAL_ADMIN")
                        .with(authentication(new ManagementAuthentication(globalAdmin()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.format").value("management-audit-export-v1-json"))
                .andExpect(jsonPath("$.result.events").isArray());
    }

    @Test
    void templateAuthorCannotQueryAuditEvents() throws Exception {
        mockMvc.perform(get("/api/management/v1/admin/audit/management-events")
                        .param("actorRole", "GLOBAL_ADMIN")
                        .with(authentication(new ManagementAuthentication(templateAuthor()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    private ManagementSessionClaims globalAdmin() {
        return session("10000001", List.of("GLOBAL_ADMIN"), List.of("*"));
    }

    private ManagementSessionClaims groupAdmin() {
        return session("10000002", List.of("GROUP_ADMIN"), List.of("RETAIL", "CORP"));
    }

    private ManagementSessionClaims templateAuthor() {
        return session("10000003", List.of("TEMPLATE_AUTHOR"), List.of("RETAIL"));
    }

    private ManagementSessionClaims session(String username, List<String> roles, List<String> groups) {
        return new ManagementSessionClaims(
                username,
                "Test User",
                username + "@example.com",
                AuthSource.LOCAL,
                roles,
                groups,
                "route.global-governance-home",
                List.of("route.audit-console"),
                Instant.now().plusSeconds(3600)
        );
    }
}
