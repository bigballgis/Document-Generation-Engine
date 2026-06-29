package com.bank.docgen.apimgmt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bank.docgen.authoring.structured.FidelityValidationService;
import com.bank.docgen.apimgmt.api.ApiPolicyImpactPreviewView;
import com.bank.docgen.apimgmt.api.SaveDefaultRouteRequest;
import com.bank.docgen.apimgmt.mapping.ApiPolicyViewMapperFactory;
import com.bank.docgen.apimgmt.persistence.ApiCredentialRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyVersionRepository;
import com.bank.docgen.audit.api.PolicyUpdateAuditDetail;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.runtime.service.ContractAssemblyService;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.runtime.service.EncryptionParameterValidator;
import com.bank.docgen.runtime.service.IdempotencyConflictException;
import com.bank.docgen.runtime.service.IdempotencyService;
import com.bank.docgen.runtime.service.RuntimeGenerationService;
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
class DefaultRouteGovernanceTest {

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
    private TemplateAdGroupAuthorizationCache authorizationCache;
    @Mock
    private GroupAccessService groupAccessService;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private EncryptionParameterValidator encryptionParameterValidator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ApiManagementService managementService;
    private ApiPolicyImpactPreviewService previewService;
    private RuntimeGenerationService runtimeService;
    private ManagementSessionClaims groupAdmin;
    private UUID templateId;
    private TemplateEntity template;

    @BeforeEach
    void setUp() {
        ApiPolicyVersionSnapshotService snapshotService =
                new ApiPolicyVersionSnapshotService(apiPolicyVersionRepository, objectMapper);
        previewService = new ApiPolicyImpactPreviewService(
                templateService,
                apiPolicyRepository,
                templateVersionRepository,
                groupAccessService,
                objectMapper
        );
        managementService = new ApiManagementService(
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
                previewService,
                ApiPolicyViewMapperFactory.create(objectMapper)
        );
        runtimeService = new RuntimeGenerationService(
                templateVersionRepository,
                apiPolicyRepository,
                apiCredentialRepository,
                null,
                idempotencyService,
                encryptionParameterValidator,
                contractAssemblyService,
                null,
                objectMapper,
                mock(FidelityValidationService.class)
        );
        groupAdmin = session(List.of("GROUP_ADMIN"));
        templateId = UUID.randomUUID();
        template = publishedTemplate(templateId);

        lenient().when(templateService.requireReadableTemplate(templateId, groupAdmin)).thenReturn(template);
        lenient().when(apiPolicyRepository.save(any(ApiPolicyEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(apiPolicyVersionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(groupAccessService.canManageApiPolicy(groupAdmin)).thenReturn(true);
        lenient().when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(
                        version(templateId, "1.0.0", TemplateLifecycleStatus.PUBLISHED),
                        version(templateId, "2.0.0", TemplateLifecycleStatus.PUBLISHED)
                ));
    }

    @Test
    void setDefaultRoute_toStoppedVersion_isHardBlock() {
        ApiPolicyEntity existing = existingPolicy(templateId, 2);
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(existing));
        TemplateVersionEntity stopped = version(templateId, "2.0.0", TemplateLifecycleStatus.STOPPED);
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(templateId, "2.0.0"))
                .thenReturn(Optional.of(stopped));

        assertThatThrownBy(() -> managementService.saveDefaultRouteDomain(
                templateId,
                new SaveDefaultRouteRequest("2.0.0", false),
                groupAdmin
        ))
                .isInstanceOf(TemplateValidationException.class)
                .hasMessageContaining("api.error.apimgmt.defaultRouteTargetNotCallable");

        assertThat(existing.getPolicyVersion()).isEqualTo(2);
        verifyNoInteractions(managementAuditRecorder);
    }

    @Test
    void setDefaultRoute_validTarget_bumpsVersion_auditsDefaultRouteTarget() {
        ApiPolicyEntity existing = existingPolicy(templateId, 2);
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(existing));
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(templateId, "2.0.0"))
                .thenReturn(Optional.of(version(templateId, "2.0.0", TemplateLifecycleStatus.PUBLISHED)));

        managementService.saveDefaultRouteDomain(
                templateId,
                new SaveDefaultRouteRequest("2.0.0", true),
                groupAdmin
        );

        assertThat(existing.getPolicyVersion()).isEqualTo(3);
        assertThat(existing.getDefaultRouteReleaseVersion()).isEqualTo("2.0.0");

