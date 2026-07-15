package com.bank.docgen.runtime.service;

import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.ContextView;
import com.bank.docgen.runtime.api.EncryptionSummaryView;
import com.bank.docgen.runtime.api.GenerateRequestBody;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.sharedkernel.security.VariableHashSupport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class InvocationParameterSanitizer {

    private final ObjectMapper objectMapper;

    public InvocationParameterSanitizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String sanitizeSingleRequest(GenerateRequestBody request, String resolvedReleaseVersion) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("releaseVersion", resolvedReleaseVersion);
        // CE-G06 / ADR-0057: retention-scoped exception — sanitized variables may persist for
        // caller reconciliation and server-internal regenerate replay. Passwords remain stripped;
        // management APIs / audit / logs never return this payload (HIST C6 / ADR-0020 display ban).
        payload.put("variables", request.variables() == null ? Map.of() : request.variables());
        payload.put("variablesHash", VariableHashSupport.hashVariables(objectMapper, request.variables()));
        payload.put("output", request.output());
        payload.put("encryption", sanitizeEncryption(request.encryption(), request.output().format()));
        putContextSummary(payload, request.context());
        return writeJson(payload);
    }

    public String sanitizeBatchRequest(BatchGenerateRequestBody request, String resolvedReleaseVersion) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("releaseVersion", resolvedReleaseVersion);
        payload.put("output", request.output());
        payload.put("encryption", sanitizeEncryption(request.encryption(), request.output().format()));
        payload.put("itemsCount", request.items().size());
        payload.put("itemsHash", hashBatchItems(request));
        payload.put("items", request.items().stream().map(this::sanitizeBatchItemSummary).toList());
        // CE-G06 / ADR-0057: single-item async ASYNC_TASK rows can regenerate from top-level variables.
        if (request.items().size() == 1) {
            Map<String, Object> variables = request.items().get(0).variables();
            payload.put("variables", variables == null ? Map.of() : variables);
            payload.put("variablesHash", VariableHashSupport.hashVariables(objectMapper, variables));
        }
        putContextSummary(payload, request.context());
        if (request.originalBatchId() != null && !request.originalBatchId().isBlank()) {
            payload.put("originalBatchId", request.originalBatchId());
        }
        return writeJson(payload);
    }

    public String sanitizeBatchItem(
            BatchGenerateRequestBody.BatchGenerateItemBody item,
            BatchGenerateRequestBody request,
            String resolvedReleaseVersion
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("releaseVersion", resolvedReleaseVersion);
        payload.put("itemId", item.itemId());
        Map<String, Object> variables = item.variables() == null ? Map.of() : item.variables();
        payload.put("variables", variables);
        payload.put("variablesHash", VariableHashSupport.hashVariables(objectMapper, item.variables()));
        var output = item.output() != null ? item.output() : request.output();
        payload.put("output", output);
        var encryption = item.encryption() != null ? item.encryption() : request.encryption();
        payload.put("encryption", sanitizeEncryption(encryption, output.format()));
        return writeJson(payload);
    }

    public String stripEncryptionPasswords(String parametersJson) {
        try {
            JsonNode root = objectMapper.readTree(parametersJson);
            if (root instanceof ObjectNode objectNode) {
                sanitizeEncryptionNode(objectNode.get("encryption"), objectNode);
                sanitizeItemsArray(objectNode.get("items"));
            }
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException ex) {
            return parametersJson;
        }
    }

    private Map<String, Object> sanitizeBatchItemSummary(BatchGenerateRequestBody.BatchGenerateItemBody item) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("itemId", item.itemId());
        payload.put("variablesHash", VariableHashSupport.hashVariables(objectMapper, item.variables()));
        payload.put("output", item.output());
        payload.put("encryption", sanitizeEncryption(
                item.encryption(),
                item.output() != null ? item.output().format() : null
        ));
        return payload;
    }

    private String hashBatchItems(BatchGenerateRequestBody request) {
        List<String> itemHashes = request.items().stream()
                .map(item -> VariableHashSupport.hashVariables(objectMapper, item.variables()))
                .toList();
        return VariableHashSupport.hashPayload(objectMapper, itemHashes);
    }

    private EncryptionSummaryView sanitizeEncryption(EncryptionOptionsView encryption, String outputFormat) {
        String format = outputFormat == null ? "DOCX" : outputFormat;
        return EncryptionSummaryView.fromRequest(format, encryption);
    }

    private void putContextSummary(Map<String, Object> payload, ContextView context) {
        Map<String, String> summary = contextSummary(context);
        if (!summary.isEmpty()) {
            payload.put("contextSummary", summary);
        }
    }

    private Map<String, String> contextSummary(ContextView context) {
        Map<String, String> summary = new LinkedHashMap<>();
        if (context == null) {
            return summary;
        }
        putIfNonBlank(summary, "sourceSystem", context.sourceSystem());
        putIfNonBlank(summary, "channel", context.channel());
        putIfNonBlank(summary, "businessRequestId", context.businessRequestId());
        putIfNonBlank(summary, "upstreamTraceId", context.upstreamTraceId());
        putIfNonBlank(summary, "scenario", context.scenario());
        putIfNonBlank(summary, "locale", context.locale());
        return summary;
    }

    private static void putIfNonBlank(Map<String, String> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private void sanitizeItemsArray(JsonNode itemsNode) {
        if (!(itemsNode instanceof ArrayNode arrayNode)) {
            return;
        }
        for (JsonNode itemNode : arrayNode) {
            if (itemNode instanceof ObjectNode itemObject) {
                sanitizeEncryptionNode(itemObject.get("encryption"), itemObject);
            }
        }
    }

    private void sanitizeEncryptionNode(JsonNode encryptionNode, ObjectNode parent) {
        if (encryptionNode == null || encryptionNode.isNull()) {
            return;
        }
        boolean enabled = encryptionNode.path("enabled").asBoolean(false);
        String outputFormat = encryptionNode.path("outputFormat").asText("DOCX");
        List<String> permissions = objectMapper.convertValue(
                encryptionNode.path("permissions"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
        );
        if (permissions == null) {
            permissions = List.of();
        }
        boolean openPasswordProvided = encryptionNode.hasNonNull("openPassword")
                && !encryptionNode.get("openPassword").asText("").isBlank();
        boolean ownerPasswordProvided = encryptionNode.hasNonNull("ownerPassword")
                && !encryptionNode.get("ownerPassword").asText("").isBlank();
        parent.set(
                "encryption",
                objectMapper.valueToTree(new EncryptionSummaryView(
                        enabled,
                        outputFormat,
                        openPasswordProvided,
                        ownerPasswordProvided,
                        permissions
                ))
        );
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
