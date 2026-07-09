package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.api.ApiPolicyView;
import com.bank.docgen.apimgmt.mapping.ApiPolicyViewMapper;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.AnchorBindingView;
import com.bank.docgen.template.api.CompositionRuleView;
import com.bank.docgen.template.api.ContentModuleReferenceView;
import com.bank.docgen.template.api.TemplateExportResult;
import com.bank.docgen.template.api.VariableSchemaView;
import com.bank.docgen.template.domain.BindingValidationStatus;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.domain.VariableType;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class TemplateExportServiceTest {

    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private ApiPolicyViewMapper apiPolicyViewMapper;
    @Mock
    private TemplateContentModuleReferenceService contentModuleReferenceService;
    @Mock
    private ManagementAuditRecorder managementAuditRecorder;
    @Mock
    private TemplateService templateService;
    @Mock
    private TemplateCurrentVersionResolver templateCurrentVersionResolver;

    private TemplateExportService service;
    private UUID templateId;
    private TemplateEntity template;
    private TemplateVersionEntity version;
    private ManagementSessionClaims groupAdmin;

    @BeforeEach
    void setUp() {
        service = new TemplateExportService(
                templateRepository,
                templateVersionRepository,
                apiPolicyRepository,
                apiPolicyViewMapper,
                contentModuleReferenceService,
                managementAuditRecorder,
                templateService,
                new TemplateExportAccessSupport(new GroupAccessService()),
                new ObjectMapper().findAndRegisterModules(),
                templateCurrentVersionResolver
        );
        templateId = UUID.randomUUID();
        template = new TemplateEntity(
                templateId,
                "TPL-RETAIL-LETTER",
                "RETAIL",
                "Retail Letter",
                "Slice template",
                UUID.randomUUID(),
                "10000003"
        );
        template.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        template.setReleaseVersion("1.0.0");
        version = new TemplateVersionEntity(UUID.randomUUID(), templateId, "10000003");
        groupAdmin = session("10000002", List.of("GROUP_ADMIN"), List.of("RETAIL"));
    }

    @Test
    void exportJson_buildsBundleWithoutCredentials() {
        stubReadableTemplate();
        when(templateCurrentVersionResolver.requireExportableVersion(templateId))
                .thenReturn(version);
        when(templateService.loadRules(version)).thenReturn(List.of(
                new CompositionRuleView("rule-1", "true", "HEADER", "", "")
        ));
        when(templateService.toDetail(template)).thenReturn(detailWithArtifacts());
        ApiPolicyEntity policyEntity = org.mockito.Mockito.mock(ApiPolicyEntity.class);
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(policyEntity));
        when(apiPolicyViewMapper.toPolicyView(policyEntity)).thenReturn(policyView());
        when(contentModuleReferenceService.listReferences(templateId, groupAdmin)).thenReturn(List.of(
                new ContentModuleReferenceView("clause-a", "MOD-1", "1.0.0", true)
        ));

        TemplateExportResult result = service.exportJson(templateId, groupAdmin);

        assertThat(result.format()).isEqualTo(TemplateExportService.EXPORT_FORMAT);
        assertThat(result.bundle().metadata().externalId()).isEqualTo("TPL-RETAIL-LETTER");
        assertThat(result.bundle().variables()).hasSize(1);
        assertThat(result.bundle().bindings()).hasSize(1);
        assertThat(result.bundle().rules()).hasSize(1);
        assertThat(result.bundle().contentModuleReferences()).hasSize(1);
        assertThat(result.bundle().policySnapshot()).isNotNull();
        assertThat(result.bundle().policySnapshot().allowedAdGroups()).containsExactly("RETAIL_API");
        verify(managementAuditRecorder).recordTemplateExported(
                eq(templateId),
                eq("RETAIL"),
                eq("TPL-RETAIL-LETTER"),
                eq("10000002"),
                any()
        );
    }

    @Test
    void exportJson_rejectsDraftTemplate() {
        template.setLifecycleStatus(TemplateLifecycleStatus.DRAFT);
        stubReadableTemplate();

        assertThatThrownBy(() -> service.exportJson(templateId, groupAdmin))
                .isInstanceOf(TemplateValidationException.class);
    }

    @Test
    void exportZip_containsBundleJsonEntry() throws Exception {
        stubReadableTemplate();
        when(templateCurrentVersionResolver.requireExportableVersion(templateId))
                .thenReturn(version);
        when(templateService.loadRules(version)).thenReturn(List.of());
        when(templateService.toDetail(template)).thenReturn(detailWithArtifacts());
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.empty());
        when(contentModuleReferenceService.listReferences(templateId, groupAdmin)).thenReturn(List.of());

        TemplateExportService.TemplateExportZipArtifact artifact = service.exportZip(templateId, groupAdmin);

        assertThat(artifact.filename()).isEqualTo("TPL-RETAIL-LETTER-export.zip");
        assertThat(artifact.content()).isNotEmpty();
        try (var zip = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(artifact.content()))) {
            var entry = zip.getNextEntry();
            assertThat(entry.getName()).isEqualTo("template-export-bundle.json");
            String json = new String(zip.readAllBytes());
            assertThat(json).contains("template-export-bundle-v1-json");
            assertThat(json).doesNotContain("credential");
            assertThat(json).doesNotContain("secret");
        }
    }

    private void stubReadableTemplate() {
        when(templateRepository.findByIdAndDeletedAtIsNull(templateId)).thenReturn(Optional.of(template));
    }

    private com.bank.docgen.template.api.TemplateDetailView detailWithArtifacts() {
        return new com.bank.docgen.template.api.TemplateDetailView(
                templateId.toString(),
                "TPL-RETAIL-LETTER",
                "RETAIL",
                "Retail Letter",
                "Slice template",
                template.getMasterId().toString(),
                TemplateLifecycleStatus.PUBLISHED,
                null,
                "1.0.0",
                version.getId().toString(),
                1,
                List.of(new VariableSchemaView(
                        UUID.randomUUID().toString(),
                        "customerName",
                        VariableType.TEXT,
                        true,
                        "Customer",
                        null,
                        "Customer name",
                        null
                )),
                List.of(new AnchorBindingView(
                        UUID.randomUUID().toString(),
                        "HEADER",
                        "TEXT",
                        "{\"nodes\":[]}",
                        BindingValidationStatus.VALID
                )),
                List.of(),
                Instant.now(),
                Instant.now(),
                "test-user",
                "Test User",
                true
        );
    }

    private ApiPolicyView policyView() {
        return new ApiPolicyView(
                templateId.toString(),
                1,
                List.of("RETAIL_API"),
                "1.0.0",
                List.of("DOCX"),
                List.of("SYNC_STREAM"),
                false,
                10,
                100,
                10000,
                false,
                false,
                true,
                90,
                30,
                Instant.now()
        );
    }

    private static ManagementSessionClaims session(
            String username,
            List<String> roles,
            List<String> authorizedGroupCodes
    ) {
        return new ManagementSessionClaims(
                username,
                username,
                username + "@example.com",
                AuthSource.LOCAL,
                roles,
                authorizedGroupCodes,
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                Instant.now().plusSeconds(3600)
        );
    }
}
