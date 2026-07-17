package com.bank.docgen.contentmodule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.api.CatalogQueryPage;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.api.CreateContentModuleVersionRequest;
import com.bank.docgen.contentmodule.api.UpdateContentModuleVersionRequest;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewState;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepositoryCustom.ContentModuleCatalogFilter;
import com.bank.docgen.contentmodule.persistence.ContentModuleReviewRecordRepository;
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

/**
 * CE-K08 BDD-CE-K08-LM-001…007, 014 — legal metadata write/read, validation, catalog filters.
 */
@ExtendWith(MockitoExtension.class)
class ContentModuleLegalMetadataServiceTest {

    private static final UUID MODULE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID VERSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant FROM = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-12-31T23:59:59Z");

    @Mock
    private ContentModuleRepository moduleRepository;
    @Mock
    private ContentModuleVersionRepository versionRepository;
    @Mock
    private ContentModuleReviewRecordRepository reviewRecordRepository;
    @Mock
    private GroupAccessService groupAccessService;
    @Mock
    private ManagementAuditRecorder auditRecorder;

    private ContentModuleService service;
    private ContentModuleEntity module;
    private ContentModuleVersionEntity draftVersion;
    private ManagementSessionClaims author;
    private ManagementSessionClaims tester;

    @BeforeEach
    void setUp() {
        ContentModuleAccessService accessSupport =
                new ContentModuleAccessService(moduleRepository, groupAccessService, new ObjectMapper());
        service = new ContentModuleService(
                moduleRepository,
                versionRepository,
                reviewRecordRepository,
                groupAccessService,
                accessSupport,
                auditRecorder,
                org.mockito.Mockito.mock(com.bank.docgen.contentmodule.service.ContentModuleFullTextIndexWriter.class)
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
        author = new ManagementSessionClaims(
                "10000003",
                "Author",
                "author@example.com",
                AuthSource.LOCAL,
                List.of("TEMPLATE_AUTHOR"),
                List.of("RETAIL"),
                "route.home",
                List.of("route.home"),
                Instant.now().plusSeconds(3600)
        );
        tester = new ManagementSessionClaims(
                "10000006",
                "Tester",
                "tester@example.com",
                AuthSource.LOCAL,
                List.of("TEMPLATE_TESTER"),
                List.of("RETAIL"),
                "route.home",
                List.of("route.home"),
                Instant.now().plusSeconds(3600)
        );
        lenient().when(groupAccessService.canBrowseContentModuleCatalog(author)).thenReturn(true);
        lenient().when(groupAccessService.canViewContentModuleStructure(author)).thenReturn(true);
        lenient().when(groupAccessService.canBrowseContentModuleCatalog(tester)).thenReturn(false);
        lenient().when(reviewRecordRepository.findByModuleIdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        lenient().when(versionRepository.findByModuleIdIn(any())).thenReturn(List.of(draftVersion));
    }

    @Test
    void updateDraftVersion_persistsLegalMetadata_lm001() {
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
                new UpdateContentModuleVersionRequest(
                        "{\"blocks\":[]}",
                        "Legal meta",
                        "England and Wales",
                        FROM,
                        TO,
                        "LR-2026-001"
                ),
                author
        );

        assertThat(draftVersion.getJurisdiction()).isEqualTo("England and Wales");
        assertThat(draftVersion.getEffectiveFrom()).isEqualTo(FROM);
        assertThat(draftVersion.getEffectiveTo()).isEqualTo(TO);
        assertThat(draftVersion.getLegalReviewRef()).isEqualTo("LR-2026-001");
        assertThat(result.versions().getFirst().jurisdiction()).isEqualTo("England and Wales");
        assertThat(result.versions().getFirst().legalReviewRef()).isEqualTo("LR-2026-001");
    }

    @Test
    void createVersion_omitsLegalMetadata_lm002() {
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(Optional.of(module));
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        when(groupAccessService.canAuthorContentModules(author)).thenReturn(true);
        when(versionRepository.existsByModuleIdAndSemanticVersion(MODULE_ID, "2.0.0")).thenReturn(false);
        ArgumentCaptor<ContentModuleVersionEntity> versionCaptor =
                ArgumentCaptor.forClass(ContentModuleVersionEntity.class);
        when(versionRepository.save(versionCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        when(moduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(versionRepository.findByModuleIdOrderBySemanticVersionDesc(MODULE_ID))
                .thenReturn(List.of(draftVersion));

        service.createVersion(
                "MOD-LOAN-DISCLOSURE",
                new CreateContentModuleVersionRequest("2.0.0", "{\"blocks\":[]}", "next"),
                author
        );

        ContentModuleVersionEntity saved = versionCaptor.getValue();
        assertThat(saved.getJurisdiction()).isNull();
        assertThat(saved.getEffectiveFrom()).isNull();
        assertThat(saved.getEffectiveTo()).isNull();
        assertThat(saved.getLegalReviewRef()).isNull();
    }

    @Test
    void updateDraftVersion_rejectsInvalidEffectiveRange_lm003() {
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(Optional.of(module));
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        when(groupAccessService.canAuthorContentModules(author)).thenReturn(true);
        when(versionRepository.findByModuleIdAndSemanticVersion(MODULE_ID, "1.0.0"))
                .thenReturn(Optional.of(draftVersion));

        assertThatThrownBy(() -> service.updateDraftVersion(
                "MOD-LOAN-DISCLOSURE",
                "1.0.0",
                new UpdateContentModuleVersionRequest(
                        "{\"blocks\":[]}",
                        null,
                        null,
                        TO,
                        FROM,
                        null
                ),
                author
        ))
                .isInstanceOf(ContentModuleValidationException.class)
                .extracting(ex -> ((ContentModuleValidationException) ex).messageKey())
                .isEqualTo("api.error.contentModule.invalidEffectiveRange");
        assertThat(draftVersion.getEffectiveFrom()).isNull();
        assertThat(draftVersion.getEffectiveTo()).isNull();
    }

    @Test
    void updateDraftVersion_rejectsLegalMetadataOnNonDraft_lm004() {
        draftVersion.setReviewState(ContentModuleReviewState.APPROVED);
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(Optional.of(module));
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        when(groupAccessService.canAuthorContentModules(author)).thenReturn(true);
        when(versionRepository.findByModuleIdAndSemanticVersion(MODULE_ID, "1.0.0"))
                .thenReturn(Optional.of(draftVersion));

        assertThatThrownBy(() -> service.updateDraftVersion(
                "MOD-LOAN-DISCLOSURE",
                "1.0.0",
                new UpdateContentModuleVersionRequest(
                        "{\"blocks\":[]}",
                        null,
                        "Hong Kong",
                        FROM,
                        TO,
                        "LR-X"
                ),
                author
        ))
                .isInstanceOf(ContentModuleValidationException.class)
                .extracting(ex -> ((ContentModuleValidationException) ex).messageKey())
                .isEqualTo("api.error.contentModule.draftOnlyEditable");
        assertThat(draftVersion.getJurisdiction()).isNull();
    }

    @Test
    void list_passesJurisdictionFilter_lm005() {
        when(groupAccessService.accessibleGroupCodes(author)).thenReturn(List.of("RETAIL"));
        when(moduleRepository.searchCatalog(any(), eq(0), eq(20)))
                .thenReturn(new CatalogQueryPage<>(List.of(module), 1, 1));

        service.list(author, 0, 20, null, null, null, "England and Wales", null, null, null);

        ArgumentCaptor<ContentModuleCatalogFilter> captor =
                ArgumentCaptor.forClass(ContentModuleCatalogFilter.class);
        verify(moduleRepository).searchCatalog(captor.capture(), eq(0), eq(20));
        assertThat(captor.getValue().jurisdiction()).isEqualTo("England and Wales");
    }

    @Test
    void list_passesLegalReviewRefFilter_lm006() {
        when(groupAccessService.accessibleGroupCodes(author)).thenReturn(List.of("RETAIL"));
        when(moduleRepository.searchCatalog(any(), eq(0), eq(20)))
                .thenReturn(new CatalogQueryPage<>(List.of(module), 1, 1));

        service.list(author, 0, 20, null, null, null, null, "LR-2026-001", null, null);

        ArgumentCaptor<ContentModuleCatalogFilter> captor =
                ArgumentCaptor.forClass(ContentModuleCatalogFilter.class);
        verify(moduleRepository).searchCatalog(captor.capture(), eq(0), eq(20));
        assertThat(captor.getValue().legalReviewRef()).isEqualTo("LR-2026-001");
    }

    @Test
    void list_passesEffectiveRangeFilters_lm007() {
        when(groupAccessService.accessibleGroupCodes(author)).thenReturn(List.of("RETAIL"));
        when(moduleRepository.searchCatalog(any(), eq(0), eq(20)))
                .thenReturn(new CatalogQueryPage<>(List.of(module), 1, 1));

        service.list(author, 0, 20, null, null, null, null, null, FROM, TO);

        ArgumentCaptor<ContentModuleCatalogFilter> captor =
                ArgumentCaptor.forClass(ContentModuleCatalogFilter.class);
        verify(moduleRepository).searchCatalog(captor.capture(), eq(0), eq(20));
        assertThat(captor.getValue().effectiveFrom()).isEqualTo(FROM);
        assertThat(captor.getValue().effectiveTo()).isEqualTo(TO);
    }

    @Test
    void list_rejectsTesterCatalogBrowse_lm014() {
        assertThatThrownBy(() -> service.list(
                tester, 0, 20, null, null, null, "England and Wales", null, null, null))
                .isInstanceOf(ContentModuleAccessDeniedException.class);
    }

    @Test
    void normalizeText_blankBecomesNull() {
        assertThat(ContentModuleLegalMetadataSupport.normalizeText("  ")).isNull();
        assertThat(ContentModuleLegalMetadataSupport.normalizeText(" HK ")).isEqualTo("HK");
    }

    @Test
    void equalEffectiveRange_isAllowed() {
        ContentModuleLegalMetadataSupport.validateEffectiveRange(FROM, FROM);
    }
}
