package com.bank.docgen.runtime.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiCredentialEntity;
import com.bank.docgen.apimgmt.persistence.ApiCredentialRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.apimgmt.service.ConfigAdGroupResolver;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.PasswordHashService;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class ApiCredentialAuthenticationFilterTest {

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
    private ConfigAdGroupResolver adGroupResolver;
    @Mock
    private MessageResolver messageResolver;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TraceIdProvider traceIdProvider = new TraceIdProvider();

    private ApiCredentialAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ApiCredentialAuthenticationFilter(
                apiCredentialRepository,
                apiPolicyRepository,
                templateRepository,
                passwordHashService,
                adGroupResolver,
                objectMapper,
                traceIdProvider,
                messageResolver
        );
    }

    @Test
    void missingCredentialsWritesUnifiedErrorEnvelope() throws Exception {
        when(messageResolver.resolve("api.error.runtime.invalidCredentials"))
                .thenReturn("Invalid API credentials.");

        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/dev/v1/templates/TPL-RETAIL-LETTER/contract");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.has("metadata")).isTrue();
        assertThat(body.get("metadata").get("auditId").asText()).startsWith("AUD-");
        assertThat(body.get("metadata").get("traceId").asText()).isNotBlank();
        assertThat(body.get("error").get("code").asText()).isEqualTo(ApiErrorCodes.INVALID_CREDENTIALS);
        assertThat(body.get("error").get("category").asText()).isEqualTo("AUTHENTICATION");
        assertThat(body.get("error").get("messageKey").asText())
                .isEqualTo("api.error.runtime.invalidCredentials");
        assertThat(body.get("error").get("message").asText()).isEqualTo("Invalid API credentials.");
        assertThat(body.get("error").get("retryable").asBoolean()).isFalse();
    }

    @Test
    void malformedPolicyGroupsFailClosed() {
        ApiCredentialEntity credential = activeCredential();
        TemplateEntity template = templateEntity();
        ApiPolicyEntity policy = new ApiPolicyEntity(UUID.randomUUID(), TEMPLATE_ID, "not-json", "admin");
        when(apiCredentialRepository.findByExternalId("CRED-1")).thenReturn(Optional.of(credential));
        when(passwordHashService.matches("secret", credential.getSecretHash())).thenReturn(true);
        when(templateRepository.findByIdAndDeletedAtIsNull(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(apiPolicyRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(Optional.of(policy));

        MockHttpServletRequest request = runtimeRequest("/api/dev/v1/templates/TPL-RETAIL-LETTER/contract");

        assertThatThrownBy(() -> filter.authenticate(request))
                .isInstanceOf(RuntimeAuthenticationException.class)
                .satisfies(ex -> assertThat(((RuntimeAuthenticationException) ex).messageKey())
                        .isEqualTo("api.error.runtime.policyNotConfigured"));
    }

    @Test
    void adGroupDeniedUsesForbiddenStatusAndAuthorizationCategory() throws Exception {
        when(messageResolver.resolve("api.error.runtime.adGroupDenied"))
                .thenReturn("The caller is not authorized by AD Group policy.");

        ApiCredentialEntity credential = activeCredential();
        TemplateEntity template = templateEntity();
        ApiPolicyEntity policy = new ApiPolicyEntity(
                UUID.randomUUID(), TEMPLATE_ID, "[\"grp-a\"]", "admin");
        when(apiCredentialRepository.findByExternalId("CRED-1")).thenReturn(Optional.of(credential));
        when(passwordHashService.matches("secret", credential.getSecretHash())).thenReturn(true);
        when(templateRepository.findByIdAndDeletedAtIsNull(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(apiPolicyRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(Optional.of(policy));
        when(adGroupResolver.isAuthorized(eq("svc-caller"), any())).thenReturn(false);

        MockHttpServletRequest request = runtimeRequest("/api/dev/v1/templates/TPL-RETAIL-LETTER/contract");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, mock(FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(403);
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("error").get("code").asText()).isEqualTo(ApiErrorCodes.ACCESS_DENIED);
        assertThat(body.get("error").get("category").asText()).isEqualTo("AUTHORIZATION");
    }

    @Test
    void validCredentialsProceedToFilterChain() throws Exception {
        ApiCredentialEntity credential = activeCredential();
        TemplateEntity template = templateEntity();
        ApiPolicyEntity policy = new ApiPolicyEntity(
                UUID.randomUUID(), TEMPLATE_ID, "[\"grp-a\"]", "admin");
        when(apiCredentialRepository.findByExternalId("CRED-1")).thenReturn(Optional.of(credential));
        when(passwordHashService.matches("secret", credential.getSecretHash())).thenReturn(true);
        when(templateRepository.findByIdAndDeletedAtIsNull(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(apiPolicyRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(Optional.of(policy));
        when(adGroupResolver.isAuthorized("svc-caller", List.of("grp-a"))).thenReturn(true);
        when(adGroupResolver.resolveGroups("svc-caller")).thenReturn(List.of("grp-a"));

        MockHttpServletRequest request = runtimeRequest("/api/dev/v1/templates/TPL-RETAIL-LETTER/contract");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    private MockHttpServletRequest runtimeRequest(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.addHeader(ApiCredentialAuthenticationFilter.HEADER_CREDENTIAL_ID, "CRED-1");
        request.addHeader(ApiCredentialAuthenticationFilter.HEADER_CREDENTIAL_SECRET, "secret");
        request.addHeader(ApiCredentialAuthenticationFilter.HEADER_ACCESS_ACCOUNT, "svc-caller");
        return request;
    }

    private ApiCredentialEntity activeCredential() {
        return new ApiCredentialEntity(UUID.randomUUID(), "CRED-1", TEMPLATE_ID, "hash", "admin");
    }

    private TemplateEntity templateEntity() {
        return new TemplateEntity(
                TEMPLATE_ID, "TPL-RETAIL-LETTER", "G1", "name", "desc", UUID.randomUUID(), "admin");
    }
}
