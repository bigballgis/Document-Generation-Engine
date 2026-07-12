package com.bank.docgen.runtime.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.service.TemplateCallabilitySupport;
import com.bank.docgen.template.service.TemplateNotFoundException;
import com.bank.docgen.template.service.TemplateValidationException;
import java.util.HashSet;
import java.util.Set;

/**
 * Package-private policy / mode / request validation for batch generation.
 */
final class BatchGenerationPolicySupport {

    private final ApiPolicyRepository apiPolicyRepository;
    private final EncryptionParameterValidator encryptionParameterValidator;
    private final TemplateVersionRepository templateVersionRepository;
    private final BatchGenerationJsonSupport json;

    BatchGenerationPolicySupport(
            ApiPolicyRepository apiPolicyRepository,
            EncryptionParameterValidator encryptionParameterValidator,
            TemplateVersionRepository templateVersionRepository,
            BatchGenerationJsonSupport json
    ) {
        this.apiPolicyRepository = apiPolicyRepository;
        this.encryptionParameterValidator = encryptionParameterValidator;
        this.templateVersionRepository = templateVersionRepository;
        this.json = json;
    }

    ApiPolicyEntity requireBatchPolicy(TemplateEntity template, BatchGenerateRequestBody request) {
        String format = request.output().format();
        if (!"DOCX".equalsIgnoreCase(format) && !"PDF".equalsIgnoreCase(format)) {
            throw new TemplateValidationException("api.error.runtime.outputFormatUnsupported");
        }
        ApiPolicyEntity policy = apiPolicyRepository.findByTemplateId(template.getId())
                .orElseThrow(() -> new TemplateValidationException("api.error.runtime.policyNotConfigured"));
        if (json.readStringList(policy.getOutputFormatsJson()).stream().noneMatch(item -> item.equalsIgnoreCase(format))) {
            throw new TemplateValidationException("api.error.runtime.outputFormatUnsupported");
        }
        if (!policy.isBatchEnabled()) {
            throw new RuntimeBatchValidationException(
                    ApiErrorCodes.OUTPUT_MODE_NOT_ALLOWED,
                    "api.error.runtime.batchNotEnabled"
            );
        }
        return policy;
    }

    void requireSyncMode(BatchGenerateRequestBody request, ApiPolicyEntity policy) {
        OutputModePolicyValidator.validateBatchEndpoint(
                request.output().mode(),
                json.readStringList(policy.getOutputModesJson()),
                true
        );
    }

    void requireAsyncMode(BatchGenerateRequestBody request, ApiPolicyEntity policy) {
        OutputModePolicyValidator.validateBatchEndpoint(
                request.output().mode(),
                json.readStringList(policy.getOutputModesJson()),
                false
        );
    }

    void validateBatchRequest(BatchGenerateRequestBody request, ApiPolicyEntity policy) {
        encryptionParameterValidator.validate(request.encryption(), policy, request.output().format());
        for (BatchGenerateRequestBody.BatchGenerateItemBody item : request.items()) {
            EncryptionOptionsView itemEncryption = item.encryption() != null ? item.encryption() : request.encryption();
            String outputFormat = item.output() != null ? item.output().format() : request.output().format();
            encryptionParameterValidator.validate(itemEncryption, policy, outputFormat);
        }
        if (request.items().size() > policy.getMaxBatchSize()) {
            throw new RuntimeBatchValidationException(
                    ApiErrorCodes.BATCH_LIMIT_EXCEEDED,
                    "api.error.runtime.batchLimitExceeded"
            );
        }
        Set<String> itemIds = new HashSet<>();
        for (BatchGenerateRequestBody.BatchGenerateItemBody item : request.items()) {
            if (!itemIds.add(item.itemId())) {
                throw new RuntimeBatchValidationException(
                        ApiErrorCodes.ITEM_ID_DUPLICATED,
                        "api.error.runtime.itemIdDuplicated"
                );
            }
        }
    }

    String resolveVersion(TemplateEntity template, ApiPolicyEntity policy, String releaseVersion) {
        String resolvedVersion = releaseVersion != null ? releaseVersion : policy.getDefaultRouteReleaseVersion();
        if (resolvedVersion == null) {
            throw new TemplateValidationException("api.error.runtime.releaseVersionRequired");
        }
        TemplateVersionEntity version = templateVersionRepository
                .findByTemplateIdAndReleaseVersion(template.getId(), resolvedVersion)
                .orElseThrow(TemplateNotFoundException::new);
        TemplateCallabilitySupport.assertReleaseVersionCallable(template, version, resolvedVersion);
        return resolvedVersion;
    }
}
