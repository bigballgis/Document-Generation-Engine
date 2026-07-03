package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "docgen.demo-catalog.seed-enabled=true")
class DemoFullFlowCatalogSeederIntegrationTest {

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private ApiPolicyRepository apiPolicyRepository;

    @Test
    void seedsPublishedFullFlowTemplateWithApiPolicy() {
        var template = templateRepository.findByExternalIdAndDeletedAtIsNull(
                DemoFullFlowCatalogSeeder.DEMO_FULL_FLOW_EXTERNAL_ID
        );
        assertThat(template).isPresent();
        assertThat(template.get().getName()).isEqualTo(DemoFullFlowCatalogSeeder.DEMO_FULL_FLOW_NAME);
        assertThat(template.get().getGroupCode()).isEqualTo(DemoCatalogSeeder.DEMO_GROUP_CODE);
        assertThat(template.get().getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.PUBLISHED);
        assertThat(template.get().getReleaseVersion()).isEqualTo(DemoFullFlowCatalogSeeder.DEMO_FULL_FLOW_RELEASE_VERSION);

        var policy = apiPolicyRepository.findByTemplateId(template.get().getId());
        assertThat(policy).isPresent();
        assertThat(policy.get().getDefaultRouteReleaseVersion())
                .isEqualTo(DemoFullFlowCatalogSeeder.DEMO_FULL_FLOW_RELEASE_VERSION);
        assertThat(policy.get().getPolicyVersion()).isGreaterThanOrEqualTo(1);
    }
}
