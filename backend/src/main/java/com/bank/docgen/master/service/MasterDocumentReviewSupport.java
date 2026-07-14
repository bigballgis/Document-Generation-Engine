package com.bank.docgen.master.service;

import com.bank.docgen.master.api.DecideMasterReviewRequest;
import com.bank.docgen.master.api.MasterDocumentDetailView;
import com.bank.docgen.master.api.SubmitMasterReviewRequest;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.domain.MasterReviewAction;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterReviewRecordEntity;
import com.bank.docgen.master.persistence.MasterReviewRecordRepository;
import com.bank.docgen.sharedkernel.lifecycle.SelfApprovalGuard;
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
    private final SelfApprovalGuard selfApprovalGuard;

    MasterDocumentReviewSupport(
            MasterReviewRecordRepository masterReviewRecordRepository,
            MasterDocxUploadSupport docxUploadSupport,
            MasterDocumentAccessSupport access,
            MasterDocumentViewSupport views,
            SelfApprovalGuard selfApprovalGuard
    ) {
        this.masterReviewRecordRepository = masterReviewRecordRepository;
        this.docxUploadSupport = docxUploadSupport;
        this.access = access;
        this.views = views;
        this.selfApprovalGuard = selfApprovalGuard;
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
        String lastSubmitActor = latestSubmittedActor(masterId);
        SelfApprovalGuard.EnforceOutcome outcome = selfApprovalGuard.enforce(new SelfApprovalGuard.EnforceRequest(
                session.username(),
                lastSubmitActor,
                Boolean.TRUE.equals(request.exceptionIntervention()),
                request.exceptionReason(),
                request.secondaryConfirmed(),
                session,
                "api.error.lifecycle.selfApprovalForbidden",
                "api.error.lifecycle.exceptionInterventionNotAllowed",
                "api.error.lifecycle.exceptionReasonRequired",
                "api.error.lifecycle.exceptionSecondaryConfirmRequired"
        ));
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
                session.username(),
                outcome.selfApprovalException(),
                outcome.exceptionReason()
        ));
        return views.toDetail(master);
    }

    /**
     * CE-G01: most recent {@code SUBMITTED} review row actor, or {@code null} when
     * none exists (CMP-3 — migration gaps do not trigger the block).
     */
    private String latestSubmittedActor(UUID masterId) {
        return masterReviewRecordRepository.findByMasterIdOrderByCreatedAtDesc(masterId).stream()
                .filter(record -> record.getAction() == MasterReviewAction.SUBMITTED)
                .map(MasterReviewRecordEntity::getActorUsername)
                .findFirst()
                .orElse(null);
    }
}
