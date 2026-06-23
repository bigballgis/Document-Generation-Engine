package com.bank.docgen.runtime.security;

import com.bank.docgen.apimgmt.domain.ApiCredentialStatus;
import com.bank.docgen.apimgmt.persistence.ApiCredentialEntity;
import com.bank.docgen.apimgmt.persistence.ApiCredentialRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.apimgmt.service.ConfigAdGroupResolver;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.security.PasswordHashService;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiCredentialAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER_CREDENTIAL_ID = "X-Api-Credential-Id";
    public static final String HEADER_CREDENTIAL_SECRET = "X-Api-Credential-Secret";
    public static final String HEADER_ACCESS_ACCOUNT = "X-Access-Account";

    private final ApiCredentialRepository apiCredentialRepository;
    private final ApiPolicyRepository apiPolicyRepository;
    private final TemplateRepository templateRepository;
    private final PasswordHashService passwordHashService;
    private final ConfigAdGroupResolver adGroupResolver;
    private final ObjectMapper objectMapper;

    public ApiCredentialAuthenticationFilter(
            ApiCredentialRepository apiCredentialRepository,
            ApiPolicyRepository apiPolicyRepository,
            TemplateRepository templateRepository,
            PasswordHashService passwordHashService,
            ConfigAdGroupResolver adGroupResolver,
            ObjectMapper objectMapper
    ) {
        this.apiCredentialRepository = apiCredentialRepository;
        this.apiPolicyRepository = apiPolicyRepository;
        this.templateRepository = templateRepository;
        this.passwordHashService = passwordHashService;
        this.adGroupResolver = adGroupResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/management/") || !path.matches("/api/[^/]+/v1/.*");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            RuntimeSessionClaims claims = authenticate(request);
            org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .setAuthentication(new RuntimeAuthentication(claims));
            filterChain.doFilter(request, response);
        } catch (RuntimeAuthenticationException ex) {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":{\"code\":\"%s\",\"messageKey\":\"%s\"}}".formatted(ex.code(), ex.messageKey()));
        }
    }

    RuntimeSessionClaims authenticate(HttpServletRequest request) {
        String credentialId = request.getHeader(HEADER_CREDENTIAL_ID);
        String secret = request.getHeader(HEADER_CREDENTIAL_SECRET);
        String accessAccount = request.getHeader(HEADER_ACCESS_ACCOUNT);
        if (credentialId == null || credentialId.isBlank() || secret == null || secret.isBlank()) {
            throw new RuntimeAuthenticationException(
                    ApiErrorCodes.INVALID_CREDENTIALS,
                    "api.error.runtime.invalidCredentials"
            );
        }
        if (accessAccount == null || accessAccount.isBlank()) {
            throw new RuntimeAuthenticationException(
                    ApiErrorCodes.REQUEST_BODY_INVALID,
                    "api.error.runtime.accessAccountRequired"
            );
        }
        ApiCredentialEntity credential = apiCredentialRepository.findByExternalId(credentialId)
                .orElseThrow(() -> new RuntimeAuthenticationException(
                        ApiErrorCodes.INVALID_CREDENTIALS,
                        "api.error.runtime.invalidCredentials"
                ));
        if (credential.getStatus() != ApiCredentialStatus.ACTIVE
                || !passwordHashService.matches(secret, credential.getSecretHash())) {
            throw new RuntimeAuthenticationException(
                    ApiErrorCodes.INVALID_CREDENTIALS,
                    "api.error.runtime.invalidCredentials"
            );
        }
        TemplateEntity template = templateRepository.findByIdAndDeletedAtIsNull(credential.getTemplateId())
                .orElseThrow(() -> new RuntimeAuthenticationException(
                        ApiErrorCodes.INVALID_CREDENTIALS,
                        "api.error.runtime.invalidCredentials"
                ));
        String pathTemplateId = extractTemplateIdFromPath(request.getRequestURI());
        if (pathTemplateId != null && !pathTemplateId.equals(template.getExternalId())) {
            throw new RuntimeAuthenticationException(
                    ApiErrorCodes.ACCESS_DENIED,
                    "api.error.runtime.templateCredentialMismatch"
            );
        }
        ApiPolicyEntity policy = apiPolicyRepository.findByTemplateId(template.getId())
                .orElseThrow(() -> new RuntimeAuthenticationException(
                        ApiErrorCodes.ACCESS_DENIED,
                        "api.error.runtime.policyNotConfigured"
                ));
        List<String> allowedGroups = readGroups(policy.getAllowedAdGroupsJson());
        if (!adGroupResolver.isAuthorized(accessAccount, allowedGroups)) {
            throw new RuntimeAuthenticationException(
                    ApiErrorCodes.ACCESS_DENIED,
                    "api.error.runtime.adGroupDenied"
            );
        }
        return new RuntimeSessionClaims(
                credential.getId(),
                credential.getExternalId(),
                template.getId(),
                template.getExternalId(),
                accessAccount,
                adGroupResolver.resolveGroups(accessAccount)
        );
    }

    private String extractTemplateIdFromPath(String path) {
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("templates".equals(parts[i]) && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        return null;
    }

    private List<String> readGroups(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }
}
