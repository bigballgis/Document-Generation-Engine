package com.bank.docgen.runtime.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.runtime.api.AdGroupAuthorizationSummaryView;
import com.bank.docgen.runtime.api.ApiPolicySummaryView;
import com.bank.docgen.runtime.api.BatchLimitsView;
import com.bank.docgen.runtime.api.CallableVersionView;
import com.bank.docgen.runtime.api.ContractResultView;
import com.bank.docgen.runtime.api.DefaultRouteSummaryView;
import com.bank.docgen.runtime.api.EncryptionCapabilitiesView;
import com.bank.docgen.runtime.api.ErrorCodeSummaryView;
import com.bank.docgen.runtime.api.RuntimeCredentialSummaryView;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.service.TemplateValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ContractAssemblyService {

    private final MessageResolver messageResolver;
    private final ObjectMapper objectMapper;
    private final TemplateVersionRepository templateVersionRepository;

    public ContractAssemblyService(
            MessageResolver messageResolver,
            ObjectMapper objectMapper,
            TemplateVersionRepository templateVersionRepository
    ) {
        this.messageResolver = messageResolver;
        this.objectMapper = objectMapper;
        this.templateVersionRepository = templateVersionRepository;
    }

    public ContractResultView assemble(
            TemplateEntity template,
            ApiPolicyEntity policy,
            String environment,
            RuntimeCredentialSummaryView credentialSummary
    ) {
        return new ContractResultView(
                template.getExternalId(),
                runtimePaths(template, environment),
                buildDefaultRoute(template, policy, environment),
                toPolicySummary(policy, credentialSummary),
                buildCallableVersions(template, environment),
                List.of("GenerateRequest", "BatchGenerateRequest", "OutputOptions", "EncryptionOptions"),
                standardErrorCodes(),
                List.of("generate-sync-docx", "batch-generate-sync", "batch-generate-async")
        );
    }

    public List<CallableVersionView> listCallableVersions(TemplateEntity template, String environment) {
        return buildCallableVersions(template, environment);
    }

    private List<String> runtimePaths(TemplateEntity template, String environment) {
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

    private List<CallableVersionView> buildCallableVersions(TemplateEntity template, String environment) {
        if (template.getLifecycleStatus() != TemplateLifecycleStatus.PUBLISHED) {
            return List.of();
        }
        String base = "/api/" + environment + "/v1/templates/" + template.getExternalId() + "/versions/";
        return templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(template.getId()).stream()
                .filter(version -> version.getLifecycleStatus() == TemplateLifecycleStatus.PUBLISHED
                        && version.getReleaseVersion() != null
                        && !version.getReleaseVersion().isBlank())
                .map(version -> new CallableVersionView(
                        version.getReleaseVersion(),
                        base + version.getReleaseVersion() + "/generate"
                ))
                .toList();
    }

    private DefaultRouteSummaryView buildDefaultRoute(
            TemplateEntity template,
            ApiPolicyEntity policy,
            String environment
    ) {
        String base = "/api/" + environment + "/v1/templates/" + template.getExternalId();
        String targetVersion = policy.getDefaultRouteReleaseVersion();
        String explicitUrl = targetVersion == null ? null
                : base + "/versions/" + targetVersion + "/generate";
        return new DefaultRouteSummaryView(
                base + "/default/generate",
                targetVersion,
                template.getLifecycleStatus().name(),
                policy.getUpdatedAt(),
                policy.getUpdatedBy(),
                explicitUrl
        );
    }

    private ApiPolicySummaryView toPolicySummary(
            ApiPolicyEntity policy,
            RuntimeCredentialSummaryView credentialSummary
    ) {
        List<String> allowedGroups = readStringList(policy.getAllowedAdGroupsJson());
        return new ApiPolicySummaryView(
                policy.getPolicyVersion(),
                policy.getUpdatedAt(),
                policy.getUpdatedBy(),
                readStringList(policy.getOutputFormatsJson()),
                readStringList(policy.getOutputModesJson()),
                new BatchLimitsView(policy.getMaxBatchSize(), policy.getMaxBatchSize()),
                new EncryptionCapabilitiesView(
                        policy.isDocxEncryptionEnabled(),
                        policy.isPdfEncryptionEnabled(),
                        List.of("ALLOW_PRINT", "ALLOW_COPY", "ALLOW_EDIT", "ALLOW_ANNOTATE", "ALLOW_FORM_FILL")
                ),
                new AdGroupAuthorizationSummaryView(
                        !allowedGroups.isEmpty(),
                        300,
                        messageResolver.resolve("api.contract.adGroupsConfigured", allowedGroups.size()),
                        messageResolver.resolve("api.contract.adGroupFailClosedEnforced")
                ),
                credentialSummary
        );
    }

    private List<ErrorCodeSummaryView> standardErrorCodes() {
        return List.of(
                errorCode(ApiErrorCategories.AUTHENTICATION, ApiErrorCodes.INVALID_CREDENTIALS,
                        "api.error.runtime.invalidCredentials", false),
                errorCode(ApiErrorCategories.AUTHORIZATION, ApiErrorCodes.ACCESS_DENIED,
                        "api.error.authorization.accessDenied", false),
                errorCode(ApiErrorCategories.RUNTIME, ApiErrorCodes.REQUEST_BODY_INVALID,
                        "api.error.validation.requestBodyInvalid", false),
                errorCode(ApiErrorCategories.RUNTIME, ApiErrorCodes.BATCH_LIMIT_EXCEEDED,
                        "api.error.runtime.batchLimitExceeded", false),
                errorCode(ApiErrorCategories.RUNTIME, ApiErrorCodes.ITEM_ID_DUPLICATED,
                        "api.error.runtime.itemIdDuplicated", false),
                errorCode(ApiErrorCategories.RUNTIME, ApiErrorCodes.ASYNC_TASK_NOT_FOUND,
                        "api.error.runtime.asyncTaskNotFound", false),
                errorCode(ApiErrorCategories.ENCRYPTION, ApiErrorCodes.ENCRYPTION_PARAMETER_INVALID,
                        "api.error.encryption.encryptionParameterInvalid", false),
                errorCode(ApiErrorCategories.ENCRYPTION, ApiErrorCodes.ENCRYPTION_NOT_ALLOWED,
                        "api.error.encryption.encryptionNotAllowed", false),
                errorCode(ApiErrorCategories.RUNTIME, ApiErrorCodes.DOCUMENT_NOT_FOUND,
                        "api.error.runtime.documentNotFound", false),
                errorCode(ApiErrorCategories.RUNTIME, ApiErrorCodes.DOWNLOAD_URL_EXPIRED,
                        "api.error.runtime.downloadUrlExpired", false)
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
