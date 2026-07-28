package com.bank.docgen.apimgmt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.api.SaveAdGroupsRequest;
import com.bank.docgen.apimgmt.api.SaveOutputPolicyRequest;
import com.bank.docgen.apimgmt.mapping.ApiPolicyViewMapperFactory;
import com.bank.docgen.apimgmt.persistence.ApiCredentialRepository;
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
class AdGroupAuthorizationCacheTest {

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
    private TemplateAdGroupAuthorizationCache authorizationCache;
    private ApiManagementService service;
    private ManagementSessionClaims groupAdmin;
    private UUID templateId;

    @BeforeEach
    void setUp() {
        authorizationCache = new TemplateAdGroupAuthorizationCache();
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
        TemplateEntity template = publishedTemplate(templateId);

        lenient().when(templateService.requireReadableTemplate(templateId, groupAdmin)).thenReturn(template);
        lenient().when(apiPolicyRepository.save(any(ApiPolicyEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(apiPolicyVersionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(apiPolicyImpactPreviewService.preview(any(), any(), any())).thenReturn(
                new com.bank.docgen.apimgmt.api.ApiPolicyImpactPreviewView(
                        List.of("AD_GROUP_AUTHORIZATION"),
                        false,
                        List.of(),
                        false,
                        1,
                        2,
                        "api.apimgmt.policyImpact.safe",
                        null,
                        null,
                        null,
                        null
                )
        );
    }

    @Test
    void saveAdGroups_invalidatesAuthorizationCache_forTemplate() {
        ApiPolicyEntity existing = existingPolicy(templateId, 1);
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(existing));
        authorizationCache.rememberAllowedGroups(templateId, List.of("RETAIL_API"));
        assertThat(authorizationCache.getAllowedGroups(templateId)).isPresent();

        service.saveAdGroupsDomain(
                templateId,
                new SaveAdGroupsRequest(List.of("RETAIL_API", "WHOLESALE_API"), true),
                groupAdmin
        );

        assertThat(authorizationCache.getAllowedGroups(templateId)).isEmpty();
    }

    @Test
    void otherDomainSave_doesNotInvalidateAdGroupCache() {
        ApiPolicyEntity existing = existingPolicy(templateId, 1);
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(existing));
        authorizationCache.rememberAllowedGroups(templateId, List.of("RETAIL_API"));

        service.saveOutputDomain(
                templateId,
                new SaveOutputPolicyRequest(List.of("DOCX", "PDF"), List.of("SYNC_STREAM"), true),
                groupAdmin
        );

        assertThat(authorizationCache.getAllowedGroups(templateId)).isPresent();
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
