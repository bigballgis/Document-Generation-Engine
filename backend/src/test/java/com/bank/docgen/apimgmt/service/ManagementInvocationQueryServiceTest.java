package com.bank.docgen.apimgmt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.api.ManagementInvocationDetailView;
import com.bank.docgen.apimgmt.api.ManagementInvocationSummaryView;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.audit.persistence.AuditSearchPage;
import com.bank.docgen.runtime.domain.InvocationKind;
import com.bank.docgen.runtime.domain.InvocationStatus;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordEntity;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.service.TemplateService;
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

    private ManagementInvocationQueryService service;
    private UUID templateId;
    private UUID credentialId;
    private ManagementSessionClaims session;

    @BeforeEach
    void setUp() {
        service = new ManagementInvocationQueryService(
                templateService,
                apiPolicyRepository,
                invocationRecordRepository
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
        stubReadableTemplate();
        stubPolicy();
        ApiInvocationRecordEntity entity = sampleEntity("INV-TEST001", "req-1", "SUCCESS");
        when(invocationRecordRepository.searchManagementInvocations(
                eq(templateId),
                any(),
                any(),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
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
        stubReadableTemplate();
        stubPolicy();
        ApiInvocationRecordEntity entity = sampleEntity("INV-PAGE001", "req-page", "SUCCESS");
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
                eq(1),
                eq(20)
        )).thenReturn(new AuditSearchPage<>(List.of(entity), 25, 2));

        ManagementInvocationFilters filters = new ManagementInvocationFilters(
                "FAILED",
                "SINGLE",
                "req",
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-06-30T23:59:59Z"),
                credentialId
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
    void listInvocations_normalizesPageSizeToMax100() {
        stubReadableTemplate();
        stubPolicy();
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
        stubReadableTemplate();
        stubPolicy();
        ApiInvocationRecordEntity entity = sampleEntity("INV-DETAIL", "req-detail", "SUCCESS");
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
    }

    @Test
    void getInvocationDetail_crossTemplateDenied() {
        stubReadableTemplate();
        stubPolicy();
        UUID otherTemplateId = UUID.randomUUID();
        ApiInvocationRecordEntity entity = new ApiInvocationRecordEntity(
                UUID.randomUUID(),
                "INV-OTHER",
                InvocationKind.SINGLE,
                InvocationStatus.SUCCEEDED,
                "dev",
                otherTemplateId,
                "TPL-OTHER",
                credentialId,
                "svc-account-prod",
                "req-other",
                "idem-other",
                "EXPLICIT_VERSION",
                "1.0.0",
                "1.0.0",
                "DOCX",
                "SYNC_STREAM",
                "SUCCESS",
                120L,
                "{\"variables\":{\"secret\":\"value\"}}",
                null,
                null,
                false,
                Instant.now().plusSeconds(3600),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                Instant.now(),
                Instant.now()
        );
        when(invocationRecordRepository.findByInvocationExternalId("INV-OTHER"))
                .thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.getInvocationDetail(templateId, "INV-OTHER", session))
                .isInstanceOf(ApiManagementNotFoundException.class);
    }

    @Test
    void getInvocationDetail_expiredRecordDenied() {
        stubReadableTemplate();
        stubPolicy();
        ApiInvocationRecordEntity entity = sampleEntity("INV-EXPIRED", "req-expired", "SUCCESS");
        entity = expiredEntity(entity);
        when(invocationRecordRepository.findByInvocationExternalId("INV-EXPIRED"))
                .thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.getInvocationDetail(templateId, "INV-EXPIRED", session))
                .isInstanceOf(ApiManagementNotFoundException.class);
    }

    @Test
    void maskAccessAccount_masksLongValues() {
        assertThat(ManagementInvocationQueryService.maskAccessAccount("svc-account")).isEqualTo("svc***");
    }

    private void stubReadableTemplate() {
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
    }

    private void stubPolicy() {
        when(apiPolicyRepository.findByTemplateId(templateId))
                .thenReturn(Optional.of(new ApiPolicyEntity(UUID.randomUUID(), templateId, "[]", "10000001")));
    }

    private ApiInvocationRecordEntity sampleEntity(String invocationId, String requestId, String outcome) {
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
                "1.0.0",
                "1.0.0",
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
