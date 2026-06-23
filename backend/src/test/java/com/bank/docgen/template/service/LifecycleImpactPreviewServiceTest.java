package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.LifecycleImpactPreviewRequest;
import com.bank.docgen.template.api.LifecycleImpactPreviewView;
import com.bank.docgen.template.domain.LifecycleGovernanceAction;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
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
class LifecycleImpactPreviewServiceTest {

    @Mock
    private TemplateService templateService;
    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;

    private LifecycleImpactPreviewService service;
    private ManagementSessionClaims groupAdmin;
    private UUID templateId;
    private TemplateEntity template;
    private TemplateVersionEntity version;

    @BeforeEach
    void setUp() {
        service = new LifecycleImpactPreviewService(
                templateService,
                templateVersionRepository,
                apiPolicyRepository
        );
        groupAdmin = new ManagementSessionClaims(
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
        templateId = UUID.randomUUID();
        template = publishedTemplate(templateId);
        version = publishedVersion(templateId);
    }

    @Test
    void preview_forStop_listsCallableVersionsAffected() {
        when(templateService.requireReadableTemplate(templateId, groupAdmin)).thenReturn(template);
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(version));
        when(apiPolicyRepository.findByTemplateId(templateId))
                .thenReturn(Optional.of(apiPolicy(templateId, "1.0.0")));

        LifecycleImpactPreviewView preview = service.preview(
                templateId,
                new LifecycleImpactPreviewRequest(LifecycleGovernanceAction.STOP, null),
                groupAdmin
        );

        assertThat(preview.callableReleaseVersions()).containsExactly("1.0.0");
        assertThat(preview.defaultRouteImpacted()).isTrue();
        assertThat(preview.summaryMessageKey()).isEqualTo("api.template.lifecycleImpact.defaultRouteAffected");
    }

    @Test
    void preview_forDeprecate_flagsDefaultRouteImpact() {
        template.setLifecycleStatus(TemplateLifecycleStatus.STOPPED);
        version.setLifecycleStatus(TemplateLifecycleStatus.STOPPED);
        when(templateService.requireReadableTemplate(templateId, groupAdmin)).thenReturn(template);
        when(apiPolicyRepository.findByTemplateId(templateId))
                .thenReturn(Optional.of(apiPolicy(templateId, "1.0.0")));

        LifecycleImpactPreviewView preview = service.preview(
                templateId,
                new LifecycleImpactPreviewRequest(LifecycleGovernanceAction.DEPRECATE, null),
                groupAdmin
        );

        assertThat(preview.callableReleaseVersions()).isEmpty();
        assertThat(preview.defaultRouteImpacted()).isFalse();
        assertThat(preview.summaryMessageKey()).isEqualTo("api.template.lifecycleImpact.deprecateSummary");
    }

    @Test
    void preview_forVersionDeactivate_showsCallableVersionDelta() {
        when(templateService.requireReadableTemplate(templateId, groupAdmin)).thenReturn(template);
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(version));
        when(apiPolicyRepository.findByTemplateId(templateId))
                .thenReturn(Optional.of(apiPolicy(templateId, "1.0.0")));

        LifecycleImpactPreviewView preview = service.preview(
                templateId,
                new LifecycleImpactPreviewRequest(LifecycleGovernanceAction.DEACTIVATE_VERSION, "1.0.0"),
                groupAdmin
        );

        assertThat(preview.releaseVersion()).isEqualTo("1.0.0");
        assertThat(preview.callableReleaseVersions()).containsExactly("1.0.0");
        assertThat(preview.defaultRouteImpacted()).isTrue();
    }

    @Test
    void preview_excludesSensitiveData() {
        when(templateService.requireReadableTemplate(templateId, groupAdmin)).thenReturn(template);
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(version));
        when(apiPolicyRepository.findByTemplateId(templateId))
                .thenReturn(Optional.of(apiPolicy(templateId, "1.0.0")));

        LifecycleImpactPreviewView preview = service.preview(
                templateId,
                new LifecycleImpactPreviewRequest(LifecycleGovernanceAction.STOP, null),
                groupAdmin
        );

        assertThat(preview.callableReleaseVersions()).doesNotContainNull();
        assertThat(preview.summaryMessageKey()).doesNotContain("password");
        assertThat(preview.summaryMessageKey()).doesNotContain("secret");
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

    private ApiPolicyEntity apiPolicy(UUID templateId, String defaultRoute) {
        ApiPolicyEntity policy = new ApiPolicyEntity(UUID.randomUUID(), templateId, "[\"RETAIL_API\"]", "10000001");
        policy.replaceConfiguration(
                "[\"RETAIL_API\"]",
                defaultRoute,
                "[\"DOCX\"]",
                "[\"SYNC_STREAM\"]",
                false,
                10,
                false,
                false,
                "10000001"
        );
        return policy;
    }
}
