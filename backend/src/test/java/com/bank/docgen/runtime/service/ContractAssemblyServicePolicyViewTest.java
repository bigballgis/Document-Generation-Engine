package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.runtime.api.ContractResultView;
import com.bank.docgen.runtime.api.RuntimeCredentialSummaryView;
import com.bank.docgen.runtime.domain.ContractViewAudience;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContractAssemblyServicePolicyViewTest {

    @Mock
    private MessageResolver messageResolver;
    @Mock
    private TemplateVersionRepository templateVersionRepository;

    private ContractAssemblyService service;
    private TemplateEntity template;
    private ApiPolicyEntity policy;
    private RuntimeCredentialSummaryView credentialSummary;

    @BeforeEach
    void setUp() {
        service = new ContractAssemblyService(messageResolver, new ObjectMapper(), templateVersionRepository);
        template = new TemplateEntity(
                UUID.randomUUID(),
                "TPL-001",
                "RETAIL",
                "Sample",
                null,
                UUID.randomUUID(),
                "10000001"
        );
        template.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        template.setReleaseVersion("1.0.0");

        policy = new ApiPolicyEntity(UUID.randomUUID(), template.getId(), "[\"RETAIL_API\"]", "10000001");
        policy.update(
                "[\"RETAIL_API\",\"CORP_API\"]",
                "1.0.0",
                "[\"DOCX\",\"PDF\"]",
                "[\"SYNC_STREAM\",\"ASYNC_TASK\"]",
                true,
                50,
                true,
                false,
                "10000002"
        );

        credentialSummary = new RuntimeCredentialSummaryView("cred-ext-001", "ACTIVE", "fp-cred-ext-001");

        lenient().when(messageResolver.resolve(any())).thenAnswer(invocation -> invocation.getArgument(0).toString());
        lenient().when(messageResolver.resolve(any(), anyInt())).thenReturn("2 authorized AD groups configured.");
        lenient().when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(template.getId()))
                .thenReturn(List.of());
    }

    @Test
    void assemble_adminAudience_includesApiPolicyBaselineFields() {
        ContractResultView contract = service.assemble(
                template,
                policy,
                "dev",
                credentialSummary,
                ContractViewAudience.ADMIN
        );

        assertThat(contract.apiPolicy().policyVersion()).isEqualTo(2);
        assertThat(contract.apiPolicy().updatedAt()).isNotNull();
        assertThat(contract.apiPolicy().updatedBy()).isEqualTo("10000002");
        assertThat(contract.apiPolicy().allowedOutputFormats()).containsExactly("DOCX", "PDF");
        assertThat(contract.apiPolicy().allowedOutputModes()).containsExactly("SYNC_STREAM", "ASYNC_TASK");
        assertThat(contract.apiPolicy().batchLimits().syncMaxItems()).isEqualTo(50);
        assertThat(contract.apiPolicy().batchLimits().asyncMaxItems()).isEqualTo(10000);
        assertThat(contract.apiPolicy().encryptionCapabilities().docxEnabled()).isTrue();
        assertThat(contract.apiPolicy().adGroupAuthorizationSummary().authorizationScopeSummary())
                .isEqualTo("2 authorized AD groups configured.");
        assertThat(contract.apiPolicy().adGroupAuthorizationSummary().effectivePolicyDescription()).isNotBlank();
        assertThat(contract.apiPolicy().credentialSummary().credentialExternalId()).isEqualTo("cred-ext-001");
        assertThat(contract.apiPolicy().credentialSummary().fingerprintSummary()).isEqualTo("fp-cred-ext-001");
        assertThat(contract.defaultRoute().updatedAt()).isNotNull();
        assertThat(contract.defaultRoute().updatedBy()).isEqualTo("10000002");
    }

    @Test
    void assemble_callerAudience_excludesManagementDetail() {
        ContractResultView contract = service.assemble(
                template,
                policy,
                "dev",
                credentialSummary,
                ContractViewAudience.CALLER
        );

        assertThat(contract.apiPolicy().policyVersion()).isEqualTo(2);
        assertThat(contract.apiPolicy().updatedAt()).isNull();
        assertThat(contract.apiPolicy().updatedBy()).isNull();
        assertThat(contract.apiPolicy().allowedOutputFormats()).containsExactly("DOCX", "PDF");
        assertThat(contract.apiPolicy().adGroupAuthorizationSummary().authorizationScopeSummary()).isNull();
        assertThat(contract.apiPolicy().adGroupAuthorizationSummary().effectivePolicyDescription()).isNull();
        assertThat(contract.apiPolicy().credentialSummary().credentialExternalId()).isNull();
        assertThat(contract.apiPolicy().credentialSummary().fingerprintSummary()).isNull();
        assertThat(contract.apiPolicy().credentialSummary().status()).isEqualTo("ACTIVE");
        assertThat(contract.defaultRoute().updatedAt()).isNull();
        assertThat(contract.defaultRoute().updatedBy()).isNull();
    }

    @Test
    void assemble_neverExposesRawAdGroupNamesOrSecrets() {
        ContractResultView adminContract = service.assemble(
                template,
                policy,
                "dev",
                credentialSummary,
                ContractViewAudience.ADMIN
        );
        ContractResultView callerContract = service.assemble(
                template,
                policy,
                "dev",
                credentialSummary,
                ContractViewAudience.CALLER
        );

        String adminJson = contractJson(adminContract);
        String callerJson = contractJson(callerContract);

        assertThat(adminJson).doesNotContain("RETAIL_API", "CORP_API", "secret", "ciphertext");
        assertThat(callerJson).doesNotContain("RETAIL_API", "CORP_API", "secret", "ciphertext");
    }

    private String contractJson(ContractResultView contract) {
        try {
            return new ObjectMapper().findAndRegisterModules().writeValueAsString(contract);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
