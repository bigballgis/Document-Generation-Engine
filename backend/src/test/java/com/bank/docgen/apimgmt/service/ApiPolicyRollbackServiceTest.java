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
import com.bank.docgen.apimgmt.api.RollbackApiPolicyRequest;
import com.bank.docgen.apimgmt.mapping.ApiPolicyViewMapperFactory;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyVersionEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyVersionRepository;
import com.bank.docgen.audit.api.PolicyUpdateAuditDetail;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
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
class ApiPolicyRollbackServiceTest {

    @Mock
    private TemplateService templateService;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private ApiPolicyVersionRepository apiPolicyVersionRepository;
    @Mock
    private ManagementAuditRecorder managementAuditRecorder;
    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private GroupAccessService groupAccessService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TemplateAdGroupAuthorizationCache authorizationCache = new TemplateAdGroupAuthorizationCache();

    private ApiPolicyRollbackService rollbackService;
    private ApiPolicyImpactPreviewService previewService;
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
        rollbackService = new ApiPolicyRollbackService(
                templateService,
                apiPolicyRepository,
                apiPolicyVersionRepository,
                previewService,
                snapshotService,
                managementAuditRecorder,
                authorizationCache,
                groupAccessService,
                objectMapper,
                ApiPolicyViewMapperFactory.create(objectMapper)
        );
        groupAdmin = session(List.of("GROUP_ADMIN"));
        templateId = UUID.randomUUID();
        template = publishedTemplate(templateId);

        lenient().when(templateService.requireReadableTemplate(templateId, groupAdmin)).thenReturn(template);
        lenient().when(apiPolicyRepository.save(any(ApiPolicyEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(apiPolicyVersionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(groupAccessService.canManageApiPolicy(groupAdmin)).thenReturn(true);
    }

    @Test
    void rollback_toPreviousVersion_createsNewHigherVersion() throws Exception {
        ApiPolicyEntity head = existingPolicy(templateId, 3);
        head.replaceConfiguration(
                "[\"RETAIL_API\"]",
                "1.0.0",
                "[\"DOCX\",\"PDF\",\"HTML\"]",
                "[\"SYNC_STREAM\"]",
                false,
                10,
                false,
                false,
                "10000001"
        );
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(head));
        when(apiPolicyVersionRepository.findByTemplateIdAndPolicyVersion(templateId, 1))
                .thenReturn(Optional.of(historySnapshot(templateId, 1, versionOneConfigJson())));
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(version(templateId, "1.0.0", TemplateLifecycleStatus.PUBLISHED)));

        rollbackService.rollback(templateId, new RollbackApiPolicyRequest(1, true), groupAdmin);

        assertThat(head.getPolicyVersion()).isEqualTo(4);
        assertThat(head.getOutputFormatsJson()).doesNotContain("HTML");
        assertThat(head.getOutputFormatsJson()).doesNotContain("PDF");
        assertThat(head.getOutputFormatsJson()).contains("DOCX");
        verify(apiPolicyVersionRepository).save(any(ApiPolicyVersionEntity.class));
    }

    @Test
    void rollback_runsImpactPreview_andRespectsHardBlock() throws Exception {
        ApiPolicyEntity head = existingPolicy(templateId, 2);
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(head));
        when(apiPolicyVersionRepository.findByTemplateIdAndPolicyVersion(templateId, 1))
                .thenReturn(Optional.of(historySnapshot(templateId, 1, blockedDefaultRouteConfigJson())));
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(version(templateId, "1.0.0", TemplateLifecycleStatus.PUBLISHED)));

        ApiPolicyImpactPreviewView preview = rollbackService.previewRollback(templateId, 1, groupAdmin);
        assertThat(preview.blocking()).isTrue();
        assertThat(preview.defaultRouteImpacted()).isTrue();

        assertThatThrownBy(() -> rollbackService.rollback(
                templateId,
                new RollbackApiPolicyRequest(1, true),
                groupAdmin
        ))
                .isInstanceOf(TemplateValidationException.class)
                .hasMessageContaining("api.error.apimgmt.policyImpactBlocked");

        assertThat(head.getPolicyVersion()).isEqualTo(2);
        verifyNoInteractions(managementAuditRecorder);
    }

    @Test
    void rollback_audit_recordsRollbackFlag_andSourceVersion() throws Exception {
        ApiPolicyEntity head = existingPolicy(templateId, 3);
        head.replaceConfiguration(
                "[\"RETAIL_API\"]",
                "1.0.0",
                "[\"DOCX\",\"PDF\"]",
                "[\"SYNC_STREAM\"]",
                false,
                10,
                false,
                false,
                "10000001"
        );
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(head));
        when(apiPolicyVersionRepository.findByTemplateIdAndPolicyVersion(templateId, 1))
                .thenReturn(Optional.of(historySnapshot(templateId, 1, versionOneConfigJson())));
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(version(templateId, "1.0.0", TemplateLifecycleStatus.PUBLISHED)));

        rollbackService.rollback(templateId, new RollbackApiPolicyRequest(1, true), groupAdmin);

        ArgumentCaptor<PolicyUpdateAuditDetail> auditCaptor = ArgumentCaptor.forClass(PolicyUpdateAuditDetail.class);
        verify(managementAuditRecorder).recordPolicyUpdated(
                eq(templateId),
                eq("RETAIL"),
                eq(3),
                eq(4),
                any(),
                eq("10000002"),
                any(),
                auditCaptor.capture()
        );
        assertThat(auditCaptor.getValue().rollback()).isTrue();
        assertThat(auditCaptor.getValue().rollbackSourcePolicyVersion()).isEqualTo(1);
        assertThat(auditCaptor.getValue().confirmed()).isTrue();
    }

    private String versionOneConfigJson() throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "allowedAdGroups", List.of("RETAIL_API"),
                "defaultRouteReleaseVersion", "1.0.0",
                "outputFormats", List.of("DOCX"),
                "outputModes", List.of("SYNC_STREAM"),
                "batchEnabled", false,
                "maxBatchSize", 10,
                "batchSyncMaxItems", 10,
                "batchAsyncMaxItems", 10000,
                "docxEncryptionEnabled", false,
                "pdfEncryptionEnabled", false
        ));
    }

    private String blockedDefaultRouteConfigJson() throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "allowedAdGroups", List.of("RETAIL_API"),
                "defaultRouteReleaseVersion", "2.0.0",
                "outputFormats", List.of("DOCX"),
                "outputModes", List.of("SYNC_STREAM"),
                "batchEnabled", false,
                "maxBatchSize", 10,
                "batchSyncMaxItems", 10,
                "batchAsyncMaxItems", 10000,
                "docxEncryptionEnabled", false,
                "pdfEncryptionEnabled", false
        ));
    }

    private ApiPolicyVersionEntity historySnapshot(UUID templateId, int version, String configJson) {
        return new ApiPolicyVersionEntity(
                UUID.randomUUID(),
                templateId,
                version,
                "[\"OUTPUT_POLICY\"]",
                configJson,
                "10000001",
                Instant.parse("2026-01-01T00:00:00Z")
        );
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
