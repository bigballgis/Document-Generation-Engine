package com.bank.docgen.apimgmt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.api.ApiAccessAlertView;
import com.bank.docgen.apimgmt.domain.ApiAccessAlertType;
import com.bank.docgen.apimgmt.domain.ApiCredentialLifecycleSupport;
import com.bank.docgen.apimgmt.domain.ApiCredentialStatus;
import com.bank.docgen.apimgmt.persistence.ApiCredentialEntity;
import com.bank.docgen.apimgmt.persistence.ApiCredentialRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiAccessAlertQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-08T00:00:00Z");

    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private ApiCredentialRepository apiCredentialRepository;

    private ApiAccessAlertQueryService service;
    private ManagementSessionClaims groupAdmin;

    @BeforeEach
    void setUp() {
        service = new ApiAccessAlertQueryService(
                new GroupAccessService(),
                templateRepository,
                apiPolicyRepository,
                apiCredentialRepository,
                new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        groupAdmin = new ManagementSessionClaims(
                "10000002",
                "Admin",
                "admin@example.com",
                AuthSource.LOCAL,
                List.of("GROUP_ADMIN"),
                List.of("RETAIL"),
                "/",
                List.of(),
                NOW.plus(1, ChronoUnit.HOURS)
        );
    }

    @Test
    void listAlerts_returnsMissingAdGroupNoCredentialsAndExpiringCredentialAlerts() {
        UUID templateId = UUID.randomUUID();
        TemplateEntity template = publishedTemplate(templateId, "RETAIL", "Retail Statement", "TPL-RETAIL");
        ApiPolicyEntity policy = new ApiPolicyEntity(UUID.randomUUID(), templateId, "[]", "10000001");
        ApiCredentialEntity expiringCredential = credential(
                templateId,
                "CRED-EXP",
                NOW.minus(ApiCredentialLifecycleSupport.DEFAULT_EXPIRY_DAYS - 5, ChronoUnit.DAYS)
        );

        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInAndLifecycleStatusOrderByUpdatedAtDesc(
                List.of("RETAIL"),
                TemplateLifecycleStatus.PUBLISHED
        )).thenReturn(List.of(template));
        when(apiPolicyRepository.findByTemplateIdIn(List.of(templateId))).thenReturn(List.of(policy));
        when(apiCredentialRepository.findByTemplateIdIn(List.of(templateId)))
                .thenReturn(List.of(expiringCredential));

        List<ApiAccessAlertView> alerts = service.listAlerts(groupAdmin);

        assertThat(alerts).extracting(ApiAccessAlertView::alertType)
                .containsExactlyInAnyOrder(
                        ApiAccessAlertType.MISSING_AD_GROUP,
                        ApiAccessAlertType.EXPIRING_CREDENTIAL
                );
        ApiAccessAlertView missingAdGroup = alerts.stream()
                .filter(alert -> alert.alertType() == ApiAccessAlertType.MISSING_AD_GROUP)
                .findFirst()
                .orElseThrow();
        assertThat(missingAdGroup.hubDeepLinkPath())
                .isEqualTo("/templates/" + templateId + "?tab=apiAccess#domain=AD_GROUP_AUTHORIZATION");
        assertThat(missingAdGroup.detailMessageKey()).isEqualTo("apiPolicy.home.alerts.missingAdGroup");

        ApiAccessAlertView expiring = alerts.stream()
                .filter(alert -> alert.alertType() == ApiAccessAlertType.EXPIRING_CREDENTIAL)
                .findFirst()
                .orElseThrow();
        assertThat(expiring.credentialExternalId()).isEqualTo("CRED-EXP");
        assertThat(expiring.expiresAt()).isEqualTo(ApiCredentialLifecycleSupport.resolveExpiresAt(expiringCredential));
    }

    @Test
    void listAlerts_returnsNoCredentialsWhenOnlyRevokedCredentialsExist() {
        UUID templateId = UUID.randomUUID();
        TemplateEntity template = publishedTemplate(templateId, "RETAIL", "Retail Statement", "TPL-RETAIL");
        ApiPolicyEntity policy = new ApiPolicyEntity(UUID.randomUUID(), templateId, "[\"G1\"]", "10000001");
        ApiCredentialEntity revoked = credential(templateId, "CRED-REV", NOW.minus(10, ChronoUnit.DAYS));
        revoked.revoke();

        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInAndLifecycleStatusOrderByUpdatedAtDesc(
                List.of("RETAIL"),
                TemplateLifecycleStatus.PUBLISHED
        )).thenReturn(List.of(template));
        when(apiPolicyRepository.findByTemplateIdIn(List.of(templateId))).thenReturn(List.of(policy));
        when(apiCredentialRepository.findByTemplateIdIn(List.of(templateId))).thenReturn(List.of(revoked));

        List<ApiAccessAlertView> alerts = service.listAlerts(groupAdmin);

        assertThat(alerts).extracting(ApiAccessAlertView::alertType)
                .containsExactly(ApiAccessAlertType.NO_CREDENTIALS);
        assertThat(alerts.getFirst().hubDeepLinkPath())
                .isEqualTo("/templates/" + templateId + "?tab=apiAccess");
    }

    @Test
    void listAlerts_excludesTemplatesOutsideAuthorizedGroups() {
        ManagementSessionClaims wholesaleAdmin = new ManagementSessionClaims(
                "10000003",
                "Admin",
                "admin@example.com",
                AuthSource.LOCAL,
                List.of("GROUP_ADMIN"),
                List.of("WHOLESALE"),
                "/",
                List.of(),
                NOW.plus(1, ChronoUnit.HOURS)
        );

        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInAndLifecycleStatusOrderByUpdatedAtDesc(
                List.of("WHOLESALE"),
                TemplateLifecycleStatus.PUBLISHED
        )).thenReturn(List.of());

        assertThat(service.listAlerts(wholesaleAdmin)).isEmpty();
        verifyNoInteractions(apiPolicyRepository, apiCredentialRepository);
    }

    @Test
    void listAlerts_deniedForNonApiAdmin() {
        ManagementSessionClaims author = new ManagementSessionClaims(
                "10000004",
                "Author",
                "author@example.com",
                AuthSource.LOCAL,
                List.of("TEMPLATE_AUTHOR"),
                List.of("RETAIL"),
                "/",
                List.of(),
                NOW.plus(1, ChronoUnit.HOURS)
        );

        assertThatThrownBy(() -> service.listAlerts(author))
                .isInstanceOf(ApiManagementAccessDeniedException.class);
        verifyNoInteractions(templateRepository, apiPolicyRepository, apiCredentialRepository);
    }

    @Test
    void listAlerts_treatsMalformedAllowedAdGroupsJsonAsEmpty() {
        UUID templateId = UUID.randomUUID();
        TemplateEntity template = publishedTemplate(templateId, "RETAIL", "Retail Statement", "TPL-RETAIL");
        ApiPolicyEntity policy = new ApiPolicyEntity(UUID.randomUUID(), templateId, "not-json", "10000001");

        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInAndLifecycleStatusOrderByUpdatedAtDesc(
                List.of("RETAIL"),
                TemplateLifecycleStatus.PUBLISHED
        )).thenReturn(List.of(template));
        when(apiPolicyRepository.findByTemplateIdIn(List.of(templateId))).thenReturn(List.of(policy));
        when(apiCredentialRepository.findByTemplateIdIn(List.of(templateId))).thenReturn(List.of());

        List<ApiAccessAlertView> alerts = service.listAlerts(groupAdmin);

        assertThat(alerts).extracting(ApiAccessAlertView::alertType)
                .containsExactlyInAnyOrder(
                        ApiAccessAlertType.MISSING_AD_GROUP,
                        ApiAccessAlertType.NO_CREDENTIALS
                );
    }

    @Test
    void listAlerts_sortsWhenTemplateNameIsNull() {
        UUID templateId = UUID.randomUUID();
        TemplateEntity template = publishedTemplate(templateId, "RETAIL", null, "TPL-RETAIL");
        ApiPolicyEntity policy = new ApiPolicyEntity(UUID.randomUUID(), templateId, "[\"G1\"]", "10000001");

        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInAndLifecycleStatusOrderByUpdatedAtDesc(
                List.of("RETAIL"),
                TemplateLifecycleStatus.PUBLISHED
        )).thenReturn(List.of(template));
        when(apiPolicyRepository.findByTemplateIdIn(List.of(templateId))).thenReturn(List.of(policy));
        when(apiCredentialRepository.findByTemplateIdIn(List.of(templateId))).thenReturn(List.of());

        assertThat(service.listAlerts(groupAdmin)).isNotEmpty();
    }

    @Test
    void listAlerts_honorsExplicitExpiringSoonStatus() {
        UUID templateId = UUID.randomUUID();
        TemplateEntity template = publishedTemplate(templateId, "RETAIL", "Retail Statement", "TPL-RETAIL");
        ApiPolicyEntity policy = new ApiPolicyEntity(UUID.randomUUID(), templateId, "[\"G1\"]", "10000001");
        ApiCredentialEntity credential = credential(
                templateId,
                "CRED-SOON",
                NOW.minus(30, ChronoUnit.DAYS)
        );
        setCredentialStatus(credential, ApiCredentialStatus.EXPIRING_SOON);

        when(templateRepository.findByDeletedAtIsNullAndGroupCodeInAndLifecycleStatusOrderByUpdatedAtDesc(
                List.of("RETAIL"),
                TemplateLifecycleStatus.PUBLISHED
        )).thenReturn(List.of(template));
        when(apiPolicyRepository.findByTemplateIdIn(List.of(templateId))).thenReturn(List.of(policy));
        when(apiCredentialRepository.findByTemplateIdIn(List.of(templateId))).thenReturn(List.of(credential));

        List<ApiAccessAlertView> alerts = service.listAlerts(groupAdmin);

        assertThat(alerts).extracting(ApiAccessAlertView::alertType)
                .contains(ApiAccessAlertType.EXPIRING_CREDENTIAL);
    }

    private static TemplateEntity publishedTemplate(
            UUID templateId,
            String groupCode,
            String name,
            String externalId
    ) {
        TemplateEntity template = new TemplateEntity(
                templateId,
                externalId,
                groupCode,
                name,
                null,
                UUID.randomUUID(),
                "10000001"
        );
        template.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        return template;
    }

    private static ApiCredentialEntity credential(UUID templateId, String externalId, Instant createdAt) {
        ApiCredentialEntity credential = new ApiCredentialEntity(
                UUID.randomUUID(),
                externalId,
                templateId,
                "hash",
                "10000001"
        );
        try {
            var field = ApiCredentialEntity.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(credential, createdAt);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        return credential;
    }

    private static void setCredentialStatus(ApiCredentialEntity credential, ApiCredentialStatus status) {
        try {
            var field = ApiCredentialEntity.class.getDeclaredField("status");
            field.setAccessible(true);
            field.set(credential, status);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