        ArgumentCaptor<PolicyUpdateAuditDetail> auditCaptor = ArgumentCaptor.forClass(PolicyUpdateAuditDetail.class);
        verify(managementAuditRecorder).recordPolicyUpdated(
                eq(templateId),
                eq("RETAIL"),
                eq(2),
                eq(3),
                eq(List.of("DEFAULT_ROUTE_TARGET")),
                eq("10000002"),
                any(),
                auditCaptor.capture()
        );
        assertThat(auditCaptor.getValue().configDiffSummary())
                .anyMatch(line -> line.contains("1.0.0") && line.contains("2.0.0"));
    }

    @Test
    void preview_includesContractDiffAndIdempotencyImpact() {
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(existingPolicy(templateId, 1)));
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(
                        version(templateId, "1.0.0", TemplateLifecycleStatus.PUBLISHED),
                        version(templateId, "2.0.0", TemplateLifecycleStatus.PUBLISHED)
                ));

        ApiPolicyImpactPreviewView preview = previewService.previewDefaultRoute(
                templateId,
                new SaveDefaultRouteRequest("2.0.0", false),
                groupAdmin
        );

        assertThat(preview.contractDiffSummary()).contains("currentTarget=1.0.0").contains("candidateTarget=2.0.0");
        assertThat(preview.idempotencyImpactSummary())
                .isEqualTo("api.apimgmt.policyImpact.idempotencyDefaultRouteGuard");
        assertThat(preview.defaultRouteImpacted()).isTrue();
        assertThat(preview.blocking()).isFalse();
    }

    @Test
    void staleIdempotencyKey_afterRouteChange_returnsDefaultRouteChangedConflict() {
        ApiPolicyEntity policy = existingPolicy(templateId, 2);
        policy.updateDefaultRouteDomain("2.0.0", "10000001");
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(policy));
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(templateId, "2.0.0"))
                .thenReturn(Optional.of(version(templateId, "2.0.0", TemplateLifecycleStatus.PUBLISHED)));
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(
                        version(templateId, "2.0.0", TemplateLifecycleStatus.PUBLISHED),
                        version(templateId, "1.0.0", TemplateLifecycleStatus.PUBLISHED)
                ));

        when(idempotencyService.hashRequest(any())).thenAnswer(invocation -> {
            String payload = invocation.getArgument(0);
            return payload.contains("\"releaseVersion\":\"2.0.0\"") ? "hash-new-route" : "hash-old-route";
        });
        when(idempotencyService.findExisting(eq("idem-route"), eq(templateId), eq("hash-new-route")))
                .thenThrow(new IdempotencyConflictException("idem-route"));
        when(idempotencyService.findLiveRecord("idem-route", templateId))
                .thenReturn(Optional.of(new com.bank.docgen.runtime.persistence.GenerationIdempotencyEntity(
                        UUID.randomUUID(),
                        "idem-route",
                        templateId,
                        "hash-old-route",
                        "COMPLETED",
                        Instant.now().plusSeconds(3600)
                )));

        lenient().doNothing().when(encryptionParameterValidator).validate(any(), any(), any());

        assertThatThrownBy(() -> runtimeService.generateSync(
                template,
                new RuntimeSessionClaims(
                        UUID.randomUUID(),
                        "CRED-1",
                        templateId,
                        "TPL-001",
                        "svc-caller",
                        List.of("RETAIL_API")
                ),
                null,
                new com.bank.docgen.runtime.api.GenerateRequestBody(
                        new com.bank.docgen.runtime.api.OutputOptionsView("DOCX", "SYNC_STREAM"),
                        java.util.Map.of("customerName", "Alice"),
                        new com.bank.docgen.sharedkernel.api.EncryptionOptionsView(false, null, null, List.of()),
                        "req-1",
                        "idem-route"
                )
        ))
                .isInstanceOf(IdempotencyConflictException.class)
                .satisfies(ex -> {
                    IdempotencyConflictException conflict = (IdempotencyConflictException) ex;
                    assertThat(conflict.conflictType())
                            .isEqualTo(IdempotencyConflictException.DEFAULT_ROUTE_CHANGED);
                    assertThat(conflict.originalResolvedReleaseVersion()).isEqualTo("1.0.0");
                });
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

    private TemplateVersionEntity version(UUID templateId, String releaseVersion, TemplateLifecycleStatus status) {
        TemplateVersionEntity version = new TemplateVersionEntity(UUID.randomUUID(), templateId, "10000001");
        version.setReleaseVersion(releaseVersion);
        version.setLifecycleStatus(status);
        return version;
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
