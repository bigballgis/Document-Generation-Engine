package com.bank.docgen.contentmodule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.api.CatalogQueryPage;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.api.ContentModuleSummaryView;
import com.bank.docgen.contentmodule.domain.ContentModuleCatalogDisplayStatus;
import com.bank.docgen.contentmodule.domain.ContentModuleLifecycleState;
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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentModuleCatalogStatusCeU20Test {

    private static final UUID MODULE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID VERSION_OLDER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VERSION_HEAD = UUID.fromString("22222222-2222-2222-2222-222222222222");

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
    private ManagementSessionClaims author;

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
                org.mockito.Mockito.mock(com.bank.docgen.contentmodule.service.ContentModuleFullTextIndexWriter.class),
                org.mockito.Mockito.mock(com.bank.docgen.contentmodule.service.ContentModuleNestingService.class)
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
        author = new ManagementSessionClaims(
                "10000003",
                "Author",
                "author@example.com",
                AuthSource.LOCAL,
                List.of("DOCUMENT_AUTHOR"),
                List.of("RETAIL"),
                "route.home",
                List.of("route.home"),
                Instant.now().plusSeconds(3600)
        );
        when(groupAccessService.canBrowseContentModuleCatalog(author)).thenReturn(true);
        when(groupAccessService.accessibleGroupCodes(author)).thenReturn(List.of("RETAIL"));
    }

    @Test
    void list_projectsHeadReviewAndLifecycleState_ceU20() {
        ContentModuleVersionEntity older = version(
                VERSION_OLDER, "1.0.0", ContentModuleReviewState.APPROVED, ContentModuleLifecycleState.ACTIVE);
        ContentModuleVersionEntity head = version(
                VERSION_HEAD, "1.1.0", ContentModuleReviewState.DRAFT, null);
        // Same updatedAt → semanticVersion tie-break picks 1.1.0 when we force equal timestamps via reflection-free path:
        // create older first then touch head so head has later updatedAt.
        older.setChangeDescription("older");
        head.setChangeDescription("head");

        when(moduleRepository.searchCatalog(any(), eq(0), eq(20)))
                .thenReturn(new CatalogQueryPage<>(List.of(module), 1, 1));
        when(versionRepository.findByModuleIdIn(any())).thenReturn(List.of(older, head));

        PageView<ContentModuleSummaryView> page = service.list(
                author, 0, 20, null, null, null, null, null, null, null, null);

        ContentModuleSummaryView summary = page.content().getFirst();
        assertThat(summary.reviewState()).isEqualTo("DRAFT");
        assertThat(summary.lifecycleState()).isNull();
    }

    @Test
    void list_headTieBreakUsesGreaterSemanticVersion_ceU20() {
        Instant sameTs = Instant.parse("2026-07-01T12:00:00Z");
        ContentModuleVersionEntity v100 = version(
                VERSION_OLDER, "1.0.0", ContentModuleReviewState.SUBMITTED, null);
        ContentModuleVersionEntity v110 = version(
                VERSION_HEAD, "1.1.0", ContentModuleReviewState.APPROVED, ContentModuleLifecycleState.ACTIVE);
        forceUpdatedAt(v100, sameTs);
        forceUpdatedAt(v110, sameTs);

        when(moduleRepository.searchCatalog(any(), eq(0), eq(20)))
                .thenReturn(new CatalogQueryPage<>(List.of(module), 1, 1));
        when(versionRepository.findByModuleIdIn(any())).thenReturn(List.of(v100, v110));

        PageView<ContentModuleSummaryView> page = service.list(
                author, 0, 20, null, null, null, null, null, null, null, null);

        assertThat(page.content().getFirst().reviewState()).isEqualTo("APPROVED");
        assertThat(page.content().getFirst().lifecycleState()).isEqualTo("ACTIVE");
    }

    @Test
    void list_passesStatusFilterToRepository_ceU20() {
        when(moduleRepository.searchCatalog(any(), eq(0), eq(20)))
                .thenReturn(new CatalogQueryPage<>(List.of(), 0, 0));

        service.list(author, 0, 20, null, null, null, null, null, null, null, "DRAFT");

        ArgumentCaptor<ContentModuleCatalogFilter> captor =
                ArgumentCaptor.forClass(ContentModuleCatalogFilter.class);
        verify(moduleRepository).searchCatalog(captor.capture(), eq(0), eq(20));
        assertThat(captor.getValue().status()).isEqualTo(ContentModuleCatalogDisplayStatus.DRAFT);
    }

    @Test
    void list_unknownStatus_returnsEmptyPageWithoutRepositorySearch_ceU20() {
        PageView<ContentModuleSummaryView> page = service.list(
                author, 0, 20, null, null, null, null, null, null, null, "NOT_A_REAL_STATUS");

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        verify(moduleRepository, org.mockito.Mockito.never()).searchCatalog(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void list_blankStatus_omitsStatusFilter_ceU20() {
        when(moduleRepository.searchCatalog(any(), eq(0), eq(20)))
                .thenReturn(new CatalogQueryPage<>(List.of(), 0, 0));

        service.list(author, 0, 20, null, null, null, null, null, null, null, "  ");

        ArgumentCaptor<ContentModuleCatalogFilter> captor =
                ArgumentCaptor.forClass(ContentModuleCatalogFilter.class);
        verify(moduleRepository).searchCatalog(captor.capture(), eq(0), eq(20));
        assertThat(captor.getValue().status()).isNull();
    }

    private static ContentModuleVersionEntity version(
            UUID id,
            String semver,
            ContentModuleReviewState reviewState,
            ContentModuleLifecycleState lifecycleState
    ) {
        ContentModuleVersionEntity entity = new ContentModuleVersionEntity(
                id, MODULE_ID, semver, "{}", "change", "10000003");
        entity.setReviewState(reviewState);
        if (lifecycleState != null) {
            entity.setLifecycleState(lifecycleState);
        }
        return entity;
    }

    private static void forceUpdatedAt(ContentModuleVersionEntity entity, Instant updatedAt) {
        try {
            var field = ContentModuleVersionEntity.class.getDeclaredField("updatedAt");
            field.setAccessible(true);
            field.set(entity, updatedAt);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
