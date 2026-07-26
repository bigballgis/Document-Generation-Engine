package com.bank.docgen.runtime.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.runtime.api.AdGroupAuthorizationSummaryView;
import com.bank.docgen.runtime.api.ApiPolicySummaryView;
import com.bank.docgen.runtime.api.BatchLimitsView;
import com.bank.docgen.runtime.api.CallableVersionView;
import com.bank.docgen.runtime.api.DefaultRouteSummaryView;
import com.bank.docgen.runtime.api.EncryptionCapabilitiesView;
import com.bank.docgen.runtime.api.ErrorCodeSummaryView;
import com.bank.docgen.runtime.api.RuntimeCredentialSummaryView;
import com.bank.docgen.runtime.domain.ContractViewAudience;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.bank.docgen.template.service.TemplateValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

/**
 * Package-private contract view builders (paths, versions, policy summary, error codes).
 */
final class ContractAssemblyViewSupport {

    private final MessageResolver messageResolver;
    private final ObjectMapper objectMapper;
    private final TemplateVersionRepository templateVersionRepository;
    private final VariableSchemaRepository variableSchemaRepository;

    ContractAssemblyViewSupport(
            MessageResolver messageResolver,
            ObjectMapper objectMapper,
            TemplateVersionRepository templateVersionRepository,
            VariableSchemaRepository variableSchemaRepository
    ) {
        this.messageResolver = messageResolver;
        this.objectMapper = objectMapper;
        this.templateVersionRepository = templateVersionRepository;
        this.variableSchemaRepository = variableSchemaRepository;
    }

    List<String> runtimePaths(TemplateEntity template, String environment) {
        String basePath = "/api/" + environment + "/v1/templates/" + template.getExternalId();
        return List.of(
                basePath + "/contract",
                basePath + "/versions",
                basePath + "/default/generate",
                basePath + "/versions/{releaseVersion}/generate",
                basePath + "/default/batch-generate",
                basePath + "/versions/{releaseVersion}/batch-generate",
                basePath + "/tasks/{taskId}",
                basePath + "/tasks/{taskId}/cancel",
                "/api/" + environment + "/v1/documents/{documentId}/download"
        );
    }

    List<CallableVersionView> buildCallableVersions(
            TemplateEntity template,
            String environment,
            boolean includeVariables
    ) {
        if (template.getLifecycleStatus() != TemplateLifecycleStatus.PUBLISHED) {
            return List.of();
        }
        String base = "/api/" + environment + "/v1/templates/" + template.getExternalId() + "/versions/";
        return templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(template.getId()).stream()
                .filter(version -> version.getLifecycleStatus() == TemplateLifecycleStatus.PUBLISHED
                        && version.getReleaseVersion() != null
                        && !version.getReleaseVersion().isBlank())
                .map(version -> toCallableVersion(version, base, includeVariables))
                .toList();
    }

    private CallableVersionView toCallableVersion(
            TemplateVersionEntity version,
            String base,
            boolean includeVariables
    ) {
        return new CallableVersionView(
                version.getReleaseVersion(),
                base + version.getReleaseVersion() + "/generate",
                Boolean.FALSE,
                null,
                includeVariables
                        ? ContractVariableSchemaProjector.project(
                                variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(version.getId())
                        )
                        : null
        );
    }

    DefaultRouteSummaryView buildDefaultRoute(
            TemplateEntity template,
            ApiPolicyEntity policy,
            String environment,
            ContractViewAudience audience
    ) {
        String base = "/api/" + environment + "/v1/templates/" + template.getExternalId();
        String targetVersion = policy.getDefaultRouteReleaseVersion();
        String explicitUrl = targetVersion == null ? null
                : base + "/versions/" + targetVersion + "/generate";
        boolean includeDetail = audience == ContractViewAudience.ADMIN;
        return new DefaultRouteSummaryView(
                base + "/default/generate",
                targetVersion,
                template.getLifecycleStatus().name(),
                includeDetail ? policy.getUpdatedAt() : null,
                includeDetail ? policy.getUpdatedBy() : null,
                explicitUrl
        );
    }

    ApiPolicySummaryView toPolicySummary(
            ApiPolicyEntity policy,
            RuntimeCredentialSummaryView credentialSummary,
            ContractViewAudience audience
    ) {
        List<String> allowedGroups = readStringList(policy.getAllowedAdGroupsJson());
        boolean includeDetail = audience == ContractViewAudience.ADMIN;
        AdGroupAuthorizationSummaryView adGroupSummary = new AdGroupAuthorizationSummaryView(
                !allowedGroups.isEmpty(),
                300,
                includeDetail
                        ? messageResolver.resolve("api.contract.adGroupsConfigured", allowedGroups.size())
                        : null,
                includeDetail
                        ? messageResolver.resolve("api.contract.adGroupFailClosedEnforced")
                        : null
        );
        return new ApiPolicySummaryView(
                policy.getPolicyVersion(),
                includeDetail ? policy.getUpdatedAt() : null,
                includeDetail ? policy.getUpdatedBy() : null,
                readStringList(policy.getOutputFormatsJson()),
                readStringList(policy.getOutputModesJson()),
                new BatchLimitsView(policy.getBatchSyncMaxItems(), policy.getBatchAsyncMaxItems()),
                new EncryptionCapabilitiesView(
                        policy.isDocxEncryptionEnabled(),
                        policy.isPdfEncryptionEnabled(),
                        List.of("ALLOW_PRINT", "ALLOW_COPY", "ALLOW_EDIT", "ALLOW_ANNOTATE", "ALLOW_FORM_FILL")
                ),
                adGroupSummary,
                toCredentialSummary(credentialSummary, includeDetail)
        );
    }

