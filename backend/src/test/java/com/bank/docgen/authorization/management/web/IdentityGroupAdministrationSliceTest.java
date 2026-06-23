package com.bank.docgen.authorization.management.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.domain.ManagementRoute;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IdentityGroupAdministrationSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final ManagementSessionClaims globalAdmin =
            session("10000001", List.of("GLOBAL_ADMIN"), List.of("*"));
    private final ManagementSessionClaims retailGroupAdmin =
            session("10000002", List.of("GROUP_ADMIN"), List.of("RETAIL"));
    private final ManagementSessionClaims templateAuthor =
            session("10000003", List.of("TEMPLATE_AUTHOR"), List.of("RETAIL"));

    // ---- Group management ----

    @Test
    void globalAdminCreatesGroupThenConflict() throws Exception {
        mockMvc.perform(post("/api/management/v1/groups")
                        .with(authentication(new ManagementAuthentication(globalAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupCode":"WEALTH","displayName":"Wealth","dimension":"DEPARTMENT"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.groupCode").value("WEALTH"))
                .andExpect(jsonPath("$.result.dimension").value("DEPARTMENT"))
                .andExpect(jsonPath("$.result.enabled").value(true));

        mockMvc.perform(post("/api/management/v1/groups")
                        .with(authentication(new ManagementAuthentication(globalAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupCode":"WEALTH","displayName":"Wealth Again","dimension":"DEPARTMENT"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("GROUP_CODE_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.error.category").value("CONFLICT"));
    }

    @Test
    void groupAdminCannotCreateGroup() throws Exception {
        mockMvc.perform(post("/api/management/v1/groups")
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupCode":"PRIVATE","displayName":"Private","dimension":"BUSINESS_LINE"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("GROUP_MANAGEMENT_NOT_ALLOWED"));
    }

    @Test
    void groupAdminListsOnlyScopedGroups() throws Exception {
        mockMvc.perform(get("/api/management/v1/groups")
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[?(@.groupCode=='RETAIL')]").exists())
                .andExpect(jsonPath("$.result.content[?(@.groupCode=='CORP')]").doesNotExist());
    }

    @Test
    void missingGroupReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/management/v1/groups/" + UUID.randomUUID())
                        .with(authentication(new ManagementAuthentication(globalAdmin))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("GROUP_NOT_FOUND"))
                .andExpect(jsonPath("$.error.category").value("NOT_FOUND"));
    }

    @Test
    void globalAdminUpdatesDisablesEnablesGroup() throws Exception {
        String id = createGroup("INSURE", "Insurance", "DEPARTMENT");

        mockMvc.perform(put("/api/management/v1/groups/" + id)
                        .with(authentication(new ManagementAuthentication(globalAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Insurance Renamed"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.displayName").value("Insurance Renamed"));

        mockMvc.perform(post("/api/management/v1/groups/" + id + "/disable")
                        .with(authentication(new ManagementAuthentication(globalAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.enabled").value(false));

        mockMvc.perform(post("/api/management/v1/groups/" + id + "/enable")
                        .with(authentication(new ManagementAuthentication(globalAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.enabled").value(true));
    }

    // ---- User management ----

    @Test
    void globalAdminCreatesUserWithoutLeakingPassword() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/management/v1/users")
                        .with(authentication(new ManagementAuthentication(globalAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserBody("30000001", "TEMPLATE_AUTHOR", "RETAIL")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.username").value("30000001"))
                .andExpect(jsonPath("$.result.roles[0]").value("TEMPLATE_AUTHOR"))
                .andExpect(jsonPath("$.result.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.result.password").doesNotExist())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("InitSecret1234"));
    }

    @Test
    void duplicateUsernameReturnsConflict() throws Exception {
        mockMvc.perform(post("/api/management/v1/users")
                        .with(authentication(new ManagementAuthentication(globalAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserBody("30000002", "TEMPLATE_AUTHOR", "RETAIL")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/management/v1/users")
                        .with(authentication(new ManagementAuthentication(globalAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserBody("30000002", "TEMPLATE_TESTER", "RETAIL")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("USERNAME_ALREADY_EXISTS"));
    }

    @Test
    void groupAdminCreatesUserWithinScope() throws Exception {
        mockMvc.perform(post("/api/management/v1/users")
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserBody("30000003", "TEMPLATE_TESTER", "RETAIL")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.authorizedGroupCodes[0]").value("RETAIL"));
    }

    @Test
    void groupAdminOutOfRangeScopeIsRejected() throws Exception {
        mockMvc.perform(post("/api/management/v1/users")
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"30000004","displayName":"N","email":"n@example.com",
                                 "initialPassword":"InitSecret1234","roles":["TEMPLATE_AUTHOR"],
                                 "authorizedGroupCodes":["RETAIL","CORP"]}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("GROUP_SCOPE_OUT_OF_RANGE"));
    }

    @Test
    void groupAdminPrivilegedRoleIsRejected() throws Exception {
        mockMvc.perform(post("/api/management/v1/users")
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserBody("30000005", "GLOBAL_ADMIN", "RETAIL")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ROLE_ASSIGNMENT_NOT_ALLOWED"));
    }

    @Test
    void nonAdminCannotManageUsers() throws Exception {
        mockMvc.perform(post("/api/management/v1/users")
                        .with(authentication(new ManagementAuthentication(templateAuthor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserBody("30000006", "TEMPLATE_AUTHOR", "RETAIL")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void groupAdminCannotDeleteUser() throws Exception {
        String id = createUser(globalAdmin, "30000007", "TEMPLATE_AUTHOR", "RETAIL");

        mockMvc.perform(delete("/api/management/v1/users/" + id)
                        .with(authentication(new ManagementAuthentication(retailGroupAdmin))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("USER_DELETE_NOT_ALLOWED"));
    }

    @Test
    void globalAdminDeletesUserThenNotFound() throws Exception {
        String id = createUser(globalAdmin, "30000008", "TEMPLATE_AUTHOR", "RETAIL");

        mockMvc.perform(delete("/api/management/v1/users/" + id)
                        .with(authentication(new ManagementAuthentication(globalAdmin))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/management/v1/users/" + id)
                        .with(authentication(new ManagementAuthentication(globalAdmin))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
    }

    @Test
    void globalAdminDisablesEnablesAndResetsPassword() throws Exception {
        String id = createUser(globalAdmin, "30000009", "TEMPLATE_AUTHOR", "RETAIL");

        mockMvc.perform(post("/api/management/v1/users/" + id + "/disable")
                        .with(authentication(new ManagementAuthentication(globalAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.enabled").value(false));

        mockMvc.perform(post("/api/management/v1/users/" + id + "/enable")
                        .with(authentication(new ManagementAuthentication(globalAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.enabled").value(true));

        MvcResult reset = mockMvc.perform(post("/api/management/v1/users/" + id + "/reset-password")
                        .with(authentication(new ManagementAuthentication(globalAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPassword":"ResetSecret1234"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        org.junit.jupiter.api.Assertions.assertFalse(
                reset.getResponse().getContentAsString().contains("ResetSecret1234"));
    }

    @Test
    void globalAdminUpdatesUserRolesAndScope() throws Exception {
        String id = createUser(globalAdmin, "30000010", "TEMPLATE_AUTHOR", "RETAIL");

        mockMvc.perform(put("/api/management/v1/users/" + id)
                        .with(authentication(new ManagementAuthentication(globalAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Updated","email":"u@example.com",
                                 "roles":["TEMPLATE_TESTER","TEMPLATE_APPROVER"],
                                 "authorizedGroupCodes":["RETAIL","CORP"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.displayName").value("Updated"))
                .andExpect(jsonPath("$.result.roles", org.hamcrest.Matchers.hasSize(2)));
    }

    @Test
    void getMissingUserReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/management/v1/users/" + UUID.randomUUID())
                        .with(authentication(new ManagementAuthentication(globalAdmin))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
    }

    @Test
    void identityAdministrationRouteVisibleToAdmins() {
        org.assertj.core.api.Assertions.assertThat(globalAdmin.visibleRoutes())
                .contains(ManagementRoute.IDENTITY_ADMINISTRATION.routeKey());
    }

    private String createGroup(String code, String name, String dimension) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/management/v1/groups")
                        .with(authentication(new ManagementAuthentication(globalAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupCode":"%s","displayName":"%s","dimension":"%s"}
                                """.formatted(code, name, dimension)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("result").path("id").asText();
    }

    private String createUser(ManagementSessionClaims actor, String username, String role, String group)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/management/v1/users")
                        .with(authentication(new ManagementAuthentication(actor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserBody(username, role, group)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("result").path("id").asText();
    }

    private String createUserBody(String username, String role, String group) {
        return """
                {"username":"%s","displayName":"User %s","email":"%s@example.com",
                 "initialPassword":"InitSecret1234","roles":["%s"],"authorizedGroupCodes":["%s"]}
                """.formatted(username, username, username, role, group);
    }

    private static ManagementSessionClaims session(String username, List<String> roles, List<String> groups) {
        return new ManagementSessionClaims(
                username,
                username,
                username + "@example.com",
                AuthSource.LOCAL,
                roles,
                groups,
                "route.global-governance-home",
                List.of(
                        ManagementRoute.GLOBAL_GOVERNANCE_HOME.routeKey(),
                        ManagementRoute.IDENTITY_ADMINISTRATION.routeKey()),
                Instant.now().plusSeconds(3600)
        );
    }
}
