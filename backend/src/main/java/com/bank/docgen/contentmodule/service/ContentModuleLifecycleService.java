package com.bank.docgen.contentmodule.service;

import com.bank.docgen.audit.api.ContentModuleLifecycleAuditDetail;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.api.ContentModuleLifecycleOperationApplyRequest;
import com.bank.docgen.contentmodule.api.ContentModuleLifecycleOperationResultView;
import com.bank.docgen.contentmodule.api.ContentModuleLifecycleSnapshotView;
import com.bank.docgen.contentmodule.domain.ContentModuleGovernanceActorRole;
import com.bank.docgen.contentmodule.domain.ContentModuleLifecycleOperation;
import com.bank.docgen.contentmodule.domain.ContentModuleLifecycleState;
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
public class ContentModuleLifecycleService {

    private final ContentModuleRepository moduleRepository;
    private final ContentModuleVersionRepository versionRepository;
    private final GroupAccessService groupAccessService;
    private final ContentModuleAccessService accessSupport;
    private final ManagementAuditRecorder auditRecorder;

    public ContentModuleLifecycleService(
            ContentModuleRepository moduleRepository,
            ContentModuleVersionRepository versionRepository,
            GroupAccessService groupAccessService,
            ContentModuleAccessService accessSupport,
            ManagementAuditRecorder auditRecorder
    ) {
        this.moduleRepository = moduleRepository;
        this.versionRepository = versionRepository;
        this.groupAccessService = groupAccessService;
        this.accessSupport = accessSupport;
        this.auditRecorder = auditRecorder;
    }

    @Transactional
    public ContentModuleLifecycleOperationResultView apply(
            String moduleId,
            ContentModuleLifecycleOperationApplyRequest request,
            ManagementSessionClaims session
    ) {
        validateRequest(request);
        ContentModuleEntity module = accessSupport.requireReadableModule(moduleId, session);
        accessSupport.assertLifecycleActorSession(session, request.actorRole());
        if (!groupAccessService.canManageContentModuleLifecycle(session)) {
            throw lifecycleRoleDenied();
        }
        if (request.actorRole() != ContentModuleGovernanceActorRole.GLOBAL_ADMIN
                && request.actorRole() != ContentModuleGovernanceActorRole.GROUP_ADMIN) {
            throw lifecycleRoleDenied();
        }

        ContentModuleVersionEntity version = resolveTargetVersion(module.getId(), request.operationType());
        applyOperation(version, request.operationType(), session.username());

        versionRepository.save(version);
        module.setUpdatedBy(session.username());
        moduleRepository.save(module);

        auditRecorder.recordContentModuleLifecycleOperation(
                module.getId(),
                module.getGroupCode(),
                module.getModuleCode(),
                request.operationType().name(),
                version.getSemanticVersion(),
                version.getLifecycleState().name(),
                session.username(),
                accessSupport.actorSummary(session),
                toAuditDetail(request.impactSummary())
        );

        return new ContentModuleLifecycleOperationResultView(
                true,
                null,
                null,
                toSnapshot(module, version),
                request.impactSummary()
        );
    }

