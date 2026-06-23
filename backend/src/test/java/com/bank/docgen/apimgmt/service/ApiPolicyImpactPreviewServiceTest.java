package com.bank.docgen.apimgmt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.api.ApiPolicyImpactPreviewView;
import com.bank.docgen.apimgmt.api.UpsertApiPolicyRequest;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.service.TemplateService;
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
class ApiPolicyImpactPreviewServiceTest {

    @Mock
    private TemplateService templateService;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private GroupAccessService groupAccessService;

    private ApiPolicyImpactPreviewService service;
    private ManagementSessionClaims groupAdmin;
    private UUID templateId;
    private TemplateEntity template;
    private TemplateVersionEntity callableVersion;

    @BeforeEach
    void setUp() {
        service = new ApiPolicyImpactPreviewService(
                templateService,
                apiPolicyRepository,
                templateVersionRepository,
                groupAccessService,
                new ObjectMapper()
        );
        groupAdmin = session(List.of("GROUP_ADMIN"));
        templateId = UUID.randomUUID();
        template = publishedTemplate(templateId);
        callableVersion = publishedVersion(templateId, "1.0.0");
    }

    @Test
    void preview_blocksWhenDefaultRouteNotCallable() {
        when(groupAccessService.canManageApiPolicy(groupAdmin)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, groupAdmin)).thenReturn(template);
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(templateId, "1.0.0"))
                .thenReturn(Optional.of(callableVersion));
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(existingPolicy(templateId, 3)));

        ApiPolicyImpactPreviewView preview = service.preview(
                templateId,
                new UpsertApiPolicyRequest(
                        List.of("RETAIL_API"),
                        "2.0.0",
                        List.of("DOCX"),
                        List.of("SYNC_STREAM"),
                        false,
                        10,
                        false,
                        false
                ),
                groupAdmin
        );

        assertThat(preview.blocking()).isTrue();
        assertThat(preview.defaultRouteImpacted()).isTrue();
        assertThat(preview.changedAreas()).contains("DEFAULT_ROUTE_TARGET");
        assertThat(preview.currentPolicyVersion()).isEqualTo(3);
        assertThat(preview.nextPolicyVersion()).isEqualTo(4);
    }

    @Test
    void preview_nonBlockingWhenDefaultRouteRemainsCallable() {
        when(groupAccessService.canManageApiPolicy(groupAdmin)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, groupAdmin)).thenReturn(template);
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(templateId, "1.0.0"))
                .thenReturn(Optional.of(callableVersion));
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(existingPolicy(templateId, 1)));

        ApiPolicyImpactPreviewView preview = service.preview(
                templateId,
                new UpsertApiPolicyRequest(
                        List.of("RETAIL_API"),
                        "1.0.0",
                        List.of("DOCX", "PDF"),
                        List.of("SYNC_STREAM"),
                        false,
                        10,
                        false,
                        false
                ),
                groupAdmin
        );

        assertThat(preview.blocking()).isFalse();
        assertThat(preview.defaultRouteImpacted()).isFalse();
        assertThat(preview.changedAreas()).contains("OUTPUT_POLICY");
        assertThat(preview.warnings()).isEmpty();
    }

    @Test
    void preview_newPolicyStartsFromVersionOne() {
        when(groupAccessService.canManageApiPolicy(groupAdmin)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, groupAdmin)).thenReturn(template);
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(templateId, "1.0.0"))
                .thenReturn(Optional.of(callableVersion));
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.empty());

        ApiPolicyImpactPreviewView preview = service.preview(
                templateId,
                new UpsertApiPolicyRequest(
                        List.of("RETAIL_API"),
                        "1.0.0",
                        List.of("DOCX"),
                        List.of("SYNC_STREAM"),
                        false,
                        10,
                        false,
                        false
                ),
                groupAdmin
        );

        assertThat(preview.currentPolicyVersion()).isZero();
        assertThat(preview.nextPolicyVersion()).isEqualTo(1);
        assertThat(preview.blocking()).isFalse();
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

    private TemplateVersionEntity publishedVersion(UUID templateId, String releaseVersion) {
        TemplateVersionEntity version = new TemplateVersionEntity(UUID.randomUUID(), templateId, "10000001");
        version.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        version.setReleaseVersion(releaseVersion);
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
