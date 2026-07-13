package com.bank.docgen.rendering;

import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalog;
import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalogEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;

/**
 * Package-private loader for the default master style catalog used by DOCX assembly.
 */
final class DocxMasterStyleCatalogSupport {

    private static final String DEFAULT_STYLE_CATALOG_RESOURCE = "authoring/default-master-style-catalog-v1.json";

    private DocxMasterStyleCatalogSupport() {
    }

    static MasterStyleCatalog loadDefault(ObjectMapper mapper) {
        try (InputStream inputStream = new ClassPathResource(DEFAULT_STYLE_CATALOG_RESOURCE).getInputStream()) {
            JsonNode root = mapper.readTree(inputStream);
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
            throw new IllegalStateException("Unable to load master style catalog: " + DEFAULT_STYLE_CATALOG_RESOURCE, ex);
        }
    }
}
