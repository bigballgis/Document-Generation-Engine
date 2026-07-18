package com.bank.docgen.dashboard.service;

import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.dashboard.api.DashboardSummaryView;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Durable Dashboard Overview summary aggregates (PRR-D01c). Fail-closed on empty group scope.
 */
@Service
public class DashboardSummaryService {

    private final GroupAccessService groupAccessService;
    private final MasterDocumentRepository masterDocumentRepository;
    private final TemplateRepository templateRepository;

    public DashboardSummaryService(
            GroupAccessService groupAccessService,
            MasterDocumentRepository masterDocumentRepository,
            TemplateRepository templateRepository
    ) {
        this.groupAccessService = groupAccessService;
        this.masterDocumentRepository = masterDocumentRepository;
        this.templateRepository = templateRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryView summarize(ManagementSessionClaims session) {
        List<String> groupCodes = groupAccessService.accessibleGroupCodes(session);
        if (groupCodes.isEmpty()) {
            return DashboardSummaryView.zeros();
        }

        boolean allGroups = groupCodes.contains("*");
        List<String> scopedGroups = allGroups ? List.of() : List.copyOf(groupCodes);

        Map<MasterDocumentStatus, Long> masterCounts =
                masterDocumentRepository.countGroupedByStatus(scopedGroups, allGroups);
        Map<TemplateLifecycleStatus, Long> templateCounts =
                templateRepository.countGroupedByLifecycleStatus(scopedGroups, allGroups);

        long masterPendingReview = countOf(masterCounts, MasterDocumentStatus.PENDING_REVIEW);
        long masterVersionsInProgress = countOf(masterCounts, MasterDocumentStatus.DRAFT)
                + countOf(masterCounts, MasterDocumentStatus.REJECTED);
        long catalogMasters = sumCounts(masterCounts);

        long templateVersionsInWorkflow = countOf(templateCounts, TemplateLifecycleStatus.DRAFT)
                + countOf(templateCounts, TemplateLifecycleStatus.TESTING)
                + countOf(templateCounts, TemplateLifecycleStatus.APPROVAL)
                + countOf(templateCounts, TemplateLifecycleStatus.PENDING_RELEASE);
        long publishedVersions = countOf(templateCounts, TemplateLifecycleStatus.PUBLISHED);
        long stoppedVersions = countOf(templateCounts, TemplateLifecycleStatus.STOPPED);
        long catalogTemplates = sumCounts(templateCounts);

        return new DashboardSummaryView(
                masterPendingReview,
                masterVersionsInProgress,
                templateVersionsInWorkflow,
                publishedVersions,
                stoppedVersions,
                catalogMasters,
                catalogTemplates
        );
    }

    private static long countOf(Map<? extends Enum<?>, Long> counts, Enum<?> key) {
        return counts.getOrDefault(key, 0L);
    }

    private static long sumCounts(Map<?, Long> counts) {
        long total = 0L;
        for (Long value : counts.values()) {
            if (value != null) {
                total += value;
            }
        }
        return total;
    }
}
