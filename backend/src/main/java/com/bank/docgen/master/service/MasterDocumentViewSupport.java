package com.bank.docgen.master.service;

import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.master.api.MasterAnchorView;
import com.bank.docgen.master.api.MasterDocumentDetailView;
import com.bank.docgen.master.api.MasterDocumentSummaryView;
import com.bank.docgen.master.api.MasterReviewRecordView;
import com.bank.docgen.master.persistence.MasterAnchorRepository;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterReviewRecordEntity;
import com.bank.docgen.master.persistence.MasterReviewRecordRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Package-private view mapping and display-name enrichment for master documents.
 */
final class MasterDocumentViewSupport {

    private final MasterAnchorRepository masterAnchorRepository;
    private final MasterReviewRecordRepository masterReviewRecordRepository;
    private final ManagementUserDisplayService managementUserDisplayService;

    MasterDocumentViewSupport(
            MasterAnchorRepository masterAnchorRepository,
            MasterReviewRecordRepository masterReviewRecordRepository,
            ManagementUserDisplayService managementUserDisplayService
    ) {
        this.masterAnchorRepository = masterAnchorRepository;
        this.masterReviewRecordRepository = masterReviewRecordRepository;
        this.managementUserDisplayService = managementUserDisplayService;
    }

    MasterDocumentSummaryView toSummary(MasterDocumentEntity master, long anchorCount) {
        return new MasterDocumentSummaryView(
                master.getId().toString(),
                master.getGroupCode(),
                master.getName(),
                master.getStatus().name(),
                master.getOriginalFilename(),
                Math.toIntExact(anchorCount),
                master.getUpdatedBy(),
                master.getUpdatedAt(),
                null
        );
    }

    List<MasterDocumentSummaryView> enrichMasterSummaries(List<MasterDocumentSummaryView> summaries) {
        if (summaries.isEmpty()) {
            return summaries;
        }
        Set<String> usernames = summaries.stream()
                .map(MasterDocumentSummaryView::updatedBy)
                .filter(username -> username != null && !username.isBlank())
                .collect(Collectors.toSet());
        Map<String, String> displayNames = managementUserDisplayService.lookupDisplayNames(usernames);
        return summaries.stream()
                .map(summary -> new MasterDocumentSummaryView(
                        summary.id(),
                        summary.groupCode(),
                        summary.name(),
                        summary.status(),
                        summary.originalFilename(),
                        summary.anchorCount(),
                        summary.updatedBy(),
                        summary.updatedAt(),
                        summary.updatedBy() == null ? null : displayNames.get(summary.updatedBy())
                ))
                .toList();
    }

    Map<UUID, Long> loadAnchorCounts(List<MasterDocumentEntity> masters) {
        if (masters.isEmpty()) {
            return Map.of();
        }
        List<UUID> masterIds = masters.stream().map(MasterDocumentEntity::getId).toList();
        return masterAnchorRepository.countByMasterIdIn(masterIds).stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
    }

    MasterDocumentDetailView toDetail(MasterDocumentEntity master) {
        List<MasterReviewRecordEntity> reviewRecords =
                masterReviewRecordRepository.findByMasterIdOrderByCreatedAtDesc(master.getId());
        return new MasterDocumentDetailView(
                master.getId().toString(),
                master.getGroupCode(),
                master.getName(),
                master.getDescription(),
                master.getStatus().name(),
                master.getOriginalFilename(),
                master.getChangeSummary(),
                master.getAnchors().stream()
                        .map(anchor -> new MasterAnchorView(
                                anchor.getAnchorId(),
                                anchor.getDisplayLabel(),
                                anchor.getDocumentSequence()))
                        .toList(),
                reviewRecords.stream()
                        .map(record -> new MasterReviewRecordView(
                                record.getAction().name(),
                                record.getDecision(),
                                record.getChangeSummary(),
                                record.getCommentSummary(),
                                record.getActorUsername(),
                                record.getCreatedAt()))
                        .toList(),
                master.getCreatedBy(),
                master.getUpdatedBy(),
                master.getCreatedAt(),
                master.getUpdatedAt()
        );
    }
}
