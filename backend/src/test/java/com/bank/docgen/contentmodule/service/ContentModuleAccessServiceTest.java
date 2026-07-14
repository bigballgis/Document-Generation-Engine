package com.bank.docgen.contentmodule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.domain.ContentModuleGovernanceActorRole;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ContentModuleAccessServiceTest {

    private static final UUID MODULE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock
    private ContentModuleRepository moduleRepository;
    @Mock
    private GroupAccessService groupAccessService;

    private ContentModuleAccessService accessSupport;
    private ContentModuleEntity module;

    @BeforeEach
    void setUp() {
        accessSupport = new ContentModuleAccessService(moduleRepository, groupAccessService, new ObjectMapper());
        module = new ContentModuleEntity(
                MODULE_ID,
                "MOD-LOAN-DISCLOSURE",
                "RETAIL",
                "Loan Disclosure",
                "desc",
                "[\"WHOLESALE\"]",
                "10000003"
        );
    }

    @Test
    void resolveModule_byModuleCode() {
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(Optional.of(module));

        assertThat(accessSupport.resolveModule("MOD-LOAN-DISCLOSURE")).contains(module);
    }

    @Test
    void resolveModule_byUuid() {
        when(moduleRepository.findByIdAndDeletedAtIsNull(MODULE_ID)).thenReturn(Optional.of(module));

        assertThat(accessSupport.resolveModule(MODULE_ID.toString())).contains(module);
    }

    @Test
    void requireExistingModule_rejectsBlankId() {
        assertThatThrownBy(() -> accessSupport.requireExistingModule(" "))
                .isInstanceOf(ContentModuleGovernanceException.class)
                .satisfies(ex -> {
                    ContentModuleGovernanceException governance = (ContentModuleGovernanceException) ex;
                    assertThat(governance.errorCode()).isEqualTo("MODULE_ID_REQUIRED");
                    assertThat(governance.httpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                });
    }

    @Test
    void requireExistingModule_throwsWhenMissing() {
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accessSupport.requireExistingModule("MISSING"))
                .isInstanceOf(ContentModuleNotFoundException.class);
    }

    @Test
    void requireReadableModule_allowsSharedGroupAccess() {
        ManagementSessionClaims wholesaleUser = session("10000007", List.of("TEMPLATE_AUTHOR"), List.of("WHOLESALE"));
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(Optional.of(module));
        when(groupAccessService.canAccessGroup(wholesaleUser, "RETAIL")).thenReturn(false);
        when(groupAccessService.canAccessGroup(wholesaleUser, "WHOLESALE")).thenReturn(true);

        ContentModuleEntity readable = accessSupport.requireReadableModule("MOD-LOAN-DISCLOSURE", wholesaleUser);

        assertThat(readable.getModuleCode()).isEqualTo("MOD-LOAN-DISCLOSURE");
    }

    @Test
    void requireReadableModule_deniesWhenNoGroupAccess() {
        ManagementSessionClaims outsider = session("10000008", List.of("TEMPLATE_AUTHOR"), List.of("CORPORATE"));
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(Optional.of(module));
        when(groupAccessService.canAccessGroup(outsider, "RETAIL")).thenReturn(false);
        when(groupAccessService.canAccessGroup(outsider, "WHOLESALE")).thenReturn(false);

        assertThatThrownBy(() -> accessSupport.requireReadableModule("MOD-LOAN-DISCLOSURE", outsider))
                .isInstanceOf(ContentModuleAccessDeniedException.class);
    }

    @Test
    void requireAuthoringModule_requiresAuthorRole() {
        ManagementSessionClaims tester = session("10000006", List.of("TEMPLATE_TESTER"), List.of("RETAIL"));
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(Optional.of(module));
        when(groupAccessService.canAccessGroup(tester, "RETAIL")).thenReturn(true);
        when(groupAccessService.canAuthorContentModules(tester)).thenReturn(false);

        assertThatThrownBy(() -> accessSupport.requireAuthoringModule("MOD-LOAN-DISCLOSURE", tester))
                .isInstanceOf(ContentModuleAccessDeniedException.class);
    }

    @Test
    void readSharedGroupCodes_returnsEmptyOnInvalidJson() {
        module.setSharedGroupCodesJson("not-json");

        assertThat(accessSupport.readSharedGroupCodes(module)).isEmpty();
    }

    @Test
    void writeSharedGroupCodes_normalizesAndDedupes() {
        String json = accessSupport.writeSharedGroupCodes(Arrays.asList(" retail ", "RETAIL", null, " "));

        assertThat(json).isEqualTo("[\"RETAIL\"]");
    }

    @Test
    void writeSharedGroupCodes_sortsForStableOrder() {
        String json = accessSupport.writeSharedGroupCodes(List.of("WEALTH", "CORP"));

        assertThat(json).isEqualTo("[\"CORP\",\"WEALTH\"]");
    }

    @Test
    void writeSharedGroupCodesExcludingOwner_dropsOwningGroup() {
        String json = accessSupport.writeSharedGroupCodesExcludingOwner(
                List.of("RETAIL", "WEALTH", "retail"),
                "RETAIL"
        );

        assertThat(json).isEqualTo("[\"WEALTH\"]");
    }

    @Test
    void assertActorSession_matchesSessionRole() {
        ManagementSessionClaims approver = session("10000005", List.of("TEMPLATE_APPROVER"), List.of("RETAIL"));

        accessSupport.assertActorSession(approver, ContentModuleGovernanceActorRole.APPROVER);
    }

    @Test
    void assertActorSession_rejectsMismatchedRole() {
        ManagementSessionClaims author = session("10000003", List.of("TEMPLATE_AUTHOR"), List.of("RETAIL"));

        assertThatThrownBy(() -> accessSupport.assertActorSession(author, ContentModuleGovernanceActorRole.APPROVER))
                .isInstanceOf(ContentModuleGovernanceException.class)
                .extracting(ex -> ((ContentModuleGovernanceException) ex).errorCode())
                .isEqualTo("MODULE_REVIEW_ROLE_DENIED");
    }

    @Test
    void assertLifecycleActorSession_allowsGroupAdminOnly() {
        ManagementSessionClaims groupAdmin = session("10000002", List.of("GROUP_ADMIN"), List.of("RETAIL"));

        accessSupport.assertLifecycleActorSession(groupAdmin, ContentModuleGovernanceActorRole.GROUP_ADMIN);
    }

    @Test
    void assertLifecycleActorSession_rejectsAuthorRole() {
        ManagementSessionClaims author = session("10000003", List.of("TEMPLATE_AUTHOR"), List.of("RETAIL"));

        assertThatThrownBy(() -> accessSupport.assertLifecycleActorSession(
                author,
                ContentModuleGovernanceActorRole.TEMPLATE_AUTHOR
        ))
                .isInstanceOf(ContentModuleGovernanceException.class)
                .extracting(ex -> ((ContentModuleGovernanceException) ex).errorCode())
                .isEqualTo("CONTENT_MODULE_ROLE_DENIED");
    }

    @Test
    void sessionHasActorRole_coversAllRoles() {
        assertThat(accessSupport.sessionHasActorRole(
                session("g", List.of("GLOBAL_ADMIN"), List.of("*")),
                ContentModuleGovernanceActorRole.GLOBAL_ADMIN
        )).isTrue();
        assertThat(accessSupport.sessionHasActorRole(
                session("d", List.of("MASTER_DESIGNER"), List.of("RETAIL")),
                ContentModuleGovernanceActorRole.MASTER_DESIGNER
        )).isTrue();
    }

    @Test
    void actorSummary_formatsDisplayNameAndUsername() {
        ManagementSessionClaims author = new ManagementSessionClaims(
                "10000003",
                "Template Author",
                "author@example.com",
                AuthSource.LOCAL,
                List.of("TEMPLATE_AUTHOR"),
                List.of("RETAIL"),
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                Instant.now().plusSeconds(3600)
        );

        assertThat(accessSupport.actorSummary(author)).isEqualTo("Template Author (10000003)");
        assertThat(accessSupport.publicModuleId(module)).isEqualTo("MOD-LOAN-DISCLOSURE");
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
