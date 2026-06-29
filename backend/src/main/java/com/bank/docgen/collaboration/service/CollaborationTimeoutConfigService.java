package com.bank.docgen.collaboration.service;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.collaboration.api.CollaborationTimeoutConfigView;
import com.bank.docgen.collaboration.api.UpsertCollaborationTimeoutConfigRequest;
import com.bank.docgen.collaboration.domain.CollaborationTimeoutScope;
import com.bank.docgen.collaboration.persistence.CollaborationTimeoutConfigEntity;
import com.bank.docgen.collaboration.persistence.CollaborationTimeoutConfigRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CollaborationTimeoutConfigService {

    private final CollaborationTimeoutConfigRepository repository;
    private final CollaborationTimeoutResolver resolver;
    private final GroupAccessService groupAccessService;
    private final ManagementAuditRecorder auditRecorder;

    public CollaborationTimeoutConfigService(
            CollaborationTimeoutConfigRepository repository,
            CollaborationTimeoutResolver resolver,
            GroupAccessService groupAccessService,
            ManagementAuditRecorder auditRecorder
    ) {
        this.repository = repository;
        this.resolver = resolver;
        this.groupAccessService = groupAccessService;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public CollaborationTimeoutConfigView resolve(String groupCode, ManagementSessionClaims session) {
        if (groupCode != null && !groupCode.isBlank()) {
            requireMaintainPermission(CollaborationTimeoutScope.GROUP, session);
            requireReadableGroup(groupCode, session);
            return resolver.resolveForGroup(groupCode);
        }
        requireMaintainPermission(CollaborationTimeoutScope.GLOBAL, session);
        return resolver.resolveGlobal();
    }

    @Transactional
    public CollaborationTimeoutConfigView upsert(
            UpsertCollaborationTimeoutConfigRequest request,
            ManagementSessionClaims session
    ) {
        CollaborationTimeoutScope scope = parseScope(request.scopeType());
        requireMaintainPermission(scope, session);
        if (scope == CollaborationTimeoutScope.GROUP) {
            requireReadableGroup(request.groupCode(), session);
        }

        CollaborationTimeoutConfigEntity entity = repository
                .findByScopeTypeAndGroupCode(scope, scope == CollaborationTimeoutScope.GLOBAL ? null : request.groupCode())
                .orElseGet(() -> new CollaborationTimeoutConfigEntity(
                        UUID.randomUUID(),
                        scope,
                        scope == CollaborationTimeoutScope.GLOBAL ? null : request.groupCode(),
                        request.testThresholdHours(),
                        request.approvalThresholdHours(),
                        request.pendingReleaseThresholdHours(),
                        request.remediationThresholdHours()
                ));
        entity.update(
                request.testThresholdHours(),
                request.approvalThresholdHours(),
                request.pendingReleaseThresholdHours(),
                request.remediationThresholdHours()
        );
        repository.save(entity);

        auditRecorder.recordCollaborationTimeoutConfigUpdated(
                scope.name(),
                entity.getGroupCode(),
                session.username(),
                session.displayName(),
                "test=" + request.testThresholdHours()
                        + ",approval=" + request.approvalThresholdHours()
                        + ",pendingRelease=" + request.pendingReleaseThresholdHours()
                        + ",remediation=" + request.remediationThresholdHours()
        );
        return resolver.toView(entity);
    }

    private void requireMaintainPermission(CollaborationTimeoutScope scope, ManagementSessionClaims session) {
        if (scope == CollaborationTimeoutScope.GLOBAL) {
            if (!session.roles().contains("GLOBAL_ADMIN")) {
                throw new CollaborationWorkItemAccessDeniedException();
            }
            return;
        }
        if (!groupAccessService.canMaintainCollaborationTimeoutConfig(session)) {
            throw new CollaborationWorkItemAccessDeniedException();
        }
    }

    private void requireReadableGroup(String groupCode, ManagementSessionClaims session) {
        if (groupCode == null || groupCode.isBlank()) {
            throw new CollaborationWorkItemValidationException("api.error.validation.requestBodyInvalid");
        }
        if (!groupAccessService.canAccessGroup(session, groupCode)) {
            throw new CollaborationWorkItemAccessDeniedException();
        }
    }

    private CollaborationTimeoutScope parseScope(String scopeType) {
        try {
            return CollaborationTimeoutScope.valueOf(scopeType);
        } catch (IllegalArgumentException ex) {
            throw new CollaborationWorkItemValidationException("api.error.validation.requestBodyInvalid");
        }
    }
}