    /**
     * FOS-W9-3: catalogue retryable flags + categories match exception advice / rate-limit filter.
     * Includes RATE_LIMIT_EXCEEDED / GENERATION_TIMEOUT / GENERATION_SERVICE_UNAVAILABLE.
     * REQUEST_BODY_INVALID uses VALIDATION (C18), not RUNTIME.
     */
    List<ErrorCodeSummaryView> standardErrorCodes() {
        return List.of(
                errorCode(ApiErrorCategories.AUTHENTICATION, ApiErrorCodes.INVALID_CREDENTIALS,
                        "api.error.runtime.invalidCredentials", false),
                errorCode(ApiErrorCategories.AUTHORIZATION, ApiErrorCodes.ACCESS_DENIED,
                        "api.error.authorization.accessDenied", false),
                errorCode(ApiErrorCategories.VALIDATION, ApiErrorCodes.REQUEST_BODY_INVALID,
                        "api.error.validation.requestBodyInvalid", false),
                errorCode(ApiErrorCategories.RUNTIME, ApiErrorCodes.RATE_LIMIT_EXCEEDED,
                        "api.error.runtime.rateLimitExceeded", true),
                errorCode(ApiErrorCategories.RUNTIME, ApiErrorCodes.RATE_LIMIT_BACKEND_UNAVAILABLE,
                        "api.error.runtime.rateLimitBackendUnavailable", true),
                errorCode(ApiErrorCategories.GENERATION, ApiErrorCodes.GENERATION_TIMEOUT,
                        "api.error.generation.generationTimeout", true),
                errorCode(ApiErrorCategories.GENERATION, ApiErrorCodes.GENERATION_SERVICE_UNAVAILABLE,
                        "api.error.generation.generationServiceUnavailable", true),
                errorCode(ApiErrorCategories.RUNTIME, ApiErrorCodes.BATCH_LIMIT_EXCEEDED,
                        "api.error.runtime.batchLimitExceeded", false),
                errorCode(ApiErrorCategories.RUNTIME, ApiErrorCodes.ITEM_ID_DUPLICATED,
                        "api.error.runtime.itemIdDuplicated", false),
                errorCode(ApiErrorCategories.BATCH, ApiErrorCodes.ORIGINAL_BATCH_NOT_FOUND,
                        "api.error.batch.originalBatchNotFound", false),
                errorCode(ApiErrorCategories.RUNTIME, ApiErrorCodes.ASYNC_TASK_NOT_FOUND,
                        "api.error.runtime.asyncTaskNotFound", false),
                errorCode(ApiErrorCategories.ENCRYPTION, ApiErrorCodes.ENCRYPTION_PARAMETER_INVALID,
                        "api.error.encryption.encryptionParameterInvalid", false),
                errorCode(ApiErrorCategories.ENCRYPTION, ApiErrorCodes.ENCRYPTION_NOT_ALLOWED,
                        "api.error.encryption.encryptionNotAllowed", false),
                errorCode(ApiErrorCategories.GENERATION, ApiErrorCodes.PDF_ARCHIVAL_ENCRYPTION_MUTEX,
                        "api.error.generation.pdfArchivalEncryptionMutex", false),
                errorCode(ApiErrorCategories.RUNTIME, ApiErrorCodes.DOCUMENT_NOT_FOUND,
                        "api.error.runtime.documentNotFound", false),
                errorCode(ApiErrorCategories.RUNTIME, ApiErrorCodes.DOWNLOAD_URL_EXPIRED,
                        "api.error.runtime.downloadUrlExpired", false)
        );
    }

    private RuntimeCredentialSummaryView toCredentialSummary(
            RuntimeCredentialSummaryView credentialSummary,
            boolean includeDetail
    ) {
        if (credentialSummary == null) {
            return null;
        }
        if (includeDetail) {
            return credentialSummary;
        }
        return new RuntimeCredentialSummaryView(
                null,
                credentialSummary.status(),
                null,
                credentialSummary.expiresAt()
        );
    }

    private ErrorCodeSummaryView errorCode(
            String category,
            String code,
            String messageKey,
            boolean retryable
    ) {
        return new ErrorCodeSummaryView(
                category,
                code,
                messageKey,
                retryable,
                messageResolver.resolve(messageKey)
        );
    }

    private List<String> readStringList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new TemplateValidationException("api.error.runtime.outputFormatUnsupported");
        }
    }
}
