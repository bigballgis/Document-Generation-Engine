package com.bank.docgen.apimgmt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.api.ApiPolicyImpactPreviewView;
import com.bank.docgen.apimgmt.api.SaveAdGroupsRequest;
import com.bank.docgen.apimgmt.api.SaveBatchLimitsRequest;
import com.bank.docgen.apimgmt.api.SaveDefaultRouteRequest;
import com.bank.docgen.apimgmt.api.SaveEncryptionPolicyRequest;
import com.bank.docgen.apimgmt.api.SaveInvocationRetentionRequest;
import com.bank.docgen.apimgmt.api.SaveOutputPolicyRequest;
import com.bank.docgen.apimgmt.api.SaveDefaultRouteRequest;
import com.bank.docgen.apimgmt.persistence.ApiCredentialRepository;
import com.bank.docgen.apimgmt.mapping.ApiPolicyViewMapperFactory;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyVersionRepository;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.runtime.service.ContractAssemblyService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.sharedkernel.security.PasswordHashService;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.service.TemplateService;
import com.bank.docgen.template.service.TemplateValidationException;
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

@ExtendWith(MockitoExtension.class)
class ApiPolicyDomainSaveServiceTest {

    @Mock
    private TemplateService templateService;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private ApiCredentialRepository apiCredentialRepository;
    @Mock
    private PasswordHashService passwordHashService;
    @Mock
    private ManagementAuditRecorder managementAuditRecorder;
    @Mock
    private ContractAssemblyService contractAssemblyService;
    @Mock
    private ApiPolicyVersionRepository apiPolicyVersionRepository;
    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private ApiPolicyImpactPreviewService apiPolicyImpactPreviewService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TemplateAdGroupAuthorizationCache authorizationCache = new TemplateAdGroupAuthorizationCache();

    private ApiManagementService service;
    private ManagementSessionClaims groupAdmin;
    private UUID templateId;
    private TemplateEntity template;

    @BeforeEach
    void setUp() {
        ApiPolicyVersionSnapshotService snapshotService =
                new ApiPolicyVersionSnapshotService(apiPolicyVersionRepository, objectMapper);
        service = new ApiManagementService(
                templateService,
                apiPolicyRepository,
                apiCredentialRepository,
                new GroupAccessService(),
                passwordHashService,
                managementAuditRecorder,
                contractAssemblyService,
                objectMapper,
                snapshotService,
                templateVersionRepository,
                authorizationCache,
                apiPolicyImpactPreviewService,
                ApiPolicyViewMapperFactory.create(objectMapper)
        );
        groupAdmin = session(List.of("GROUP_ADMIN"));
        templateId = UUID.randomUUID();
        template = publishedTemplate(templateId);

        lenient().when(templateService.requireReadableTemplate(templateId, groupAdmin)).thenReturn(template);
        lenient().when(apiPolicyRepository.save(any(ApiPolicyEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(apiPolicyVersionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(apiPolicyImpactPreviewService.preview(any(), any(), any())).thenReturn(safePreview());
    }

    @Test
    void saveOutputDomain_changesOnlyOutput_bumpsVersion_auditsOutputPolicy() {
        ApiPolicyEntity existing = existingPolicy(templateId, 2);
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(existing));

        service.saveOutputDomain(
                templateId,
                new SaveOutputPolicyRequest(List.of("DOCX", "PDF"), List.of("SYNC_STREAM", "ASYNC_JOB"), true),
                groupAdmin
        );

        assertThat(existing.getPolicyVersion()).isEqualTo(3);
        assertThat(existing.getOutputFormatsJson()).contains("PDF");
        assertThat(existing.getOutputModesJson()).contains("ASYNC_JOB");
        assertThat(existing.getAllowedAdGroupsJson()).isEqualTo("[\"RETAIL_API\"]");
        assertThat(existing.getDefaultRouteReleaseVersion()).isEqualTo("1.0.0");

        verify(managementAuditRecorder).recordPolicyUpdated(
                eq(templateId),
                eq("RETAIL"),
                eq(2),
                eq(3),
                eq(List.of("OUTPUT_POLICY")),
                eq("10000002"),
                any(),
                any()
        );
    }

    @Test
    void saveBatchLimits_persistsSyncAndAsyncMax() {
        ApiPolicyEntity existing = existingPolicy(templateId, 1);
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(existing));

        service.saveBatchLimitsDomain(
                templateId,
                new SaveBatchLimitsRequest(true, 50, 5000, true),
                groupAdmin
        );

        assertThat(existing.getPolicyVersion()).isEqualTo(2);
        assertThat(existing.isBatchEnabled()).isTrue();
        assertThat(existing.getBatchSyncMaxItems()).isEqualTo(50);
        assertThat(existing.getBatchAsyncMaxItems()).isEqualTo(5000);
        assertThat(existing.getMaxBatchSize()).isEqualTo(50);

        verify(managementAuditRecorder).recordPolicyUpdated(
                eq(templateId),
                eq("RETAIL"),
                eq(1),
                eq(2),
                eq(List.of("BATCH_LIMIT")),
                eq("10000002"),
                any(),
                any()
        );
    }

    @Test
    void saveAdGroups_changesOnlyAdGroups_auditsAdGroupAuthorization() {
        ApiPolicyEntity existing = existingPolicy(templateId, 4);
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(existing));

        service.saveAdGroupsDomain(
                templateId,
                new SaveAdGroupsRequest(List.of("RETAIL_API", "WHOLESALE_API"), true),
                groupAdmin
        );

        assertThat(existing.getPolicyVersion()).isEqualTo(5);
        assertThat(existing.getAllowedAdGroupsJson()).contains("WHOLESALE_API");
        assertThat(existing.getOutputFormatsJson()).isEqualTo("[\"DOCX\"]");

        verify(managementAuditRecorder).recordPolicyUpdated(
                eq(templateId),
                eq("RETAIL"),
                eq(4),
                eq(5),
                eq(List.of("AD_GROUP_AUTHORIZATION")),
                eq("10000002"),
                any(),
                any()
        );
    }

    @Test
    void saveEncryption_auditsEncryptionCapability() {
        ApiPolicyEntity existing = existingPolicy(templateId, 1);
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(existing));

        service.saveEncryptionDomain(
                templateId,
                new SaveEncryptionPolicyRequest(true, false, true),
                groupAdmin
        );

        assertThat(existing.getPolicyVersion()).isEqualTo(2);
        assertThat(existing.isDocxEncryptionEnabled()).isTrue();
        assertThat(existing.isPdfEncryptionEnabled()).isFalse();

        ArgumentCaptor<List<String>> changedAreasCaptor = ArgumentCaptor.forClass(List.class);
        verify(managementAuditRecorder).recordPolicyUpdated(
                eq(templateId),
                eq("RETAIL"),
                eq(1),
                eq(2),
                changedAreasCaptor.capture(),
                eq("10000002"),
                any(),
                any()
        );
        assertThat(changedAreasCaptor.getValue()).containsExactly("ENCRYPTION_CAPABILITY");
        assertThat(changedAreasCaptor.getValue()).doesNotContain("ENCRYPTION_POLICY");
    }

