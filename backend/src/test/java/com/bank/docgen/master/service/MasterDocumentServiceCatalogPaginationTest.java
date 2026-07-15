package com.bank.docgen.master.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.api.CatalogQueryPage;
import com.bank.docgen.authorization.management.api.CatalogSortKey;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.api.MasterDocumentSummaryView;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterAnchorRepository;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterDocumentRepositoryCustom.MasterCatalogFilter;
import com.bank.docgen.master.persistence.MasterReviewRecordRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.master.rendering.DocxAnchorExtractor;
import com.bank.docgen.sharedkernel.lifecycle.SelfApprovalGuard;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
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

@ExtendWith(MockitoExtension.class)
class MasterDocumentServiceCatalogPaginationTest {

    @Mock
    private MasterDocumentRepository masterDocumentRepository;
    @Mock
    private MasterAnchorRepository masterAnchorRepository;
    @Mock
    private MasterReviewRecordRepository masterReviewRecordRepository;
    @Mock
    private MasterRevisionLineRepository masterRevisionLineRepository;
    @Mock
    private ObjectStoragePort objectStoragePort;
    @Mock
    private DocxAnchorExtractor docxAnchorExtractor;
    @Mock
    private GroupAccessService groupAccessService;
    @Mock
    private ManagementUserDisplayService managementUserDisplayService;

    @Mock
    private MasterImpactAnalysisService impactAnalysisService;

    private MasterDocumentService service;
    private ManagementSessionClaims session;

    @BeforeEach
    void setUp() {
        service = new MasterDocumentService(
                masterDocumentRepository,
                masterAnchorRepository,
                masterReviewRecordRepository,
                masterRevisionLineRepository,
                objectStoragePort,
                docxAnchorExtractor,
                groupAccessService,
                managementUserDisplayService,
                new SelfApprovalGuard(),
                new com.fasterxml.jackson.databind.ObjectMapper(),
                impactAnalysisService,
                50L * 1024L * 1024L
        );
        session = new ManagementSessionClaims(
                "10000001",
                "Admin",
                "admin@example.com",
                AuthSource.LOCAL,
                List.of("GLOBAL_ADMIN"),
                List.of("*"),
                "route.home",
                List.of("route.home"),
                Instant.now().plusSeconds(3600)
        );
    }

    @Test
    void list_returnsPageViewWithFiltersAndDefaultSort() {
        UUID masterId = UUID.randomUUID();
        MasterDocumentEntity master = new MasterDocumentEntity(
                masterId, "RETAIL", "Loan Master", "desc", "storage/key", "master.docx", "10000004");
        master.setStatus(MasterDocumentStatus.APPROVED);
        when(groupAccessService.accessibleGroupCodes(session)).thenReturn(List.of("*"));
        when(groupAccessService.canAccessGroup(session, "RETAIL")).thenReturn(true);
        when(masterDocumentRepository.searchCatalog(any(), eq(0), eq(20)))
                .thenReturn(new CatalogQueryPage<>(List.of(master), 1, 1));
        when(masterAnchorRepository.countByMasterIdIn(any())).thenReturn(List.<Object[]>of(new Object[]{masterId, 2L}));
        when(managementUserDisplayService.lookupDisplayNames(Set.of("10000004")))
                .thenReturn(Map.of("10000004", "Master Designer (10000004)"));

        PageView<MasterDocumentSummaryView> page = service.list(
                session, 0, 20, "loan", "RETAIL", "APPROVED", "groupAsc");

        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content().getFirst().updatedByDisplayName()).isEqualTo("Master Designer (10000004)");

        ArgumentCaptor<MasterCatalogFilter> filterCaptor = ArgumentCaptor.forClass(MasterCatalogFilter.class);
        verify(masterDocumentRepository).searchCatalog(filterCaptor.capture(), eq(0), eq(20));
        assertThat(filterCaptor.getValue().groupCodeExact()).isEqualTo("RETAIL");
        assertThat(filterCaptor.getValue().status()).isEqualTo(MasterDocumentStatus.APPROVED);
        assertThat(filterCaptor.getValue().search()).isEqualTo("loan");
        assertThat(filterCaptor.getValue().sort()).isEqualTo(CatalogSortKey.GROUP_CODE_ASC);
    }

    @Test
    void list_unknownStatus_returnsEmptyPage() {
        when(groupAccessService.accessibleGroupCodes(session)).thenReturn(List.of("*"));

        PageView<MasterDocumentSummaryView> page = service.list(
                session, 0, 20, null, null, "NOPE", null);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }
}
