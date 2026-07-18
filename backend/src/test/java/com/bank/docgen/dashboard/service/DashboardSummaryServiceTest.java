package com.bank.docgen.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.dashboard.api.DashboardSummaryView;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateRepository;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BDD-PRR-D01C-002 / 004 / 009 — authorized-scope bucket counts for Dashboard Overview.
 */
@ExtendWith(MockitoExtension.class)
class DashboardSummaryServiceTest {

    @Mock
    private GroupAccessService groupAccessService;
    @Mock
    private MasterDocumentRepository masterDocumentRepository;
    @Mock
    private TemplateRepository templateRepository;

    private DashboardSummaryService service;
    private ManagementSessionClaims session;

    @BeforeEach
    void setUp() {
        service = new DashboardSummaryService(
                groupAccessService, masterDocumentRepository, templateRepository);
        session = new ManagementSessionClaims(
                "10000002",
                "Group Admin",
                "ga@example.com",
                AuthSource.LOCAL,
                List.of("GROUP_ADMIN"),
                List.of("RETAIL"),
                "route.home",
                List.of("route.home"),
                Instant.now().plusSeconds(3600)
        );
    }

    @Test
    void summarize_mapsBucketsFromGroupedCounts() {
        when(groupAccessService.accessibleGroupCodes(session)).thenReturn(List.of("RETAIL"));
        Map<MasterDocumentStatus, Long> masterCounts = new EnumMap<>(MasterDocumentStatus.class);
        masterCounts.put(MasterDocumentStatus.PENDING_REVIEW, 3L);
        masterCounts.put(MasterDocumentStatus.DRAFT, 2L);
        masterCounts.put(MasterDocumentStatus.REJECTED, 1L);
        masterCounts.put(MasterDocumentStatus.APPROVED, 10L);
        when(masterDocumentRepository.countGroupedByStatus(eq(List.of("RETAIL")), eq(false)))
                .thenReturn(masterCounts);

        Map<TemplateLifecycleStatus, Long> templateCounts = new EnumMap<>(TemplateLifecycleStatus.class);
        templateCounts.put(TemplateLifecycleStatus.DRAFT, 4L);
        templateCounts.put(TemplateLifecycleStatus.TESTING, 1L);
        templateCounts.put(TemplateLifecycleStatus.APPROVAL, 2L);
        templateCounts.put(TemplateLifecycleStatus.PENDING_RELEASE, 1L);
        templateCounts.put(TemplateLifecycleStatus.PUBLISHED, 20L);
        templateCounts.put(TemplateLifecycleStatus.STOPPED, 5L);
        templateCounts.put(TemplateLifecycleStatus.DEPRECATED, 3L);
        when(templateRepository.countGroupedByLifecycleStatus(eq(List.of("RETAIL")), eq(false)))
                .thenReturn(templateCounts);

        DashboardSummaryView summary = service.summarize(session);

        assertThat(summary.masterPendingReview()).isEqualTo(3L);
        assertThat(summary.masterVersionsInProgress()).isEqualTo(3L);
        assertThat(summary.catalogMasters()).isEqualTo(16L);
        assertThat(summary.templateVersionsInWorkflow()).isEqualTo(8L);
        assertThat(summary.publishedVersions()).isEqualTo(20L);
        assertThat(summary.stoppedVersions()).isEqualTo(5L);
        assertThat(summary.catalogTemplates()).isEqualTo(36L);
        verify(masterDocumentRepository).countGroupedByStatus(List.of("RETAIL"), false);
        verify(templateRepository).countGroupedByLifecycleStatus(List.of("RETAIL"), false);
    }

    @Test
    void summarize_emptyAuthorizedGroups_returnsZerosWithoutQuerying() {
        when(groupAccessService.accessibleGroupCodes(session)).thenReturn(List.of());

        DashboardSummaryView summary = service.summarize(session);

        assertThat(summary.masterPendingReview()).isZero();
        assertThat(summary.masterVersionsInProgress()).isZero();
        assertThat(summary.catalogMasters()).isZero();
        assertThat(summary.templateVersionsInWorkflow()).isZero();
        assertThat(summary.publishedVersions()).isZero();
        assertThat(summary.stoppedVersions()).isZero();
        assertThat(summary.catalogTemplates()).isZero();
    }

    @Test
    void summarize_globalAdmin_usesAllGroupsScope() {
        ManagementSessionClaims admin = new ManagementSessionClaims(
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
        when(groupAccessService.accessibleGroupCodes(admin)).thenReturn(List.of("*"));
        when(masterDocumentRepository.countGroupedByStatus(eq(List.of()), eq(true)))
                .thenReturn(Map.of(MasterDocumentStatus.PENDING_REVIEW, 1L));
        when(templateRepository.countGroupedByLifecycleStatus(eq(List.of()), eq(true)))
                .thenReturn(Map.of(TemplateLifecycleStatus.PUBLISHED, 2L));

        DashboardSummaryView summary = service.summarize(admin);

        assertThat(summary.masterPendingReview()).isEqualTo(1L);
        assertThat(summary.catalogMasters()).isEqualTo(1L);
        assertThat(summary.publishedVersions()).isEqualTo(2L);
        assertThat(summary.catalogTemplates()).isEqualTo(2L);
        verify(masterDocumentRepository).countGroupedByStatus(List.of(), true);
        verify(templateRepository).countGroupedByLifecycleStatus(List.of(), true);
    }
}
