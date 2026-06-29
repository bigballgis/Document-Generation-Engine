package com.bank.docgen.contentmodule.service;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.api.ContentModuleReviewSnapshotView;
import com.bank.docgen.contentmodule.api.ContentModuleReviewTransitionRequest;
import com.bank.docgen.contentmodule.api.ContentModuleReviewTransitionResultView;
import com.bank.docgen.contentmodule.domain.ContentModuleGovernanceActorRole;
import com.bank.docgen.contentmodule.domain.ContentModuleLifecycleState;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewOperation;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewState;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentModuleReviewService {

    private final ContentModuleRepository moduleRepository;
    private final ContentModuleVersionRepository versionRepository;
    private final GroupAccessService groupAccessService;
    private final ContentModuleAccessSupport accessSupport;
    private final ManagementAuditRecorder auditRecorder;

    public ContentModuleReviewService(
            ContentModuleRepository moduleRepository,
            ContentModuleVersionRepository versionRepository,
            GroupAccessService groupAccessService,
            ContentModuleAccessSupport accessSupport,
            ManagementAuditRecorder auditRecorder
    ) {
        this.moduleRepository = moduleRepository;
        this.versionRepository = versionRepository;
        this.groupAccessService = groupAccessService;
        this.accessSupport = accessSupport;
        this.auditRecorder = auditRecorder;
    }

    @Transactional
    public ContentModuleReviewTransitionResultView transition(
            String moduleId,
            ContentModuleReviewTransitionRequest request,
            ManagementSessionClaims session
    ) {
        validateRequest(request);
        ContentModuleEntity module = accessSupport.requireReadableModule(moduleId, session);
        assertOperationRole(request, session);

        ContentModuleVersionEntity version = resolveTargetVersion(module.getId(), request.operation());
        applyTransition(version, request, session.username());

        versionRepository.save(version);
        module.setUpdatedBy(session.username());
        moduleRepository.save(module);

        auditRecorder.recordContentModuleReviewTransition(
                module.getId(),
                module.getGroupCode(),
                module.getModuleCode(),
                request.operation().name(),
                version.getSemanticVersion(),
                version.getReviewState().name(),
                session.username(),
                accessSupport.actorSummary(session)
        );

        return new ContentModuleReviewTransitionResultView(
                true,
                null,
                null,
                toSnapshot(module, version)
        );
    }

    private void validateRequest(ContentModuleReviewTransitionRequest request) {
        if (request.operation() == null || request.actorRole() == null
                || request.actorId() == null || request.actorId().isBlank()) {
            throw new ContentModuleGovernanceException(
                    "MODULE_REVIEW_REQUEST_INVALID",
                    "api.error.contentModule.reviewRequestInvalid",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }
        if (request.operation() == ContentModuleReviewOperation.SUBMIT_FOR_REVIEW
                && (request.changeDescription() == null || request.changeDescription().isBlank())) {
            throw new ContentModuleGovernanceException(
                    "MODULE_CHANGE_DESCRIPTION_REQUIRED",
                    "api.error.contentModule.changeDescriptionRequired",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }
        if (request.operation() == ContentModuleReviewOperation.REJECT_REVIEW
                && (request.rejectionReason() == null || request.rejectionReason().isBlank())) {
            throw new ContentModuleGovernanceException(
                    "MODULE_REJECTION_REASON_REQUIRED",
                    "api.error.contentModule.rejectionReasonRequired",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }
    }

    private void assertOperationRole(ContentModuleReviewTransitionRequest request, ManagementSessionClaims session) {
        accessSupport.assertActorSession(session, request.actorRole());
        switch (request.operation()) {
            case SUBMIT_FOR_REVIEW -> {
                if (!isAuthorRole(request.actorRole()) || !groupAccessService.canAuthorContentModules(session)) {
                    throw roleDenied();
                }
            }
            case APPROVE_REVIEW, REJECT_REVIEW -> {
                if (!groupAccessService.canDecideContentModuleReviews(session)) {
                    throw roleDenied();
                }
                if (request.actorRole() != ContentModuleGovernanceActorRole.APPROVER
                        && request.actorRole() != ContentModuleGovernanceActorRole.GROUP_ADMIN
                        && request.actorRole() != ContentModuleGovernanceActorRole.GLOBAL_ADMIN) {
                    throw roleDenied();
                }
            }
            default -> throw roleDenied();
        }
    }

    private boolean isAuthorRole(ContentModuleGovernanceActorRole actorRole) {
        return actorRole == ContentModuleGovernanceActorRole.TEMPLATE_AUTHOR
                || actorRole == ContentModuleGovernanceActorRole.MASTER_DESIGNER
                || actorRole == ContentModuleGovernanceActorRole.GROUP_ADMIN
                || actorRole == ContentModuleGovernanceActorRole.GLOBAL_ADMIN;
    }

    private ContentModuleGovernanceException roleDenied() {
        return new ContentModuleGovernanceException(
                "MODULE_REVIEW_ROLE_DENIED",
                "api.error.contentModule.reviewRoleDenied",
                HttpStatus.FORBIDDEN
        );
    }

    private ContentModuleVersionEntity resolveTargetVersion(UUID moduleId, ContentModuleReviewOperation operation) {
        List<ContentModuleVersionEntity> candidates = switch (operation) {
            case SUBMIT_FOR_REVIEW -> versionRepository.findByModuleIdAndReviewStateOrderBySemanticVersionDesc(
                    moduleId, ContentModuleReviewState.DRAFT);
            case APPROVE_REVIEW, REJECT_REVIEW -> versionRepository.findByModuleIdAndReviewStateOrderBySemanticVersionDesc(
                    moduleId, ContentModuleReviewState.SUBMITTED);
        };
        if (candidates.isEmpty()) {
            throw new ContentModuleGovernanceException(
                    "MODULE_REVIEW_STATE_TRANSITION_DENIED",
                    "api.error.contentModule.reviewStateTransitionDenied",
                    HttpStatus.CONFLICT
            );
        }
        return candidates.getFirst();
    }

    private void applyTransition(
            ContentModuleVersionEntity version,
            ContentModuleReviewTransitionRequest request,
            String actorUsername
    ) {
        switch (request.operation()) {
            case SUBMIT_FOR_REVIEW -> {
                if (version.getReviewState() != ContentModuleReviewState.DRAFT) {
                    throw stateDenied();
                }
                version.setReviewState(ContentModuleReviewState.SUBMITTED);
                version.setChangeDescription(request.changeDescription().trim());
            }
            case APPROVE_REVIEW -> {
                if (version.getReviewState() != ContentModuleReviewState.SUBMITTED) {
                    throw stateDenied();
                }
                version.setReviewState(ContentModuleReviewState.APPROVED);
                version.setLifecycleState(ContentModuleLifecycleState.ACTIVE);
                version.setRejectionReason(null);
            }
            case REJECT_REVIEW -> {
                if (version.getReviewState() != ContentModuleReviewState.SUBMITTED) {
                    throw stateDenied();
                }
                version.setReviewState(ContentModuleReviewState.DRAFT);
                version.setRejectionReason(request.rejectionReason().trim());
            }
            default -> throw stateDenied();
        }
        version.setUpdatedBy(actorUsername);
    }

    private ContentModuleGovernanceException stateDenied() {
        return new ContentModuleGovernanceException(
                "MODULE_REVIEW_STATE_TRANSITION_DENIED",
                "api.error.contentModule.reviewStateTransitionDenied",
                HttpStatus.CONFLICT
        );
    }

    private ContentModuleReviewSnapshotView toSnapshot(
            ContentModuleEntity module,
            ContentModuleVersionEntity version
    ) {
        return new ContentModuleReviewSnapshotView(
                accessSupport.publicModuleId(module),
                version.getReviewState().name(),
                version.getUpdatedAt(),
                version.getUpdatedBy(),
                version.getRejectionReason()
        );
    }
}
