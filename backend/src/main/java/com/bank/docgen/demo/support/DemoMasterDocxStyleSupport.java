package com.bank.docgen.demo.support;

import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalog;
import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalogEntry;
import com.bank.docgen.rendering.DocxMasterStyleRegistry;
import com.bank.docgen.rendering.DocxWordCompatibilitySupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFStyles;

/**
 * Applies the shared P23 bank style manifest ({@code deploy/demo-shared/}) to build-time demo master DOCX assets.
 */
public final class DemoMasterDocxStyleSupport {

    public static final long MARGIN_BASELINE_TWIPS = 1440L;

    public static final List<String> REQUIRED_BANK_STYLE_KEYS = List.of(
            "Heading1",
            "Heading2",
            "Heading3",
            "ClauseBody",
            "DefinedTerm",
            "TableHeader",
            "ScheduleTitle",
            "SignatureBlock"
    );

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Path SHARED_MANIFEST = Path.of("..", "deploy", "demo-shared", "demo-bank-style-manifest.json");

    private DemoMasterDocxStyleSupport() {
    }

    public static Path sharedManifestPath() {
        return SHARED_MANIFEST;
    }

    public static MasterStyleCatalog loadSharedStyleCatalog() {
        return loadManifest().catalog();
    }

    public static void applySharedBankStyles(XWPFDocument document) {
        SharedStyleManifest manifest = loadManifest();
        DocxWordCompatibilitySupport.ensureWordCompatiblePackage(document);
        XWPFStyles styles = document.getStyles();
        if (styles == null) {
            return;
        }
        for (BankStyleDefinition definition : manifest.styleDefinitions()) {
            DocxMasterStyleRegistry.registerBankParagraphStyle(
                    styles,
                    definition.styleKey(),
                    definition.fontSizeHalfPoints(),
                    definition.fontFamily(),
                    definition.bold(),
                    definition.eastAsiaFontFamily(),
                    definition.spacingBeforeTwips(),
                    definition.spacingAfterTwips()
            );
        }
    }

    public static void assertSharedBankStylesPresent(byte[] docxBytes) throws IOException {
        String stylesXml = DemoMasterDocxAssertions.readStylesXml(docxBytes);
        for (String styleKey : REQUIRED_BANK_STYLE_KEYS) {
            if (!stylesXml.contains("w:styleId=\"" + styleKey + "\"")) {
                throw new AssertionError("Missing bank style in styles.xml: " + styleKey);
            }
        }
    }

    private static SharedStyleManifest loadManifest() {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(Files.readString(SHARED_MANIFEST));
            Map<String, MasterStyleCatalogEntry> catalogEntries = new LinkedHashMap<>();
            List<BankStyleDefinition> definitions = new ArrayList<>();
            String cjkFont = root.path("fonts").path("cjk").asText("");
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
                    catalogEntries.put(
                            styleKey,
                            new MasterStyleCatalogEntry(
                                    styleKey,
                                    Set.copyOf(applicable),
                                    styleNode.path("renderPurpose").asText("")
                            )
                    );
                    definitions.add(new BankStyleDefinition(
                            styleKey,
                            styleNode.path("fontFamily").asText("Calibri"),
                            styleNode.path("fontSizeHalfPoints").asInt(
                                    DocxMasterStyleRegistry.resolveDefaultFontSizeHalfPoints(styleKey)
                            ),
                            styleNode.path("bold").asBoolean(false),
                            cjkFont,
                            styleNode.path("spacingBeforeTwips").asInt(0),
                            styleNode.path("spacingAfterTwips").asInt(0)
                    ));
                }
            }
            MasterStyleCatalog catalog = new MasterStyleCatalog(
                    root.path("catalogVersion").asText("demo-bank-v1"),
                    catalogEntries
            );
            return new SharedStyleManifest(catalog, List.copyOf(definitions));
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load shared demo bank style manifest: " + SHARED_MANIFEST, ex);
        }
    }

    private record SharedStyleManifest(MasterStyleCatalog catalog, List<BankStyleDefinition> styleDefinitions) {
    }

    private record BankStyleDefinition(
            String styleKey,
            String fontFamily,
            int fontSizeHalfPoints,
            boolean bold,
            String eastAsiaFontFamily,
            int spacingBeforeTwips,
            int spacingAfterTwips
    ) {
    }
}
