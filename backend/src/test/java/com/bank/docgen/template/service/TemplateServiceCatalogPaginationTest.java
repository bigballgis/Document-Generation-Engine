package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.api.CatalogQueryPage;
import com.bank.docgen.authorization.management.api.CatalogSortKey;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.TemplateSummaryView;
import com.bank.docgen.template.domain.ApprovalSubState;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.mapping.TemplateViewMapper;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateRepositoryCustom.TemplateCatalogFilter;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class TemplateServiceCatalogPaginationTest {

    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private MasterDocumentRepository masterDocumentRepository;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private GroupAccessService groupAccessService;
    @Mock
    private TemplateStructuredAuthoringService structuredAuthoringService;
    @Mock
    private TemplateBindingConfigurationService bindingConfigurationService;
    @Mock
    private TemplateViewMapper templateViewMapper;
    @Mock
    private TemplateCurrentVersionResolver templateCurrentVersionResolver;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ManagementUserDisplayService managementUserDisplayService;

    private TemplateService service;
    private ManagementSessionClaims globalAdmin;
    private ManagementSessionClaims retailAuthor;

    @BeforeEach
    void setUp() {
        service = new TemplateService(
                templateRepository,
                templateVersionRepository,
                masterDocumentRepository,
                apiPolicyRepository,
                groupAccessService,
                structuredAuthoringService,
                bindingConfigurationService,
                templateViewMapper,
                templateCurrentVersionResolver,
                eventPublisher,
                managementUserDisplayService
        );
        globalAdmin = session("10000001", List.of("GLOBAL_ADMIN"), List.of("*"));
        retailAuthor = session("10000003", List.of("TEMPLATE_AUTHOR"), List.of("RETAIL"));
    }

    @Test
    void list_normalizesPageSizeAndDefaultsGroupFirstSort() {
        when(groupAccessService.accessibleGroupCodes(globalAdmin)).thenReturn(List.of("*"));
        when(templateRepository.searchCatalog(any(), eq(0), eq(100)))
                .thenReturn(new CatalogQueryPage<>(List.of(), 0, 0));

        PageView<TemplateSummaryView> page = service.list(
                globalAdmin, -1, 250, null, null, null, null, "bogusSort");

        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(100);
        ArgumentCaptor<TemplateCatalogFilter> filterCaptor = ArgumentCaptor.forClass(TemplateCatalogFilter.class);
        verify(templateRepository).searchCatalog(filterCaptor.capture(), eq(0), eq(100));
        assertThat(filterCaptor.getValue().sort()).isEqualTo(CatalogSortKey.GROUP_CODE_ASC);
        assertThat(filterCaptor.getValue().allGroups()).isTrue();
    }

    @Test
    void list_appliesSearchLifecycleAndApprovalFilters() {
        when(groupAccessService.accessibleGroupCodes(globalAdmin)).thenReturn(List.of("*"));
        when(templateRepository.searchCatalog(any(), anyInt(), anyInt()))
                .thenReturn(new CatalogQueryPage<>(List.of(), 0, 0));

        service.list(globalAdmin, 0, 20, "loan", null, "APPROVAL", "PENDING_DECISION", "nameAsc");

        ArgumentCaptor<TemplateCatalogFilter> filterCaptor = ArgumentCaptor.forClass(TemplateCatalogFilter.class);
        verify(templateRepository).searchCatalog(filterCaptor.capture(), eq(0), eq(20));
        TemplateCatalogFilter filter = filterCaptor.getValue();
        assertThat(filter.search()).isEqualTo("loan");
        assertThat(filter.lifecycleStatus()).isEqualTo(TemplateLifecycleStatus.APPROVAL);
        assertThat(filter.approvalSubState()).isEqualTo(ApprovalSubState.PENDING_DECISION);
        assertThat(filter.sort()).isEqualTo(CatalogSortKey.NAME_ASC);
    }

    @Test
    void list_unknownLifecycleStatus_returnsEmptyPageWithoutQuery() {
        when(groupAccessService.accessibleGroupCodes(globalAdmin)).thenReturn(List.of("*"));

        PageView<TemplateSummaryView> page = service.list(
                globalAdmin, 0, 20, null, null, "NOT_A_STATUS", null, null);

        assertThat(page.totalElements()).isZero();
        assertThat(page.content()).isEmpty();
    }

    @Test
    void list_unauthorizedGroupFilter_returnsEmptyPage() {
        when(groupAccessService.accessibleGroupCodes(retailAuthor)).thenReturn(List.of("RETAIL"));
        when(groupAccessService.canAccessGroup(retailAuthor, "CORP")).thenReturn(false);

        PageView<TemplateSummaryView> page = service.list(
                retailAuthor, 0, 20, null, "CORP", null, null, null);

        assertThat(page.totalElements()).isZero();
        assertThat(page.content()).isEmpty();
    }

    @Test
    void list_emptyAccessibleGroups_returnsEmptyPage() {
        when(groupAccessService.accessibleGroupCodes(retailAuthor)).thenReturn(List.of());

        PageView<TemplateSummaryView> page = service.list(retailAuthor, 0, 20);

        assertThat(page.totalElements()).isZero();
        assertThat(page.content()).isEmpty();
    }

    @Test
    void list_mapsRepositoryPageToView() {
        UUID templateId = UUID.randomUUID();
        TemplateEntity entity = new TemplateEntity(
                templateId, "TPL-001", "RETAIL", "Loan", null, UUID.randomUUID(), "10000003");
        TemplateSummaryView summary = new TemplateSummaryView(
                templateId.toString(),
                "TPL-001",
                "RETAIL",
                "Loan",
                TemplateLifecycleStatus.DRAFT,
                ApprovalSubState.PENDING_SUBMIT,
                null,
                0,
                UUID.randomUUID().toString(),
                "10000003",
                Instant.parse("2026-01-01T00:00:00Z"),
                null
        );
        when(groupAccessService.accessibleGroupCodes(globalAdmin)).thenReturn(List.of("*"));
        when(templateRepository.searchCatalog(any(), eq(1), eq(20)))
                .thenReturn(new CatalogQueryPage<>(List.of(entity), 45, 3));
        when(templateViewMapper.toSummary(entity)).thenReturn(summary);
        when(managementUserDisplayService.lookupDisplayNames(Set.of("10000003")))
                .thenReturn(Map.of("10000003", "Author"));

        PageView<TemplateSummaryView> page = service.list(globalAdmin, 1, 20);

        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.totalElements()).isEqualTo(45);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().updatedByDisplayName()).isEqualTo("Author");
    }

    private static ManagementSessionClaims session(String username, List<String> roles, List<String> groups) {
        return new ManagementSessionClaims(
                username,
                username,
                username + "@example.com",
                AuthSource.LOCAL,
                roles,
                groups,
                "route.home",
                List.of("route.home"),
                Instant.now().plusSeconds(3600)
        );
    }
}
