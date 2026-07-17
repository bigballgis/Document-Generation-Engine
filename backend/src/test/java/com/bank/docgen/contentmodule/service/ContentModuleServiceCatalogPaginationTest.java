package com.bank.docgen.contentmodule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.api.CatalogQueryPage;
import com.bank.docgen.authorization.management.api.CatalogSortKey;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.api.ContentModuleSummaryView;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContentModuleServiceCatalogPaginationTest {

    private static final UUID MODULE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

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
        when(groupAccessService.canBrowseContentModuleCatalog(author)).thenReturn(true);
        when(versionRepository.findByModuleIdIn(any())).thenReturn(List.of(
                new ContentModuleVersionEntity(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        MODULE_ID,
                        "1.0.0",
                        "{}",
                        "Initial",
                        "10000003"
                )
        ));
    }

    @Test
    void list_returnsPageViewWithSearchAndGroupFilter() {
        when(groupAccessService.accessibleGroupCodes(author)).thenReturn(List.of("RETAIL"));
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        when(moduleRepository.searchCatalog(any(), eq(0), eq(20)))
                .thenReturn(new CatalogQueryPage<>(List.of(module), 1, 1));

        PageView<ContentModuleSummaryView> page = service.list(
                author, 0, 20, "loan", "retail", "moduleCodeAsc");

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().moduleCode()).isEqualTo("MOD-LOAN-DISCLOSURE");
        assertThat(page.content().getFirst().reviewState()).isEqualTo("DRAFT");
        assertThat(page.totalElements()).isEqualTo(1);

        ArgumentCaptor<ContentModuleCatalogFilter> filterCaptor =
                ArgumentCaptor.forClass(ContentModuleCatalogFilter.class);
        verify(moduleRepository).searchCatalog(filterCaptor.capture(), eq(0), eq(20));
        assertThat(filterCaptor.getValue().groupCodeExact()).isEqualTo("RETAIL");
        assertThat(filterCaptor.getValue().search()).isEqualTo("loan");
        assertThat(filterCaptor.getValue().sort()).isEqualTo(CatalogSortKey.MODULE_CODE_ASC);
    }

    @Test
    void list_unauthorizedGroup_returnsEmptyPage() {
        when(groupAccessService.accessibleGroupCodes(author)).thenReturn(List.of("RETAIL"));
        when(groupAccessService.canAccessGroup(author, "WHOLESALE")).thenReturn(false);

        PageView<ContentModuleSummaryView> page = service.list(
                author, 0, 20, null, "WHOLESALE", null);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }
}
