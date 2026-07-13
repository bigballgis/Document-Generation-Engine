package com.bank.docgen.runtime.service;

import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.BatchResultView;
import com.bank.docgen.template.service.TemplateValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Package-private JSON (de)serialization for batch generation requests and results.
 */
final class BatchGenerationJsonSupport {

    private final ObjectMapper objectMapper;

    BatchGenerationJsonSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    String writeBatchResult(BatchResultView batchResult) {
        try {
            return objectMapper.writeValueAsString(batchResult);
        } catch (JsonProcessingException ex) {
            throw new TemplateValidationException("api.error.rendering.generationFailed");
        }
    }

    BatchResultView readBatchResult(String json) {
        try {
            return objectMapper.readValue(json, BatchResultView.class);
        } catch (JsonProcessingException ex) {
            throw new TemplateValidationException("api.error.rendering.generationFailed");
        }
    }

    String writeRequest(BatchGenerateRequestBody request, String releaseVersion) {
        try {
            LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
            payload.put("releaseVersion", releaseVersion);
            payload.put("items", request.items());
            payload.put("output", request.output());
            payload.put("encryption", request.encryption());
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return releaseVersion;
        }
    }

    String writeRequestPayload(BatchGenerateRequestBody request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            throw new TemplateValidationException("api.error.validation.requestBodyInvalid");
        }
    }

    BatchGenerateRequestBody readRequestPayload(String json) {
        try {
            return objectMapper.readValue(json, BatchGenerateRequestBody.class);
        } catch (JsonProcessingException ex) {
            throw new TemplateValidationException("api.error.validation.requestBodyInvalid");
        }
    }

    List<String> readStringList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new TemplateValidationException("api.error.runtime.outputFormatUnsupported");
        }
    }
}
