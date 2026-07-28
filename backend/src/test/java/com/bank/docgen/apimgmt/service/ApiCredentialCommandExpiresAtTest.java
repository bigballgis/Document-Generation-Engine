package com.bank.docgen.apimgmt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.api.ApiCredentialCreatedView;
import com.bank.docgen.apimgmt.api.RotateCredentialResponse;
import com.bank.docgen.apimgmt.domain.ApiCredentialLifecycleSupport;
import com.bank.docgen.apimgmt.mapping.ApiPolicyViewMapperFactory;
import com.bank.docgen.apimgmt.persistence.ApiCredentialEntity;
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
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.service.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class ApiCredentialCommandExpiresAtTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID CREDENTIAL_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private final TemplateService templateService = Mockito.mock(TemplateService.class);
    private final ApiPolicyRepository apiPolicyRepository = Mockito.mock(ApiPolicyRepository.class);
    private final ApiCredentialRepository apiCredentialRepository = Mockito.mock(ApiCredentialRepository.class);
    private final PasswordHashService passwordHashService = Mockito.mock(PasswordHashService.class);
    private final ManagementAuditRecorder auditRecorder = Mockito.mock(ManagementAuditRecorder.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ApiManagementService service;
    private ManagementSessionClaims admin;
    private TemplateEntity template;

    @BeforeEach
    void setUp() {
        service = new ApiManagementService(
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
        admin = new ManagementSessionClaims(
                "admin", "Admin", "admin@bank.test", AuthSource.LOCAL,
                List.of("GROUP_ADMIN"), List.of("G1"), "/", List.of(), Instant.now().plusSeconds(600)
        );
        template = new TemplateEntity(
                TEMPLATE_ID, "TPL-001", "RETAIL", "Sample", null, UUID.randomUUID(), "10000001"
        );
        when(templateService.requireReadableTemplate(TEMPLATE_ID, admin)).thenReturn(template);
        when(apiPolicyRepository.findByTemplateId(TEMPLATE_ID))
                .thenReturn(Optional.of(new ApiPolicyEntity(UUID.randomUUID(), TEMPLATE_ID, "[\"G1\"]", "admin")));
        when(passwordHashService.hash(any())).thenReturn("hashed-secret");
    }

    @Test
    void createCredential_persistsDefaultExpiresAtOfCreatedAtPlusOneHundredEightyDays() {
        ArgumentCaptor<ApiCredentialEntity> captors = ArgumentCaptor.forClass(ApiCredentialEntity.class);
        when(apiCredentialRepository.save(any(ApiCredentialEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Instant before = Instant.now().minusSeconds(2);
        ApiCredentialCreatedView created = service.createCredential(TEMPLATE_ID, admin);
        Instant after = Instant.now().plusSeconds(2);

        verify(apiCredentialRepository).save(captors.capture());
        ApiCredentialEntity saved = captors.getValue();
        Instant expectedMin = before.plus(ApiCredentialLifecycleSupport.DEFAULT_EXPIRY_DAYS, ChronoUnit.DAYS);
        Instant expectedMax = after.plus(ApiCredentialLifecycleSupport.DEFAULT_EXPIRY_DAYS, ChronoUnit.DAYS);

        assertThat(saved.getExpiresAt()).isNotNull();
        assertThat(saved.getExpiresAt()).isAfterOrEqualTo(expectedMin);
        assertThat(saved.getExpiresAt()).isBeforeOrEqualTo(expectedMax);
        assertThat(saved.getExpiresAt())
                .isEqualTo(ApiCredentialLifecycleSupport.defaultExpiresAt(saved.getCreatedAt()));
        assertThat(created.secret()).isNotBlank();
    }

    @Test
    void rotateCredential_rebasesExpiresAtAndSetsTwentyEightDayGrace() {
        Instant fixedExpiry = Instant.parse("2026-12-01T00:00:00Z");
        ApiCredentialEntity credential = new ApiCredentialEntity(
                CREDENTIAL_ID, "CRED-ROTATE", TEMPLATE_ID, "hash", "admin"
        );
        setExpiresAt(credential, fixedExpiry);
        when(apiCredentialRepository.findById(CREDENTIAL_ID)).thenReturn(Optional.of(credential));
        when(apiCredentialRepository.save(any(ApiCredentialEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordHashService.hash(any())).thenReturn("hash-rotated");

        Instant before = Instant.now().minusSeconds(2);
        RotateCredentialResponse response = service.rotateCredential(TEMPLATE_ID, CREDENTIAL_ID, admin);
        Instant after = Instant.now().plusSeconds(2);

        assertThat(credential.getExpiresAt()).isNotEqualTo(fixedExpiry);
        assertThat(credential.getExpiresAt())
                .isAfterOrEqualTo(before.plus(ApiCredentialLifecycleSupport.DEFAULT_EXPIRY_DAYS, ChronoUnit.DAYS));
        assertThat(credential.getExpiresAt())
                .isBeforeOrEqualTo(after.plus(ApiCredentialLifecycleSupport.DEFAULT_EXPIRY_DAYS, ChronoUnit.DAYS));
        assertThat(credential.getPreviousSecretHash()).isEqualTo("hash");
        assertThat(credential.getRotationGracePeriodEndsAt())
                .isAfterOrEqualTo(before.plus(ApiCredentialLifecycleSupport.ROTATION_GRACE_DAYS, ChronoUnit.DAYS));
        assertThat(response.rotationGracePeriodEndsAt()).isEqualTo(credential.getRotationGracePeriodEndsAt());
        assertThat(response.expiresAt()).isEqualTo(credential.getExpiresAt());
        assertThat(response.secret()).isNotBlank();
        verify(apiCredentialRepository).save(credential);
    }

    @Test
    void createCredential_honorsOptionalExpiryDays() {
        when(apiCredentialRepository.save(any(ApiCredentialEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Instant before = Instant.now().minusSeconds(2);
        ApiCredentialCreatedView created = service.createCredential(TEMPLATE_ID, admin, 30);
        Instant after = Instant.now().plusSeconds(2);
        assertThat(created.expiresAt()).isAfterOrEqualTo(before.plus(30, ChronoUnit.DAYS));
        assertThat(created.expiresAt()).isBeforeOrEqualTo(after.plus(30, ChronoUnit.DAYS));
    }

    @Test
    void rotateCredential_rejectsEffectivelyExpired() {
        ApiCredentialEntity credential = new ApiCredentialEntity(
                CREDENTIAL_ID, "CRED-EXPIRED", TEMPLATE_ID, "hash", "admin"
        );
        setExpiresAt(credential, Instant.now().minus(1, ChronoUnit.DAYS));
        when(apiCredentialRepository.findById(CREDENTIAL_ID)).thenReturn(Optional.of(credential));
        org.junit.jupiter.api.Assertions.assertThrows(
                com.bank.docgen.template.service.TemplateValidationException.class,
                () -> service.rotateCredential(TEMPLATE_ID, CREDENTIAL_ID, admin)
        );
    }

    private static void setExpiresAt(ApiCredentialEntity credential, Instant expiresAt) {
        try {
            var field = ApiCredentialEntity.class.getDeclaredField("expiresAt");
            field.setAccessible(true);
            field.set(credential, expiresAt);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
