package com.bank.docgen.contentmodule.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.web.ManagementAuthentication;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContentModuleControllerTest {

    private static final String MODULE_CODE = "MOD-LOAN-DISCLOSURE";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContentModuleRepository moduleRepository;

    @Autowired
    private ContentModuleVersionRepository versionRepository;

    private ManagementSessionClaims author;
    private ManagementSessionClaims approver;
    private ManagementSessionClaims groupAdmin;
    private ManagementSessionClaims tester;

    @BeforeEach
    void setUp() {
        moduleRepository.deleteAll();
        versionRepository.deleteAll();
        author = session("10000003", List.of("TEMPLATE_AUTHOR"), List.of("RETAIL"));
        approver = session("10000005", List.of("TEMPLATE_APPROVER"), List.of("RETAIL"));
        groupAdmin = session("10000002", List.of("GROUP_ADMIN"), List.of("RETAIL"));
        tester = session("10000006", List.of("TEMPLATE_TESTER"), List.of("RETAIL"));
    }

    @Test
    void createListAndUpdateDraftVersion() throws Exception {
        mockMvc.perform(post("/api/management/v1/content-modules")
                        .with(authentication(new ManagementAuthentication(author)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "moduleCode":"MOD-LOAN-DISCLOSURE",
                                  "groupCode":"RETAIL",
                                  "name":"Loan Disclosure Module",
                                  "description":"Standard clauses",
                                  "sharedGroupCodes":[],
                                  "semanticVersion":"1.0.0",
                                  "contentStructureJson":"{\\"blocks\\":[]}",
                                  "changeDescription":"Initial draft"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.moduleCode").value(MODULE_CODE))
                .andExpect(jsonPath("$.result.versions[0].reviewState").value("DRAFT"));

        mockMvc.perform(get("/api/management/v1/content-modules")
                        .param("groupCode", "RETAIL")
                        .with(authentication(new ManagementAuthentication(author))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content.length()").value(1));

        mockMvc.perform(put("/api/management/v1/content-modules/" + MODULE_CODE + "/versions/1.0.0")
                        .with(authentication(new ManagementAuthentication(author)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contentStructureJson":"{\\"blocks\\":[{\\"type\\":\\"paragraph\\"}]}",
                                  "changeDescription":"Updated draft"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.versions[0].changeDescription").value("Updated draft"));
    }

    @Test
    void getModuleDetail_returnsVersions() throws Exception {
        seedDraftModule();

        mockMvc.perform(get("/api/management/v1/content-modules/" + MODULE_CODE)
                        .with(authentication(new ManagementAuthentication(author))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.moduleCode").value(MODULE_CODE))
                .andExpect(jsonPath("$.result.versions[0].semanticVersion").value("1.0.0"));
    }

    @Test
    void createVersion_addsDraftVersion() throws Exception {
        seedDraftModule();

        mockMvc.perform(post("/api/management/v1/content-modules/" + MODULE_CODE + "/versions")
                        .with(authentication(new ManagementAuthentication(author)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "semanticVersion":"2.0.0",
                                  "contentStructureJson":"{\\"blocks\\":[]}",
                                  "changeDescription":"Next draft"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.versions.length()").value(2));
    }

    @Test
    void reviewWorkflow_submitApproveAndStop() throws Exception {
        seedApprovedModuleViaWorkflow();

        mockMvc.perform(post("/api/management/v1/content-modules/" + MODULE_CODE + "/lifecycle/operation/apply")
                        .with(authentication(new ManagementAuthentication(groupAdmin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operationType":"STOP_USE",
                                  "actorRole":"GROUP_ADMIN",
                                  "actorId":"group-admin-a",
                                  "impactSummaryViewed":true,
                                  "secondConfirmation":true,
                                  "impactSummary":{
                                    "referenceTemplateCount":2,
                                    "referenceTemplateListHint":"TPL-LOAN-NOTICE,TPL-RENEWAL-NOTICE",
                                    "impactedReleaseVersionsHint":"v1.0.0,v1.1.0",
                                    "defaultRouteAffected":true,
                                    "recentCallSummary":"recentCalls=12/7d",
                                    "remediationHint":"migrate callers to MOD-LOAN-DISCLOSURE-V3",
                                    "templateStopRequired":true,
                                    "releaseStopRequired":true
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.applied").value(true))
                .andExpect(jsonPath("$.result.snapshot.state").value("STOPPED"));
    }

    @Test
    void testerCannotCreateModule() throws Exception {
        mockMvc.perform(post("/api/management/v1/content-modules")
                        .with(authentication(new ManagementAuthentication(tester)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "moduleCode":"MOD-DENIED",
                                  "groupCode":"RETAIL",
                                  "name":"Denied",
                                  "semanticVersion":"1.0.0",
                                  "contentStructureJson":"{}"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void testerCannotListOrGetModules() throws Exception {
        seedDraftModule();

        mockMvc.perform(get("/api/management/v1/content-modules")
                        .param("groupCode", "RETAIL")
                        .with(authentication(new ManagementAuthentication(tester))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/management/v1/content-modules/" + MODULE_CODE)
                        .with(authentication(new ManagementAuthentication(tester))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getModuleDetail_returnsDraftContentStructureForAuthor() throws Exception {
        seedDraftModule();

        mockMvc.perform(get("/api/management/v1/content-modules/" + MODULE_CODE)
                        .with(authentication(new ManagementAuthentication(author))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.versions[0].contentStructureJson").value("{\"blocks\":[]}"));
    }

    @Test
    void listAccessibleWithoutGroupCode_returnsModulesAcrossAuthorizedGroups() throws Exception {
        seedDraftModule();

        mockMvc.perform(get("/api/management/v1/content-modules")
                        .with(authentication(new ManagementAuthentication(author))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content.length()").value(1))
                .andExpect(jsonPath("$.result.content[0].moduleCode").value(MODULE_CODE));
    }

    @Test
    void listIncludesModulesSharedIntoGroup() throws Exception {
        UUID corpModuleId = UUID.randomUUID();
        moduleRepository.save(new ContentModuleEntity(
                corpModuleId,
                "MOD-CORP-SHARED",
                "CORP",
                "Corp module shared to retail",
                "desc",
                "[\"RETAIL\"]",
                author.username()
        ));
        versionRepository.save(new ContentModuleVersionEntity(
                UUID.randomUUID(),
                corpModuleId,
                "1.0.0",
                "{\"blocks\":[]}",
                "Initial",
                author.username()
        ));
        seedDraftModule();

        mockMvc.perform(get("/api/management/v1/content-modules")
                        .param("groupCode", "RETAIL")
                        .with(authentication(new ManagementAuthentication(author))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content.length()").value(2))
                .andExpect(jsonPath("$.result.content[?(@.moduleCode=='MOD-CORP-SHARED')]").exists())
                .andExpect(jsonPath("$.result.content[?(@.moduleCode=='MOD-LOAN-DISCLOSURE')]").exists());
    }

    @Test
    void submitWithoutChangeDescriptionReturns422() throws Exception {
        seedDraftModule();

        mockMvc.perform(post("/api/management/v1/content-modules/" + MODULE_CODE + "/review/transition")
                        .with(authentication(new ManagementAuthentication(author)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operation":"SUBMIT_FOR_REVIEW",
                                  "actorRole":"TEMPLATE_AUTHOR",
                                  "actorId":"author-a"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("MODULE_CHANGE_DESCRIPTION_REQUIRED"));
    }

    private void seedDraftModule() {
        UUID moduleId = UUID.randomUUID();
        moduleRepository.save(new ContentModuleEntity(
                moduleId,
                MODULE_CODE,
                "RETAIL",
                "Loan Disclosure Module",
                "desc",
                "[]",
                author.username()
        ));
        versionRepository.save(new ContentModuleVersionEntity(
                UUID.randomUUID(),
                moduleId,
                "1.0.0",
                "{\"blocks\":[]}",
                "Initial",
                author.username()
        ));
    }

    private void seedApprovedModuleViaWorkflow() throws Exception {
        seedDraftModule();

        mockMvc.perform(post("/api/management/v1/content-modules/" + MODULE_CODE + "/review/transition")
                        .with(authentication(new ManagementAuthentication(author)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operation":"SUBMIT_FOR_REVIEW",
                                  "actorRole":"TEMPLATE_AUTHOR",
                                  "actorId":"author-a",
                                  "changeDescription":"ready for review"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.snapshot.state").value("SUBMITTED"));

        mockMvc.perform(post("/api/management/v1/content-modules/" + MODULE_CODE + "/review/transition")
                        .with(authentication(new ManagementAuthentication(approver)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operation":"APPROVE_REVIEW",
                                  "actorRole":"APPROVER",
                                  "actorId":"approver-a"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.snapshot.state").value("APPROVED"));
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
