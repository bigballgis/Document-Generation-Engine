package com.bank.docgen.apimgmt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.api.InvocationRegeneratedAuditDetail;
import com.bank.docgen.apimgmt.api.ManagementInvocationRegenerateRequest;
import com.bank.docgen.apimgmt.api.ManagementInvocationRegenerateView;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.apimgmt.persistence.InvocationRegenerationEntity;
import com.bank.docgen.apimgmt.persistence.InvocationRegenerationRepository;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.rendering.DocxAssemblyException;
import com.bank.docgen.rendering.RenderingOperationException;
import com.bank.docgen.runtime.domain.InvocationKind;
import com.bank.docgen.runtime.domain.InvocationStatus;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordEntity;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordRepository;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.service.TemplateService;
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

@ExtendWith(MockitoExtension.class)
class InvocationRegenerationServiceTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID SNAPSHOT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final String HASH = "b".repeat(64);

    @Mock
    private TemplateService templateService;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private ApiInvocationRecordRepository invocationRecordRepository;
    @Mock
    private InvocationRegenerationRepository regenerationRepository;
    @Mock
    private GroupAccessService groupAccessService;
    @Mock
    private ManagementAuditRecorder managementAuditRecorder;
    @Mock
    private InvocationRegenerationAssemblySupport assemblySupport;

    private InvocationRegenerationService service;
    private TemplateEntity template;
    private ManagementSessionClaims globalAdmin;

    @BeforeEach
    void setUp() {
        service = new InvocationRegenerationService(
                templateService,
                apiPolicyRepository,
                invocationRecordRepository,
                regenerationRepository,
                groupAccessService,
                managementAuditRecorder,
                assemblySupport
        );
        template = new TemplateEntity(
                TEMPLATE_ID,
                "TPL-001",
                "GRP-A",
                "Demo",
                null,
                null,
                "U0000001"
        );
        globalAdmin = session(List.of("GLOBAL_ADMIN"), List.of("*"));
    }

    @Test
    void regenerate_succeedsForGlobalAdminWithSpecimenArtifact() {
        ApiInvocationRecordEntity invocation = regenerableInvocation(InvocationKind.SINGLE);
        when(groupAccessService.canRegenerateInvocation(globalAdmin)).thenReturn(true);
        when(templateService.requireReadableTemplate(TEMPLATE_ID, globalAdmin)).thenReturn(template);
        when(apiPolicyRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(Optional.of(
                com.bank.docgen.apimgmt.persistence.ApiPolicyEntity.createSkeleton(TEMPLATE_ID, "U0000001")
        ));
        when(invocationRecordRepository.findByInvocationExternalId("INV-OK01"))
                .thenReturn(Optional.of(invocation));
        when(assemblySupport.assembleSpecimen(
                eq(template),
                eq(invocation),
                eq("PDF"),
                any(UUID.class)
        )).thenReturn(new InvocationRegenerationAssemblySupport.AssembledRegeneration(
                "regenerations/regen-1/output.pdf",
                "application/pdf"
        ));
        when(regenerationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ManagementInvocationRegenerateView view = service.regenerate(
                TEMPLATE_ID,
                "INV-OK01",
                new ManagementInvocationRegenerateRequest("PDF"),
                globalAdmin
        );

        assertThat(view.specimen()).isTrue();
        assertThat(view.encryptionReapplied()).isFalse();
        assertThat(view.releaseBundleSnapshotId()).isEqualTo(SNAPSHOT_ID);
        assertThat(view.releaseBundleHash()).isEqualTo(HASH);
        assertThat(view.artifactPath()).startsWith("regenerations/");
        assertThat(view.outputFormat()).isEqualTo("PDF");
        verify(managementAuditRecorder).recordInvocationRegenerated(any());
        ArgumentCaptor<InvocationRegenerationEntity> saved = ArgumentCaptor.forClass(
                InvocationRegenerationEntity.class
        );
        verify(regenerationRepository).save(saved.capture());
        assertThat(saved.getValue().getOutcome()).isEqualTo("SUCCESS");
    }

    @Test
    void regenerate_rejectsTemplateAuthor() {
        ManagementSessionClaims author = session(List.of("TEMPLATE_AUTHOR"), List.of("GRP-A"));
        when(groupAccessService.canRegenerateInvocation(author)).thenReturn(false);

        assertThatThrownBy(() -> service.regenerate(
                TEMPLATE_ID,
                "INV-OK01",
                null,
                author
        )).isInstanceOf(ApiManagementAccessDeniedException.class);

        verify(regenerationRepository, never()).save(any());
        verify(assemblySupport, never()).assembleSpecimen(any(), any(), any(), any());
    }

    @Test
    void regenerate_rejectsBatchRoot() {
        ApiInvocationRecordEntity root = regenerableInvocation(InvocationKind.BATCH_ROOT);
        when(groupAccessService.canRegenerateInvocation(globalAdmin)).thenReturn(true);
        when(templateService.requireReadableTemplate(TEMPLATE_ID, globalAdmin)).thenReturn(template);
        when(apiPolicyRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(Optional.of(
                com.bank.docgen.apimgmt.persistence.ApiPolicyEntity.createSkeleton(TEMPLATE_ID, "U0000001")
        ));
        when(invocationRecordRepository.findByInvocationExternalId("INV-ROOT01"))
                .thenReturn(Optional.of(root));

        assertThatThrownBy(() -> service.regenerate(
                TEMPLATE_ID,
                "INV-ROOT01",
                null,
                globalAdmin
        )).isInstanceOf(InvocationRegenerationException.class)
                .extracting(ex -> ((InvocationRegenerationException) ex).errorCode())
                .isEqualTo("INVOCATION_KIND_NOT_REGENERABLE");

        verify(managementAuditRecorder).recordInvocationRegenerated(any());
    }

    @Test
    void regenerate_rejectsExpiredRecordWith410Semantics() {
        ApiInvocationRecordEntity expired = regenerableInvocation(
                InvocationKind.SINGLE,
                "INV-OLD01",
                Instant.now().minusSeconds(60)
        );
        when(groupAccessService.canRegenerateInvocation(globalAdmin)).thenReturn(true);
        when(templateService.requireReadableTemplate(TEMPLATE_ID, globalAdmin)).thenReturn(template);
        when(apiPolicyRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(Optional.of(
                com.bank.docgen.apimgmt.persistence.ApiPolicyEntity.createSkeleton(TEMPLATE_ID, "U0000001")
        ));
        when(invocationRecordRepository.findByInvocationExternalId("INV-OLD01"))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.regenerate(
                TEMPLATE_ID,
                "INV-OLD01",
                null,
                globalAdmin
        )).isInstanceOf(InvocationRegenerationException.class)
                .extracting(ex -> ((InvocationRegenerationException) ex).errorCode())
                .isEqualTo("INVOCATION_RECORD_EXPIRED");
    }

    @Test
    void regenerate_rejectsMissingFingerprint() {
        ApiInvocationRecordEntity legacy = regenerableInvocation(InvocationKind.SINGLE);
        legacy.applyReleaseBundleFingerprint(null, null);
        when(groupAccessService.canRegenerateInvocation(globalAdmin)).thenReturn(true);
        when(templateService.requireReadableTemplate(TEMPLATE_ID, globalAdmin)).thenReturn(template);
        when(apiPolicyRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(Optional.of(
                com.bank.docgen.apimgmt.persistence.ApiPolicyEntity.createSkeleton(TEMPLATE_ID, "U0000001")
        ));
        when(invocationRecordRepository.findByInvocationExternalId("INV-LEGACY"))
                .thenReturn(Optional.of(legacy));

        assertThatThrownBy(() -> service.regenerate(
                TEMPLATE_ID,
                "INV-LEGACY",
                null,
                globalAdmin
        )).isInstanceOf(InvocationRegenerationException.class)
                .extracting(ex -> ((InvocationRegenerationException) ex).errorCode())
                .isEqualTo("RELEASE_BUNDLE_SNAPSHOT_UNAVAILABLE");
    }

    @Test
    void regenerate_rejectsUnsupportedOutputFormatEarly() {
        ApiInvocationRecordEntity invocation = regenerableInvocation(InvocationKind.SINGLE);
        when(groupAccessService.canRegenerateInvocation(globalAdmin)).thenReturn(true);
        when(templateService.requireReadableTemplate(TEMPLATE_ID, globalAdmin)).thenReturn(template);
        when(apiPolicyRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(Optional.of(
                com.bank.docgen.apimgmt.persistence.ApiPolicyEntity.createSkeleton(TEMPLATE_ID, "U0000001")
        ));
        when(invocationRecordRepository.findByInvocationExternalId("INV-OK01"))
                .thenReturn(Optional.of(invocation));

        assertThatThrownBy(() -> service.regenerate(
                TEMPLATE_ID,
                "INV-OK01",
                new ManagementInvocationRegenerateRequest("XLSX"),
                globalAdmin
        )).isInstanceOf(InvocationRegenerationException.class)
                .extracting(ex -> ((InvocationRegenerationException) ex).errorCode())
                .isEqualTo(ApiErrorCodes.OUTPUT_FORMAT_NOT_ALLOWED);

        verify(assemblySupport, never()).assembleSpecimen(any(), any(), any(), any());
        ArgumentCaptor<InvocationRegeneratedAuditDetail> audit =
                ArgumentCaptor.forClass(InvocationRegeneratedAuditDetail.class);
        verify(managementAuditRecorder).recordInvocationRegenerated(audit.capture());
        assertThat(audit.getValue().errorCode()).isEqualTo(ApiErrorCodes.OUTPUT_FORMAT_NOT_ALLOWED);
        assertThat(audit.getValue().templateId()).isEqualTo(TEMPLATE_ID);
        assertThat(audit.getValue().groupCode()).isEqualTo("GRP-A");
    }

    @Test
    void regenerate_mapsUnexpectedRuntimeAsInternalNotSpecimenWatermark() {
        ApiInvocationRecordEntity invocation = regenerableInvocation(InvocationKind.SINGLE);
        stubAuthorizedInvocation(invocation);
        when(assemblySupport.assembleSpecimen(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("storage unavailable"));

        assertThatThrownBy(() -> service.regenerate(
                TEMPLATE_ID,
                "INV-OK01",
                new ManagementInvocationRegenerateRequest("PDF"),
                globalAdmin
        )).isInstanceOf(InvocationRegenerationException.class)
                .extracting(ex -> ((InvocationRegenerationException) ex).errorCode())
                .isEqualTo(ApiErrorCodes.INTERNAL_ERROR);

        ArgumentCaptor<InvocationRegeneratedAuditDetail> audit =
                ArgumentCaptor.forClass(InvocationRegeneratedAuditDetail.class);
        verify(managementAuditRecorder).recordInvocationRegenerated(audit.capture());
        assertThat(audit.getValue().errorCode()).isEqualTo(ApiErrorCodes.INTERNAL_ERROR);
        assertThat(audit.getValue().errorCode()).isNotEqualTo(ApiErrorCodes.SPECIMEN_WATERMARK_FAILED);
    }

    @Test
    void regenerate_mapsRenderingGenerationFailedAsInternalNotSpecimen() {
        ApiInvocationRecordEntity invocation = regenerableInvocation(InvocationKind.SINGLE);
        stubAuthorizedInvocation(invocation);
        when(assemblySupport.assembleSpecimen(any(), any(), any(), any()))
                .thenThrow(new RenderingOperationException("api.error.rendering.generationFailed"));

        assertThatThrownBy(() -> service.regenerate(
                TEMPLATE_ID,
                "INV-OK01",
                null,
                globalAdmin
        )).isInstanceOf(InvocationRegenerationException.class)
                .extracting(ex -> ((InvocationRegenerationException) ex).errorCode())
                .isEqualTo(ApiErrorCodes.INTERNAL_ERROR);

        ArgumentCaptor<InvocationRegeneratedAuditDetail> audit =
                ArgumentCaptor.forClass(InvocationRegeneratedAuditDetail.class);
        verify(managementAuditRecorder).recordInvocationRegenerated(audit.capture());
        assertThat(audit.getValue().errorCode()).isNotEqualTo(ApiErrorCodes.SPECIMEN_WATERMARK_FAILED);
    }

    @Test
    void regenerate_mapsDocxAssemblyFailureAsRenderingFailedNotSpecimen() {
        ApiInvocationRecordEntity invocation = regenerableInvocation(InvocationKind.SINGLE);
        stubAuthorizedInvocation(invocation);
        when(assemblySupport.assembleSpecimen(any(), any(), any(), any()))
                .thenThrow(new DocxAssemblyException(new RuntimeException("assemble boom")));

        assertThatThrownBy(() -> service.regenerate(
                TEMPLATE_ID,
                "INV-OK01",
                null,
                globalAdmin
        )).isInstanceOf(InvocationRegenerationException.class)
                .extracting(ex -> ((InvocationRegenerationException) ex).errorCode())
                .isEqualTo(ApiErrorCodes.RENDERING_FAILED);

        ArgumentCaptor<InvocationRegeneratedAuditDetail> audit =
                ArgumentCaptor.forClass(InvocationRegeneratedAuditDetail.class);
        verify(managementAuditRecorder).recordInvocationRegenerated(audit.capture());
        assertThat(audit.getValue().errorCode()).isEqualTo(ApiErrorCodes.RENDERING_FAILED);
    }

    @Test
    void regenerate_successAuditIncludesTemplateScope() {
        ApiInvocationRecordEntity invocation = regenerableInvocation(InvocationKind.SINGLE);
        stubAuthorizedInvocation(invocation);
        when(assemblySupport.assembleSpecimen(any(), any(), eq("PDF"), any(UUID.class)))
                .thenReturn(new InvocationRegenerationAssemblySupport.AssembledRegeneration(
                        "regenerations/regen-1/output.pdf",
                        "application/pdf"
                ));
        when(regenerationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.regenerate(
                TEMPLATE_ID,
                "INV-OK01",
                new ManagementInvocationRegenerateRequest("PDF"),
                globalAdmin
        );

        ArgumentCaptor<InvocationRegeneratedAuditDetail> audit =
                ArgumentCaptor.forClass(InvocationRegeneratedAuditDetail.class);
        verify(managementAuditRecorder).recordInvocationRegenerated(audit.capture());
        assertThat(audit.getValue().templateId()).isEqualTo(TEMPLATE_ID);
        assertThat(audit.getValue().groupCode()).isEqualTo("GRP-A");
        assertThat(audit.getValue().outcome()).isEqualTo("SUCCESS");
    }

    private void stubAuthorizedInvocation(ApiInvocationRecordEntity invocation) {
        when(groupAccessService.canRegenerateInvocation(globalAdmin)).thenReturn(true);
        when(templateService.requireReadableTemplate(TEMPLATE_ID, globalAdmin)).thenReturn(template);
        when(apiPolicyRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(Optional.of(
                com.bank.docgen.apimgmt.persistence.ApiPolicyEntity.createSkeleton(TEMPLATE_ID, "U0000001")
        ));
        when(invocationRecordRepository.findByInvocationExternalId(invocation.getInvocationExternalId()))
                .thenReturn(Optional.of(invocation));
    }

    private ApiInvocationRecordEntity regenerableInvocation(InvocationKind kind) {
        String id = kind == InvocationKind.BATCH_ROOT ? "INV-ROOT01" : "INV-OK01";
        return regenerableInvocation(kind, id, Instant.now().plusSeconds(3600));
    }

    private ApiInvocationRecordEntity regenerableInvocation(
            InvocationKind kind,
            String invocationId,
            Instant recordExpiresAt
    ) {
        Instant now = Instant.now();
        ApiInvocationRecordEntity entity = new ApiInvocationRecordEntity(
                UUID.randomUUID(),
                invocationId,
                kind,
                InvocationStatus.SUCCEEDED,
                "dev",
                TEMPLATE_ID,
                "TPL-001",
                UUID.randomUUID(),
                "svc-account",
                "req-1",
                "idem-1",
                "EXPLICIT_VERSION",
                "1.0.0",
                "1.0.0",
                "PDF",
                "SYNC_STREAM",
                "SUCCESS",
                12L,
                "{\"variables\":{\"name\":\"Alice\"},\"variablesHash\":\"" + "c".repeat(64) + "\"}",
                "DOC-1",
                "generated/doc-1/output.pdf",
                true,
                recordExpiresAt,
                now.plusSeconds(1800),
                null,
                null,
                null,
                null,
                null,
                "audit-1",
                kind != InvocationKind.SINGLE,
                now,
                now
        );
        entity.applyReleaseBundleFingerprint(SNAPSHOT_ID, HASH);
        return entity;
    }

    private static ManagementSessionClaims session(List<String> roles, List<String> groups) {
        return new ManagementSessionClaims(
                "admin",
                "Admin",
                "admin@example.com",
                AuthSource.LOCAL,
                roles,
                groups,
                "/",
                List.of(),
                Instant.now().plusSeconds(3600)
        );
    }
}
