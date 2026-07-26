package com.bank.docgen.contentmodule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.api.ContentModuleReviewTransitionRequest;
import com.bank.docgen.contentmodule.domain.ContentModuleGovernanceActorRole;
import com.bank.docgen.contentmodule.domain.ContentModuleLifecycleState;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewOperation;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewState;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleReviewRecordRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.LifecycleAuthorizationException;
import com.bank.docgen.sharedkernel.lifecycle.SelfApprovalGuard;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CE-G01 acceptance: content-module review self-approval block
 * (BDD-CE-G01-C-001..003 + audit payload + Q1 submittedBy).
 */
@ExtendWith(MockitoExtension.class)
class ContentModuleSelfApprovalBlockTest {

    private static final UUID MODULE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID VERSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock private ContentModuleRepository moduleRepository;
    @Mock private ContentModuleVersionRepository versionRepository;
    @Mock private ContentModuleReviewRecordRepository reviewRecordRepository;
    @Mock private GroupAccessService groupAccessService;
    @Mock private ManagementAuditRecorder auditRecorder;

    private ContentModuleReviewService reviewService;
    private ContentModuleAccessService accessSupport;
    private ContentModuleEntity module;
    private ContentModuleVersionEntity submittedVersion;

    @BeforeEach
    void setUp() {
        accessSupport = new ContentModuleAccessService(moduleRepository, groupAccessService, new ObjectMapper());
        reviewService = new ContentModuleReviewService(
                moduleRepository, versionRepository, reviewRecordRepository, groupAccessService, accessSupport,
                auditRecorder, new SelfApprovalGuard());
        module = new ContentModuleEntity(MODULE_ID, "MOD-LOAN", "RETAIL", "Loan", "desc", "[]", "10000003");
        submittedVersion = new ContentModuleVersionEntity(VERSION_ID, MODULE_ID, "1.0.0",
                "{\"blocks\":[]}", "Initial", "10000003");
        submittedVersion.setReviewState(ContentModuleReviewState.SUBMITTED);
        submittedVersion.setSubmittedBy("alice");
    }

    @Test
    void c001_sameActorApprove_isBlocked403_andStateUnchanged() {
        ManagementSessionClaims alice = session("alice", List.of("GROUP_ADMIN"));
        stubReadableAndDecide(alice);

        assertThatThrownBy(() -> reviewService.transition("MOD-LOAN",
                new ContentModuleReviewTransitionRequest(
                        ContentModuleReviewOperation.APPROVE_REVIEW, ContentModuleGovernanceActorRole.APPROVER, "approver-a", null, null,
                        VERSION_ID, null),
                alice))
                .isInstanceOf(LifecycleAuthorizationException.class)
                .satisfies(ex -> {
                    LifecycleAuthorizationException e = (LifecycleAuthorizationException) ex;
                    assertThat(e.errorCode()).isEqualTo(ApiErrorCodes.SELF_APPROVAL_FORBIDDEN);
                    assertThat(e.messageKey()).isEqualTo("api.error.lifecycle.selfApprovalForbidden");
                    assertThat(e.httpStatus().value()).isEqualTo(403);
                });

        assertThat(submittedVersion.getReviewState()).isEqualTo(ContentModuleReviewState.SUBMITTED);
        verify(auditRecorder, never()).recordContentModuleReviewTransition(
                any(), any(), any(), any(), any(), any(), any(), any(), any(Boolean.class), any());
    }

