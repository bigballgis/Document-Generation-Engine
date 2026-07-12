package com.bank.docgen.rendering;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalog;
import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalogEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class DocxAssembler {

    private static final Pattern ANCHOR_PATTERN = Pattern.compile("\\{\\{anchor:([A-Za-z0-9_.-]+)}}");
    private static final String MASTER_FILLER_MARKER =
            "Section-level anchor in the master layout container";
    private static final String DEFAULT_STYLE_CATALOG_RESOURCE = "authoring/default-master-style-catalog-v1.json";

    private final MasterStyleCatalog styleCatalog;
    private final StructuredContentDocxWriter structuredContentDocxWriter;
    private final OoxmlOutputValidator ooxmlOutputValidator;
    private final DocgenRenderingProperties renderingProperties;

    @Autowired
    public DocxAssembler(
            ObjectMapper objectMapper,
            StructuredContentImageResolver imageResolver,
            OoxmlOutputValidator ooxmlOutputValidator,
            DocgenRenderingProperties renderingProperties
    ) {
        this.styleCatalog = loadDefaultStyleCatalog(objectMapper);
        this.structuredContentDocxWriter = new StructuredContentDocxWriter(
                objectMapper,
                styleCatalog,
                imageResolver
        );
        this.ooxmlOutputValidator = ooxmlOutputValidator;
        this.renderingProperties = renderingProperties;
    }

    /**
     * Test-only constructor: OOXML validation enabled with a default validator.
     */
    DocxAssembler(ObjectMapper objectMapper, StructuredContentImageResolver imageResolver) {
        this(objectMapper, imageResolver, new OoxmlOutputValidator(), validationEnabledProperties());
    }

    private static DocgenRenderingProperties validationEnabledProperties() {
        DocgenRenderingProperties properties = new DocgenRenderingProperties();
        properties.setOoxmlValidationEnabled(true);
        return properties;
    }

    public byte[] assemble(InputStream masterDocx, Map<String, String> anchorContent) {
        try (XWPFDocument document = new XWPFDocument(masterDocx); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            removeMasterLayoutFillerParagraphs(document);
            DocxPlainAnchorParagraphSupport.replaceAnchorsInDocumentBody(document, anchorContent, ANCHOR_PATTERN);
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        DocxPlainAnchorParagraphSupport.replaceInParagraphs(cell.getParagraphs(), anchorContent, ANCHOR_PATTERN);
                    }
                }
            }
            document.getHeaderList().forEach(header -> DocxPlainAnchorParagraphSupport.replaceInParagraphs(header.getParagraphs(), anchorContent, ANCHOR_PATTERN));
            document.getFooterList().forEach(footer -> DocxPlainAnchorParagraphSupport.replaceInParagraphs(footer.getParagraphs(), anchorContent, ANCHOR_PATTERN));
            DocxWordCompatibilitySupport.ensureWordCompatiblePackage(document);
            document.write(output);
            return validatedBytes(output.toByteArray());
        } catch (IOException ex) {
            throw new DocxAssemblyException(ex);
        }
    }

    public String renderStructuredContent(String structuredContentJson, Map<String, Object> variables) {
        return renderStructuredContent(structuredContentJson, variables, Map.of());
    }

    public String renderStructuredContent(
            String structuredContentJson,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures
    ) {
        return structuredContentDocxWriter.renderPlainTextProjection(
                structuredContentJson,
                variables,
                pinnedModuleStructures
        );
    }

    public Map<String, String> buildAnchorReplacements(
            Map<String, String> bindingJsonByAnchor,
            Map<String, Object> variables
    ) {
        return buildAnchorReplacements(bindingJsonByAnchor, variables, Map.of());
    }

    public Map<String, String> buildAnchorReplacements(
            Map<String, String> bindingJsonByAnchor,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures
    ) {
        return bindingJsonByAnchor.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> renderStructuredContent(entry.getValue(), variables, pinnedModuleStructures)
                ));
    }

    private void removeMasterLayoutFillerParagraphs(XWPFDocument document) {
        List<IBodyElement> bodyElements = document.getBodyElements();
        for (int index = bodyElements.size() - 1; index >= 0; index--) {
            IBodyElement element = bodyElements.get(index);
            if (element instanceof XWPFParagraph paragraph) {
                String text = paragraph.getText();
                if (text != null && text.contains(MASTER_FILLER_MARKER)) {
                    document.removeBodyElement(index);
                }
            }
        }
    }

    public byte[] assembleFromBytes(byte[] masterBytes, Map<String, String> anchorContent) {
        return assemble(new ByteArrayInputStream(masterBytes), anchorContent);
    }

    public byte[] assembleStructured(
            InputStream masterDocx,
            Map<String, String> bindingJsonByAnchor,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures
    ) {
        try (XWPFDocument document = new XWPFDocument(masterDocx); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            removeMasterLayoutFillerParagraphs(document);
            DocxStructuredAnchorSupport.replaceInDocumentBody(
                    document,
                    bindingJsonByAnchor,
                    variables,
                    pinnedModuleStructures,
                    structuredContentDocxWriter,
                    ANCHOR_PATTERN
            );
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        DocxStructuredAnchorSupport.replaceInParagraphs(
                                document,
                                cell,
                                cell.getParagraphs(),
                                bindingJsonByAnchor,
                                variables,
                                pinnedModuleStructures,
                                structuredContentDocxWriter,
                                ANCHOR_PATTERN
                        );
                    }
                }
            }
            for (XWPFHeader header : document.getHeaderList()) {
                DocxStructuredAnchorSupport.replaceInParagraphs(
                        document,
                        header,
                        header.getParagraphs(),
                        bindingJsonByAnchor,
                        variables,
                        pinnedModuleStructures,
                        structuredContentDocxWriter,
                        ANCHOR_PATTERN
                );
            }
            for (var footer : document.getFooterList()) {
                DocxStructuredAnchorSupport.replaceInParagraphs(
                        document,
                        footer,
                        footer.getParagraphs(),
                        bindingJsonByAnchor,
                        variables,
                        pinnedModuleStructures,
                        structuredContentDocxWriter,
                        ANCHOR_PATTERN
                );
            }
            DocxWordCompatibilitySupport.ensureWordCompatiblePackage(document);
            document.write(output);
            return validatedBytes(output.toByteArray());
        } catch (IOException ex) {
            throw new DocxAssemblyException(ex);
        }
    }

    public byte[] assembleStructuredFromBytes(
            byte[] masterBytes,
            Map<String, String> bindingJsonByAnchor,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures
    ) {
        return assembleStructured(
                new ByteArrayInputStream(masterBytes),
                bindingJsonByAnchor,
                variables,
                pinnedModuleStructures
        );
    }

    private byte[] validatedBytes(byte[] assembledBytes) {
        if (renderingProperties.isOoxmlValidationEnabled()) {
            ooxmlOutputValidator.validate(assembledBytes);
        }
        return assembledBytes;
    }

    private MasterStyleCatalog loadDefaultStyleCatalog(ObjectMapper mapper) {
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
