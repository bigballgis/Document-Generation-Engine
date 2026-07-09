package com.bank.docgen.apimgmt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.mapping.ApiPolicyViewMapperFactory;
import com.bank.docgen.apimgmt.persistence.ApiCredentialEntity;
import com.bank.docgen.apimgmt.persistence.ApiCredentialRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyVersionRepository;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.runtime.service.ContractAssemblyService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.sharedkernel.security.PasswordHashService;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.service.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class ApiManagementServiceCredentialRotationTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID CREDENTIAL_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private final TemplateService templateService = Mockito.mock(TemplateService.class);
    private final ApiPolicyRepository apiPolicyRepository = Mockito.mock(ApiPolicyRepository.class);
    private final ApiCredentialRepository apiCredentialRepository = Mockito.mock(ApiCredentialRepository.class);
    private final PasswordHashService passwordHashService = Mockito.mock(PasswordHashService.class);
    private final ManagementAuditRecorder auditRecorder = Mockito.mock(ManagementAuditRecorder.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ApiManagementService service = new ApiManagementService(
            templateService,
            apiPolicyRepository,
            apiCredentialRepository,
            new GroupAccessService(),
            passwordHashService,
            auditRecorder,
            Mockito.mock(ContractAssemblyService.class),
            objectMapper,
            new ApiPolicyVersionSnapshotService(
                    Mockito.mock(ApiPolicyVersionRepository.class),
                    objectMapper
            ),
            Mockito.mock(TemplateVersionRepository.class),
            new TemplateAdGroupAuthorizationCache(),
            Mockito.mock(ApiPolicyImpactPreviewService.class),
            ApiPolicyViewMapperFactory.create(objectMapper)
    );

    @Test
    void secondRotationRecordsGenerationAndPreviousFingerprint() throws Exception {
        ManagementSessionClaims admin = new ManagementSessionClaims(
                "admin", "Admin", "admin@bank.test", AuthSource.LOCAL,
                List.of("GROUP_ADMIN"), List.of("G1"), "/", List.of(), Instant.now().plusSeconds(600)
        );
        TemplateEntity template = new TemplateEntity(
                TEMPLATE_ID, "TPL-001", "RETAIL", "Sample", null, UUID.randomUUID(), "10000001"
        );
        ApiCredentialEntity credential = new ApiCredentialEntity(
                CREDENTIAL_ID, "CRED-ROTATE", TEMPLATE_ID, "hash", "admin"
        );
        credential.rotateSecret("hash-1");

        when(templateService.requireReadableTemplate(TEMPLATE_ID, admin)).thenReturn(template);
        when(apiCredentialRepository.findById(CREDENTIAL_ID)).thenReturn(Optional.of(credential));
        when(passwordHashService.hash(Mockito.anyString())).thenReturn("hash-2");

        service.rotateCredential(TEMPLATE_ID, CREDENTIAL_ID, admin);

        assertThat(credential.getRotationGeneration()).isEqualTo(2);

        ArgumentCaptor<Integer> generationCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> fingerprintCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditRecorder).recordCredentialRotated(
                eq(TEMPLATE_ID),
                eq("RETAIL"),
                eq(CREDENTIAL_ID),
                eq("CRED-ROTATE"),
                eq("admin"),
                Mockito.anyString(),
                generationCaptor.capture(),
                fingerprintCaptor.capture()
        );
        assertThat(generationCaptor.getValue()).isEqualTo(2);
        assertThat(fingerprintCaptor.getValue()).isEqualTo("fp-CRED-ROTATE");
    }
}
