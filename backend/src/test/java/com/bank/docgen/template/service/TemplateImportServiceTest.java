package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.ImportTemplateRequest;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.api.TemplateExportMetadataView;
import com.bank.docgen.template.api.TemplateImportResult;
import com.bank.docgen.template.api.VariableSchemaView;
import com.bank.docgen.template.domain.TemplateImportConflictPolicy;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemplateImportServiceTest {

    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private MasterDocumentRepository masterDocumentRepository;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private TemplateService templateService;
    @Mock
    private TemplateContentModuleReferenceService contentModuleReferenceService;
    @Mock
    private ManagementAuditRecorder managementAuditRecorder;
    @Mock
    private TemplateCurrentVersionResolver templateCurrentVersionResolver;

    private TemplateImportService service;
    private UUID templateId;
    private UUID masterId;
    private TemplateExportBundleView bundle;
    private ManagementSessionClaims groupAdmin;

    @BeforeEach
    void setUp() {
        service = new TemplateImportService(
                templateRepository,
                templateVersionRepository,
                masterDocumentRepository,
                apiPolicyRepository,
                templateService,
                contentModuleReferenceService,
                managementAuditRecorder,
                new TemplateExportAccessSupport(new GroupAccessService()),
                new TemplateImportBundleValidator(new ObjectMapper().findAndRegisterModules()),
                new ObjectMapper().findAndRegisterModules(),
                templateCurrentVersionResolver
        );
        templateId = UUID.randomUUID();
        masterId = UUID.randomUUID();
        bundle = exportBundle();
        groupAdmin = session("10000002", List.of("GROUP_ADMIN"), List.of("RETAIL"));
    }

    @Test
    void importBundle_createsDraftTemplate() {
        stubFreshImport();

        TemplateImportResult result = service.importBundle(
                new ImportTemplateRequest(masterId.toString(), bundle, null),
                groupAdmin
        );

        assertThat(result.template().lifecycleStatus()).isEqualTo(TemplateLifecycleStatus.DRAFT);
        assertThat(result.importSummary().resolvedTemplateId()).isEqualTo(templateId.toString());
        assertThat(result.importSummary().newDevelopmentVersion()).isEqualTo(1);
        assertThat(result.importSummary().importBatchId()).isNotBlank();
        verify(templateService).upsertVariable(eq(templateId), any(), eq(groupAdmin));
        verify(templateService, never()).upsertBinding(any(), any(), any());
        verify(managementAuditRecorder).recordTemplateImported(
                eq(templateId),
                eq("RETAIL"),
                eq("TPL-IMPORT-LETTER"),
                any(),
                eq(1),
                eq("10000002"),
                any()
        );
    }

    @Test
    void importBundle_rejectsConflictByDefault() {
        when(templateRepository.findByIdAndDeletedAtIsNull(templateId))
                .thenReturn(Optional.of(existingTemplate()));

        assertThatThrownBy(() -> service.importBundle(
                new ImportTemplateRequest(masterId.toString(), bundle, null),
                groupAdmin
        )).isInstanceOf(TemplateValidationException.class);
    }

    @Test
    void importBundle_keepTemplateId_resetsExistingTemplateToDraft() {
        TemplateEntity existing = existingTemplate();
        existing.setLifecycleStatus(TemplateLifecycleStatus.DRAFT);
        TemplateVersionEntity version = new TemplateVersionEntity(UUID.randomUUID(), templateId, "10000003");

        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(masterId)).thenReturn(Optional.of(approvedMaster()));
        when(templateRepository.findByIdAndDeletedAtIsNull(templateId)).thenReturn(Optional.of(existing));
        when(templateRepository.findByExternalIdAndDeletedAtIsNull("TPL-IMPORT-LETTER")).thenReturn(Optional.of(existing));
        when(templateCurrentVersionResolver.requireLatestVersion(templateId)).thenReturn(version);
        when(templateService.toDetail(existing)).thenReturn(detail(TemplateLifecycleStatus.DRAFT));

        TemplateImportResult result = service.importBundle(
                new ImportTemplateRequest(
                        masterId.toString(),
                        bundle,
                        TemplateImportConflictPolicy.KEEP_TEMPLATE_ID
                ),
                groupAdmin
        );

        assertThat(result.template().lifecycleStatus()).isEqualTo(TemplateLifecycleStatus.DRAFT);
        ArgumentCaptor<TemplateEntity> saved = ArgumentCaptor.forClass(TemplateEntity.class);
        verify(templateRepository).save(saved.capture());
        assertThat(saved.getValue().getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.DRAFT);
        assertThat(saved.getValue().getReleaseVersion()).isNull();
    }

    @Test
    void importBundle_deniesTester() {
        ManagementSessionClaims tester = session("10000006", List.of("TEMPLATE_TESTER"), List.of("RETAIL"));

        assertThatThrownBy(() -> service.importBundle(
                new ImportTemplateRequest(masterId.toString(), bundle, null),
                tester
        )).isInstanceOf(TemplateAccessDeniedException.class);
    }

    private void stubFreshImport() {
        MasterDocumentEntity master = approvedMaster();
        TemplateEntity importedTemplate = new TemplateEntity(
                templateId,
                "TPL-IMPORT-LETTER",
                "RETAIL",
                "Import Letter",
                "Import test template",
                masterId,
                "10000002"
        );
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(masterId)).thenReturn(Optional.of(master));
        when(templateRepository.findByExternalIdAndDeletedAtIsNull("TPL-IMPORT-LETTER")).thenReturn(Optional.empty());
        when(templateRepository.findByIdAndDeletedAtIsNull(templateId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(importedTemplate));
        when(templateService.toDetail(importedTemplate)).thenReturn(detail(TemplateLifecycleStatus.DRAFT));
    }

    private MasterDocumentEntity approvedMaster() {
        MasterDocumentEntity master = new MasterDocumentEntity(
                masterId,
                "RETAIL",
                "Retail Master",
                "Retail master document",
                "storage/master.docx",
                "master.docx",
                "10000002"
        );
        master.setStatus(MasterDocumentStatus.APPROVED);
        return master;
    }

    private TemplateEntity existingTemplate() {
        return new TemplateEntity(
                templateId,
                "TPL-IMPORT-LETTER",
                "RETAIL",
                "Import Letter",
                "Import test template",
                masterId,
                "10000003"
        );
    }

    private TemplateExportBundleView exportBundle() {
        return new TemplateExportBundleView(
                TemplateExportService.EXPORT_FORMAT,
                new TemplateExportMetadataView(
                        templateId.toString(),
                        "TPL-IMPORT-LETTER",
                        "RETAIL",
                        "Import Letter",
                        "Import test template",
                        UUID.randomUUID().toString(),
                        TemplateLifecycleStatus.PUBLISHED,
                        "1.0.0",
                        UUID.randomUUID().toString(),
                        1,
                        Instant.now()
                ),
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
                List.of(),
                List.of(),
                List.of(),
                null
        );
    }

    private TemplateDetailView detail(TemplateLifecycleStatus status) {
        return new TemplateDetailView(
                templateId.toString(),
                "TPL-IMPORT-LETTER",
                "RETAIL",
                "Import Letter",
                "Import test template",
                masterId.toString(),
                status,
                null,
                null,
                UUID.randomUUID().toString(),
                1,
                List.of(),
                List.of(),
                List.of(),
                Instant.now(),
                Instant.now(),
                false
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
