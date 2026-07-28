package com.bank.docgen.runtime.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.runtime.api.GenerateRequestBody;
import com.bank.docgen.sharedkernel.api.FieldError;
import com.bank.docgen.template.service.TemplateValidationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

/**
 * Package-private generate-request validation / serialization for RuntimeGenerationService.
 */
final class RuntimeGenerateRequestSupport {

    private final ObjectMapper objectMapper;

    RuntimeGenerateRequestSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    void validateGenerateRequest(GenerateRequestBody request, ApiPolicyEntity policy) {
        List<FieldError> shapeErrors = new ArrayList<>();
        if (request.output() == null) {
            shapeErrors.add(new FieldError("output", "REQUIRED", "output"));
        }
        if (request.variables() == null) {
            shapeErrors.add(new FieldError("variables", "REQUIRED", "variables"));
        }
        if (request.requestId() == null || request.requestId().isBlank()) {
            shapeErrors.add(new FieldError("requestId", "REQUIRED", "requestId"));
        }
        if (request.idempotencyKey() == null || request.idempotencyKey().isBlank()) {
            shapeErrors.add(new FieldError("idempotencyKey", "REQUIRED", "idempotencyKey"));
        }
        if (!shapeErrors.isEmpty()) {
            throw new GenerateRequestShapeException(shapeErrors);
        }
        String format = request.output().format();
        if (!"DOCX".equalsIgnoreCase(format) && !"PDF".equalsIgnoreCase(format)) {
            throw new TemplateValidationException("api.error.runtime.outputFormatUnsupported");
        }
        if (readStringList(policy.getOutputFormatsJson()).stream().noneMatch(item -> item.equalsIgnoreCase(format))) {
            throw new TemplateValidationException("api.error.runtime.outputFormatUnsupported");
        }
        OutputModePolicyValidator.validateSyncGenerate(
                request.output().mode(),
                readStringList(policy.getOutputModesJson())
        );
    }

    String contentTypeForFormat(String outputFormat) {
        if ("PDF".equalsIgnoreCase(outputFormat)) {
            return "application/pdf";
        }
        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    }

    List<String> readStringList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new TemplateValidationException("api.error.runtime.outputFormatUnsupported");
        }
    }

    String writeRequest(GenerateRequestBody request, String releaseVersion) {
        try {
            java.util.LinkedHashMap<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("releaseVersion", releaseVersion);
            payload.put("variables", request.variables());
            payload.put("output", request.output());
            payload.put("encryption", request.encryption());
            return objectMapper.writeValueAsString(payload);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            return releaseVersion;
        }
    }
}
