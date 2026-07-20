package com.bank.docgen.runtime.service;

import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.ContextView;
import com.bank.docgen.runtime.api.EncryptionSummaryView;
import com.bank.docgen.runtime.api.GenerateRequestBody;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.sharedkernel.security.VariableHashSupport;
import com.bank.docgen.template.domain.VariablePiiCategory;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.bank.docgen.template.port.CompositionInclusionAxes;
import com.bank.docgen.template.service.CompositionInclusionEvaluator;
import com.bank.docgen.template.service.CompositionInclusionRuleService;
import com.bank.docgen.template.port.CompositionInclusionUnsatisfiedException;
import com.bank.docgen.template.service.TemplateContentModuleReferenceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class InvocationParameterSanitizer {

    private final ObjectMapper objectMapper;
    private final TemplateVersionRepository templateVersionRepository;
    private final VariableSchemaRepository variableSchemaRepository;
    private final CompositionInclusionRuleService compositionInclusionRuleService;
    private final TemplateContentModuleReferenceService contentModuleReferenceService;

    public InvocationParameterSanitizer(
            ObjectMapper objectMapper,
            TemplateVersionRepository templateVersionRepository,
            VariableSchemaRepository variableSchemaRepository,
            CompositionInclusionRuleService compositionInclusionRuleService,
            TemplateContentModuleReferenceService contentModuleReferenceService
    ) {
        this.objectMapper = objectMapper;
        this.templateVersionRepository = templateVersionRepository;
        this.variableSchemaRepository = variableSchemaRepository;
        this.compositionInclusionRuleService = compositionInclusionRuleService;
        this.contentModuleReferenceService = contentModuleReferenceService;
    }

    public String sanitizeSingleRequest(
            GenerateRequestBody request,
            String resolvedReleaseVersion,
            UUID templateId
    ) {
        String json = sanitizeSingleRequest(
                request,
                resolvedReleaseVersion,
                loadPiiCategories(templateId, resolvedReleaseVersion)
        );
        return appendCompositionInclusionSummary(json, request, resolvedReleaseVersion, templateId);
    }

    /**
     * Package-visible overload for unit tests with an explicit schema category map.
     */
    String sanitizeSingleRequest(
            GenerateRequestBody request,
            String resolvedReleaseVersion,
            Map<String, VariablePiiCategory> piiCategories
    ) {
        Map<String, Object> originalVariables = request.variables() == null ? Map.of() : request.variables();
        InvocationRetentionVariableRedactor.Result redaction =
                InvocationRetentionVariableRedactor.redact(originalVariables, piiCategories);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("releaseVersion", resolvedReleaseVersion);
        // IBL-A5 / ADR-0057: retain cleartext only for piiCategory=NONE; strip passwords;
        // management APIs / audit / logs never return this payload (HIST C6 / ADR-0020 display ban).
        putRedactedVariables(payload, originalVariables, redaction);
        payload.put("output", request.output());
        payload.put("encryption", sanitizeEncryption(request.encryption(), request.output().format()));
        putContextSummary(payload, request.context());
        return writeJson(payload);
    }

    public String sanitizeBatchRequest(
            BatchGenerateRequestBody request,
            String resolvedReleaseVersion,
            UUID templateId
    ) {
        return sanitizeBatchRequest(
                request,
                resolvedReleaseVersion,
                loadPiiCategories(templateId, resolvedReleaseVersion)
        );
    }

    String sanitizeBatchRequest(
            BatchGenerateRequestBody request,
            String resolvedReleaseVersion,
            Map<String, VariablePiiCategory> piiCategories
    ) {
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
            Map<String, Object> original = variables == null ? Map.of() : variables;
            InvocationRetentionVariableRedactor.Result redaction =
                    InvocationRetentionVariableRedactor.redact(original, piiCategories);
            putRedactedVariables(payload, original, redaction);
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
            String resolvedReleaseVersion,
            UUID templateId
    ) {
        return sanitizeBatchItem(
                item,
                request,
                resolvedReleaseVersion,
                loadPiiCategories(templateId, resolvedReleaseVersion)
        );
    }

    String sanitizeBatchItem(
            BatchGenerateRequestBody.BatchGenerateItemBody item,
            BatchGenerateRequestBody request,
            String resolvedReleaseVersion,
            Map<String, VariablePiiCategory> piiCategories
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("releaseVersion", resolvedReleaseVersion);
        payload.put("itemId", item.itemId());
        Map<String, Object> original = item.variables() == null ? Map.of() : item.variables();
        InvocationRetentionVariableRedactor.Result redaction =
                InvocationRetentionVariableRedactor.redact(original, piiCategories);
        putRedactedVariables(payload, original, redaction);
        var output = item.output() != null ? item.output() : request.output();
        payload.put("output", output);
        var encryption = item.encryption() != null ? item.encryption() : request.encryption();
        payload.put("encryption", sanitizeEncryption(encryption, output.format()));
        // IBL-A6 / A6-C3: BATCH_ITEM rows must retain parent batch contextSummary (incl. locale).
        putContextSummary(payload, request.context());
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

    Map<String, VariablePiiCategory> loadPiiCategories(UUID templateId, String resolvedReleaseVersion) {
        if (templateId == null || resolvedReleaseVersion == null || resolvedReleaseVersion.isBlank()) {
            return Map.of();
        }
        return templateVersionRepository
                .findByTemplateIdAndReleaseVersion(templateId, resolvedReleaseVersion.trim())
                .map(version -> {
                    List<VariableSchemaEntity> schema = variableSchemaRepository
                            .findByTemplateVersionIdOrderByVariableKeyAsc(version.getId());
                    Map<String, VariablePiiCategory> categories = new LinkedHashMap<>();
                    for (VariableSchemaEntity field : schema) {
                        categories.put(field.getVariableKey(), field.getPiiCategory());
                    }
                    return Map.<String, VariablePiiCategory>copyOf(categories);
                })
                .orElse(Map.of());
    }

    private void putRedactedVariables(
            Map<String, Object> payload,
            Map<String, Object> originalVariables,
            InvocationRetentionVariableRedactor.Result redaction
    ) {
        payload.put("variables", redaction.variables());
        payload.put("variablesHash", VariableHashSupport.hashVariables(objectMapper, originalVariables));
        if (!redaction.redactedVariableKeys().isEmpty()) {
            payload.put("redactedVariableKeys", redaction.redactedVariableKeys());
            payload.put("redactedPiiCategories", redaction.redactedPiiCategories());
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
        putIfNonBlank(summary, "jurisdiction", context.jurisdiction());
        putIfNonBlank(summary, "product", context.product());
        putIfNonBlank(summary, "legalEntityCode", context.legalEntityCode());
        return summary;
    }

    private static void putIfNonBlank(Map<String, String> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private String appendCompositionInclusionSummary(
            String json,
            GenerateRequestBody request,
            String resolvedReleaseVersion,
            UUID templateId
    ) {
        if (templateId == null || resolvedReleaseVersion == null || resolvedReleaseVersion.isBlank()) {
            return json;
        }
        try {
            TemplateVersionEntity version = templateVersionRepository
                    .findByTemplateIdAndReleaseVersion(templateId, resolvedReleaseVersion.trim())
                    .orElse(null);
            if (version == null) {
                return json;
            }
            CompositionInclusionAxes axes = request.context() == null
                    ? CompositionInclusionAxes.empty()
                    : CompositionInclusionAxes.of(
                            request.context().jurisdiction(),
                            request.context().product(),
                            request.context().channel()
                    );
            var evaluation = CompositionInclusionEvaluator.evaluate(
                    new java.util.ArrayList<>(contentModuleReferenceService.listReferenceKeys(version.getId())),
                    compositionInclusionRuleService.loadRules(version),
                    axes
            );
            ObjectNode root = (ObjectNode) objectMapper.readTree(json);
            root.set("compositionInclusionSummary", objectMapper.valueToTree(evaluation.decisions()));
            return objectMapper.writeValueAsString(root);
        } catch (CompositionInclusionUnsatisfiedException | JsonProcessingException ignored) {
            // Success-path audit only; required-unsatisfied failures do not persist this summary.
            return json;
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
