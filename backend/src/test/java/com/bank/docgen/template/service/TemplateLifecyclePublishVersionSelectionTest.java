package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.apimgmt.service.ApiPolicyMaterializationService;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.sharedkernel.lifecycle.SelfApprovalGuard;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.PublishTemplateRequest;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.collaboration.service.CollaborationWorkItemWriter;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
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
class TemplateLifecyclePublishVersionSelectionTest {

    @Mock
    private TemplateService templateService;
    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private TemplateLifecycleRecordRepository lifecycleRecordRepository;
    @Mock
    private GroupAccessService groupAccessService;
    @Mock
    private LifecycleImpactPreviewService lifecycleImpactPreviewService;
    @Mock
    private MessageResolver messageResolver;
    @Mock
    private PublishGateService publishGateService;
    @Mock
    private DecisionFormService decisionFormService;
    @Mock
    private TemplateContentModuleReferenceService contentModuleReferenceService;
    @Mock
    private CollaborationWorkItemWriter collaborationWorkItemWriter;
    @Mock
    private com.bank.docgen.authoring.structured.RenderProfileService renderProfileService;
    @Mock
    private ApprovalSubStateResolver approvalSubStateResolver;
    @Mock
    private ApiPolicyMaterializationService apiPolicyMaterializationService;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private VersionFidelityWarningService versionFidelityWarningService;
    @Mock
    private MasterDocumentRepository masterDocumentRepository;
    @Mock
    private MasterRevisionLineRepository masterRevisionLineRepository;
    @Mock
    private ObjectStoragePort objectStoragePort;

    private TemplateLifecycleService service;
    private ManagementSessionClaims groupAdmin;
    private UUID templateId;
    private UUID revisionId;
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
                decisionFormService,
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
                new SelfApprovalGuard()
        );
        groupAdmin = new ManagementSessionClaims(
                "10000002",
                "Group Admin",
                "group.admin@example.com",
                AuthSource.LOCAL,
                List.of("GROUP_ADMIN"),
                List.of("RETAIL"),
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                Instant.now().plusSeconds(3600)
        );
        templateId = UUID.randomUUID();
        revisionId = UUID.randomUUID();
        template = new TemplateEntity(
                templateId,
                "TPL-001",
                "RETAIL",
                "Sample",
                null,
                UUID.randomUUID(),
                "10000002"
        );
        template.setLifecycleStatus(TemplateLifecycleStatus.PENDING_RELEASE);
    }

    @Test
    void publishSelectsHighestUnreleasedDevVersion() {
        TemplateVersionEntity publishedVersion = version(1, "1.0.0", TemplateLifecycleStatus.PUBLISHED);
        TemplateVersionEntity candidateVersion = version(2, null, TemplateLifecycleStatus.DRAFT);

        when(groupAccessService.canPublishTemplates(groupAdmin)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, groupAdmin)).thenReturn(template);
        org.mockito.Mockito.doNothing().when(publishGateService).assertReady(templateId, groupAdmin);
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(candidateVersion, publishedVersion));
        when(templateService.toDetail(template)).thenReturn(detail());
        when(messageResolver.resolve(any(), any())).thenReturn("Published release 2.0.0");
        stubApprovedMasterPin();

        service.publish(templateId, new PublishTemplateRequest("2.0.0", true), groupAdmin);

        assertThat(candidateVersion.getReleaseVersion()).isEqualTo("2.0.0");
        assertThat(candidateVersion.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.PUBLISHED);
        assertThat(candidateVersion.getMasterRevisionId()).isEqualTo(revisionId);
        assertThat(publishedVersion.getReleaseVersion()).isEqualTo("1.0.0");
        verify(templateVersionRepository).save(candidateVersion);
        verify(contentModuleReferenceService).lockReferencesForPublish(candidateVersion.getId());
        verify(renderProfileService).lockForPublish(candidateVersion);
        verify(collaborationWorkItemWriter).resolveOpenPendingReleaseWorkItems(template, groupAdmin);
    }

    private void stubApprovedMasterPin() {
        MasterDocumentEntity master = new MasterDocumentEntity(
                template.getMasterId(), "RETAIL", "Retail Master", "desc",
                "masters/live.docx", "master.docx", "10000002"
        );
        master.setStatus(MasterDocumentStatus.APPROVED);
        try {
            java.lang.reflect.Field f = MasterDocumentEntity.class.getDeclaredField("currentRevisionLineId");
            f.setAccessible(true);
            f.set(master, revisionId);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
        MasterRevisionLineEntity revision = new MasterRevisionLineEntity(
                revisionId, template.getMasterId(), "masters/R1.docx", "master.docx",
                1, MasterDocumentStatus.APPROVED, 1, true, "initial", "10000002"
        );
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(template.getMasterId()))
                .thenReturn(Optional.of(master));
        when(masterRevisionLineRepository.findByIdAndMasterIdAndDeletedAtIsNull(revisionId, template.getMasterId()))
                .thenReturn(Optional.of(revision));
        when(objectStoragePort.get("masters/R1.docx"))
                .thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
    }

    private TemplateVersionEntity version(
            int devVersionNumber,
            String releaseVersion,
            TemplateLifecycleStatus lifecycleStatus
    ) {
        TemplateVersionEntity version = new TemplateVersionEntity(UUID.randomUUID(), templateId, "10000002");
        version.setDevVersionNumber(devVersionNumber);
        version.setReleaseVersion(releaseVersion);
        version.setLifecycleStatus(lifecycleStatus);
        return version;
    }

    private TemplateDetailView detail() {
        return new TemplateDetailView(
                templateId.toString(),
                template.getExternalId(),
                template.getGroupCode(),
                template.getName(),
                template.getDescription(),
                template.getMasterId().toString(),
                template.getLifecycleStatus(),
                null,
                "2.0.0",
                UUID.randomUUID().toString(),
                2,
                List.of(),
                List.of(),
                List.of(),
                Instant.now(),
                Instant.now(),
                null,
                null,
                false, null);
    }
}
