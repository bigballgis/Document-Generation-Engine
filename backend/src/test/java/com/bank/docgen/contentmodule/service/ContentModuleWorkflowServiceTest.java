package com.bank.docgen.contentmodule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.api.ContentModuleWorkflowTaskView;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewState;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

/**
 * BDD-PRR-A03-001…005 — SUBMITTED bounded query + inbox projection.
 */
@ExtendWith(MockitoExtension.class)
class ContentModuleWorkflowServiceTest {

    private static final int SCAN_PAGE_SIZE = 100;
    private static final int INBOX_LIMIT = 500;

    @Mock
    private ContentModuleRepository moduleRepository;
    @Mock
    private ContentModuleVersionRepository versionRepository;
    @Mock
    private GroupAccessService groupAccessService;
    @Mock
    private ContentModuleAccessService accessSupport;

    private ContentModuleWorkflowService service;
    private ManagementSessionClaims approver;

    @BeforeEach
    void setUp() {
        service = new ContentModuleWorkflowService(
                moduleRepository,
                versionRepository,
                groupAccessService,
                accessSupport,
                INBOX_LIMIT,
                SCAN_PAGE_SIZE
        );
        approver = session("10000005", List.of("GROUP_ADMIN"), List.of("RETAIL"));
        lenient().when(groupAccessService.canBrowseContentModuleCatalog(any())).thenReturn(true);
        lenient().when(groupAccessService.canDecideContentModuleReviews(any())).thenReturn(true);
        lenient().when(groupAccessService.canAuthorContentModules(any())).thenReturn(false);
        lenient().when(accessSupport.readSharedGroupCodes(any())).thenReturn(List.of());
        lenient().when(accessSupport.publicModuleId(any())).thenAnswer(inv -> {
            ContentModuleEntity module = inv.getArgument(0);
            return module.getModuleCode();
        });
    }

    @Test
    void listWorkflowTasks_scansSubmittedInBoundedPages() {
        // BDD-PRR-A03-001
        int total = SCAN_PAGE_SIZE * 2;
        List<ContentModuleVersionEntity> page0 = new ArrayList<>();
        List<ContentModuleVersionEntity> page1 = new ArrayList<>();
        List<ContentModuleEntity> modules = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            ContentModuleEntity module = module("RETAIL", "MOD-" + i);
            modules.add(module);
            ContentModuleVersionEntity version = submittedVersion(module.getId(), Instant.now().minusSeconds(i));
            if (i < SCAN_PAGE_SIZE) {
                page0.add(version);
            } else {
                page1.add(version);
            }
        }
        when(versionRepository.findByReviewStateOrderByUpdatedAtDesc(
                eq(ContentModuleReviewState.SUBMITTED), any(Pageable.class)))
                .thenReturn(page0)
                .thenReturn(page1)
                .thenReturn(List.of());
        when(moduleRepository.findAllById(any())).thenAnswer(inv -> findModules(modules, inv.getArgument(0)));
        when(groupAccessService.canAccessGroup(any(), eq("RETAIL"))).thenReturn(true);

        List<ContentModuleWorkflowTaskView> tasks = service.listWorkflowTasks(approver);

