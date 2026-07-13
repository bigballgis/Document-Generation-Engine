package com.bank.docgen.contentmodule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.api.CatalogQueryPage;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.api.CreateContentModuleRequest;
import com.bank.docgen.contentmodule.api.CreateContentModuleVersionRequest;
import com.bank.docgen.contentmodule.api.UpdateContentModuleVersionRequest;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewState;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class ContentModuleServiceTest {

    private static final UUID MODULE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID VERSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private ContentModuleRepository moduleRepository;
    @Mock
    private ContentModuleVersionRepository versionRepository;
    @Mock
    private GroupAccessService groupAccessService;
    @Mock
    private ManagementAuditRecorder auditRecorder;

    private ContentModuleService service;
    private ContentModuleAccessService accessSupport;
    private ContentModuleEntity module;
    private ContentModuleVersionEntity draftVersion;
    private ManagementSessionClaims author;
    private ManagementSessionClaims tester;

    @BeforeEach
    void setUp() {
        accessSupport = new ContentModuleAccessService(moduleRepository, groupAccessService, new ObjectMapper());
        service = new ContentModuleService(
                moduleRepository,
                versionRepository,
                groupAccessService,
                accessSupport,
                auditRecorder
        );
        module = new ContentModuleEntity(
                MODULE_ID,
                "MOD-LOAN-DISCLOSURE",
                "RETAIL",
                "Loan Disclosure",
                "desc",
                "[]",
                "10000003"
        );
        draftVersion = new ContentModuleVersionEntity(
                VERSION_ID,
                MODULE_ID,
                "1.0.0",
                "{\"blocks\":[]}",
                "Initial",
                "10000003"
        );
        author = session("10000003", List.of("TEMPLATE_AUTHOR"), List.of("RETAIL"));
        tester = session("10000006", List.of("TEMPLATE_TESTER"), List.of("RETAIL"));
        lenient().when(groupAccessService.canBrowseContentModuleCatalog(author)).thenReturn(true);
        lenient().when(groupAccessService.canViewContentModuleStructure(author)).thenReturn(true);
        lenient().when(groupAccessService.canBrowseContentModuleCatalog(tester)).thenReturn(false);
    }

    @Test
    void list_returnsSummariesForAccessibleGroup() {
        when(groupAccessService.canBrowseContentModuleCatalog(author)).thenReturn(true);
        when(groupAccessService.canAccessGroup(author, "retail")).thenReturn(true);
        when(groupAccessService.accessibleGroupCodes(author)).thenReturn(List.of("RETAIL"));
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        when(moduleRepository.searchCatalog(any(), eq(0), eq(100)))
                .thenReturn(new CatalogQueryPage<>(List.of(module), 1, 1));

        var result = service.list("retail", author);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().moduleCode()).isEqualTo("MOD-LOAN-DISCLOSURE");
    }

    @Test
    void list_includesModulesSharedIntoGroup() {
        ContentModuleEntity sharedModule = new ContentModuleEntity(
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                "MOD-CORP-SHARED",
                "CORP",
                "Shared Into Retail",
                "desc",
                "[\"RETAIL\"]",
                "10000003"
        );
        when(groupAccessService.canBrowseContentModuleCatalog(author)).thenReturn(true);
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        when(groupAccessService.accessibleGroupCodes(author)).thenReturn(List.of("RETAIL"));
        when(moduleRepository.searchCatalog(any(), eq(0), eq(100)))
                .thenReturn(new CatalogQueryPage<>(List.of(module, sharedModule), 2, 1));

        var result = service.list("RETAIL", author);

        assertThat(result).extracting(summary -> summary.moduleCode())
                .containsExactlyInAnyOrder("MOD-LOAN-DISCLOSURE", "MOD-CORP-SHARED");
    }

    @Test
    void list_rejectsTesterCatalogBrowse() {
        assertThatThrownBy(() -> service.list("RETAIL", tester))
                .isInstanceOf(ContentModuleAccessDeniedException.class);
    }

    @Test
    void listAccessible_returnsModulesAcrossAuthorizedGroups() {
        when(groupAccessService.canBrowseContentModuleCatalog(author)).thenReturn(true);
        when(groupAccessService.accessibleGroupCodes(author)).thenReturn(List.of("RETAIL", "WHOLESALE"));
        when(moduleRepository.searchCatalog(any(), eq(0), eq(100)))
                .thenReturn(new CatalogQueryPage<>(List.of(module), 1, 1));

        var result = service.listAccessible(author);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().groupCode()).isEqualTo("RETAIL");
    }

    @Test
    void list_rejectsBlankGroupCode() {
        when(groupAccessService.canBrowseContentModuleCatalog(author)).thenReturn(true);

        assertThatThrownBy(() -> service.list(" ", author))
                .isInstanceOf(ContentModuleValidationException.class)
                .extracting(ex -> ((ContentModuleValidationException) ex).messageKey())
                .isEqualTo("api.error.contentModule.groupCodeRequired");
    }

    @Test
    void list_rejectsInaccessibleGroup() {
        when(groupAccessService.canBrowseContentModuleCatalog(author)).thenReturn(true);
        when(groupAccessService.canAccessGroup(author, "WHOLESALE")).thenReturn(false);

        assertThatThrownBy(() -> service.list("WHOLESALE", author))
                .isInstanceOf(ContentModuleAccessDeniedException.class);
    }

    @Test
    void get_returnsDetailWithVersions() {
        when(groupAccessService.canBrowseContentModuleCatalog(author)).thenReturn(true);
        when(groupAccessService.canViewContentModuleStructure(author)).thenReturn(true);
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(Optional.of(module));
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        when(versionRepository.findByModuleIdOrderBySemanticVersionDesc(MODULE_ID))
                .thenReturn(List.of(draftVersion));

        var result = service.get("MOD-LOAN-DISCLOSURE", author);

        assertThat(result.moduleCode()).isEqualTo("MOD-LOAN-DISCLOSURE");
        assertThat(result.versions()).hasSize(1);
        assertThat(result.versions().getFirst().reviewState()).isEqualTo(ContentModuleReviewState.DRAFT);
        assertThat(result.versions().getFirst().contentStructureJson()).isEqualTo("{\"blocks\":[]}");
    }

    @Test
    void get_rejectsTesterCatalogBrowse() {
        assertThatThrownBy(() -> service.get("MOD-LOAN-DISCLOSURE", tester))
                .isInstanceOf(ContentModuleAccessDeniedException.class);
    }

    @Test
    void get_omitsContentStructureWhenViewerCannotViewStructure() {
        ManagementSessionClaims catalogViewer = session("10000005", List.of("TEMPLATE_APPROVER"), List.of("RETAIL"));
        when(groupAccessService.canBrowseContentModuleCatalog(catalogViewer)).thenReturn(true);
        when(groupAccessService.canViewContentModuleStructure(catalogViewer)).thenReturn(false);
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(Optional.of(module));
        when(groupAccessService.canAccessGroup(catalogViewer, "RETAIL")).thenReturn(true);
        when(versionRepository.findByModuleIdOrderBySemanticVersionDesc(MODULE_ID))
                .thenReturn(List.of(draftVersion));

        var result = service.get("MOD-LOAN-DISCLOSURE", catalogViewer);

        assertThat(result.versions()).hasSize(1);
        assertThat(result.versions().getFirst().contentStructureJson()).isNull();
    }

    @Test
    void create_persistsModuleAndInitialVersion() {
        when(groupAccessService.canAuthorContentModules(author)).thenReturn(true);
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        when(moduleRepository.existsByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE")).thenReturn(false);
        when(moduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(versionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(versionRepository.findByModuleIdOrderBySemanticVersionDesc(any())).thenReturn(List.of());

        var result = service.create(
                new CreateContentModuleRequest(
                        "mod-loan-disclosure",
                        "retail",
                        "Loan Disclosure",
                        "desc",
                        List.of(),
                        "1.0.0",
                        "{\"blocks\":[]}",
                        "Initial draft"
                ),
                author
        );

        assertThat(result.moduleCode()).isEqualTo("MOD-LOAN-DISCLOSURE");
        verify(auditRecorder).recordContentModuleCreated(any(), any(), any(), any(), any());
    }

    @Test
    void create_rejectsDuplicateModuleCode() {
        when(groupAccessService.canAuthorContentModules(author)).thenReturn(true);
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        when(moduleRepository.existsByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new CreateContentModuleRequest(
                        "MOD-LOAN-DISCLOSURE",
                        "RETAIL",
                        "Loan Disclosure",
                        null,
                        null,
                        "1.0.0",
                        "{\"blocks\":[]}",
                        null
                ),
                author
        ))
                .isInstanceOf(ContentModuleValidationException.class)
                .extracting(ex -> ((ContentModuleValidationException) ex).messageKey())
                .isEqualTo("api.error.contentModule.moduleCodeExists");
    }

    @Test
    void create_rejectsBlankContentStructure() {
        when(groupAccessService.canAuthorContentModules(author)).thenReturn(true);
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        when(moduleRepository.existsByModuleCodeAndDeletedAtIsNull("MOD-NEW")).thenReturn(false);

        assertThatThrownBy(() -> service.create(
                new CreateContentModuleRequest(
                        "MOD-NEW",
                        "RETAIL",
                        "New Module",
                        null,
                        null,
                        "1.0.0",
                        " ",
                        null
                ),
                author
        ))
                .isInstanceOf(ContentModuleValidationException.class)
                .extracting(ex -> ((ContentModuleValidationException) ex).messageKey())
                .isEqualTo("api.error.contentModule.contentStructureRequired");

        verify(moduleRepository, never()).save(any());
    }

    @Test
    void create_rejectsNonAuthor() {
        assertThatThrownBy(() -> service.create(
                new CreateContentModuleRequest(
                        "MOD-DENIED",
                        "RETAIL",
                        "Denied",
                        null,
                        null,
                        "1.0.0",
                        "{}",
                        null
                ),
                tester
        ))
                .isInstanceOf(ContentModuleAccessDeniedException.class);
    }

    @Test
    void createVersion_addsNewDraftVersion() {
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(Optional.of(module));
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        when(groupAccessService.canAuthorContentModules(author)).thenReturn(true);
        when(versionRepository.existsByModuleIdAndSemanticVersion(MODULE_ID, "2.0.0")).thenReturn(false);
        when(versionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(moduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(versionRepository.findByModuleIdOrderBySemanticVersionDesc(MODULE_ID))
                .thenReturn(List.of(draftVersion));

        var result = service.createVersion(
                "MOD-LOAN-DISCLOSURE",
                new CreateContentModuleVersionRequest("2.0.0", "{\"blocks\":[]}", "next draft"),
                author
        );

        assertThat(result.versions()).isNotEmpty();
        verify(auditRecorder).recordContentModuleVersionCreated(any(), any(), any(), any(), any(), any());
    }

    @Test
    void create_rejectsInaccessibleGroup() {
        when(groupAccessService.canAuthorContentModules(author)).thenReturn(true);
        when(groupAccessService.canAccessGroup(author, "WHOLESALE")).thenReturn(false);

        assertThatThrownBy(() -> service.create(
                new CreateContentModuleRequest(
                        "MOD-NEW",
                        "WHOLESALE",
                        "New Module",
                        null,
                        null,
                        "1.0.0",
                        "{\"blocks\":[]}",
                        null
                ),
                author
        ))
                .isInstanceOf(ContentModuleAccessDeniedException.class);
    }

    @Test
    void createVersion_rejectsBlankContentStructure() {
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(Optional.of(module));
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        when(groupAccessService.canAuthorContentModules(author)).thenReturn(true);
        when(versionRepository.existsByModuleIdAndSemanticVersion(MODULE_ID, "2.0.0")).thenReturn(false);

        assertThatThrownBy(() -> service.createVersion(
                "MOD-LOAN-DISCLOSURE",
                new CreateContentModuleVersionRequest("2.0.0", " ", null),
                author
        ))
                .isInstanceOf(ContentModuleValidationException.class)
                .extracting(ex -> ((ContentModuleValidationException) ex).messageKey())
                .isEqualTo("api.error.contentModule.contentStructureRequired");
    }

    @Test
    void createVersion_rejectsDuplicateSemanticVersion() {
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(Optional.of(module));
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        when(groupAccessService.canAuthorContentModules(author)).thenReturn(true);
        when(versionRepository.existsByModuleIdAndSemanticVersion(MODULE_ID, "1.0.0")).thenReturn(true);

        assertThatThrownBy(() -> service.createVersion(
                "MOD-LOAN-DISCLOSURE",
                new CreateContentModuleVersionRequest("1.0.0", "{\"blocks\":[]}", null),
                author
        ))
                .isInstanceOf(ContentModuleValidationException.class)
                .extracting(ex -> ((ContentModuleValidationException) ex).messageKey())
                .isEqualTo("api.error.contentModule.versionExists");
    }

    @Test
    void updateDraftVersion_updatesContentAndDescription() {
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(Optional.of(module));
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        when(groupAccessService.canAuthorContentModules(author)).thenReturn(true);
        when(versionRepository.findByModuleIdAndSemanticVersion(MODULE_ID, "1.0.0"))
                .thenReturn(Optional.of(draftVersion));
        when(versionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(moduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(versionRepository.findByModuleIdOrderBySemanticVersionDesc(MODULE_ID))
                .thenReturn(List.of(draftVersion));

        var result = service.updateDraftVersion(
                "MOD-LOAN-DISCLOSURE",
                "1.0.0",
                new UpdateContentModuleVersionRequest("{\"blocks\":[{\"type\":\"paragraph\"}]}", "Updated"),
                author
        );

        assertThat(draftVersion.getChangeDescription()).isEqualTo("Updated");
        assertThat(result.versions().getFirst().changeDescription()).isEqualTo("Updated");
        verify(auditRecorder).recordContentModuleVersionUpdated(any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateDraftVersion_rejectsNonDraftVersion() {
        draftVersion.setReviewState(ContentModuleReviewState.SUBMITTED);
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(Optional.of(module));
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        when(groupAccessService.canAuthorContentModules(author)).thenReturn(true);
        when(versionRepository.findByModuleIdAndSemanticVersion(MODULE_ID, "1.0.0"))
                .thenReturn(Optional.of(draftVersion));

        assertThatThrownBy(() -> service.updateDraftVersion(
                "MOD-LOAN-DISCLOSURE",
                "1.0.0",
                new UpdateContentModuleVersionRequest("{\"blocks\":[]}", null),
                author
        ))
                .isInstanceOf(ContentModuleValidationException.class)
                .extracting(ex -> ((ContentModuleValidationException) ex).messageKey())
                .isEqualTo("api.error.contentModule.draftOnlyEditable");
    }

    @Test
    void updateDraftVersion_rejectsMissingVersion() {
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(Optional.of(module));
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        when(groupAccessService.canAuthorContentModules(author)).thenReturn(true);
        when(versionRepository.findByModuleIdAndSemanticVersion(MODULE_ID, "9.9.9"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateDraftVersion(
                "MOD-LOAN-DISCLOSURE",
                "9.9.9",
                new UpdateContentModuleVersionRequest("{\"blocks\":[]}", null),
                author
        ))
                .isInstanceOf(ContentModuleNotFoundException.class);
    }

    @Test
    void create_normalizesSharedGroupCodes() {
        when(groupAccessService.canAuthorContentModules(author)).thenReturn(true);
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        when(moduleRepository.existsByModuleCodeAndDeletedAtIsNull("MOD-SHARED")).thenReturn(false);
        when(moduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(versionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(versionRepository.findByModuleIdOrderBySemanticVersionDesc(any())).thenReturn(List.of());

        service.create(
                new CreateContentModuleRequest(
                        "MOD-SHARED",
                        "RETAIL",
                        "Shared Module",
                        null,
                        List.of(" wholesale ", "WHOLESALE"),
                        "1.0.0",
                        "{\"blocks\":[]}",
                        null
                ),
                author
        );

        ArgumentCaptor<ContentModuleEntity> captor = ArgumentCaptor.forClass(ContentModuleEntity.class);
        verify(moduleRepository).save(captor.capture());
        assertThat(captor.getValue().getSharedGroupCodesJson()).isEqualTo("[\"WHOLESALE\"]");
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
