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
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MasterRevisionLineService {

    static final String CURRENT_LINE_LABEL = "CURRENT";

    private static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private final MasterDocumentRepository masterDocumentRepository;
    private final MasterReviewRecordRepository masterReviewRecordRepository;
    private final ObjectStoragePort objectStoragePort;
    private final GroupAccessService groupAccessService;

    public MasterRevisionLineService(
            MasterDocumentRepository masterDocumentRepository,
            MasterReviewRecordRepository masterReviewRecordRepository,
            ObjectStoragePort objectStoragePort,
            GroupAccessService groupAccessService
    ) {
        this.masterDocumentRepository = masterDocumentRepository;
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
        MasterDocumentEntity master = requireReadableMasterWithAnchors(masterId, session);
        MasterRevisionLineSummaryView currentLine = toSummary(master);
        return PageView.of(List.of(currentLine), page, size);
    }

    @Transactional(readOnly = true)
    public MasterRevisionLineDetailView get(
            UUID masterId,
            UUID revisionLineId,
            ManagementSessionClaims session
    ) {
        MasterDocumentEntity master = requireReadableMasterWithAnchors(masterId, session);
        requireCurrentRevisionLine(master, revisionLineId);
        return toDetail(master);
    }

    @Transactional(readOnly = true)
    public MasterDocumentService.MasterDownloadArtifact openDownload(
            UUID masterId,
            UUID revisionLineId,
            ManagementSessionClaims session
    ) {
        MasterDocumentEntity master = requireReadableMaster(masterId, session);
        requireCurrentRevisionLine(master, revisionLineId);
        try {
            InputStream stream = objectStoragePort.get(master.getStorageKey());
            return new MasterDocumentService.MasterDownloadArtifact(
                    stream,
                    master.getOriginalFilename(),
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

    private MasterDocumentEntity requireReadableMasterWithAnchors(UUID masterId, ManagementSessionClaims session) {
        MasterDocumentEntity master = masterDocumentRepository.findWithAnchorsByIdAndDeletedAtIsNull(masterId)
                .orElseThrow(MasterNotFoundException::new);
        if (!groupAccessService.canAccessGroup(session, master.getGroupCode())) {
            throw new MasterAccessDeniedException();
        }
        return master;
    }

    private void requireCurrentRevisionLine(MasterDocumentEntity master, UUID revisionLineId) {
        if (!master.getCurrentRevisionLineId().equals(revisionLineId)) {
            throw new MasterNotFoundException();
        }
    }

    private MasterRevisionLineSummaryView toSummary(MasterDocumentEntity master) {
        return new MasterRevisionLineSummaryView(
                master.getCurrentRevisionLineId().toString(),
                CURRENT_LINE_LABEL,
                master.getStatus().name(),
                master.getOriginalFilename(),
                master.getAnchors().size(),
                master.getUpdatedAt(),
                master.getUpdatedBy(),
                true
        );
    }

    private MasterRevisionLineDetailView toDetail(MasterDocumentEntity master) {
        List<MasterReviewRecordEntity> reviewRecords =
                masterReviewRecordRepository.findByMasterIdOrderByCreatedAtDesc(master.getId());
        return new MasterRevisionLineDetailView(
                master.getCurrentRevisionLineId().toString(),
                master.getId().toString(),
                CURRENT_LINE_LABEL,
                master.getStatus().name(),
                master.getOriginalFilename(),
                master.getChangeSummary(),
                true,
                master.getAnchors().stream()
                        .map(anchor -> new MasterAnchorView(anchor.getAnchorId(), anchor.getDisplayLabel()))
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
