package com.bank.docgen.runtime.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.domain.ApiCredentialLifecycleSupport;
import com.bank.docgen.apimgmt.persistence.ApiCredentialEntity;
import com.bank.docgen.apimgmt.persistence.ApiCredentialRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.apimgmt.service.AdGroupResolver;
import com.bank.docgen.apimgmt.service.TemplateAdGroupAuthorizationCache;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.PasswordHashService;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class ApiCredentialAuthenticationFilterRotationGraceTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock
    private ApiCredentialRepository apiCredentialRepository;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private PasswordHashService passwordHashService;
    @Mock
    private AdGroupResolver adGroupResolver;
    @Mock
    private MessageResolver messageResolver;
    @Mock
    private TemplateAdGroupAuthorizationCache authorizationCache;

    private ApiCredentialAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ApiCredentialAuthenticationFilter(
                apiCredentialRepository,
                apiPolicyRepository,
                templateRepository,
                passwordHashService,
                adGroupResolver,
                authorizationCache,
                new ObjectMapper(),
                new TraceIdProvider(),
                messageResolver
        );
    }

    @Test
    void previousSecretAcceptedWithinTwentyEightDayGrace() {
        ApiCredentialEntity credential = new ApiCredentialEntity(
                UUID.randomUUID(), "CRED-1", TEMPLATE_ID, "hash-new", "admin"
        );
        credential.rotateSecret("hash-new", Instant.now());
        // simulate prior hash retention after rotateSecret already set previous=old constructor hash
        // rotateSecret moved constructor hash to previous; set previous explicitly for clarity
        setPrevious(credential, "hash-old", Instant.now().plus(10, ChronoUnit.DAYS));

        TemplateEntity template = new TemplateEntity(
                TEMPLATE_ID, "TPL-RETAIL-LETTER", "RETAIL", "Letter", null, UUID.randomUUID(), "10000001"
        );
        ApiPolicyEntity policy = new ApiPolicyEntity(UUID.randomUUID(), TEMPLATE_ID, "[\"G1\"]", "admin");
        when(apiCredentialRepository.findByExternalId("CRED-1")).thenReturn(Optional.of(credential));
        when(passwordHashService.matches("old-secret", "hash-new")).thenReturn(false);
        when(passwordHashService.matches("old-secret", "hash-old")).thenReturn(true);
        when(templateRepository.findByIdAndDeletedAtIsNull(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(apiPolicyRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(Optional.of(policy));
        when(authorizationCache.getAllowedGroups(TEMPLATE_ID)).thenReturn(Optional.of(List.of("G1")));
        when(adGroupResolver.resolveGroups("svc-caller")).thenReturn(List.of("G1"));

        MockHttpServletRequest request = runtimeRequest();
        request.addHeader(ApiCredentialAuthenticationFilter.HEADER_CREDENTIAL_SECRET, "old-secret");

        RuntimeSessionClaims claims = filter.authenticate(request);
        assertThat(claims.credentialExternalId()).isEqualTo("CRED-1");
        assertThat(ApiCredentialLifecycleSupport.ROTATION_GRACE_DAYS).isEqualTo(28);
    }

    @Test
    void previousSecretRejectedAfterGraceEnds() {
        ApiCredentialEntity credential = new ApiCredentialEntity(
                UUID.randomUUID(), "CRED-1", TEMPLATE_ID, "hash-new", "admin"
        );
        setPrevious(credential, "hash-old", Instant.now().minus(1, ChronoUnit.MINUTES));
        when(apiCredentialRepository.findByExternalId("CRED-1")).thenReturn(Optional.of(credential));
        when(passwordHashService.matches("old-secret", "hash-new")).thenReturn(false);

        MockHttpServletRequest request = runtimeRequest();
        request.addHeader(ApiCredentialAuthenticationFilter.HEADER_CREDENTIAL_SECRET, "old-secret");

        assertThatThrownBy(() -> filter.authenticate(request))
                .isInstanceOf(RuntimeAuthenticationException.class)
                .satisfies(ex -> assertThat(((RuntimeAuthenticationException) ex).code())
                        .isEqualTo(ApiErrorCodes.INVALID_CREDENTIALS));
    }

    private static MockHttpServletRequest runtimeRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/dev/v1/templates/TPL-RETAIL-LETTER/contract");
        request.addHeader(ApiCredentialAuthenticationFilter.HEADER_CREDENTIAL_ID, "CRED-1");
        request.addHeader(ApiCredentialAuthenticationFilter.HEADER_ACCESS_ACCOUNT, "svc-caller");
        return request;
    }

    private static void setPrevious(ApiCredentialEntity credential, String previousHash, Instant endsAt) {
        try {
            var hashField = ApiCredentialEntity.class.getDeclaredField("previousSecretHash");
            hashField.setAccessible(true);
            hashField.set(credential, previousHash);
            var endsField = ApiCredentialEntity.class.getDeclaredField("rotationGracePeriodEndsAt");
            endsField.setAccessible(true);
            endsField.set(credential, endsAt);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
