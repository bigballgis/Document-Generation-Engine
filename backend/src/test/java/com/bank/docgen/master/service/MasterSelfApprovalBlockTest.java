package com.bank.docgen.master.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.api.DecideMasterReviewRequest;
import com.bank.docgen.master.api.MasterDocumentDetailView;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.domain.MasterReviewAction;
import com.bank.docgen.master.persistence.MasterAnchorRepository;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterReviewRecordEntity;
import com.bank.docgen.master.persistence.MasterReviewRecordRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.master.rendering.DocxAnchorExtractor;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.LifecycleAuthorizationException;
import com.bank.docgen.sharedkernel.lifecycle.SelfApprovalGuard;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CE-G01 acceptance: master review self-approval block (BDD-CE-G01-M-001..003, X-006).
 */
@ExtendWith(MockitoExtension.class)
class MasterSelfApprovalBlockTest {

    @Mock private MasterDocumentRepository masterDocumentRepository;
    @Mock private MasterAnchorRepository masterAnchorRepository;
    @Mock private MasterReviewRecordRepository masterReviewRecordRepository;
    @Mock private MasterRevisionLineRepository masterRevisionLineRepository;
    @Mock private ObjectStoragePort objectStoragePort;
    @Mock private DocxAnchorExtractor docxAnchorExtractor;
    @Mock private GroupAccessService groupAccessService;
    @Mock private ManagementUserDisplayService managementUserDisplayService;

    private MasterDocumentService service;
    private UUID masterId;
    private MasterDocumentEntity master;

    @BeforeEach
    void setUp() {
        service = new MasterDocumentService(
                masterDocumentRepository,
                masterAnchorRepository,
                masterReviewRecordRepository,
                masterRevisionLineRepository,
                objectStoragePort,
                docxAnchorExtractor,
                groupAccessService,
                managementUserDisplayService,
                new SelfApprovalGuard(),
                new com.fasterxml.jackson.databind.ObjectMapper(),
                4096L
        );
        masterId = UUID.randomUUID();
        master = new MasterDocumentEntity(masterId, "RETAIL", "Letterhead", "desc",
                "storage/key", "letter.docx", "10000003");
        master.setStatus(MasterDocumentStatus.PENDING_REVIEW);
    }

    @Test
    void m001_sameActorApproval_isBlocked403_andStateUnchanged() {
        ManagementSessionClaims alice = session("alice", List.of("MASTER_DESIGNER"));
        stubReadable(alice);
        when(groupAccessService.canReviewMasters(alice)).thenReturn(true);
        stubLatestSubmitter("alice");

        assertThatThrownBy(() -> service.decideReview(masterId,
                new DecideMasterReviewRequest("APPROVED", "ok"), alice))
                .isInstanceOf(LifecycleAuthorizationException.class)
                .satisfies(ex -> {
                    LifecycleAuthorizationException e = (LifecycleAuthorizationException) ex;
                    assertThat(e.errorCode()).isEqualTo(ApiErrorCodes.SELF_APPROVAL_FORBIDDEN);
                    assertThat(e.messageKey()).isEqualTo("api.error.lifecycle.selfApprovalForbidden");
                    assertThat(e.httpStatus().value()).isEqualTo(403);
                });

        assertThat(master.getStatus()).isEqualTo(MasterDocumentStatus.PENDING_REVIEW);
        verify(masterReviewRecordRepository, never()).save(argThat(r ->
                r.getAction() == MasterReviewAction.APPROVED || r.getAction() == MasterReviewAction.REJECTED));
    }