    @Test
    void saveDomain_onUnpublishedTemplate_throwsTemplateNotPublished() {
        TemplateEntity draft = publishedTemplate(templateId);
        draft.setLifecycleStatus(TemplateLifecycleStatus.DRAFT);
        lenient().when(templateService.requireReadableTemplate(templateId, groupAdmin)).thenReturn(draft);

        assertThatThrownBy(() -> service.saveOutputDomain(
                templateId,
                new SaveOutputPolicyRequest(List.of("DOCX"), List.of("SYNC_STREAM"), false),
                groupAdmin
        ))
                .isInstanceOf(TemplateValidationException.class)
                .hasMessageContaining("api.error.apimgmt.templateNotPublished");

        verifyNoInteractions(apiPolicyRepository);
    }

    @Test
    void saveDomain_byUnauthorizedGroup_returns403() {
        ManagementSessionClaims author = session(List.of("DOCUMENT_AUTHOR"));

        assertThatThrownBy(() -> service.saveOutputDomain(
                templateId,
                new SaveOutputPolicyRequest(List.of("DOCX"), List.of("SYNC_STREAM"), false),
                author
        ))
                .isInstanceOf(ApiManagementAccessDeniedException.class);

        verifyNoInteractions(templateService, apiPolicyRepository);
    }

