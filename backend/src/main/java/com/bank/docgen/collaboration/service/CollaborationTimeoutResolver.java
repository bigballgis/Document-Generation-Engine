package com.bank.docgen.collaboration.service;

import com.bank.docgen.collaboration.api.CollaborationTimeoutConfigView;
import com.bank.docgen.collaboration.domain.CollaborationTimeoutScope;
import com.bank.docgen.collaboration.persistence.CollaborationTimeoutConfigEntity;
import com.bank.docgen.collaboration.persistence.CollaborationTimeoutConfigRepository;
import org.springframework.stereotype.Component;

@Component
public class CollaborationTimeoutResolver {

    static final int DEFAULT_TEST_THRESHOLD_HOURS = 72;
    static final int DEFAULT_APPROVAL_THRESHOLD_HOURS = 72;
    static final int DEFAULT_PENDING_RELEASE_THRESHOLD_HOURS = 48;
    static final int DEFAULT_REMEDIATION_THRESHOLD_HOURS = 168;

    private final CollaborationTimeoutConfigRepository repository;

    public CollaborationTimeoutResolver(CollaborationTimeoutConfigRepository repository) {
        this.repository = repository;
    }

    public CollaborationTimeoutConfigView resolveForGroup(String groupCode) {
        return repository.findByScopeTypeAndGroupCode(CollaborationTimeoutScope.GROUP, groupCode)
                .map(this::toView)
                .orElseGet(this::globalOrDefault);
    }

    public CollaborationTimeoutConfigView resolveGlobal() {
        return globalOrDefault();
    }

    private CollaborationTimeoutConfigView globalOrDefault() {
        return repository.findByScopeTypeAndGroupCode(CollaborationTimeoutScope.GLOBAL, null)
                .map(this::toView)
                .orElse(new CollaborationTimeoutConfigView(
                        CollaborationTimeoutScope.GLOBAL.name(),
                        null,
                        DEFAULT_TEST_THRESHOLD_HOURS,
                        DEFAULT_APPROVAL_THRESHOLD_HOURS,
                        DEFAULT_PENDING_RELEASE_THRESHOLD_HOURS,
                        DEFAULT_REMEDIATION_THRESHOLD_HOURS,
                        null
                ));
    }

    public CollaborationTimeoutConfigView toView(CollaborationTimeoutConfigEntity entity) {
        return new CollaborationTimeoutConfigView(
                entity.getScopeType().name(),
                entity.getGroupCode(),
                entity.getTestThresholdHours(),
                entity.getApprovalThresholdHours(),
                entity.getPendingReleaseThresholdHours(),
                entity.getRemediationThresholdHours(),
                entity.getUpdatedAt().toString()
        );
    }
}
