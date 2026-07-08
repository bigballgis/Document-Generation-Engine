package com.bank.docgen.apimgmt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.api.ApiRoutesSummaryView;
import com.bank.docgen.apimgmt.api.ExplicitRoutePathView;
import com.bank.docgen.apimgmt.mapping.ApiPolicyViewMapperFactory;
import com.bank.docgen.apimgmt.persistence.ApiCredentialRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyVersionRepository;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.runtime.api.CallableVersionView;
import com.bank.docgen.runtime.api.DefaultRouteSummaryView;
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
import org.junit.jupiter.api.Test;

class ApiManagementServiceRoutesSummaryTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private final TemplateService templateService = mock(TemplateService.class);
    private final ApiPolicyRepository apiPolicyRepository = mock(ApiPolicyRepository.class);
    private final ApiCredentialRepository apiCredentialRepository = mock(ApiCredentialRepository.class);
    private final ContractAssemblyService contractAssemblyService = mock(ContractAssemblyService.class);
    private final ApiPolicyImpactPreviewService apiPolicyImpactPreviewService = mock(ApiPolicyImpactPreviewService.class);

    private final ApiManagementService service = new ApiManagementService(
            templateService,
            apiPolicyRepository,
            apiCredentialRepository,
            new GroupAccessService(),
            mock(PasswordHashService.class),
            mock(ManagementAuditRecorder.class),
            contractAssemblyService,
            new ObjectMapper(),
            new ApiPolicyVersionSnapshotService(
                    mock(ApiPolicyVersionRepository.class),
                    new ObjectMapper()
            ),
            mock(TemplateVersionRepository.class),
            new TemplateAdGroupAuthorizationCache(),
            apiPolicyImpactPreviewService,
            ApiPolicyViewMapperFactory.create(new ObjectMapper())
    );

    @Test
    void getRoutesSummary_assemblesPathsFromContractAssemblyService() {
        ManagementSessionClaims session = session(List.of("GROUP_ADMIN"));
        TemplateEntity template = template();
        ApiPolicyEntity policy = new ApiPolicyEntity(UUID.randomUUID(), TEMPLATE_ID, "[\"G1\"]", "10000001");
        policy.updateDefaultRouteDomain("2.0.0", "10000001");

        when(templateService.requireReadableTemplate(TEMPLATE_ID, session)).thenReturn(template);
        when(apiPolicyRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(Optional.of(policy));
        when(contractAssemblyService.summarizeDefaultRoute(template, policy, "dev"))
                .thenReturn(new DefaultRouteSummaryView(
                        "/api/dev/v1/templates/TPL-001/default/generate",
                        "2.0.0",
                        TemplateLifecycleStatus.PUBLISHED.name(),
                        null,
                        null,
                        "/api/dev/v1/templates/TPL-001/versions/2.0.0/generate"
                ));
        when(contractAssemblyService.listCallableVersions(template, "dev"))
                .thenReturn(List.of(
                        new CallableVersionView("2.0.0", "/api/dev/v1/templates/TPL-001/versions/2.0.0/generate"),
                        new CallableVersionView("1.0.0", "/api/dev/v1/templates/TPL-001/versions/1.0.0/generate")
                ));

        ApiRoutesSummaryView summary = service.getRoutesSummary(TEMPLATE_ID, "dev", session);

        assertThat(summary.templateExternalId()).isEqualTo("TPL-001");
        assertThat(summary.defaultRouteReleaseVersion()).isEqualTo("2.0.0");
        assertThat(summary.defaultGeneratePath())
                .isEqualTo("/api/dev/v1/templates/TPL-001/default/generate");
        assertThat(summary.explicitPaths()).containsExactly(
                new ExplicitRoutePathView("2.0.0", "/api/dev/v1/templates/TPL-001/versions/2.0.0/generate"),
                new ExplicitRoutePathView("1.0.0", "/api/dev/v1/templates/TPL-001/versions/1.0.0/generate")
        );
    }

    @Test
    void getRoutesSummary_deniedForNonApiAdmin() {
        assertThatThrownBy(() -> service.getRoutesSummary(TEMPLATE_ID, "dev", session(List.of("TEMPLATE_AUTHOR"))))
                .isInstanceOf(ApiManagementAccessDeniedException.class);
        verifyNoInteractions(templateService, apiPolicyRepository, contractAssemblyService);
    }

    private static ManagementSessionClaims session(List<String> roles) {
        return new ManagementSessionClaims(
                "user",
                "User",
                "user@bank.test",
                AuthSource.LOCAL,
                roles,
                List.of("RETAIL"),
                "/",
                List.of(),
                Instant.now().plusSeconds(600)
        );
    }

    private static TemplateEntity template() {
        TemplateEntity template = new TemplateEntity(
                TEMPLATE_ID,
                "TPL-001",
                "RETAIL",
                "Retail Statement",
                null,
                UUID.randomUUID(),
                "10000001"
        );
        template.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        return template;
    }
}
