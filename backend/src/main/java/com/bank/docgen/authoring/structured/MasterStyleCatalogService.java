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
import java.util.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Loads the approved master style catalog and validates style refs + limited direct format (P18-T03).
 */
@Service
public class MasterStyleCatalogService {

    public static final String MESSAGE_KEY_MISSING_STYLE = "generation.warning.fidelity.missingStyleReference";
    public static final String MESSAGE_KEY_INAPPLICABLE_STYLE = "generation.warning.fidelity.inapplicableStyle";
    public static final String MESSAGE_KEY_DIRECT_FORMAT_OUT_OF_WHITELIST =
            "generation.warning.fidelity.directFormatOutOfWhitelist";
    public static final String MESSAGE_KEY_DIRECT_FORMAT_GLOBAL_LAYOUT =
            "generation.warning.fidelity.directFormatGlobalLayout";

    private static final String DEFAULT_CATALOG_RESOURCE = "authoring/default-master-style-catalog-v1.json";

    private final ObjectMapper objectMapper;
    private final MasterStyleCatalog defaultCatalog;

    public MasterStyleCatalogService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.defaultCatalog = loadCatalogResource(DEFAULT_CATALOG_RESOURCE);
    }

    public MasterStyleCatalog loadForMaster(UUID masterId) {
        return defaultCatalog;
    }

    public StructuredContentValidationResult validate(String structuredContentJson, MasterStyleCatalog catalog) {
        MasterStyleCatalog resolvedCatalog = catalog == null ? defaultCatalog : catalog;
        List<StructuredContentFidelityIssue> blockers = new ArrayList<>();
        List<StructuredContentFidelityIssue> warnings = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(structuredContentJson);
            if (!root.isObject() || !root.get("nodes").isArray()) {
                return StructuredContentValidationResult.of(blockers, warnings);
            }
            JsonNode nodes = root.get("nodes");
            for (int index = 0; index < nodes.size(); index++) {
                walkNode(nodes.get(index), "nodes[" + index + "]", null, resolvedCatalog, blockers, warnings);
            }
        } catch (IOException ex) {
            return StructuredContentValidationResult.of(blockers, warnings);
        }
        return StructuredContentValidationResult.of(blockers, warnings);
    }

    private void walkNode(
            JsonNode node,
            String location,
            String parentNodeType,
            MasterStyleCatalog catalog,
            List<StructuredContentFidelityIssue> blockers,
            List<StructuredContentFidelityIssue> warnings
    ) {
        if (!node.isObject()) {
            return;
        }
        String rawType = node.path("type").asText("");
        StructuredContentNodeType nodeType = StructuredContentNodeType.fromJsonType(rawType).orElse(null);
        String contextNodeType = nodeType == StructuredContentNodeType.STYLE_REF ? parentNodeType : rawType;

        validateDirectFormat(node.get("directFormat"), location, blockers);
        if (node.has("styleRef") && !node.get("styleRef").isNull()) {
            validateStyleReference(
                    node.get("styleRef").asText(""),
                    contextNodeType,
                    location,
                    catalog,
                    blockers
            );
        }
        JsonNode children = node.get("children");
        if (children != null && children.isArray()) {
            for (int index = 0; index < children.size(); index++) {
                walkNode(children.get(index), location + ".children[" + index + "]", rawType, catalog, blockers, warnings);
            }
        }
    }

    private void validateStyleReference(
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
                    MESSAGE_KEY_MISSING_STYLE,
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
                    MESSAGE_KEY_MISSING_STYLE,
                    location,
                    "Style '" + sanitize(normalizedKey) + "' is not in the approved master style catalog.",
                    "Choose a style from the master catalog or update the letterhead."
            ));
            return;
        }
        if (contextNodeType != null && !contextNodeType.isBlank() && !entry.appliesToNodeType(contextNodeType)) {
            blockers.add(styleIssue(
                    FidelityWarningCode.INAPPLICABLE_STYLE,
                    MESSAGE_KEY_INAPPLICABLE_STYLE,
                    location,
                    "Style '" + sanitize(normalizedKey) + "' does not apply to node type '" + sanitize(contextNodeType) + "'.",
                    "Select a style that applies to this node type."
            ));
        }
    }

    private void validateDirectFormat(
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
                        MESSAGE_KEY_DIRECT_FORMAT_GLOBAL_LAYOUT,
                        location,
                        "Direct format field '" + sanitize(fieldName) + "' modifies global layout at " + location + ".",
                        "Remove global layout direct formatting; use master layout controls instead."
                ));
            } else if (!DirectFormatRules.WHITELIST.contains(fieldName)) {
                blockers.add(styleIssue(
                        FidelityWarningCode.DIRECT_FORMAT_OUT_OF_WHITELIST,
                        MESSAGE_KEY_DIRECT_FORMAT_OUT_OF_WHITELIST,
                        location,
                        "Direct format field '" + sanitize(fieldName) + "' is outside the v1 whitelist at " + location + ".",
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

    private MasterStyleCatalog loadCatalogResource(String resourcePath) {
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