    private void validateRequest(ContentModuleLifecycleOperationApplyRequest request) {
        if (request.operationType() == null || request.actorRole() == null
                || request.actorId() == null || request.actorId().isBlank()) {
            throw new ContentModuleGovernanceException(
                    "CONTENT_MODULE_REQUEST_INVALID",
                    "api.error.contentModule.lifecycleRequestInvalid",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }
        if (!Boolean.TRUE.equals(request.impactSummaryViewed())
                || !Boolean.TRUE.equals(request.secondConfirmation())) {
            throw new ContentModuleGovernanceException(
                    "CONTENT_MODULE_IMPACT_CONFIRMATION_REQUIRED",
                    "api.error.contentModule.impactConfirmationRequired",
                    HttpStatus.CONFLICT
            );
        }
        if ((request.operationType() == ContentModuleLifecycleOperation.STOP_USE
                || request.operationType() == ContentModuleLifecycleOperation.DEPRECATE)
                && request.impactSummary() == null) {
            throw new ContentModuleGovernanceException(
                    "CONTENT_MODULE_IMPACT_CONFIRMATION_REQUIRED",
                    "api.error.contentModule.impactConfirmationRequired",
                    HttpStatus.CONFLICT
            );
        }
    }

    private ContentModuleVersionEntity resolveTargetVersion(
            UUID moduleId,
            ContentModuleLifecycleOperation operation
    ) {
        List<ContentModuleVersionEntity> candidates = switch (operation) {
            case STOP_USE -> versionRepository.findByModuleIdAndReviewStateAndLifecycleStateOrderBySemanticVersionDesc(
                    moduleId,
                    ContentModuleReviewState.APPROVED,
                    ContentModuleLifecycleState.ACTIVE);
            case RECOVER -> versionRepository.findByModuleIdAndReviewStateAndLifecycleStateOrderBySemanticVersionDesc(
                    moduleId,
                    ContentModuleReviewState.APPROVED,
                    ContentModuleLifecycleState.STOPPED);
            case DEPRECATE -> versionRepository.findByModuleIdAndReviewStateAndLifecycleStateOrderBySemanticVersionDesc(
                    moduleId,
                    ContentModuleReviewState.APPROVED,
                    ContentModuleLifecycleState.STOPPED);
        };
        if (candidates.isEmpty()) {
            throw new ContentModuleGovernanceException(
                    "CONTENT_MODULE_STATE_TRANSITION_DENIED",
                    "api.error.contentModule.lifecycleStateTransitionDenied",
                    HttpStatus.CONFLICT
            );
        }
        return candidates.getFirst();
    }

    private void applyOperation(
            ContentModuleVersionEntity version,
            ContentModuleLifecycleOperation operation,
            String actorUsername
    ) {
        if (version.getReviewState() != ContentModuleReviewState.APPROVED) {
            throw stateDenied();
        }
        switch (operation) {
            case STOP_USE -> {
                if (version.getLifecycleState() != ContentModuleLifecycleState.ACTIVE) {
                    throw stateDenied();
                }
                version.setLifecycleState(ContentModuleLifecycleState.STOPPED);
            }
            case RECOVER -> {
                if (version.getLifecycleState() != ContentModuleLifecycleState.STOPPED) {
                    throw stateDenied();
                }
                version.setLifecycleState(ContentModuleLifecycleState.ACTIVE);
            }
            case DEPRECATE -> {
                if (version.getLifecycleState() != ContentModuleLifecycleState.STOPPED) {
                    throw stateDenied();
                }
                version.setLifecycleState(ContentModuleLifecycleState.DEPRECATED);
            }
            default -> throw stateDenied();
        }
        version.setUpdatedBy(actorUsername);
    }

    private ContentModuleGovernanceException lifecycleRoleDenied() {
        return new ContentModuleGovernanceException(
                "CONTENT_MODULE_ROLE_DENIED",
                "api.error.contentModule.lifecycleRoleDenied",
                HttpStatus.FORBIDDEN
        );
    }

    private ContentModuleGovernanceException stateDenied() {
        return new ContentModuleGovernanceException(
                "CONTENT_MODULE_STATE_TRANSITION_DENIED",
                "api.error.contentModule.lifecycleStateTransitionDenied",
                HttpStatus.CONFLICT
        );
    }

    private ContentModuleLifecycleSnapshotView toSnapshot(
            ContentModuleEntity module,
            ContentModuleVersionEntity version
    ) {
        return new ContentModuleLifecycleSnapshotView(
                accessSupport.publicModuleId(module),
                version.getLifecycleState().name(),
                version.getUpdatedAt(),
                version.getUpdatedBy()
        );
    }

    private ContentModuleLifecycleAuditDetail toAuditDetail(
            com.bank.docgen.contentmodule.api.ContentModuleLifecycleImpactSummaryView impactSummary
    ) {
        if (impactSummary == null) {
            return null;
        }
        return new ContentModuleLifecycleAuditDetail(
                impactSummary.referenceTemplateCount(),
                impactSummary.referenceTemplateListHint(),
                impactSummary.impactedReleaseVersionsHint(),
                impactSummary.defaultRouteAffected(),
                impactSummary.recentCallSummary(),
                impactSummary.remediationHint(),
                impactSummary.templateStopRequired(),
                impactSummary.releaseStopRequired()
        );
    }
}