    @Test
    void m002_sameActorGroupAdminException_isAllowedAndPersistsExceptionAudit() {
        ManagementSessionClaims alice = session("alice", List.of("GROUP_ADMIN"));
        stubReadable(alice);
        when(groupAccessService.canReviewMasters(alice)).thenReturn(true);
        stubLatestSubmitter("alice");
        when(masterReviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MasterDocumentDetailView result = service.decideReview(masterId,
                new DecideMasterReviewRequest("APPROVED", "ok", true,
                        "Solo approval due to reviewer unavailability", true), alice);

        assertThat(master.getStatus()).isEqualTo(MasterDocumentStatus.APPROVED);
        ArgumentCaptor<MasterReviewRecordEntity> captor =
                ArgumentCaptor.forClass(MasterReviewRecordEntity.class);
        verify(masterReviewRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(MasterReviewAction.APPROVED);
        assertThat(captor.getValue().isSelfApprovalException()).isTrue();
        assertThat(captor.getValue().getExceptionReason())
                .isEqualTo("Solo approval due to reviewer unavailability");
    }

    @Test
    void m003_differentActorApproval_succeedsWithoutException() {
        ManagementSessionClaims bob = session("bob", List.of("MASTER_DESIGNER"));
        stubReadable(bob);
        when(groupAccessService.canReviewMasters(bob)).thenReturn(true);
        stubLatestSubmitter("alice");
        when(masterReviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.decideReview(masterId, new DecideMasterReviewRequest("APPROVED", "ok"), bob);

        assertThat(master.getStatus()).isEqualTo(MasterDocumentStatus.APPROVED);
        ArgumentCaptor<MasterReviewRecordEntity> captor =
                ArgumentCaptor.forClass(MasterReviewRecordEntity.class);
        verify(masterReviewRecordRepository).save(captor.capture());
        assertThat(captor.getValue().isSelfApprovalException()).isFalse();
        assertThat(captor.getValue().getExceptionReason()).isNull();
    }

    @Test
    void x006_sameActorReject_alsoBlocked() {
        ManagementSessionClaims alice = session("alice", List.of("MASTER_DESIGNER"));
        stubReadable(alice);
        when(groupAccessService.canReviewMasters(alice)).thenReturn(true);
        stubLatestSubmitter("alice");

        assertThatThrownBy(() -> service.decideReview(masterId,
                new DecideMasterReviewRequest("REJECTED", "needs work"), alice))
                .isInstanceOf(LifecycleAuthorizationException.class)
                .satisfies(ex -> assertThat(((LifecycleAuthorizationException) ex).errorCode())
                        .isEqualTo(ApiErrorCodes.SELF_APPROVAL_FORBIDDEN));
        assertThat(master.getStatus()).isEqualTo(MasterDocumentStatus.PENDING_REVIEW);
    }

    @Test
    void x001_noSubmitRecord_doesNotBlock() {
        ManagementSessionClaims alice = session("alice", List.of("MASTER_DESIGNER"));
        stubReadable(alice);
        when(groupAccessService.canReviewMasters(alice)).thenReturn(true);
        when(masterReviewRecordRepository.findByMasterIdOrderByCreatedAtDesc(masterId))
                .thenReturn(List.of());
        when(masterReviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.decideReview(masterId, new DecideMasterReviewRequest("APPROVED", "ok"), alice);
        assertThat(master.getStatus()).isEqualTo(MasterDocumentStatus.APPROVED);
    }

    private void stubReadable(ManagementSessionClaims session) {
        when(masterDocumentRepository.findWithAnchorsByIdAndDeletedAtIsNull(masterId))
                .thenReturn(Optional.of(master));
        when(groupAccessService.canAccessGroup(session, "RETAIL")).thenReturn(true);
    }

    private void stubLatestSubmitter(String actor) {
        when(masterReviewRecordRepository.findByMasterIdOrderByCreatedAtDesc(masterId))
                .thenReturn(List.of(submitRecord(actor)));
    }

    private MasterReviewRecordEntity submitRecord(String actor) {
        return new MasterReviewRecordEntity(UUID.randomUUID(), masterId, MasterReviewAction.SUBMITTED,
                null, null, "change", actor);
    }

    private ManagementSessionClaims session(String username, List<String> roles) {
        return new ManagementSessionClaims(
                username, username, username + "@example.com", AuthSource.LOCAL,
                roles, List.of("RETAIL"), "route.master-home",
                List.of("route.master-home"), Instant.now().plusSeconds(3600));
    }
}
