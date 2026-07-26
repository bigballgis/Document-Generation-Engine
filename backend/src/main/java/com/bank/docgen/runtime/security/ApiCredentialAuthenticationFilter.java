package com.bank.docgen.runtime.security;

import com.bank.docgen.apimgmt.domain.ApiCredentialLifecycleSupport;
import com.bank.docgen.apimgmt.domain.ApiCredentialStatus;
import com.bank.docgen.apimgmt.persistence.ApiCredentialEntity;
import com.bank.docgen.apimgmt.persistence.ApiCredentialRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.apimgmt.service.AdGroupAuthorization;
import com.bank.docgen.apimgmt.service.AdGroupResolver;
import com.bank.docgen.apimgmt.service.TemplateAdGroupAuthorizationCache;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.ErrorDetail;
import com.bank.docgen.sharedkernel.api.ErrorEnvelope;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
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
import java.time.Instant;
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
    private final AdGroupResolver adGroupResolver;
    private final TemplateAdGroupAuthorizationCache authorizationCache;
    private final ObjectMapper objectMapper;
    private final TraceIdProvider traceIdProvider;
    private final MessageResolver messageResolver;

    public ApiCredentialAuthenticationFilter(
            ApiCredentialRepository apiCredentialRepository,
            ApiPolicyRepository apiPolicyRepository,
            TemplateRepository templateRepository,
            PasswordHashService passwordHashService,
            AdGroupResolver adGroupResolver,
            TemplateAdGroupAuthorizationCache authorizationCache,
            ObjectMapper objectMapper,
            TraceIdProvider traceIdProvider,
            MessageResolver messageResolver
    ) {
        this.apiCredentialRepository = apiCredentialRepository;
        this.apiPolicyRepository = apiPolicyRepository;
        this.templateRepository = templateRepository;
        this.passwordHashService = passwordHashService;
        this.adGroupResolver = adGroupResolver;
        this.authorizationCache = authorizationCache;
        this.objectMapper = objectMapper;
        this.traceIdProvider = traceIdProvider;
        this.messageResolver = messageResolver;
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
            writeErrorResponse(request, response, ex);
        }
    }

    private void writeErrorResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            RuntimeAuthenticationException ex
    ) throws IOException {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        ErrorDetail error = new ErrorDetail(
                ex.code(),
                categoryFor(ex.code()),
                messageResolver.resolve(ex.messageKey()),
                ex.messageKey(),
                false,
                null
        );
        response.setStatus(statusFor(ex.code()));
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), new ErrorEnvelope(Metadata.minimal(auditId, traceId), error));
    }

    private static int statusFor(String code) {
        if (ApiErrorCodes.ACCESS_DENIED.equals(code)) {
            return HttpServletResponse.SC_FORBIDDEN;
        }
        return HttpServletResponse.SC_UNAUTHORIZED;
    }

    private static String categoryFor(String code) {
        if (ApiErrorCodes.INVALID_CREDENTIALS.equals(code)
                || ApiErrorCodes.API_CREDENTIAL_EXPIRED.equals(code)
                || ApiErrorCodes.API_CREDENTIAL_REVOKED.equals(code)) {
            return ApiErrorCategories.AUTHENTICATION;
        }
        if (ApiErrorCodes.REQUEST_BODY_INVALID.equals(code)) {
            return ApiErrorCategories.VALIDATION;
        }
        if (ApiErrorCodes.ACCESS_DENIED.equals(code)) {
            return ApiErrorCategories.AUTHORIZATION;
        }
        return ApiErrorCategories.RUNTIME;
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
        Instant now = Instant.now();
        if (!matchesCurrentOrGraceSecret(secret, credential, now)) {
            throw new RuntimeAuthenticationException(
                    ApiErrorCodes.INVALID_CREDENTIALS,
                    "api.error.runtime.invalidCredentials"
            );
        }
        ApiCredentialStatus effectiveStatus = ApiCredentialLifecycleSupport.resolveEffectiveStatus(
                credential,
                now
        );
        if (effectiveStatus == ApiCredentialStatus.REVOKED) {
            throw new RuntimeAuthenticationException(
                    ApiErrorCodes.API_CREDENTIAL_REVOKED,
                    "api.error.runtime.apiCredentialRevoked"
            );
        }
        if (effectiveStatus == ApiCredentialStatus.EXPIRED) {
            throw new RuntimeAuthenticationException(
                    ApiErrorCodes.API_CREDENTIAL_EXPIRED,
                    "api.error.runtime.apiCredentialExpired"
            );
        }
        if (!ApiCredentialLifecycleSupport.isActiveCredential(credential, now)) {
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
        List<String> allowedGroups = authorizationCache.getAllowedGroups(template.getId())
                .orElseGet(() -> {
                    List<String> groups = readGroups(policy.getAllowedAdGroupsJson());
                    authorizationCache.rememberAllowedGroups(template.getId(), groups);
                    return groups;
                });
        List<String> callerGroups = adGroupResolver.resolveGroups(accessAccount);
        if (!AdGroupAuthorization.isAuthorized(callerGroups, allowedGroups)) {
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
                callerGroups
        );
    }

    /**
     * FOS-W10-1: accept current secret always; accept previous secret only within 28-day grace.
     */
    private boolean matchesCurrentOrGraceSecret(
            String secret,
            ApiCredentialEntity credential,
            Instant now
    ) {
        if (passwordHashService.matches(secret, credential.getSecretHash())) {
            return true;
        }
        if (!ApiCredentialLifecycleSupport.isPreviousSecretWithinGrace(credential, now)) {
            return false;
        }
        return passwordHashService.matches(secret, credential.getPreviousSecretHash());
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
            throw new RuntimeAuthenticationException(
                    ApiErrorCodes.ACCESS_DENIED,
                    "api.error.runtime.policyNotConfigured"
            );
        }
    }
}
