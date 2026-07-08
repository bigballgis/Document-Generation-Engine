package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.runtime.api.DefaultRouteSummaryView;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContractAssemblyServiceRoutesSummaryTest {

    @Mock
    private MessageResolver messageResolver;
    @Mock
    private TemplateVersionRepository templateVersionRepository;

    private ContractAssemblyService service;
    private UUID templateId;
    private TemplateEntity template;
    private ApiPolicyEntity policy;

    @BeforeEach
    void setUp() {
        service = new ContractAssemblyService(messageResolver, new ObjectMapper(), templateVersionRepository);
        templateId = UUID.randomUUID();
        template = new TemplateEntity(
                templateId,
                "TPL-001",
                "RETAIL",
                "Sample",
                null,
                UUID.randomUUID(),
                "10000001"
        );
        template.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        policy = new ApiPolicyEntity(UUID.randomUUID(), templateId, "[\"G1\"]", "10000001");
        policy.updateDefaultRouteDomain("2.0.0", "10000001");
    }

    @Test
    void summarizeDefaultRoute_returnsDevDefaultGeneratePath() {
        DefaultRouteSummaryView summary = service.summarizeDefaultRoute(template, policy, "dev");

        assertThat(summary.url()).isEqualTo("/api/dev/v1/templates/TPL-001/default/generate");
        assertThat(summary.currentTargetReleaseVersion()).isEqualTo("2.0.0");
        assertThat(summary.explicitVersionUrl())
                .isEqualTo("/api/dev/v1/templates/TPL-001/versions/2.0.0/generate");
    }

    @Test
    void listCallableVersions_returnsExplicitGeneratePathsPerPublishedRelease() {
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(
                        version(2, "2.0.0", TemplateLifecycleStatus.PUBLISHED),
                        version(1, "1.0.0", TemplateLifecycleStatus.PUBLISHED)
                ));

        assertThat(service.listCallableVersions(template, "dev"))
                .extracting(view -> view.explicitVersionUrl())
                .containsExactly(
                        "/api/dev/v1/templates/TPL-001/versions/2.0.0/generate",
                        "/api/dev/v1/templates/TPL-001/versions/1.0.0/generate"
                );
    }

    private TemplateVersionEntity version(
            int devVersionNumber,
            String releaseVersion,
            TemplateLifecycleStatus lifecycleStatus
    ) {
        TemplateVersionEntity version = new TemplateVersionEntity(UUID.randomUUID(), templateId, "10000001");
        version.setDevVersionNumber(devVersionNumber);
        version.setReleaseVersion(releaseVersion);
        version.setLifecycleStatus(lifecycleStatus);
        return version;
    }
}
