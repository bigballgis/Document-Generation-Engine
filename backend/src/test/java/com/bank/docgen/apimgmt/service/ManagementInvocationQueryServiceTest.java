package com.bank.docgen.apimgmt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.api.ManagementInvocationDetailView;
import com.bank.docgen.apimgmt.api.ManagementInvocationSummaryView;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.audit.persistence.AuditSearchPage;
import com.bank.docgen.runtime.domain.InvocationKind;
import com.bank.docgen.runtime.domain.InvocationStatus;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordEntity;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.service.TemplateService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManagementInvocationQueryServiceTest {

    @Mock
    private TemplateService templateService;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private ApiInvocationRecordRepository invocationRecordRepository;
    @Mock
    private GroupAccessService groupAccessService;

    private ManagementInvocationQueryService service;
    private UUID templateId;
    private UUID credentialId;
    private ManagementSessionClaims session;

    @BeforeEach
    void setUp() {
        service = new ManagementInvocationQueryService(
                templateService,
                apiPolicyRepository,
                invocationRecordRepository,
                groupAccessService
        );
        templateId = UUID.randomUUID();
        credentialId = UUID.randomUUID();
        session = new ManagementSessionClaims(
                "10000002",
                "Admin",
                "admin@example.com",
                AuthSource.LOCAL,
                List.of("GROUP_ADMIN"),
                List.of("RETAIL"),
                "route.template-authoring-home",
                List.of("route.template-authoring-home"),
                Instant.now().plusSeconds(3600)
        );
    }

    @Test
    void listRecentInvocations_returnsSummaryWithoutParameters() {
        stubManageableTemplate();
        ApiInvocationRecordEntity entity = sampleEntity("INV-TEST001", "req-1", "SUCCESS", "1.0.0");
        when(invocationRecordRepository.searchManagementInvocations(
                eq(templateId),
                any(),
                any(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(0),
                eq(10)
        )).thenReturn(new AuditSearchPage<>(List.of(entity), 1, 1));

        List<ManagementInvocationSummaryView> result = service.listRecentInvocations(templateId, 10, session);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().invocationId()).isEqualTo("INV-TEST001");
        assertThat(result.getFirst().accessAccountSummary()).isEqualTo("svc***");
    }

    @Test
    void listInvocations_returnsPaginatedSummaryWithoutParameters() {
        stubManageableTemplate();
        ApiInvocationRecordEntity entity = sampleEntity("INV-PAGE001", "req-page", "SUCCESS", "1.0.0");
        when(invocationRecordRepository.searchManagementInvocations(
                eq(templateId),
                any(),
                any(),
                eq("FAILED"),
                eq(InvocationKind.SINGLE),
                eq("req"),
                any(),
                any(),
                eq(credentialId),
                isNull(),
                eq(1),
                eq(20)
        )).thenReturn(new AuditSearchPage<>(List.of(entity), 25, 2));

        ManagementInvocationFilters filters = new ManagementInvocationFilters(
                "FAILED",
                "SINGLE",
                "req",
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-06-30T23:59:59Z"),
                credentialId,
                null
        );
        PageView<ManagementInvocationSummaryView> result = service.listInvocations(
                templateId,
                session,
                1,
                20,
                filters
        );

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(25);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().requestId()).isEqualTo("req-page");
    }

    @Test
    void listInvocations_filtersByResolvedReleaseVersion() {
        stubManageableTemplate();
        ApiInvocationRecordEntity entity = sampleEntity("INV-REL120", "req-rel", "FAILURE", "1.2.0");
        when(invocationRecordRepository.searchManagementInvocations(
                eq(templateId),
                any(),
                any(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq("1.2.0"),
                eq(0),
                eq(20)
        )).thenReturn(new AuditSearchPage<>(List.of(entity), 1, 1));

        PageView<ManagementInvocationSummaryView> result = service.listInvocations(
                templateId,
                session,
                0,
                20,
                new ManagementInvocationFilters(null, null, null, null, null, null, "1.2.0")
        );

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content().getFirst().resolvedReleaseVersion()).isEqualTo("1.2.0");
        assertThat(result.content().getFirst().invocationId()).isEqualTo("INV-REL120");
    }

    @Test
    void listInvocations_combinesReleaseVersionAndStatusFilters() {
        stubManageableTemplate();
        ApiInvocationRecordEntity entity = sampleEntity("INV-COMBINED", "req-c", "FAILURE", "1.2.0");
        when(invocationRecordRepository.searchManagementInvocations(
                eq(templateId),
                any(),
                any(),
                eq("FAILED"),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq("1.2.0"),
                eq(0),
                eq(20)
        )).thenReturn(new AuditSearchPage<>(List.of(entity), 1, 1));

        PageView<ManagementInvocationSummaryView> result = service.listInvocations(
                templateId,
                session,
                0,
                20,
                new ManagementInvocationFilters("FAILED", null, null, null, null, null, "1.2.0")
        );

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().invocationId()).isEqualTo("INV-COMBINED");
    }

    @Test
    void listInvocations_normalizesPageSizeToMax100() {
        stubManageableTemplate();
        when(invocationRecordRepository.searchManagementInvocations(
                eq(templateId),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                eq(0),
                eq(100)
        )).thenReturn(new AuditSearchPage<>(List.of(), 0, 0));

        PageView<ManagementInvocationSummaryView> result = service.listInvocations(
                templateId,
                session,
                0,
                500,
                ManagementInvocationFilters.empty()
        );

        assertThat(result.size()).isEqualTo(100);
    }

    @Test
    void getInvocationDetail_returnsSummaryWithoutParametersStorage() {
        stubManageableTemplate();
        ApiInvocationRecordEntity entity = sampleEntity("INV-DETAIL", "req-detail", "SUCCESS", "1.0.0");
        entity = withAuditAndDocument(entity, "audit-123", "doc-123");
        when(invocationRecordRepository.findByInvocationExternalId("INV-DETAIL"))
                .thenReturn(Optional.of(entity));

        ManagementInvocationDetailView detail = service.getInvocationDetail(templateId, "INV-DETAIL", session);

        assertThat(detail.invocationId()).isEqualTo("INV-DETAIL");
        assertThat(detail.requestId()).isEqualTo("req-detail");
        assertThat(detail.outcome()).isEqualTo("SUCCESS");
        assertThat(detail.accessAccountSummary()).isEqualTo("svc***");
        assertThat(detail.documentPresent()).isTrue();
        assertThat(detail.auditLinkHint().requestId()).isEqualTo("req-detail");
        assertThat(detail.auditLinkHint().auditId()).isEqualTo("audit-123");
        assertThat(detail.errorCode()).isNull();
        assertThat(detail.releaseBundleSnapshotId()).isNull();
        assertThat(detail.releaseBundleHash()).isNull();
    }

    @Test
    void getInvocationDetail_returnsReleaseBundleFingerprintWhenPresent() {
        stubManageableTemplate();
        UUID snapshotId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        String hash = "e".repeat(64);
        ApiInvocationRecordEntity entity = sampleEntity("INV-FP", "req-fp", "SUCCESS", "1.0.0");
        entity.applyReleaseBundleFingerprint(snapshotId, hash);
        when(invocationRecordRepository.findByInvocationExternalId("INV-FP"))
                .thenReturn(Optional.of(entity));

        ManagementInvocationDetailView detail = service.getInvocationDetail(templateId, "INV-FP", session);

        assertThat(detail.releaseBundleSnapshotId()).isEqualTo(snapshotId);
        assertThat(detail.releaseBundleHash()).isEqualTo(hash);
    }

    @Test
    void getInvocationDetail_returnsPersistedErrorEnvelope() {
        stubManageableTemplate();
        ApiInvocationRecordEntity entity = sampleEntity("INV-FAIL", "req-fail", "FAILURE", "1.2.0");
        entity.applyErrorEnvelope(
                "REQUEST_BODY_INVALID",
                "RUNTIME",
                "api.error.validation.requestBodyInvalid",
                false,
                "Request body is invalid."
        );
        when(invocationRecordRepository.findByInvocationExternalId("INV-FAIL"))
                .thenReturn(Optional.of(entity));

        ManagementInvocationDetailView detail = service.getInvocationDetail(templateId, "INV-FAIL", session);

        assertThat(detail.errorCode()).isEqualTo("REQUEST_BODY_INVALID");
        assertThat(detail.errorCategory()).isEqualTo("RUNTIME");
        assertThat(detail.errorMessageKey()).isEqualTo("api.error.validation.requestBodyInvalid");
        assertThat(detail.errorRetryable()).isFalse();
        assertThat(detail.errorMessage()).isEqualTo("Request body is invalid.");
    }

    @Test
    void getInvocationDetail_crossTemplateDenied() {
        stubManageableTemplate();
        UUID otherTemplateId = UUID.randomUUID();
        ApiInvocationRecordEntity entity = sampleEntity("INV-OTHER", "req-other", "SUCCESS", "1.0.0");
        entity = new ApiInvocationRecordEntity(
                entity.getId(),
                entity.getInvocationExternalId(),
                entity.getInvocationKind(),
                entity.getStatus(),
                entity.getEnvironment(),
                otherTemplateId,
                "TPL-OTHER",
                entity.getCredentialId(),
                entity.getAccessAccount(),
                entity.getRequestId(),
                entity.getIdempotencyKey(),
                entity.getRouteType(),
                entity.getRequestedReleaseVersion(),
                entity.getResolvedReleaseVersion(),
                entity.getOutputFormat(),
                entity.getOutputMode(),
                entity.getOutcome(),
                entity.getDurationMs(),
                entity.getParametersStorage(),
                entity.getDocumentId(),
                entity.getArtifactStorageKey(),
                entity.isArtifactSaved(),
                entity.getRecordExpiresAt(),
                entity.getDocumentExpiresAt(),
                entity.getBatchExternalId(),
                entity.getParentInvocationExternalId(),
                entity.getItemId(),
                entity.getTaskExternalId(),
                entity.getIdempotencyRecordId(),
                entity.getAuditId(),
                entity.isBatch(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
        when(invocationRecordRepository.findByInvocationExternalId("INV-OTHER"))
                .thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.getInvocationDetail(templateId, "INV-OTHER", session))
                .isInstanceOf(ApiManagementNotFoundException.class);
    }

    @Test
    void getInvocationDetail_expiredRecordDenied() {
        stubManageableTemplate();
        ApiInvocationRecordEntity entity = expiredEntity(sampleEntity("INV-EXPIRED", "req-expired", "SUCCESS", "1.0.0"));
        when(invocationRecordRepository.findByInvocationExternalId("INV-EXPIRED"))
                .thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.getInvocationDetail(templateId, "INV-EXPIRED", session))
                .isInstanceOf(ApiManagementNotFoundException.class);
    }

    @Test
    void exportInvocationsCsv_respectsFiltersAndOmitsParameters() {
        stubManageableTemplate();
        ApiInvocationRecordEntity entity = sampleEntity("INV-CSV", "req-csv", "FAILURE", "1.2.0");
        entity.applyErrorEnvelope("REQUEST_BODY_INVALID", "RUNTIME", "api.error.validation.requestBodyInvalid", false, null);
        when(invocationRecordRepository.searchManagementInvocations(
                eq(templateId),
                any(),
                any(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq("1.2.0"),
                eq(0),
                eq(ManagementInvocationQueryService.MAX_EXPORT_ROWS)
        )).thenReturn(new AuditSearchPage<>(List.of(entity), 1, 1));

        ManagementInvocationCsvExport export = service.exportInvocationsCsv(
                templateId,
                session,
                new ManagementInvocationFilters(null, null, null, null, null, null, "1.2.0")
        );

        String csv = new String(export.content(), StandardCharsets.UTF_8);
        assertThat(csv).contains("invocationId,requestId,invocationKind,status,resolvedReleaseVersion");
        assertThat(csv).contains("INV-CSV");
        assertThat(csv).contains("1.2.0");
        assertThat(csv).contains("REQUEST_BODY_INVALID");
        assertThat(csv).doesNotContain("parameters");
        assertThat(csv).doesNotContain("secret");
        assertThat(export.truncated()).isFalse();
        assertThat(export.filename()).contains(templateId.toString());
    }

    @Test
    void listInvocations_deniedWithoutCanManageApiPolicy() {
        when(groupAccessService.canManageApiPolicy(session)).thenReturn(false);

        assertThatThrownBy(() -> service.listInvocations(
                templateId,
                session,
                0,
                20,
                ManagementInvocationFilters.empty()
        )).isInstanceOf(ApiManagementAccessDeniedException.class);

        verify(invocationRecordRepository, never()).searchManagementInvocations(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Integer.class), any(Integer.class)
        );
    }

    @Test
    void exportInvocationsCsv_deniedWithoutCanManageApiPolicy() {
        when(groupAccessService.canManageApiPolicy(session)).thenReturn(false);

        assertThatThrownBy(() -> service.exportInvocationsCsv(
                templateId,
                session,
                ManagementInvocationFilters.empty()
        )).isInstanceOf(ApiManagementAccessDeniedException.class);
    }

    @Test
    void getInvocationDetail_deniedWithoutCanManageApiPolicy() {
        when(groupAccessService.canManageApiPolicy(session)).thenReturn(false);

        assertThatThrownBy(() -> service.getInvocationDetail(templateId, "INV-ANY", session))
                .isInstanceOf(ApiManagementAccessDeniedException.class);

        verify(invocationRecordRepository, never()).findByInvocationExternalId(any());
    }

    @Test
    void maskAccessAccount_masksLongValues() {
        assertThat(ManagementInvocationQueryService.maskAccessAccount("svc-account")).isEqualTo("svc***");
    }

    private void stubManageableTemplate() {
        when(groupAccessService.canManageApiPolicy(session)).thenReturn(true);
        TemplateEntity template = new TemplateEntity(
                templateId,
                "TPL-001",
                "RETAIL",
                "Sample",
                null,
                UUID.randomUUID(),
                "10000001"
        );
        template.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        when(templateService.requireReadableTemplate(templateId, session)).thenReturn(template);
        when(apiPolicyRepository.findByTemplateId(templateId))
                .thenReturn(Optional.of(new ApiPolicyEntity(UUID.randomUUID(), templateId, "[]", "10000001")));
    }

    private ApiInvocationRecordEntity sampleEntity(
            String invocationId,
            String requestId,
            String outcome,
            String releaseVersion
    ) {
        Instant now = Instant.now();
        return new ApiInvocationRecordEntity(
                UUID.randomUUID(),
                invocationId,
                InvocationKind.SINGLE,
                InvocationStatus.SUCCEEDED,
                "dev",
                templateId,
                "TPL-001",
                credentialId,
                "svc-account-prod",
                requestId,
                "idem-1",
                "EXPLICIT_VERSION",
                releaseVersion,
                releaseVersion,
                "DOCX",
                "SYNC_STREAM",
                outcome,
                120L,
                "{\"variables\":{\"secret\":\"value\"}}",
                null,
                null,
                false,
                now.plusSeconds(3600),
                now.plusSeconds(7200),
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                now,
                now
        );
    }

    private ApiInvocationRecordEntity withAuditAndDocument(
            ApiInvocationRecordEntity entity,
            String auditId,
            String documentId
    ) {
        return new ApiInvocationRecordEntity(
                entity.getId(),
                entity.getInvocationExternalId(),
                entity.getInvocationKind(),
                entity.getStatus(),
                entity.getEnvironment(),
                entity.getTemplateId(),
                entity.getTemplateExternalId(),
                entity.getCredentialId(),
                entity.getAccessAccount(),
                entity.getRequestId(),
                entity.getIdempotencyKey(),
                entity.getRouteType(),
                entity.getRequestedReleaseVersion(),
                entity.getResolvedReleaseVersion(),
                entity.getOutputFormat(),
                entity.getOutputMode(),
                entity.getOutcome(),
                entity.getDurationMs(),
                entity.getParametersStorage(),
                documentId,
                entity.getArtifactStorageKey(),
                entity.isArtifactSaved(),
                entity.getRecordExpiresAt(),
                entity.getDocumentExpiresAt(),
                entity.getBatchExternalId(),
                entity.getParentInvocationExternalId(),
                entity.getItemId(),
                entity.getTaskExternalId(),
                entity.getIdempotencyRecordId(),
                auditId,
                entity.isBatch(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ApiInvocationRecordEntity expiredEntity(ApiInvocationRecordEntity entity) {
        Instant now = Instant.now();
        return new ApiInvocationRecordEntity(
                entity.getId(),
                entity.getInvocationExternalId(),
                entity.getInvocationKind(),
                entity.getStatus(),
                entity.getEnvironment(),
                entity.getTemplateId(),
                entity.getTemplateExternalId(),
                entity.getCredentialId(),
                entity.getAccessAccount(),
                entity.getRequestId(),
                entity.getIdempotencyKey(),
                entity.getRouteType(),
                entity.getRequestedReleaseVersion(),
                entity.getResolvedReleaseVersion(),
                entity.getOutputFormat(),
                entity.getOutputMode(),
                entity.getOutcome(),
                entity.getDurationMs(),
                entity.getParametersStorage(),
                entity.getDocumentId(),
                entity.getArtifactStorageKey(),
                entity.isArtifactSaved(),
                now.minusSeconds(60),
                entity.getDocumentExpiresAt(),
                entity.getBatchExternalId(),
                entity.getParentInvocationExternalId(),
                entity.getItemId(),
                entity.getTaskExternalId(),
                entity.getIdempotencyRecordId(),
                entity.getAuditId(),
                entity.isBatch(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
