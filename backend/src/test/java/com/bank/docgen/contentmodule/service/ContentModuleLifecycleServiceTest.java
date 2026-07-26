package com.bank.docgen.contentmodule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.api.ContentModuleLifecycleAuditDetail;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.api.ContentModuleLifecycleImpactSummaryView;
import com.bank.docgen.contentmodule.api.ContentModuleLifecycleOperationApplyRequest;
import com.bank.docgen.contentmodule.domain.ContentModuleGovernanceActorRole;
import com.bank.docgen.contentmodule.domain.ContentModuleLifecycleOperation;
import com.bank.docgen.contentmodule.domain.ContentModuleLifecycleState;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewState;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ContentModuleLifecycleServiceTest {

    private static final UUID MODULE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID VERSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private ContentModuleRepository moduleRepository;
    @Mock
    private ContentModuleVersionRepository versionRepository;
    @Mock
    private GroupAccessService groupAccessService;
    @Mock
    private ManagementAuditRecorder auditRecorder;

    private ContentModuleLifecycleService lifecycleService;
    private ContentModuleAccessService accessSupport;
    private ContentModuleEntity module;
    private ContentModuleVersionEntity activeVersion;
    private ManagementSessionClaims groupAdmin;

    @BeforeEach
    void setUp() {
        accessSupport = new ContentModuleAccessService(moduleRepository, groupAccessService, new ObjectMapper());
        lifecycleService = new ContentModuleLifecycleService(
                moduleRepository,
                versionRepository,
                groupAccessService,
                accessSupport,
                auditRecorder
        );
        module = new ContentModuleEntity(
                MODULE_ID,
                "MOD-LOAN-DISCLOSURE",
                "RETAIL",
                "Loan Disclosure",
                "desc",
                "[]",
                "10000002"
        );
        activeVersion = approvedActiveVersion();
        groupAdmin = session("10000002", List.of("GROUP_ADMIN"), List.of("RETAIL"));
    }

    @Test
    void stopUse_movesActiveToStopped() {
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(java.util.Optional.of(module));
        when(groupAccessService.canAccessGroup(groupAdmin, "RETAIL")).thenReturn(true);
        when(versionRepository.findById(VERSION_ID)).thenReturn(java.util.Optional.of(activeVersion));
        when(groupAccessService.canManageContentModuleLifecycle(groupAdmin)).thenReturn(true);
        when(versionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(moduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var impact = sampleImpact();
        var result = lifecycleService.apply(
                "MOD-LOAN-DISCLOSURE",
                new ContentModuleLifecycleOperationApplyRequest(
                        ContentModuleLifecycleOperation.STOP_USE,
                        ContentModuleGovernanceActorRole.GROUP_ADMIN,
                        "group-admin-a",
                        true,
                        true,
                        impact,
                        VERSION_ID,
                        null
                ),
                groupAdmin
        );

        assertThat(result.applied()).isTrue();
        assertThat(result.snapshot().state()).isEqualTo("STOPPED");
        assertThat(result.impactSummary()).isEqualTo(impact);
        ArgumentCaptor<ContentModuleLifecycleAuditDetail> auditDetailCaptor =
                ArgumentCaptor.forClass(ContentModuleLifecycleAuditDetail.class);
        verify(auditRecorder).recordContentModuleLifecycleOperation(
                eq(MODULE_ID),
                eq("RETAIL"),
                eq("MOD-LOAN-DISCLOSURE"),
                eq("STOP_USE"),
                eq("1.0.0"),
                eq("STOPPED"),
                eq("10000002"),
                any(),
                auditDetailCaptor.capture()
        );
        ContentModuleLifecycleAuditDetail auditDetail = auditDetailCaptor.getValue();
        assertThat(auditDetail.referenceTemplateCount()).isEqualTo(2);
        assertThat(auditDetail.recentCallSummary()).isEqualTo("recentCalls=12/7d");
        assertThat(auditDetail.templateStopRequired()).isTrue();
        assertThat(auditDetail.releaseStopRequired()).isTrue();
    }

    @Test
    void recover_movesStoppedToActive() {
        activeVersion.setLifecycleState(ContentModuleLifecycleState.STOPPED);
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(java.util.Optional.of(module));
        when(groupAccessService.canAccessGroup(groupAdmin, "RETAIL")).thenReturn(true);
        when(versionRepository.findById(VERSION_ID)).thenReturn(java.util.Optional.of(activeVersion));
        when(groupAccessService.canManageContentModuleLifecycle(groupAdmin)).thenReturn(true);
        when(versionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(moduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = lifecycleService.apply(
                "MOD-LOAN-DISCLOSURE",
                new ContentModuleLifecycleOperationApplyRequest(
                        ContentModuleLifecycleOperation.RECOVER,
                        ContentModuleGovernanceActorRole.GROUP_ADMIN,
                        "group-admin-a",
                        true,
                        true,
                        null,
                        VERSION_ID,
                        null
                ),
                groupAdmin
        );

        assertThat(result.snapshot().state()).isEqualTo("ACTIVE");
    }

    @Test
    void deprecateFromStopped_isTerminal() {
        activeVersion.setLifecycleState(ContentModuleLifecycleState.STOPPED);
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(java.util.Optional.of(module));
        when(groupAccessService.canAccessGroup(groupAdmin, "RETAIL")).thenReturn(true);
        when(versionRepository.findById(VERSION_ID)).thenReturn(java.util.Optional.of(activeVersion));
        when(groupAccessService.canManageContentModuleLifecycle(groupAdmin)).thenReturn(true);
        when(versionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(moduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = lifecycleService.apply(
                "MOD-LOAN-DISCLOSURE",
                new ContentModuleLifecycleOperationApplyRequest(
                        ContentModuleLifecycleOperation.DEPRECATE,
                        ContentModuleGovernanceActorRole.GROUP_ADMIN,
                        "group-admin-a",
                        true,
                        true,
                        sampleImpact(),
                        VERSION_ID,
                        null
                ),
                groupAdmin
        );

        assertThat(result.snapshot().state()).isEqualTo("DEPRECATED");
    }

    @Test
    void recoverDeniedFromDeprecated() {
        activeVersion.setLifecycleState(ContentModuleLifecycleState.DEPRECATED);
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(java.util.Optional.of(module));
        when(groupAccessService.canAccessGroup(groupAdmin, "RETAIL")).thenReturn(true);
        when(versionRepository.findById(VERSION_ID)).thenReturn(java.util.Optional.of(activeVersion));
        when(groupAccessService.canManageContentModuleLifecycle(groupAdmin)).thenReturn(true);

        assertThatThrownBy(() -> lifecycleService.apply(
                "MOD-LOAN-DISCLOSURE",
                new ContentModuleLifecycleOperationApplyRequest(
                        ContentModuleLifecycleOperation.RECOVER,
                        ContentModuleGovernanceActorRole.GROUP_ADMIN,
                        "group-admin-a",
                        true,
                        true,
                        null,
                        VERSION_ID,
                        null
                ),
                groupAdmin
        ))
                .isInstanceOf(ContentModuleGovernanceException.class)
                .extracting(ex -> ((ContentModuleGovernanceException) ex).errorCode())
                .isEqualTo("CONTENT_MODULE_STATE_TRANSITION_DENIED");
    }

    @Test
    void stopUseRequiresImpactSummary() {
        assertThatThrownBy(() -> lifecycleService.apply(
                "MOD-LOAN-DISCLOSURE",
                new ContentModuleLifecycleOperationApplyRequest(
                        ContentModuleLifecycleOperation.STOP_USE,
                        ContentModuleGovernanceActorRole.GROUP_ADMIN,
                        "group-admin-a",
                        true,
                        true,
                        null,
                        VERSION_ID,
                        null
                ),
                groupAdmin
        ))
                .isInstanceOf(ContentModuleGovernanceException.class)
                .extracting(ex -> ((ContentModuleGovernanceException) ex).errorCode())
                .isEqualTo("CONTENT_MODULE_IMPACT_CONFIRMATION_REQUIRED");
    }

    @Test
    void authorCannotStopUse() {
        ManagementSessionClaims author = session("10000003", List.of("DOCUMENT_AUTHOR"), List.of("RETAIL"));
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(java.util.Optional.of(module));
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);

        assertThatThrownBy(() -> lifecycleService.apply(
                "MOD-LOAN-DISCLOSURE",
                new ContentModuleLifecycleOperationApplyRequest(
                        ContentModuleLifecycleOperation.STOP_USE,
                        ContentModuleGovernanceActorRole.DOCUMENT_AUTHOR,
                        "author-a",
                        true,
                        true,
                        sampleImpact(),
                        VERSION_ID,
                        null
                ),
                author
        ))
                .isInstanceOf(ContentModuleGovernanceException.class)
                .extracting(ex -> ((ContentModuleGovernanceException) ex).errorCode())
                .isEqualTo("CONTENT_MODULE_ROLE_DENIED");
    }

    @Test
    void requiresSecondConfirmation() {
        assertThatThrownBy(() -> lifecycleService.apply(
                "MOD-LOAN-DISCLOSURE",
                new ContentModuleLifecycleOperationApplyRequest(
                        ContentModuleLifecycleOperation.RECOVER,
                        ContentModuleGovernanceActorRole.GROUP_ADMIN,
                        "group-admin-a",
                        true,
                        false,
                        null,
                        VERSION_ID,
                        null
                ),
                groupAdmin
        ))
                .isInstanceOf(ContentModuleGovernanceException.class)
                .satisfies(ex -> {
                    ContentModuleGovernanceException governance = (ContentModuleGovernanceException) ex;
                    assertThat(governance.errorCode()).isEqualTo("CONTENT_MODULE_IMPACT_CONFIRMATION_REQUIRED");
                    assertThat(governance.httpStatus()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    void invalidRequest_rejectsMissingOperationType() {
        assertThatThrownBy(() -> lifecycleService.apply(
                "MOD-LOAN-DISCLOSURE",
                new ContentModuleLifecycleOperationApplyRequest(
                        null,
                        ContentModuleGovernanceActorRole.GROUP_ADMIN,
                        "group-admin-a",
                        true,
                        true,
                        null
                ),
                groupAdmin
        ))
                .isInstanceOf(ContentModuleGovernanceException.class)
                .extracting(ex -> ((ContentModuleGovernanceException) ex).errorCode())
                .isEqualTo("CONTENT_MODULE_REQUEST_INVALID");
    }

    private ContentModuleVersionEntity approvedActiveVersion() {
        ContentModuleVersionEntity version = new ContentModuleVersionEntity(
                VERSION_ID,
                MODULE_ID,
                "1.0.0",
                "{}",
                "approved",
                "10000002"
        );
        version.setReviewState(ContentModuleReviewState.APPROVED);
        version.setLifecycleState(ContentModuleLifecycleState.ACTIVE);
        return version;
    }

    private ContentModuleLifecycleImpactSummaryView sampleImpact() {
        return new ContentModuleLifecycleImpactSummaryView(
                2,
                "TPL-LOAN-NOTICE,TPL-RENEWAL-NOTICE",
                "v1.0.0,v1.1.0",
                true,
                "recentCalls=12/7d",
                "migrate callers to MOD-LOAN-DISCLOSURE-V3",
                true,
                true
        );
    }

    private ManagementSessionClaims session(String username, List<String> roles, List<String> groups) {
        return new ManagementSessionClaims(
                username,
                username,
                username + "@example.com",
                AuthSource.LOCAL,
                roles,
                groups,
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                Instant.now().plusSeconds(3600)
        );
    }
}
