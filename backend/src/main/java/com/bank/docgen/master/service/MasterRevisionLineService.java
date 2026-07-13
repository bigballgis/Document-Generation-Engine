package com.bank.docgen.master.service;

import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.api.MasterAnchorView;
import com.bank.docgen.master.api.MasterRevisionLineDetailView;
import com.bank.docgen.master.api.MasterRevisionLineSummaryView;
import com.bank.docgen.master.api.MasterReviewRecordView;
import com.bank.docgen.master.api.PinnedReleaseReference;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterReviewRecordEntity;
import com.bank.docgen.master.persistence.MasterReviewRecordRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MasterRevisionLineService {

    private static final Logger LOG = LoggerFactory.getLogger(MasterRevisionLineService.class);

    static final String CURRENT_LINE_LABEL = "CURRENT";
    static final String HISTORICAL_LINE_LABEL = "HISTORICAL";

    private static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private final MasterDocumentRepository masterDocumentRepository;
    private final MasterRevisionLineRepository masterRevisionLineRepository;
    private final MasterReviewRecordRepository masterReviewRecordRepository;
    private final ObjectStoragePort objectStoragePort;
    private final GroupAccessService groupAccessService;
    private final ManagementUserDisplayService managementUserDisplayService;
    private final TemplateVersionRepository templateVersionRepository;

    public MasterRevisionLineService(
            MasterDocumentRepository masterDocumentRepository,
            MasterRevisionLineRepository masterRevisionLineRepository,
            MasterReviewRecordRepository masterReviewRecordRepository,
            ObjectStoragePort objectStoragePort,
            GroupAccessService groupAccessService,
            ManagementUserDisplayService managementUserDisplayService,
            TemplateVersionRepository templateVersionRepository
    ) {
        this.masterDocumentRepository = masterDocumentRepository;
        this.masterRevisionLineRepository = masterRevisionLineRepository;
        this.masterReviewRecordRepository = masterReviewRecordRepository;
        this.objectStoragePort = objectStoragePort;
        this.groupAccessService = groupAccessService;
        this.managementUserDisplayService = managementUserDisplayService;
        this.templateVersionRepository = templateVersionRepository;
    }

    @Transactional(readOnly = true)
    public PageView<MasterRevisionLineSummaryView> list(
            UUID masterId,
            int page,
            int size,
            ManagementSessionClaims session
    ) {
        MasterDocumentEntity master = requireReadableMaster(masterId, session);
        Pageable pageable = PageRequest.of(Math.max(page, 0), size <= 0 ? 20 : size);
        Page<MasterRevisionLineEntity> lines = masterRevisionLineRepository
                .findByMasterIdAndDeletedAtIsNullOrderByCurrentDescCreatedAtDesc(masterId, pageable);
        List<MasterRevisionLineSummaryView> content = enrichSummaries(
                lines.getContent().stream().map(line -> toSummary(line, master)).toList());
        return new PageView<>(
                content,
                lines.getNumber(),
                lines.getSize(),
                lines.getTotalElements(),
                lines.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public MasterRevisionLineDetailView get(
            UUID masterId,
            UUID revisionLineId,
            ManagementSessionClaims session
    ) {
        MasterDocumentEntity master = requireReadableMaster(masterId, session);
        MasterRevisionLineEntity line = requireRevisionLine(masterId, revisionLineId);
        return toDetail(line, master);
    }

    @Transactional(readOnly = true)
    public MasterDocumentService.MasterDownloadArtifact openDownload(
            UUID masterId,
            UUID revisionLineId,
            ManagementSessionClaims session
    ) {
        requireReadableMaster(masterId, session);
        MasterRevisionLineEntity line = requireRevisionLine(masterId, revisionLineId);
        try {
            InputStream stream = objectStoragePort.get(line.getStorageKey());
            return new MasterDocumentService.MasterDownloadArtifact(
                    stream,
                    line.getOriginalFilename(),
                    DOCX_CONTENT_TYPE
            );
        } catch (Exception ex) {
            throw new MasterValidationException("api.error.master.downloadFailed");
        }
    }

    /**
     * CE-K01: delete a master revision line (soft-delete + object-storage cleanup). Fail-closed
     * with {@link MasterRevisionInUseException} (409) when any non-deleted published-lifecycle
     * template version pins this revision. Stopping/deprecating a release does not release the
     * pin (BDD-CE-K01-013), so the guard checks all referenced versions regardless of their
     * published-lifecycle status.
     */
    @Transactional
    public void deleteRevisionLine(UUID masterId, UUID revisionLineId, ManagementSessionClaims session) {
        requireReadableMaster(masterId, session);
        MasterRevisionLineEntity line = requireRevisionLine(masterId, revisionLineId);
        List<TemplateVersionEntity> referencingVersions =
                templateVersionRepository.findByMasterRevisionIdAndDeletedAtIsNull(revisionLineId);
        if (!referencingVersions.isEmpty()) {
            throw new MasterRevisionInUseException(buildReferences(referencingVersions));
        }
        line.setDeletedAt(Instant.now());
        masterRevisionLineRepository.save(line);
        if (objectStoragePort.exists(line.getStorageKey())) {
            try {
                objectStoragePort.delete(line.getStorageKey());
            } catch (RuntimeException ex) {
                LOG.warn("CE-K01 revision {} object storage cleanup failed: {}", revisionLineId, ex.getMessage());
            }
        }
    }

    private List<PinnedReleaseReference> buildReferences(List<TemplateVersionEntity> versions) {
        return versions.stream()
                .map(version -> {
                    String templateId = version.getTemplateId() == null ? null : version.getTemplateId().toString();
                    String releaseVersion = version.getReleaseVersion();
                    String status = version.getLifecycleStatus() == null ? null : version.getLifecycleStatus().name();
                    return new PinnedReleaseReference(templateId, releaseVersion, status);
                })
                .toList();
    }

    private MasterDocumentEntity requireReadableMaster(UUID masterId, ManagementSessionClaims session) {
        MasterDocumentEntity master = masterDocumentRepository.findByIdAndDeletedAtIsNull(masterId)
                .orElseThrow(MasterNotFoundException::new);
        if (!groupAccessService.canAccessGroup(session, master.getGroupCode())) {
            throw new MasterAccessDeniedException();
        }
        return master;
    }

    private MasterRevisionLineEntity requireRevisionLine(UUID masterId, UUID revisionLineId) {
        return masterRevisionLineRepository.findByIdAndMasterIdAndDeletedAtIsNull(revisionLineId, masterId)
                .orElseThrow(MasterNotFoundException::new);
    }

    private MasterRevisionLineSummaryView toSummary(MasterRevisionLineEntity line, MasterDocumentEntity master) {
        String status = line.isCurrent() ? master.getStatus().name() : line.getStatusSnapshot().name();
        return new MasterRevisionLineSummaryView(
                line.getId().toString(),
                line.isCurrent() ? CURRENT_LINE_LABEL : HISTORICAL_LINE_LABEL,
                status,
                line.getOriginalFilename(),
                line.getAnchorCount(),
                line.getUpdatedAt(),
                line.getUpdatedBy(),
                line.isCurrent(),
                line.getRevisionSequence(),
                null
        );
    }

    private List<MasterRevisionLineSummaryView> enrichSummaries(List<MasterRevisionLineSummaryView> summaries) {
        if (summaries.isEmpty()) {
            return summaries;
        }
        Set<String> usernames = summaries.stream()
                .map(MasterRevisionLineSummaryView::updatedBy)
                .filter(username -> username != null && !username.isBlank())
                .collect(Collectors.toSet());
        Map<String, String> displayNames = managementUserDisplayService.lookupDisplayNames(usernames);
        return summaries.stream()
                .map(summary -> new MasterRevisionLineSummaryView(
                        summary.id(),
                        summary.lineLabel(),
                        summary.status(),
                        summary.originalFilename(),
                        summary.anchorCount(),
                        summary.updatedAt(),
                        summary.updatedBy(),
                        summary.current(),
                        summary.revisionSequence(),
                        summary.updatedBy() == null ? null : displayNames.get(summary.updatedBy())
                ))
                .toList();
    }

    private MasterRevisionLineDetailView toDetail(MasterRevisionLineEntity line, MasterDocumentEntity master) {
        String status = line.isCurrent() ? master.getStatus().name() : line.getStatusSnapshot().name();
        String changeSummary = line.isCurrent() ? master.getChangeSummary() : line.getChangeSummary();
        List<MasterReviewRecordEntity> reviewRecords =
                masterReviewRecordRepository.findByMasterIdOrderByCreatedAtDesc(master.getId());
        Instant reviewUpperBound = nextRevisionCreatedAt(master.getId(), line.getRevisionSequence());
        List<MasterReviewRecordView> scopedHistory = reviewRecords.stream()
                .filter(record -> !record.getCreatedAt().isBefore(line.getCreatedAt()))
                .filter(record -> reviewUpperBound == null || record.getCreatedAt().isBefore(reviewUpperBound))
                .map(record -> new MasterReviewRecordView(
                        record.getAction().name(),
                        record.getDecision(),
                        record.getChangeSummary(),
                        record.getCommentSummary(),
                        record.getActorUsername(),
                        record.getCreatedAt()))
                .toList();
        return new MasterRevisionLineDetailView(
                line.getId().toString(),
                master.getId().toString(),
                line.isCurrent() ? CURRENT_LINE_LABEL : HISTORICAL_LINE_LABEL,
                status,
                line.getOriginalFilename(),
                changeSummary,
                line.isCurrent(),
                line.getRevisionSequence(),
                line.getAnchors().stream()
                        .map(anchor -> new MasterAnchorView(anchor.getAnchorId(), anchor.getDisplayLabel()))
                        .toList(),
                scopedHistory,
                line.getCreatedBy(),
                line.getUpdatedBy(),
                resolveUpdatedByDisplayName(line.getUpdatedBy()),
                line.getCreatedAt(),
                line.getUpdatedAt()
        );
    }

    private String resolveUpdatedByDisplayName(String updatedBy) {
        if (updatedBy == null || updatedBy.isBlank()) {
            return null;
        }
        return managementUserDisplayService.lookupDisplayNames(Set.of(updatedBy)).get(updatedBy);
    }

    private Instant nextRevisionCreatedAt(UUID masterId, int revisionSequence) {
        return masterRevisionLineRepository
                .findCreatedAtByMasterIdAndRevisionSequence(masterId, revisionSequence + 1)
                .orElse(null);
    }
}
