package com.bank.docgen.apimgmt.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiCredentialRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.runtime.service.ContractAssemblyService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.sharedkernel.security.PasswordHashService;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.service.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Authorization coverage for the previously untested apimgmt module (OPT-C).
 * Verifies the API-policy management gate fails closed for non-admin roles and
 * does not touch persistence, and that the admin path proceeds to lookup.
 */
class ApiManagementServiceAuthorizationTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    private final TemplateService templateService = mock(TemplateService.class);
    private final ApiPolicyRepository apiPolicyRepository = mock(ApiPolicyRepository.class);
    private final ApiCredentialRepository apiCredentialRepository = mock(ApiCredentialRepository.class);
    private final PasswordHashService passwordHashService = mock(PasswordHashService.class);
    private final ManagementAuditRecorder auditRecorder = mock(ManagementAuditRecorder.class);
    private final ContractAssemblyService contractAssemblyService = mock(ContractAssemblyService.class);

    private final ApiManagementService service = new ApiManagementService(
            templateService,
            apiPolicyRepository,
            apiCredentialRepository,
            new GroupAccessService(),
            passwordHashService,
            auditRecorder,
            contractAssemblyService,
            new ObjectMapper()
    );

    private ManagementSessionClaims session(List<String> roles) {
        return new ManagementSessionClaims(
                "user", "User", "user@bank.test", AuthSource.LOCAL,
                roles, List.of("G1"), "/", List.of(), Instant.now().plusSeconds(600));
    }

    @Test
    void getPolicyDeniedForNonApiAdminRoleAndDoesNotTouchPersistence() {
        assertThatThrownBy(() -> service.getPolicy(TEMPLATE_ID, session(List.of("TEMPLATE_AUTHOR"))))
                .isInstanceOf(ApiManagementAccessDeniedException.class);

        verifyNoInteractions(apiPolicyRepository);
    }

    @Test
    void getPolicyForAdminWithoutPolicyRaisesNotFound() {
        ManagementSessionClaims admin = session(List.of("GROUP_ADMIN"));
        when(templateService.requireReadableTemplate(TEMPLATE_ID, admin))
                .thenReturn(mock(TemplateEntity.class));
        when(apiPolicyRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPolicy(TEMPLATE_ID, admin))
                .isInstanceOf(ApiManagementNotFoundException.class);
    }

    @Test
    void getPolicyDeniedForEmptyRoles() {
        assertThatThrownBy(() -> service.getPolicy(TEMPLATE_ID, session(List.of())))
                .isInstanceOf(ApiManagementAccessDeniedException.class);
        verifyNoInteractions(templateService);
    }
}
