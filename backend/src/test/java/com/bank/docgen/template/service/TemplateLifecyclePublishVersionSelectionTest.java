package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.BindingValidationSummaryView;
import com.bank.docgen.template.api.BindingValidationView;
import com.bank.docgen.template.api.PublishTemplateRequest;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.time.Instant;
import java.util.List;
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

    private TemplateLifecycleService service;
    private ManagementSessionClaims groupAdmin;
    private UUID templateId;
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
                messageResolver
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
        when(templateService.validateBindings(templateId, groupAdmin)).thenReturn(nonBlockingBindings());
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(candidateVersion, publishedVersion));
        when(templateService.toDetail(template)).thenReturn(detail());
        when(messageResolver.resolve(any(), any())).thenReturn("Published release 2.0.0");

        service.publish(templateId, new PublishTemplateRequest("2.0.0"), groupAdmin);

        assertThat(candidateVersion.getReleaseVersion()).isEqualTo("2.0.0");
        assertThat(candidateVersion.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.PUBLISHED);
        assertThat(publishedVersion.getReleaseVersion()).isEqualTo("1.0.0");
        verify(templateVersionRepository).save(candidateVersion);
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

    private BindingValidationView nonBlockingBindings() {
        return new BindingValidationView(
                List.of(),
                new BindingValidationSummaryView(false, 0, 0, 0, 0, 0)
        );
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
                Instant.now()
        );
    }
}
