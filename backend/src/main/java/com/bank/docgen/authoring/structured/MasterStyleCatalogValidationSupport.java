package com.bank.docgen.authoring.structured;

import com.bank.docgen.sharedkernel.document.fidelity.FidelityWarningCode;
import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalog;
import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalogEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;

/**
 * Package-private catalog load + style/direct-format validation helpers.
 */
final class MasterStyleCatalogValidationSupport {

    private final ObjectMapper objectMapper;

    MasterStyleCatalogValidationSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    MasterStyleCatalog loadCatalogResource(String resourcePath) {
        try (InputStream inputStream = new ClassPathResource(resourcePath).getInputStream()) {
            JsonNode root = objectMapper.readTree(inputStream);
            Map<String, MasterStyleCatalogEntry> styles = new HashMap<>();
            JsonNode stylesNode = root.get("styles");
            if (stylesNode != null && stylesNode.isArray()) {
                for (JsonNode styleNode : stylesNode) {
                    String styleKey = styleNode.path("styleKey").asText("");
                    if (styleKey.isBlank()) {
                        continue;
                    }
                    List<String> applicable = new ArrayList<>();
                    JsonNode applicableNode = styleNode.get("applicableNodeTypes");
                    if (applicableNode != null && applicableNode.isArray()) {
                        applicableNode.forEach(node -> applicable.add(node.asText()));
                    }
                    styles.put(
                            styleKey,
                            new MasterStyleCatalogEntry(
                                    styleKey,
                                    Set.copyOf(applicable),
                                    styleNode.path("renderPurpose").asText("")
                            )
                    );
                }
            }
            return new MasterStyleCatalog(root.path("catalogVersion").asText("1.0"), styles);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load master style catalog: " + resourcePath, ex);
        }
    }

    void validateStyleReference(
            String styleKey,
            String contextNodeType,
            String location,
            MasterStyleCatalog catalog,
            List<StructuredContentFidelityIssue> blockers
    ) {
        String normalizedKey = styleKey == null ? "" : styleKey.trim();
        if (normalizedKey.isBlank()) {
            blockers.add(styleIssue(
                    FidelityWarningCode.MISSING_STYLE_REFERENCE,
                    MasterStyleCatalogService.MESSAGE_KEY_MISSING_STYLE,
                    location,
                    "Style reference is empty at " + location + ".",
                    "Reference an approved style from the master style catalog."
            ));
            return;
        }
        MasterStyleCatalogEntry entry = catalog.find(normalizedKey);
        if (entry == null) {
            blockers.add(styleIssue(
                    FidelityWarningCode.MISSING_STYLE_REFERENCE,
                    MasterStyleCatalogService.MESSAGE_KEY_MISSING_STYLE,
                    location,
                    "Style '" + sanitize(normalizedKey) + "' is not in the approved master style catalog.",
                    "Choose a style from the master catalog or update the letterhead."
            ));
            return;
        }
        if (contextNodeType != null && !contextNodeType.isBlank() && !entry.appliesToNodeType(contextNodeType)) {
            blockers.add(styleIssue(
                    FidelityWarningCode.INAPPLICABLE_STYLE,
                    MasterStyleCatalogService.MESSAGE_KEY_INAPPLICABLE_STYLE,
                    location,
                    "Style '" + sanitize(normalizedKey) + "' does not apply to node type '"
                            + sanitize(contextNodeType) + "'.",
                    "Select a style that applies to this node type."
            ));
        }
    }

    void validateDirectFormat(
            JsonNode directFormat,
            String location,
            List<StructuredContentFidelityIssue> blockers
    ) {
        if (directFormat == null || directFormat.isNull() || !directFormat.isObject()) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = directFormat.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            if (DirectFormatRules.GLOBAL_LAYOUT.contains(fieldName)) {
                blockers.add(styleIssue(
                        FidelityWarningCode.DIRECT_FORMAT_GLOBAL_LAYOUT,
                        MasterStyleCatalogService.MESSAGE_KEY_DIRECT_FORMAT_GLOBAL_LAYOUT,
                        location,
                        "Direct format field '" + sanitize(fieldName) + "' modifies global layout at " + location + ".",
                        "Remove global layout direct formatting; use master layout controls instead."
                ));
            } else if (!DirectFormatRules.WHITELIST.contains(fieldName)) {
                blockers.add(styleIssue(
                        FidelityWarningCode.DIRECT_FORMAT_OUT_OF_WHITELIST,
                        MasterStyleCatalogService.MESSAGE_KEY_DIRECT_FORMAT_OUT_OF_WHITELIST,
                        location,
                        "Direct format field '" + sanitize(fieldName)
                                + "' is outside the v1 whitelist at " + location + ".",
                        "Use only whitelisted direct format fields (font, size, color, spacing, indents)."
                ));
            }
        }
    }

    private StructuredContentFidelityIssue styleIssue(
            FidelityWarningCode code,
            String messageKey,
            String location,
            String detectionSummary,
            String suggestion
    ) {
        return new StructuredContentFidelityIssue(
                StructuredContentFidelitySeverity.BLOCKER,
                code,
                messageKey,
                location,
                detectionSummary,
                suggestion
        );
    }

    private String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "<empty>";
        }
        String trimmed = raw.trim();
        if (trimmed.length() > 64) {
            return trimmed.substring(0, 64) + "...";
        }
        return trimmed;
    }
}
