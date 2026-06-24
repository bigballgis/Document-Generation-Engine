package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
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
class ContractAssemblyServiceCallabilityTest {

    @Mock
    private MessageResolver messageResolver;
    @Mock
    private TemplateVersionRepository templateVersionRepository;

    private ContractAssemblyService service;
    private UUID templateId;
    private TemplateEntity template;

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
        template.setReleaseVersion("2.0.0");
    }

    @Test
    void listsAllPublishedReleaseVersions() {
        TemplateVersionEntity versionTwo = version(2, "2.0.0", TemplateLifecycleStatus.PUBLISHED);
        TemplateVersionEntity versionOne = version(1, "1.0.0", TemplateLifecycleStatus.PUBLISHED);
        TemplateVersionEntity stoppedVersion = version(1, "0.9.0", TemplateLifecycleStatus.STOPPED);

        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(versionTwo, versionOne, stoppedVersion));

        assertThat(service.listCallableVersions(template, "dev"))
                .extracting(view -> view.releaseVersion())
                .containsExactly("2.0.0", "1.0.0");
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