    @Test
    void c002_sameActorGroupAdminException_isAllowedAndAuditsException() {
        ManagementSessionClaims alice = session("alice", List.of("GROUP_ADMIN"));
        stubReadableAndDecide(alice);
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(moduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        reviewService.transition("MOD-LOAN",
                new ContentModuleReviewTransitionRequest(
                        ContentModuleReviewOperation.APPROVE_REVIEW, ContentModuleGovernanceActorRole.GROUP_ADMIN, "admin-a", null, null,
                        true, "Solo approval due to approver unavailability", true, VERSION_ID, null),
                alice);

        assertThat(submittedVersion.getReviewState()).isEqualTo(ContentModuleReviewState.APPROVED);
        assertThat(submittedVersion.getLifecycleState()).isEqualTo(ContentModuleLifecycleState.ACTIVE);
        verify(auditRecorder).recordContentModuleReviewTransition(
                eq(MODULE_ID), any(), any(), eq("APPROVE_REVIEW"), any(), any(), eq("alice"), any(),
                eq(true), eq("Solo approval due to approver unavailability"));
    }

    @Test
    void c003_differentActorApprove_succeedsWithoutException() {
        ManagementSessionClaims bob = session("bob", List.of("GROUP_ADMIN"));
        stubReadableAndDecide(bob);
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(moduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        reviewService.transition("MOD-LOAN",
                new ContentModuleReviewTransitionRequest(
                        ContentModuleReviewOperation.APPROVE_REVIEW, ContentModuleGovernanceActorRole.APPROVER, "approver-b", null, null,
                        VERSION_ID, null),
                bob);

        assertThat(submittedVersion.getReviewState()).isEqualTo(ContentModuleReviewState.APPROVED);
        verify(auditRecorder).recordContentModuleReviewTransition(
                any(), any(), any(), any(), any(), any(), any(), any(), eq(false), any());
    }

    @Test
    void x006_sameActorReject_alsoBlocked() {
        ManagementSessionClaims alice = session("alice", List.of("GROUP_ADMIN"));
        stubReadableAndDecide(alice);

        assertThatThrownBy(() -> reviewService.transition("MOD-LOAN",
                new ContentModuleReviewTransitionRequest(
                        ContentModuleReviewOperation.REJECT_REVIEW, ContentModuleGovernanceActorRole.APPROVER, "approver-a", null, "needs fix",
                        VERSION_ID, null),
                alice))
                .isInstanceOf(LifecycleAuthorizationException.class)
                .satisfies(ex -> assertThat(((LifecycleAuthorizationException) ex).errorCode())
                        .isEqualTo(ApiErrorCodes.SELF_APPROVAL_FORBIDDEN));
        assertThat(submittedVersion.getReviewState()).isEqualTo(ContentModuleReviewState.SUBMITTED);
    }

    @Test
    void submitForReview_persistsSubmittedBy() {
        ManagementSessionClaims alice = session("alice", List.of("DOCUMENT_AUTHOR"));
        ContentModuleVersionEntity draft = new ContentModuleVersionEntity(VERSION_ID, MODULE_ID, "1.0.0",
                "{\"blocks\":[]}", "Initial", "10000003");
        draft.setReviewState(ContentModuleReviewState.DRAFT);
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN"))
                .thenReturn(java.util.Optional.of(module));
        when(groupAccessService.canAccessGroup(alice, "RETAIL")).thenReturn(true);
        when(groupAccessService.canAuthorContentModules(alice)).thenReturn(true);
        when(versionRepository.findById(VERSION_ID)).thenReturn(java.util.Optional.of(draft));
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(moduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        reviewService.transition("MOD-LOAN",
                new ContentModuleReviewTransitionRequest(
                        ContentModuleReviewOperation.SUBMIT_FOR_REVIEW, ContentModuleGovernanceActorRole.DOCUMENT_AUTHOR, "author-a", "Ready for review", null,
                        VERSION_ID, null),
                alice);

        assertThat(draft.getReviewState()).isEqualTo(ContentModuleReviewState.SUBMITTED);
        assertThat(draft.getSubmittedBy()).isEqualTo("alice");
    }

    private void stubReadableAndDecide(ManagementSessionClaims session) {
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN"))
                .thenReturn(java.util.Optional.of(module));
        when(groupAccessService.canAccessGroup(session, "RETAIL")).thenReturn(true);
        when(groupAccessService.canDecideContentModuleReviews(session)).thenReturn(true);
        when(versionRepository.findById(VERSION_ID)).thenReturn(java.util.Optional.of(submittedVersion));
    }

    private ManagementSessionClaims session(String username, List<String> roles) {
        return new ManagementSessionClaims(
                username, username, username + "@example.com", AuthSource.LOCAL,
                roles, List.of("RETAIL"), "route.dashboard-home",
                List.of("route.dashboard-home"), Instant.now().plusSeconds(3600));
    }
}