    @Test
    void saveDomain_withUnconfirmedWarning_rejectsConfirmationRequired() {
        ApiPolicyEntity existing = existingPolicy(templateId, 2);
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(existing));
        when(apiPolicyImpactPreviewService.preview(any(), any(), any())).thenReturn(
                new ApiPolicyImpactPreviewView(
                        List.of("DEFAULT_ROUTE_TARGET"),
                        false,
                        List.of("api.apimgmt.policyImpact.defaultRouteChanged"),
                        true,
                        2,
                        3,
                        "api.apimgmt.policyImpact.warning",
                        "currentTarget=1.0.0,candidateTarget=2.0.0",
                        "api.apimgmt.policyImpact.idempotencyDefaultRouteGuard"
                )
        );
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(templateId, "2.0.0"))
                .thenReturn(Optional.of(publishedVersion(templateId, "2.0.0")));

        assertThatThrownBy(() -> service.saveDefaultRouteDomain(
                templateId,
                new SaveDefaultRouteRequest("2.0.0", false),
                groupAdmin
        ))
                .isInstanceOf(TemplateValidationException.class)
                .hasMessageContaining("api.error.apimgmt.policyImpactConfirmationRequired");

        assertThat(existing.getPolicyVersion()).isEqualTo(2);
        verifyNoInteractions(managementAuditRecorder);
    }

    @Test
    void saveDomain_withBlockingPreview_rejectsSave() {
        ApiPolicyEntity existing = existingPolicy(templateId, 2);
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(existing));
        when(apiPolicyImpactPreviewService.preview(any(), any(), any())).thenReturn(
                new ApiPolicyImpactPreviewView(
                        List.of("DEFAULT_ROUTE_TARGET"),
                        true,
                        List.of("api.apimgmt.policyImpact.defaultRouteNotCallable"),
                        true,
                        2,
                        3,
                        "api.apimgmt.policyImpact.blocking",
                        null,
                        null
                )
        );
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(templateId, "9.9.9"))
                .thenReturn(Optional.of(publishedVersion(templateId, "9.9.9")));

        assertThatThrownBy(() -> service.saveDefaultRouteDomain(
                templateId,
                new SaveDefaultRouteRequest("9.9.9", true),
                groupAdmin
        ))
                .isInstanceOf(TemplateValidationException.class)
                .hasMessageContaining("api.error.apimgmt.policyImpactBlocked");

        assertThat(existing.getPolicyVersion()).isEqualTo(2);
        verifyNoInteractions(managementAuditRecorder);
    }

    @Test
    void saveInvocationRetentionDomain_bumpsVersionAndRecordsChangedArea() {
        ApiPolicyEntity existing = existingPolicy(templateId, 2);
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(existing));
        when(apiPolicyRepository.save(any(ApiPolicyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var view = service.saveInvocationRetentionDomain(
                templateId,
                new SaveInvocationRetentionRequest(true, 365, 180, true),
                groupAdmin
        );

        assertThat(view.invocationRecordRetentionDays()).isEqualTo(365);
        assertThat(view.documentRetentionDays()).isEqualTo(180);
        assertThat(existing.getPolicyVersion()).isEqualTo(3);
        verify(managementAuditRecorder).recordPolicyUpdated(
                eq(templateId),
                eq("RETAIL"),
                eq(2),
                eq(3),
                eq(List.of("INVOCATION_RETENTION")),
                eq("10000002"),
                any(),
                any()
        );
    }

    @Test
    void saveInvocationRetentionDomain_requiresConfirmationWhenChanged() {
        ApiPolicyEntity existing = existingPolicy(templateId, 2);
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.saveInvocationRetentionDomain(
                templateId,
                new SaveInvocationRetentionRequest(true, 365, 180, false),
                groupAdmin
        ))
                .isInstanceOf(TemplateValidationException.class)
                .hasMessageContaining("api.error.apimgmt.policyImpactConfirmationRequired");
    }

    private ApiPolicyImpactPreviewView safePreview() {
        return new ApiPolicyImpactPreviewView(
                List.of("OUTPUT_POLICY"),
                false,
                List.of(),
                false,
                1,
                2,
                "api.apimgmt.policyImpact.safe",
                null,
                null
        );
    }

    private TemplateVersionEntity publishedVersion(UUID templateId, String releaseVersion) {
        TemplateVersionEntity version = new TemplateVersionEntity(UUID.randomUUID(), templateId, "10000001");
        version.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        version.setReleaseVersion(releaseVersion);
        return version;
    }

    private ManagementSessionClaims session(List<String> roles) {
        return new ManagementSessionClaims(
                "10000002",
                "Admin",
                "admin@example.com",
                AuthSource.LOCAL,
                roles,
                List.of("RETAIL"),
                "route.template-authoring-home",
                List.of("route.template-authoring-home"),
                Instant.now().plusSeconds(3600)
        );
    }

    private TemplateEntity publishedTemplate(UUID id) {
        TemplateEntity entity = new TemplateEntity(
                id,
                "TPL-001",
                "RETAIL",
                "Sample",
                null,
                UUID.randomUUID(),
                "10000001"
        );
        entity.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        entity.setReleaseVersion("1.0.0");
        return entity;
    }

    private ApiPolicyEntity existingPolicy(UUID templateId, int version) {
        ApiPolicyEntity policy = new ApiPolicyEntity(UUID.randomUUID(), templateId, "[\"RETAIL_API\"]", "10000001");
        policy.replaceConfiguration(
                "[\"RETAIL_API\"]",
                "1.0.0",
                "[\"DOCX\"]",
                "[\"SYNC_STREAM\"]",
                false,
                10,
                false,
                false,
                "10000001"
        );
        while (policy.getPolicyVersion() < version) {
            policy.update(
                    "[\"RETAIL_API\"]",
                    "1.0.0",
                    "[\"DOCX\"]",
                    "[\"SYNC_STREAM\"]",
                    false,
                    10,
                    false,
                    false,
                    "10000001"
            );
        }
        return policy;
    }
}
