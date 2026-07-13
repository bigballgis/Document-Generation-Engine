package com.bank.docgen.master.service;

import com.bank.docgen.master.api.DecideMasterReviewRequest;
import com.bank.docgen.master.api.MasterDocumentDetailView;
import com.bank.docgen.master.api.SubmitMasterReviewRequest;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.domain.MasterReviewAction;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterReviewRecordEntity;
import com.bank.docgen.master.persistence.MasterReviewRecordRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.util.UUID;

/**
 * Package-private master-document review submit / decide bodies.
 */
final class MasterDocumentReviewSupport {

    private final MasterReviewRecordRepository masterReviewRecordRepository;
    private final MasterDocxUploadSupport docxUploadSupport;
    private final MasterDocumentAccessSupport access;
    private final MasterDocumentViewSupport views;

    MasterDocumentReviewSupport(
            MasterReviewRecordRepository masterReviewRecordRepository,
            MasterDocxUploadSupport docxUploadSupport,
            MasterDocumentAccessSupport access,
            MasterDocumentViewSupport views
    ) {
        this.masterReviewRecordRepository = masterReviewRecordRepository;
        this.docxUploadSupport = docxUploadSupport;
        this.access = access;
        this.views = views;
    }

    MasterDocumentDetailView submitReview(
            UUID masterId,
            SubmitMasterReviewRequest request,
            ManagementSessionClaims session
    ) {
        MasterDocumentEntity master = access.requireWritableMasterWithAnchors(masterId, session);
        if (master.getStatus() != MasterDocumentStatus.DRAFT) {
            throw new MasterValidationException("api.error.master.invalidReviewTransition");
        }
        docxUploadSupport.assertAnchorIntegrity(master);
        master.setChangeSummary(request.changeSummary());
        master.setStatus(MasterDocumentStatus.PENDING_REVIEW);
        master.setUpdatedBy(session.username());
        masterReviewRecordRepository.save(new MasterReviewRecordEntity(
                UUID.randomUUID(),
                masterId,
                MasterReviewAction.SUBMITTED,
                null,
                request.changeSummary(),
                null,
                session.username()
        ));
        return views.toDetail(master);
    }

    MasterDocumentDetailView decideReview(
            UUID masterId,
            DecideMasterReviewRequest request,
            ManagementSessionClaims session
    ) {
        if (!access.canReviewMasters(session)) {
            throw new MasterAccessDeniedException();
        }
        MasterDocumentEntity master = access.requireReadableMasterWithAnchors(masterId, session);
        if (master.getStatus() != MasterDocumentStatus.PENDING_REVIEW) {
            throw new MasterValidationException("api.error.master.invalidReviewTransition");
        }
        MasterDocumentStatus nextStatus = "APPROVED".equals(request.decision())
                ? MasterDocumentStatus.APPROVED
                : MasterDocumentStatus.DRAFT;
        master.setStatus(nextStatus);
        master.setUpdatedBy(session.username());
        masterReviewRecordRepository.save(new MasterReviewRecordEntity(
                UUID.randomUUID(),
                masterId,
                "APPROVED".equals(request.decision()) ? MasterReviewAction.APPROVED : MasterReviewAction.REJECTED,
                request.decision(),
                master.getChangeSummary(),
                request.commentSummary(),
                session.username()
        ));
        return views.toDetail(master);
    }
}
