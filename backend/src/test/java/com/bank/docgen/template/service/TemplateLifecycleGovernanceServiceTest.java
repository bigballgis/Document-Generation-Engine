package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.LifecycleGovernanceRequest;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.template.domain.LifecycleAction;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
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
class TemplateLifecycleGovernanceServiceTest {

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
    private ApiPolicyRepository apiPolicyRepository;

    private TemplateLifecycleService service;
    private ManagementSessionClaims groupAdmin;
    private UUID templateId;
    private TemplateEntity template;
    private TemplateVersionEntity version;

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
                apiPolicyRepository
        );
        groupAdmin = session(List.of("GROUP_ADMIN"), List.of("RETAIL"));
        templateId = UUID.randomUUID();
        template = publishedTemplate(templateId);
        version = publishedVersion(templateId);
    }

    @Test
    void stop_fromPublished_setsStoppedAndRecordsAudit() {
        when(groupAccessService.canStopTemplates(groupAdmin)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, groupAdmin)).thenReturn(template);
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(version));
        when(templateService.toDetail(template)).thenReturn(detail(TemplateLifecycleStatus.STOPPED));

        TemplateDetailView result = service.stop(
                templateId,
                new LifecycleGovernanceRequest("Maintenance window", true),
                groupAdmin
        );

        assertThat(result.lifecycleStatus()).isEqualTo(TemplateLifecycleStatus.STOPPED);
        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.STOPPED);
        assertThat(version.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.STOPPED);
        ArgumentCaptor<TemplateLifecycleRecordEntity> recordCaptor =
                ArgumentCaptor.forClass(TemplateLifecycleRecordEntity.class);
        verify(lifecycleRecordRepository).save(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getAction()).isEqualTo(LifecycleAction.STOP);
    }

    @Test
    void stop_withoutConfirmation_throwsValidationError() {
        assertThatThrownBy(() -> service.stop(
                templateId,
                new LifecycleGovernanceRequest("Maintenance window", false),
                groupAdmin
        )).isInstanceOf(TemplateValidationException.class);
    }

    @Test
    void restore_fromStopped_setsPublished() {
        template.setLifecycleStatus(TemplateLifecycleStatus.STOPPED);
        version.setLifecycleStatus(TemplateLifecycleStatus.STOPPED);
        when(groupAccessService.canRestoreOrDeprecateTemplates(groupAdmin)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, groupAdmin)).thenReturn(template);
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(version));
        when(templateService.toDetail(template)).thenReturn(detail(TemplateLifecycleStatus.PUBLISHED));

        service.restore(templateId, new LifecycleGovernanceRequest("Restore service", true), groupAdmin);

        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.PUBLISHED);
        assertThat(version.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.PUBLISHED);
    }

    @Test
    void deprecate_fromStopped_withCallableVersion_throwsInvalidState() {
        template.setLifecycleStatus(TemplateLifecycleStatus.STOPPED);
        when(groupAccessService.canRestoreOrDeprecateTemplates(groupAdmin)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, groupAdmin)).thenReturn(template);
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(version));

        assertThatThrownBy(() -> service.deprecate(
                templateId,
                new LifecycleGovernanceRequest("End of life", true),
                groupAdmin
        )).isInstanceOf(TemplateValidationException.class);
    }

    @Test
    void stop_byUnauthorizedRole_throwsAccessDenied() {
        ManagementSessionClaims tester = session(List.of("TEMPLATE_TESTER"), List.of("RETAIL"));
        when(groupAccessService.canStopTemplates(tester)).thenReturn(false);

        assertThatThrownBy(() -> service.stop(
                templateId,
                new LifecycleGovernanceRequest("Maintenance window", true),
                tester
        )).isInstanceOf(TemplateAccessDeniedException.class);
    }

    @Test
    void deactivateVersion_setsVersionStopped_templateStaysPublished() {
        when(groupAccessService.canManageReleaseVersionState(groupAdmin)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, groupAdmin)).thenReturn(template);
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(templateId, "1.0.0"))
                .thenReturn(Optional.of(version));
        when(templateService.toDetail(template)).thenReturn(detail(TemplateLifecycleStatus.PUBLISHED));

        service.deactivateVersion(
                templateId,
                "1.0.0",
                new LifecycleGovernanceRequest("Deactivate single version", true),
                groupAdmin
        );

        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.PUBLISHED);
        assertThat(version.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.STOPPED);
        ArgumentCaptor<TemplateLifecycleRecordEntity> recordCaptor =
                ArgumentCaptor.forClass(TemplateLifecycleRecordEntity.class);
        verify(lifecycleRecordRepository).save(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getAction()).isEqualTo(LifecycleAction.DEACTIVATE_VERSION);
        assertThat(recordCaptor.getValue().getReleaseVersion()).isEqualTo("1.0.0");
    }

    @Test
    void restoreVersion_setsVersionPublished() {
        version.setLifecycleStatus(TemplateLifecycleStatus.STOPPED);
        when(groupAccessService.canManageReleaseVersionState(groupAdmin)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, groupAdmin)).thenReturn(template);
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(templateId, "1.0.0"))
                .thenReturn(Optional.of(version));
        when(templateService.toDetail(template)).thenReturn(detail(TemplateLifecycleStatus.PUBLISHED));

        service.restoreVersion(
                templateId,
                "1.0.0",
                new LifecycleGovernanceRequest("Restore version", true),
                groupAdmin
        );

        assertThat(version.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.PUBLISHED);
        ArgumentCaptor<TemplateLifecycleRecordEntity> recordCaptor =
                ArgumentCaptor.forClass(TemplateLifecycleRecordEntity.class);
        verify(lifecycleRecordRepository).save(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getAction()).isEqualTo(LifecycleAction.RESTORE_VERSION);
    }

    @Test
    void deactivateVersion_byUnauthorizedRole_throwsAccessDenied() {
        ManagementSessionClaims author = session(List.of("TEMPLATE_AUTHOR"), List.of("RETAIL"));
        when(groupAccessService.canManageReleaseVersionState(author)).thenReturn(false);

        assertThatThrownBy(() -> service.deactivateVersion(
                templateId,
                "1.0.0",
                new LifecycleGovernanceRequest("Deactivate", true),
                author
        )).isInstanceOf(TemplateAccessDeniedException.class);
    }

    private ManagementSessionClaims session(List<String> roles, List<String> groups) {
        return new ManagementSessionClaims(
                "10000001",
                "Operator",
                "operator@example.com",
                AuthSource.LOCAL,
                roles,
                groups,
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

    private TemplateVersionEntity publishedVersion(UUID templateId) {
        TemplateVersionEntity entity = new TemplateVersionEntity(UUID.randomUUID(), templateId, "10000001");
        entity.setReleaseVersion("1.0.0");
        entity.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        return entity;
    }

    private TemplateDetailView detail(TemplateLifecycleStatus status) {
        return new TemplateDetailView(
                templateId.toString(),
                "TPL-001",
                "RETAIL",
                "Sample",
                null,
                UUID.randomUUID().toString(),
                status,
                null,
                "1.0.0",
                UUID.randomUUID().toString(),
                1,
                List.of(),
                List.of(),
                List.of(),
                Instant.now(),
                Instant.now()
        );
    }
}
