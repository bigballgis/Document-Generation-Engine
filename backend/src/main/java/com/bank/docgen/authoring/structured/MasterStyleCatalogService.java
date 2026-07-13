package com.bank.docgen.authoring.structured;

import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
    private final MasterStyleCatalogValidationSupport validation;

    public MasterStyleCatalogService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.validation = new MasterStyleCatalogValidationSupport(objectMapper);
        this.defaultCatalog = validation.loadCatalogResource(DEFAULT_CATALOG_RESOURCE);
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

        validation.validateDirectFormat(node.get("directFormat"), location, blockers);
        if (node.has("styleRef") && !node.get("styleRef").isNull()) {
            validation.validateStyleReference(
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
}
