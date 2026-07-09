package com.bank.docgen.runtime.service;

import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.EncryptionSummaryView;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.sharedkernel.security.VariableHashSupport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AsyncBatchPayloadScrubber {

    private final ObjectMapper objectMapper;

    public AsyncBatchPayloadScrubber(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String scrub(BatchGenerateRequestBody request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", request.requestId());
        payload.put("idempotencyKey", request.idempotencyKey());
        payload.put("output", request.output());
        payload.put("encryption", sanitizeEncryption(request.encryption(), request.output().format()));
        payload.put("itemsCount", request.items().size());
        payload.put("items", request.items().stream().map(this::scrubItem).toList());
        return writeJson(payload);
    }

    private Map<String, Object> scrubItem(BatchGenerateRequestBody.BatchGenerateItemBody item) {
        Map<String, Object> scrubbed = new LinkedHashMap<>();
        scrubbed.put("itemId", item.itemId());
        scrubbed.put("variablesHash", VariableHashSupport.hashVariables(objectMapper, item.variables()));
        var output = item.output();
        scrubbed.put("output", output);
        scrubbed.put(
                "encryption",
                sanitizeEncryption(item.encryption(), output != null ? output.format() : null)
        );
        return scrubbed;
    }

    private EncryptionSummaryView sanitizeEncryption(EncryptionOptionsView encryption, String outputFormat) {
        String format = outputFormat == null ? "DOCX" : outputFormat;
        return EncryptionSummaryView.fromRequest(format, encryption);
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
