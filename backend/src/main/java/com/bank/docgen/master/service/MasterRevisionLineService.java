package com.bank.docgen.master.service;

import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.api.MasterAnchorView;
import com.bank.docgen.master.api.MasterRevisionLineDetailView;
import com.bank.docgen.master.api.MasterRevisionLineSummaryView;
import com.bank.docgen.master.api.MasterReviewRecordView;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterReviewRecordEntity;
import com.bank.docgen.master.persistence.MasterReviewRecordRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MasterRevisionLineService {

    static final String CURRENT_LINE_LABEL = "CURRENT";
    static final String HISTORICAL_LINE_LABEL = "HISTORICAL";

    private static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private final MasterDocumentRepository masterDocumentRepository;
    private final MasterRevisionLineRepository masterRevisionLineRepository;
    private final MasterReviewRecordRepository masterReviewRecordRepository;
    private final ObjectStoragePort objectStoragePort;
    private final GroupAccessService groupAccessService;

    public MasterRevisionLineService(
            MasterDocumentRepository masterDocumentRepository,
            MasterRevisionLineRepository masterRevisionLineRepository,
            MasterReviewRecordRepository masterReviewRecordRepository,
            ObjectStoragePort objectStoragePort,
            GroupAccessService groupAccessService
    ) {
        this.masterDocumentRepository = masterDocumentRepository;
        this.masterRevisionLineRepository = masterRevisionLineRepository;
        this.masterReviewRecordRepository = masterReviewRecordRepository;
        this.objectStoragePort = objectStoragePort;
        this.groupAccessService = groupAccessService;
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
        return new PageView<>(
                lines.getContent().stream().map(line -> toSummary(line, master)).toList(),
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
                line.getRevisionSequence()
        );
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
                line.getCreatedAt(),
                line.getUpdatedAt()
        );
    }

    private Instant nextRevisionCreatedAt(UUID masterId, int revisionSequence) {
        return masterRevisionLineRepository
                .findCreatedAtByMasterIdAndRevisionSequence(masterId, revisionSequence + 1)
                .orElse(null);
    }
}
