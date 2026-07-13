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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GenerationAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void globalAdminReceivesNotFoundWhenTemplateMissing() throws Exception {
        mockMvc.perform(get("/api/management/v1/audit/generation")
                        .param("templateExternalId", "RETAIL-ACCOUNT-OPEN")
                        .param("page", "0")
                        .param("size", "20")
                        .with(authentication(new ManagementAuthentication(globalAdmin()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TEMPLATE_NOT_FOUND"));
    }

    @Test
    void templateAuthorCannotQueryGenerationAudit() throws Exception {
        mockMvc.perform(get("/api/management/v1/audit/generation")
                        .param("templateExternalId", "RETAIL-ACCOUNT-OPEN")
                        .with(authentication(new ManagementAuthentication(templateAuthor()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    private ManagementSessionClaims globalAdmin() {
        return session("10000001", List.of("GLOBAL_ADMIN"), List.of("*"));
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
