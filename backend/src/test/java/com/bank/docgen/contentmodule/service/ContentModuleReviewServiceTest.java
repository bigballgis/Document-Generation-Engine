package com.bank.docgen.contentmodule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ContentModuleReviewServiceTest {

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

    private ContentModuleReviewService reviewService;
    private ContentModuleAccessSupport accessSupport;
    private ContentModuleEntity module;
    private ContentModuleVersionEntity draftVersion;
    private ManagementSessionClaims author;
    private ManagementSessionClaims approver;

    @BeforeEach
    void setUp() {
        accessSupport = new ContentModuleAccessSupport(moduleRepository, groupAccessService, new ObjectMapper());
        reviewService = new ContentModuleReviewService(
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
                "10000003"
        );
        draftVersion = new ContentModuleVersionEntity(
                VERSION_ID,
                MODULE_ID,
                "1.0.0",
                "{\"blocks\":[]}",
                "Initial",
                "10000003"
        );
        author = session("10000003", List.of("TEMPLATE_AUTHOR"), List.of("RETAIL"));
        approver = session("10000005", List.of("TEMPLATE_APPROVER"), List.of("RETAIL"));
    }

    @Test
    void submitForReview_movesDraftToSubmitted() {
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(java.util.Optional.of(module));
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        when(versionRepository.findByModuleIdAndReviewStateOrderBySemanticVersionDesc(
                MODULE_ID, ContentModuleReviewState.DRAFT))
                .thenReturn(List.of(draftVersion));
        when(groupAccessService.canAuthorContentModules(author)).thenReturn(true);
        when(versionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(moduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = reviewService.transition(
                "MOD-LOAN-DISCLOSURE",
                new ContentModuleReviewTransitionRequest(
                        ContentModuleReviewOperation.SUBMIT_FOR_REVIEW,
                        ContentModuleGovernanceActorRole.TEMPLATE_AUTHOR,
                        "author-a",
                        "updated clause wording",
                        null
                ),
                author
        );

        assertThat(result.applied()).isTrue();
        assertThat(result.snapshot().state()).isEqualTo("SUBMITTED");
        assertThat(draftVersion.getReviewState()).isEqualTo(ContentModuleReviewState.SUBMITTED);
        verify(auditRecorder).recordContentModuleReviewTransition(
                any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void submitForReview_requiresChangeDescription() {
        assertThatThrownBy(() -> reviewService.transition(
                "MOD-LOAN-DISCLOSURE",
                new ContentModuleReviewTransitionRequest(
                        ContentModuleReviewOperation.SUBMIT_FOR_REVIEW,
                        ContentModuleGovernanceActorRole.TEMPLATE_AUTHOR,
                        "author-a",
                        " ",
                        null
                ),
                author
        ))
                .isInstanceOf(ContentModuleGovernanceException.class)
                .satisfies(ex -> {
                    ContentModuleGovernanceException governance = (ContentModuleGovernanceException) ex;
                    assertThat(governance.errorCode()).isEqualTo("MODULE_CHANGE_DESCRIPTION_REQUIRED");
                    assertThat(governance.httpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                });
    }

    @Test
    void approveReview_setsApprovedAndActive() {
        draftVersion.setReviewState(ContentModuleReviewState.SUBMITTED);
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(java.util.Optional.of(module));
        when(groupAccessService.canAccessGroup(approver, "RETAIL")).thenReturn(true);
        when(versionRepository.findByModuleIdAndReviewStateOrderBySemanticVersionDesc(
                MODULE_ID, ContentModuleReviewState.SUBMITTED))
                .thenReturn(List.of(draftVersion));
        when(groupAccessService.canDecideContentModuleReviews(approver)).thenReturn(true);
        when(versionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(moduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = reviewService.transition(
                "MOD-LOAN-DISCLOSURE",
                new ContentModuleReviewTransitionRequest(
                        ContentModuleReviewOperation.APPROVE_REVIEW,
                        ContentModuleGovernanceActorRole.APPROVER,
                        "approver-a",
                        null,
                        null
                ),
                approver
        );

        assertThat(result.snapshot().state()).isEqualTo("APPROVED");
        assertThat(draftVersion.getLifecycleState()).isEqualTo(ContentModuleLifecycleState.ACTIVE);
    }

    @Test
    void rejectReview_returnsDraftWithReason() {
        draftVersion.setReviewState(ContentModuleReviewState.SUBMITTED);
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(java.util.Optional.of(module));
        when(groupAccessService.canAccessGroup(approver, "RETAIL")).thenReturn(true);
        when(versionRepository.findByModuleIdAndReviewStateOrderBySemanticVersionDesc(
                MODULE_ID, ContentModuleReviewState.SUBMITTED))
                .thenReturn(List.of(draftVersion));
        when(groupAccessService.canDecideContentModuleReviews(approver)).thenReturn(true);
        when(versionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(moduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = reviewService.transition(
                "MOD-LOAN-DISCLOSURE",
                new ContentModuleReviewTransitionRequest(
                        ContentModuleReviewOperation.REJECT_REVIEW,
                        ContentModuleGovernanceActorRole.APPROVER,
                        "approver-a",
                        null,
                        "wording not acceptable"
                ),
                approver
        );

        assertThat(result.snapshot().state()).isEqualTo("DRAFT");
        assertThat(result.snapshot().rejectionReason()).isEqualTo("wording not acceptable");
    }

    @Test
    void authorCannotApproveReview() {
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(java.util.Optional.of(module));
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        when(groupAccessService.canDecideContentModuleReviews(author)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.transition(
                "MOD-LOAN-DISCLOSURE",
                new ContentModuleReviewTransitionRequest(
                        ContentModuleReviewOperation.APPROVE_REVIEW,
                        ContentModuleGovernanceActorRole.TEMPLATE_AUTHOR,
                        "author-a",
                        null,
                        null
                ),
                author
        ))
                .isInstanceOf(ContentModuleGovernanceException.class)
                .extracting(ex -> ((ContentModuleGovernanceException) ex).errorCode())
                .isEqualTo("MODULE_REVIEW_ROLE_DENIED");
    }

    @Test
    void submitDeniedWhenNoDraftVersion() {
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(java.util.Optional.of(module));
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        when(versionRepository.findByModuleIdAndReviewStateOrderBySemanticVersionDesc(
                MODULE_ID, ContentModuleReviewState.DRAFT))
                .thenReturn(List.of());
        when(groupAccessService.canAuthorContentModules(author)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.transition(
                "MOD-LOAN-DISCLOSURE",
                new ContentModuleReviewTransitionRequest(
                        ContentModuleReviewOperation.SUBMIT_FOR_REVIEW,
                        ContentModuleGovernanceActorRole.TEMPLATE_AUTHOR,
                        "author-a",
                        "change",
                        null
                ),
                author
        ))
                .isInstanceOf(ContentModuleGovernanceException.class)
                .extracting(ex -> ((ContentModuleGovernanceException) ex).errorCode())
                .isEqualTo("MODULE_REVIEW_STATE_TRANSITION_DENIED");
    }

    @Test
    void rejectReview_requiresRejectionReason() {
        assertThatThrownBy(() -> reviewService.transition(
                "MOD-LOAN-DISCLOSURE",
                new ContentModuleReviewTransitionRequest(
                        ContentModuleReviewOperation.REJECT_REVIEW,
                        ContentModuleGovernanceActorRole.APPROVER,
                        "approver-a",
                        null,
                        " "
                ),
                approver
        ))
                .isInstanceOf(ContentModuleGovernanceException.class)
                .extracting(ex -> ((ContentModuleGovernanceException) ex).errorCode())
                .isEqualTo("MODULE_REJECTION_REASON_REQUIRED");
    }

    @Test
    void invalidRequest_rejectsMissingActorId() {
        assertThatThrownBy(() -> reviewService.transition(
                "MOD-LOAN-DISCLOSURE",
                new ContentModuleReviewTransitionRequest(
                        ContentModuleReviewOperation.SUBMIT_FOR_REVIEW,
                        ContentModuleGovernanceActorRole.TEMPLATE_AUTHOR,
                        " ",
                        "change",
                        null
                ),
                author
        ))
                .isInstanceOf(ContentModuleGovernanceException.class)
                .extracting(ex -> ((ContentModuleGovernanceException) ex).errorCode())
                .isEqualTo("MODULE_REVIEW_REQUEST_INVALID");
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