        assertThat(tasks).hasSize(total);
        assertThat(tasks).allMatch(t -> ContentModuleWorkflowService.KIND_PENDING_REVIEW.equals(t.kind()));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(versionRepository, atLeast(2)).findByReviewStateOrderByUpdatedAtDesc(
                eq(ContentModuleReviewState.SUBMITTED), pageableCaptor.capture());
        assertThat(pageableCaptor.getAllValues())
                .allMatch(pageable -> pageable.getPageSize() == SCAN_PAGE_SIZE);
    }

    @Test
    void listWorkflowTasks_pagesPastInvisibleGroupsToReachVisible() {
        // BDD-PRR-A03-002
        List<ContentModuleEntity> allModules = new ArrayList<>();
        List<ContentModuleVersionEntity> page0 = new ArrayList<>();
        for (int i = 0; i < SCAN_PAGE_SIZE; i++) {
            ContentModuleEntity hidden = module("CORP", "HIDDEN-" + i);
            allModules.add(hidden);
            page0.add(submittedVersion(hidden.getId(), Instant.now().minusSeconds(i + 100)));
        }
        ContentModuleEntity visible = module("RETAIL", "VISIBLE-1");
        allModules.add(visible);
        ContentModuleVersionEntity visibleVersion =
                submittedVersion(visible.getId(), Instant.now().minusSeconds(1));
        when(groupAccessService.canAccessGroup(any(), eq("CORP"))).thenReturn(false);
        when(groupAccessService.canAccessGroup(any(), eq("RETAIL"))).thenReturn(true);
        when(moduleRepository.findAllById(any())).thenAnswer(inv -> findModules(allModules, inv.getArgument(0)));
        when(versionRepository.findByReviewStateOrderByUpdatedAtDesc(
                eq(ContentModuleReviewState.SUBMITTED), any(Pageable.class)))
                .thenReturn(page0)
                .thenReturn(List.of(visibleVersion))
                .thenReturn(List.of());

        List<ContentModuleWorkflowTaskView> tasks = service.listWorkflowTasks(approver);

        assertThat(tasks).extracting(ContentModuleWorkflowTaskView::moduleCode)
                .contains("VISIBLE-1");
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(versionRepository, atLeast(2)).findByReviewStateOrderByUpdatedAtDesc(
                eq(ContentModuleReviewState.SUBMITTED), pageableCaptor.capture());
        assertThat(pageableCaptor.getAllValues().stream().map(Pageable::getPageNumber).toList())
                .contains(0, 1);
    }

    @Test
    void listWorkflowTasks_capsPendingReviewAtInboxLimit() {
        // BDD-PRR-A03-003
        int overLimit = INBOX_LIMIT + 20;
        List<ContentModuleVersionEntity> versions = new ArrayList<>();
        List<ContentModuleEntity> modules = new ArrayList<>();
        for (int i = 0; i < overLimit; i++) {
            ContentModuleEntity module = module("RETAIL", "CAP-" + i);
            modules.add(module);
            versions.add(submittedVersion(module.getId(), Instant.now().minusSeconds(i)));
        }
        when(versionRepository.findByReviewStateOrderByUpdatedAtDesc(
                eq(ContentModuleReviewState.SUBMITTED), any(Pageable.class)))
                .thenAnswer(inv -> {
                    Pageable pageable = inv.getArgument(1);
                    int from = (int) pageable.getOffset();
                    if (from >= versions.size()) {
                        return List.of();
                    }
                    int to = Math.min(from + pageable.getPageSize(), versions.size());
                    return versions.subList(from, to);
                });
        when(moduleRepository.findAllById(any())).thenAnswer(inv -> findModules(modules, inv.getArgument(0)));
        when(groupAccessService.canAccessGroup(any(), eq("RETAIL"))).thenReturn(true);

        List<ContentModuleWorkflowTaskView> tasks = service.listWorkflowTasks(approver);

        assertThat(tasks).hasSize(INBOX_LIMIT);
    }

    @Test
    void listWorkflowTasks_withoutDecideCapability_omitsPendingReview() {
        // BDD-PRR-A03-004
        when(groupAccessService.canDecideContentModuleReviews(any())).thenReturn(false);
        when(groupAccessService.canAuthorContentModules(any())).thenReturn(true);
        when(versionRepository.findDraftVersionsWithRejectionReason()).thenReturn(List.of());

        List<ContentModuleWorkflowTaskView> tasks = service.listWorkflowTasks(approver);

        assertThat(tasks).isEmpty();
        verify(versionRepository, never()).findByReviewStateOrderByUpdatedAtDesc(
                eq(ContentModuleReviewState.SUBMITTED), any(Pageable.class));
    }

    @Test
    void listWorkflowTasks_withoutCatalogBrowse_denies() {
        // BDD-PRR-A03-005
        when(groupAccessService.canBrowseContentModuleCatalog(any())).thenReturn(false);

        assertThatThrownBy(() -> service.listWorkflowTasks(approver))
                .isInstanceOf(ContentModuleAccessDeniedException.class);
        verify(versionRepository, never()).findByReviewStateOrderByUpdatedAtDesc(
                eq(ContentModuleReviewState.SUBMITTED), any(Pageable.class));
    }

    private static List<ContentModuleEntity> findModules(
            List<ContentModuleEntity> modules,
            Iterable<UUID> ids
    ) {
        List<ContentModuleEntity> found = new ArrayList<>();
        for (ContentModuleEntity module : modules) {
            for (UUID id : ids) {
                if (module.getId().equals(id)) {
                    found.add(module);
                }
            }
        }
        return found;
    }

    private ContentModuleEntity module(String groupCode, String moduleCode) {
        return new ContentModuleEntity(
                UUID.randomUUID(),
                moduleCode,
                groupCode,
                moduleCode + " name",
                null,
                "[]",
                "10000003"
        );
    }

    private ContentModuleVersionEntity submittedVersion(UUID moduleId, Instant updatedAt) {
        ContentModuleVersionEntity version = new ContentModuleVersionEntity(
                UUID.randomUUID(),
                moduleId,
                "1.0.0",
                "{\"blocks\":[]}",
                "change",
                "10000003"
        );
        version.setReviewState(ContentModuleReviewState.SUBMITTED);
        // setReviewState refreshes updatedAt; touch again via setUpdatedBy for distinct ordering.
        version.setUpdatedBy("10000003");
        return version;
    }

    private ManagementSessionClaims session(String username, List<String> roles, List<String> groups) {
        return new ManagementSessionClaims(
                username,
                "User",
                username + "@test.com",
                AuthSource.LOCAL,
                roles,
                groups,
                "route.home",
                List.of("route.home"),
                Instant.now().plusSeconds(3600)
        );
    }
}
