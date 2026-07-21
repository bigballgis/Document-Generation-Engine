package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.apimgmt.service.ApiPolicyMaterializationService;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.authoring.structured.RenderProfileService;
import com.bank.docgen.collaboration.service.CollaborationWorkItemWriter;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.LifecycleAuthorizationException;
import com.bank.docgen.sharedkernel.lifecycle.SelfApprovalGuard;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.LifecycleDecisionRequest;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.domain.ApprovalSubState;
import com.bank.docgen.template.domain.LifecycleAction;
import com.bank.docgen.template.domain.LifecycleDecision;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

/**
 * CE-G01 acceptance: template self-approval block (BDD-CE-G01-T-001..005, X-002, X-006, X-007).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TemplateSelfApprovalBlockTest {

    @Mock private TemplateService templateService;
    @Mock private TemplateRepository templateRepository;
    @Mock private TemplateVersionRepository templateVersionRepository;
    @Mock private TemplateLifecycleRecordRepository lifecycleRecordRepository;
    @Mock private GroupAccessService groupAccessService;
    @Mock private LifecycleImpactPreviewService lifecycleImpactPreviewService;
    @Mock private MessageResolver messageResolver;
    @Mock private PublishGateService publishGateService;
    @Mock private TemplateContentModuleReferenceService contentModuleReferenceService;
    @Mock private CollaborationWorkItemWriter collaborationWorkItemWriter;
    @Mock private RenderProfileService renderProfileService;
    @Mock private ApprovalSubStateResolver approvalSubStateResolver;
    @Mock private ApiPolicyMaterializationService apiPolicyMaterializationService;
    @Mock private ApiPolicyRepository apiPolicyRepository;
    @Mock private VersionFidelityWarningService versionFidelityWarningService;
    @Mock private MasterDocumentRepository masterDocumentRepository;
    @Mock private MasterRevisionLineRepository masterRevisionLineRepository;
    @Mock private ObjectStoragePort objectStoragePort;

    private TemplateLifecycleService service;
    private UUID templateId;
    private TemplateEntity template;

    @BeforeEach
    void setUp() {
        service = new TemplateLifecycleService(
                templateService,
                templateRepository,
                templateVersionRepository,
                lifecycleRecordRepository,
                groupAccessService,
                lifecycleImpactPreviewService,
                messageResolver,
                publishGateService,
                new DecisionFormService(groupAccessService),
                contentModuleReferenceService,
                collaborationWorkItemWriter,
                renderProfileService,
                approvalSubStateResolver,
                apiPolicyMaterializationService,
                apiPolicyRepository,
                versionFidelityWarningService,
                new ObjectMapper(),
                masterDocumentRepository,
                masterRevisionLineRepository,
                objectStoragePort,
                new SelfApprovalGuard(),
                new TemplateAnnualReviewSupport(java.time.Clock.systemUTC())
        );
        templateId = UUID.randomUUID();
        template = new TemplateEntity(templateId, "TPL-001", "RETAIL", "Sample", null,
                UUID.randomUUID(), "10000003");
        template.setLifecycleStatus(TemplateLifecycleStatus.APPROVAL);
    }

    @Test
    void t001_sameActorApproval_isBlocked403_andStateUnchanged() {
        ManagementSessionClaims alice = session("alice", List.of("GROUP_ADMIN"));
        stubApprovable(alice);
        stubLatestSubmitter("alice");

        assertThatThrownBy(() -> service.recordApprovalDecision(templateId, approveRequest(null, null, null), alice))
                .isInstanceOf(LifecycleAuthorizationException.class)
                .satisfies(ex -> {
                    LifecycleAuthorizationException e = (LifecycleAuthorizationException) ex;
                    assertThat(e.errorCode()).isEqualTo(ApiErrorCodes.SELF_APPROVAL_FORBIDDEN);
                    assertThat(e.messageKey()).isEqualTo("api.error.lifecycle.selfApprovalForbidden");
                    assertThat(e.httpStatus().value()).isEqualTo(403);
                });

        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.APPROVAL);
        verify(lifecycleRecordRepository, never()).save(argThat(r ->
                r.getAction() == LifecycleAction.RECORD_APPROVAL_DECISION));
    }

    @Test
    void t002_sameActorGroupAdminException_isAllowedAndPersistsExceptionAudit() {
        ManagementSessionClaims alice = session("alice", List.of("GROUP_ADMIN"));
        stubApprovable(alice);
        stubLatestSubmitter("alice");
        when(templateRepository.save(any())).thenReturn(template);
        when(collaborationWorkItemWriter.resolveOpenApprovalWorkItems(template, alice))
                .thenReturn(Optional.empty());
        when(templateService.toDetail(template)).thenReturn(detail());

        service.recordApprovalDecision(templateId,
                approveRequest(true, "Solo approval due to approver pool outage 2026-07-14", true), alice);

        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.PENDING_RELEASE);
        ArgumentCaptor<TemplateLifecycleRecordEntity> captor =
                ArgumentCaptor.forClass(TemplateLifecycleRecordEntity.class);
        verify(lifecycleRecordRepository).save(captor.capture());
        TemplateLifecycleRecordEntity record = captor.getValue();
        assertThat(record.getAction()).isEqualTo(LifecycleAction.RECORD_APPROVAL_DECISION);
        assertThat(record.isSelfApprovalException()).isTrue();
        assertThat(record.getExceptionReason()).isEqualTo("Solo approval due to approver pool outage 2026-07-14");
    }

    @Test
    void t003_nonGroupAdminException_isRejected403WithTemplateKey() {
        // Capability stub keeps decideApprovals open; role itself must not allow exception intervention.
        ManagementSessionClaims alice = session("alice", List.of("DOCUMENT_AUTHOR"));
        stubApprovable(alice);
        stubLatestSubmitter("alice");

        assertThatThrownBy(() -> service.recordApprovalDecision(templateId,
                approveRequest(true, "reason", true), alice))
                .isInstanceOf(LifecycleAuthorizationException.class)
                .satisfies(ex -> {
                    LifecycleAuthorizationException e = (LifecycleAuthorizationException) ex;
                    assertThat(e.errorCode()).isEqualTo(ApiErrorCodes.EXCEPTION_INTERVENTION_NOT_ALLOWED);
                    assertThat(e.messageKey()).isEqualTo("api.error.template.exceptionInterventionNotAllowed");
                    assertThat(e.httpStatus().value()).isEqualTo(403);
                });
        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.APPROVAL);
    }

    @Test
    void t004_groupAdminExceptionReasonBlank_isRejected422() {
        ManagementSessionClaims alice = session("alice", List.of("GROUP_ADMIN"));
        stubApprovable(alice);
        stubLatestSubmitter("alice");

        assertThatThrownBy(() -> service.recordApprovalDecision(templateId,
                approveRequest(true, "   ", true), alice))
                .isInstanceOf(LifecycleAuthorizationException.class)
                .satisfies(ex -> {
                    LifecycleAuthorizationException e = (LifecycleAuthorizationException) ex;
                    assertThat(e.errorCode()).isEqualTo(ApiErrorCodes.EXCEPTION_REASON_REQUIRED);
                    assertThat(e.messageKey()).isEqualTo("api.error.template.exceptionReasonRequired");
                    assertThat(e.httpStatus().value()).isEqualTo(422);
                });
    }

    @Test
    void t005_differentActorApproval_succeedsWithoutException() {
        ManagementSessionClaims bob = session("bob", List.of("GROUP_ADMIN"));
        stubApprovable(bob);
        stubLatestSubmitter("alice");
        when(templateRepository.save(any())).thenReturn(template);
        when(collaborationWorkItemWriter.resolveOpenApprovalWorkItems(template, bob))
                .thenReturn(Optional.empty());
        when(templateService.toDetail(template)).thenReturn(detail());

        service.recordApprovalDecision(templateId, approveRequest(null, null, null), bob);

        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.PENDING_RELEASE);
        ArgumentCaptor<TemplateLifecycleRecordEntity> captor =
                ArgumentCaptor.forClass(TemplateLifecycleRecordEntity.class);
        verify(lifecycleRecordRepository).save(captor.capture());
        assertThat(captor.getValue().isSelfApprovalException()).isFalse();
        assertThat(captor.getValue().getExceptionReason()).isNull();
    }

    @Test
    void x006_sameActorReject_alsoBlocked() {
        ManagementSessionClaims alice = session("alice", List.of("GROUP_ADMIN"));
        stubApprovable(alice);
        stubLatestSubmitter("alice");

        assertThatThrownBy(() -> service.recordApprovalDecision(templateId, rejectRequest(null, null, null), alice))
                .isInstanceOf(LifecycleAuthorizationException.class)
                .satisfies(ex -> assertThat(((LifecycleAuthorizationException) ex).errorCode())
                        .isEqualTo(ApiErrorCodes.SELF_APPROVAL_FORBIDDEN));
        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.APPROVAL);
    }

    @Test
    void x001_noSubmitRecord_doesNotBlock() {
        ManagementSessionClaims alice = session("alice", List.of("GROUP_ADMIN"));
        stubApprovable(alice);
        when(lifecycleRecordRepository.findByTemplateIdOrderByCreatedAtDesc(templateId))
                .thenReturn(List.of());
        when(templateRepository.save(any())).thenReturn(template);
        when(collaborationWorkItemWriter.resolveOpenApprovalWorkItems(template, alice))
                .thenReturn(Optional.empty());
        when(templateService.toDetail(template)).thenReturn(detail());

        service.recordApprovalDecision(templateId, approveRequest(null, null, null), alice);
        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.PENDING_RELEASE);
    }

    @Test
    void x002_globalAdminSelfApproval_requiresExceptionPath() {
        ManagementSessionClaims root = session("root", List.of("GLOBAL_ADMIN"));
        stubApprovable(root);
        stubLatestSubmitter("root");
        when(templateRepository.save(any())).thenReturn(template);
        when(collaborationWorkItemWriter.resolveOpenApprovalWorkItems(template, root))
                .thenReturn(Optional.empty());
        when(templateService.toDetail(template)).thenReturn(detail());

        assertThatThrownBy(() -> service.recordApprovalDecision(templateId, approveRequest(null, null, null), root))
                .isInstanceOf(LifecycleAuthorizationException.class)
                .satisfies(ex -> assertThat(((LifecycleAuthorizationException) ex).errorCode())
                        .isEqualTo(ApiErrorCodes.SELF_APPROVAL_FORBIDDEN));

        service.recordApprovalDecision(templateId,
                approveRequest(true, "Global admin solo approval override", true), root);
        ArgumentCaptor<TemplateLifecycleRecordEntity> captor =
                ArgumentCaptor.forClass(TemplateLifecycleRecordEntity.class);
        verify(lifecycleRecordRepository).save(captor.capture());
        assertThat(captor.getValue().isSelfApprovalException()).isTrue();
    }

    @Test
    void x004_exceptionDoesNotBypassRationaleValidation() {
        ManagementSessionClaims alice = session("alice", List.of("GROUP_ADMIN"));
        stubApprovable(alice);
        stubLatestSubmitter("alice");

        LifecycleDecisionRequest missingRationale = new LifecycleDecisionRequest(
                LifecycleDecision.APPROVED, "  ", null, null,
                true, null, null, true, null, null, null,
                true, "Solo approval", true);

        assertThatThrownBy(() -> service.recordApprovalDecision(templateId, missingRationale, alice))
                .isInstanceOf(TemplateValidationException.class)
                .hasFieldOrPropertyWithValue("messageKey", "api.error.template.decisionRationaleRequired");
        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.APPROVAL);
        verify(lifecycleRecordRepository, never()).save(argThat(r ->
                r.getAction() == LifecycleAction.RECORD_APPROVAL_DECISION));
    }

    @Test
    void x005_crossGroupAccessDenied_beforeSelfApproval() {
        ManagementSessionClaims alice = session("alice", List.of("GROUP_ADMIN"));
        when(groupAccessService.canDecideTemplateApprovals(alice)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, alice))
                .thenThrow(new TemplateAccessDeniedException());

        assertThatThrownBy(() -> service.recordApprovalDecision(templateId,
                approveRequest(true, "cross-group", true), alice))
                .isInstanceOf(TemplateAccessDeniedException.class);
        verify(lifecycleRecordRepository, never()).findByTemplateIdOrderByCreatedAtDesc(any());
    }

    @Test
    void x007_exceptionAuditFieldsSurviveSubsequentTransition() {
        ManagementSessionClaims alice = session("alice", List.of("GROUP_ADMIN"));
        stubApprovable(alice);
        stubLatestSubmitter("alice");
        when(templateRepository.save(any())).thenReturn(template);
        when(collaborationWorkItemWriter.resolveOpenApprovalWorkItems(template, alice))
                .thenReturn(Optional.empty());
        when(templateService.toDetail(template)).thenReturn(detail());

        service.recordApprovalDecision(templateId,
                approveRequest(true, "Solo approval durable reason", true), alice);

        ArgumentCaptor<TemplateLifecycleRecordEntity> captor =
                ArgumentCaptor.forClass(TemplateLifecycleRecordEntity.class);
        verify(lifecycleRecordRepository).save(captor.capture());
        TemplateLifecycleRecordEntity approvalRecord = captor.getValue();
        assertThat(approvalRecord.isSelfApprovalException()).isTrue();
        assertThat(approvalRecord.getExceptionReason()).isEqualTo("Solo approval durable reason");

        // Entity has no setters for exception fields — subsequent transitions create new rows.
        TemplateLifecycleRecordEntity laterPublish = new TemplateLifecycleRecordEntity(
                UUID.randomUUID(), templateId, LifecycleAction.PUBLISH,
                TemplateLifecycleStatus.PENDING_RELEASE, TemplateLifecycleStatus.PUBLISHED,
                null, "publish", "1.0.0", "bob");
        assertThat(approvalRecord.isSelfApprovalException()).isTrue();
        assertThat(approvalRecord.getExceptionReason()).isEqualTo("Solo approval durable reason");
        assertThat(laterPublish.isSelfApprovalException()).isFalse();
    }

    @Test
    void x008_lifecycleAuditViewExposesExceptionFields() {
        TemplateLifecycleRecordEntity record = new TemplateLifecycleRecordEntity(
                UUID.randomUUID(), templateId, LifecycleAction.RECORD_APPROVAL_DECISION,
                TemplateLifecycleStatus.APPROVAL, TemplateLifecycleStatus.PENDING_RELEASE,
                LifecycleDecision.APPROVED, "ok", null, "alice",
                true, "Solo approval audit visible");

        com.bank.docgen.audit.api.LifecycleAuditEventView view =
                new com.bank.docgen.audit.api.LifecycleAuditEventView(
                        record.getCreatedAt(),
                        record.getAction().name(),
                        templateId.toString(),
                        "Sample",
                        "TPL-001",
                        record.getAction().name(),
                        record.getFromStatus().name(),
                        record.getToStatus().name(),
                        record.getActorUsername(),
                        "Alice",
                        record.getCommentSummary(),
                        List.of(),
                        record.isSelfApprovalException(),
                        record.getExceptionReason());

        assertThat(view.selfApprovalException()).isTrue();
        assertThat(view.exceptionReason()).isEqualTo("Solo approval audit visible");
    }

    @Test
    void t004b_groupAdminExceptionSecondaryNotConfirmed_isRejected422() {
        ManagementSessionClaims alice = session("alice", List.of("GROUP_ADMIN"));
        stubApprovable(alice);
        stubLatestSubmitter("alice");

        assertThatThrownBy(() -> service.recordApprovalDecision(templateId,
                approveRequest(true, "reason", false), alice))
                .isInstanceOf(LifecycleAuthorizationException.class)
                .satisfies(ex -> {
                    LifecycleAuthorizationException e = (LifecycleAuthorizationException) ex;
                    assertThat(e.errorCode()).isEqualTo(ApiErrorCodes.EXCEPTION_SECONDARY_CONFIRM_REQUIRED);
                    assertThat(e.messageKey()).isEqualTo("api.error.template.exceptionSecondaryConfirmRequired");
                    assertThat(e.httpStatus().value()).isEqualTo(422);
                });
    }

    private void stubApprovable(ManagementSessionClaims session) {
        when(groupAccessService.canDecideTemplateApprovals(session)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, session)).thenReturn(template);
        when(approvalSubStateResolver.resolve(template)).thenReturn(ApprovalSubState.PENDING_DECISION);
        when(apiPolicyMaterializationService.ensureApiPolicySkeleton(any(), any())).thenReturn(null);
    }

    private void stubLatestSubmitter(String actor) {
        TemplateLifecycleRecordEntity submitRecord = new TemplateLifecycleRecordEntity(
                UUID.randomUUID(), templateId, LifecycleAction.SUBMIT_FOR_APPROVAL,
                TemplateLifecycleStatus.DRAFT, TemplateLifecycleStatus.APPROVAL, null, "submit", null, actor);
        when(lifecycleRecordRepository.findByTemplateIdOrderByCreatedAtDesc(templateId))
                .thenReturn(List.of(submitRecord));
    }

    private LifecycleDecisionRequest approveRequest(Boolean exception, String reason, Boolean secondary) {
        return new LifecycleDecisionRequest(
                LifecycleDecision.APPROVED, "Rationale", null, null,
                true, null, null, true, null, null, null,
                exception, reason, secondary);
    }

    private LifecycleDecisionRequest rejectRequest(Boolean exception, String reason, Boolean secondary) {
        return new LifecycleDecisionRequest(
                LifecycleDecision.REJECTED, "Rationale", "SCOPE_CHANGE", "Impact",
                null, null, null, null, "tr-1", null, null,
                exception, reason, secondary);
    }

    private TemplateDetailView detail() {
        return new TemplateDetailView(
                templateId.toString(), "TPL-001", "RETAIL", "Sample", null,
                UUID.randomUUID().toString(), TemplateLifecycleStatus.PENDING_RELEASE, null, null,
                UUID.randomUUID().toString(), 1, List.of(), List.of(), List.of(),
                Instant.now(), Instant.now(), null, null, false, null,
                null);
    }

    private ManagementSessionClaims session(String username, List<String> roles) {
        return new ManagementSessionClaims(
                username, username, username + "@example.com", AuthSource.LOCAL,
                roles, List.of("RETAIL"), "route.template-approver-home",
                List.of("route.template-approver-home"), Instant.now().plusSeconds(3600));
    }
}
