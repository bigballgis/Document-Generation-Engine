package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiCredentialRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.rendering.DocumentArtifactPipeline;
import com.bank.docgen.rendering.DocxAssembler;
import com.bank.docgen.runtime.api.CallableVersionsResultView;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.service.TemplateValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Regression guard for OPT-E4: the runtime "list callable versions" path must enforce
 * the same template-credential binding as contract/generate, otherwise a credential
 * bound to template A could enumerate template B's callable versions.
 */
class RuntimeGenerationServiceAccessTest {

    private static final UUID TEMPLATE_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TEMPLATE_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private final ContractAssemblyService contractAssemblyService = mock(ContractAssemblyService.class);

    private RuntimeGenerationService newService() {
        return new RuntimeGenerationService(
                mock(TemplateVersionRepository.class),
                mock(AnchorBindingRepository.class),
                mock(MasterDocumentRepository.class),
                mock(ApiPolicyRepository.class),
                mock(ApiCredentialRepository.class),
                mock(ObjectStoragePort.class),
                mock(DocxAssembler.class),
                mock(DocumentArtifactPipeline.class),
                mock(IdempotencyService.class),
                mock(EncryptionParameterValidator.class),
                contractAssemblyService,
                new ObjectMapper()
        );
    }

    private RuntimeSessionClaims sessionFor(UUID templateId) {
        return new RuntimeSessionClaims(
                UUID.randomUUID(), "CRED-1", templateId, "TPL-EXT", "svc-caller", List.of("grp"));
    }

    @Test
    void listVersionsDeniedWhenTemplateNotBoundToSession() {
        TemplateEntity template = mock(TemplateEntity.class);
        when(template.getId()).thenReturn(TEMPLATE_A);

        assertThatThrownBy(() -> newService()
                .listCallableVersionsResult(template, sessionFor(TEMPLATE_B), "dev"))
                .isInstanceOf(TemplateValidationException.class);
    }

    @Test
    void listVersionsAllowedWhenTemplateMatchesSession() {
        TemplateEntity template = mock(TemplateEntity.class);
        when(template.getId()).thenReturn(TEMPLATE_A);
        lenient().when(template.getExternalId()).thenReturn("TPL-EXT");
        when(contractAssemblyService.listCallableVersions(template, "dev")).thenReturn(List.of());

        CallableVersionsResultView result = newService()
                .listCallableVersionsResult(template, sessionFor(TEMPLATE_A), "dev");

        assertThat(result.templateId()).isEqualTo("TPL-EXT");
    }
}
