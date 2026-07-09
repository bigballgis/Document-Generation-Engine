package com.bank.docgen.rendering;

import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.IBody;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;

/**
 * Writes P18 structured content trees into Word body elements with formatting fidelity.
 */
public class StructuredContentDocxWriter {

    private static final int MAX_NUMBERING_LEVELS = 4;

    private final ObjectMapper objectMapper;
    private final MasterStyleCatalog styleCatalog;
    private final StructuredContentImageResolver imageResolver;

    public StructuredContentDocxWriter(
            ObjectMapper objectMapper,
            MasterStyleCatalog styleCatalog,
            StructuredContentImageResolver imageResolver
    ) {
        this.objectMapper = objectMapper;
        this.styleCatalog = styleCatalog;
        this.imageResolver = imageResolver;
    }

    public void replaceAnchorParagraph(
            XWPFDocument document,
            int paragraphIndex,
            String structuredContentJson,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures
    ) {
        replaceStructuredAnchorInParagraph(
                document,
                document,
                document.getParagraphs().get(paragraphIndex),
                structuredContentJson,
                variables,
                pinnedModuleStructures
        );
    }

    /**
     * Replaces a single anchor paragraph inside any Word body (document body, table cell, header, footer).
     */
    public void replaceStructuredAnchorInParagraph(
            XWPFDocument document,
            IBody body,
            XWPFParagraph anchorParagraph,
            String structuredContentJson,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures
    ) {
        try {
            JsonNode root = objectMapper.readTree(structuredContentJson);
            JsonNode nodes = resolveRootNodes(root);
            if (!nodes.isArray() || nodes.isEmpty()) {
                clearParagraph(anchorParagraph);
                return;
            }
            DocxMasterStyleRegistry.ensureCatalogStyles(document, styleCatalog);
            StructuredContentDocxWriteSession session = new StructuredContentDocxWriteSession(
                    objectMapper,
                    styleCatalog,
                    imageResolver,
                    document,
                    body,
                    new DocxListNumberingSupport(document),
                    variables == null ? Map.of() : variables,
                    pinnedModuleStructures == null ? Map.of() : pinnedModuleStructures,
                    new int[MAX_NUMBERING_LEVELS]
            );
            clearParagraph(anchorParagraph);
            session.writeBlockNodes(nodes, anchorParagraph, true);
        } catch (IOException ex) {
            throw new DocxAssemblyException(ex);
        }
    }

    /**
     * F1-C2: plain-text projection derived from the authoritative DOCX writer output — never a second renderer.
     */
    public String renderPlainTextProjection(
            String structuredContentJson,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures
    ) {
        try (XWPFDocument document = new XWPFDocument()) {
            document.createParagraph();
            replaceAnchorParagraph(
                    document,
                    0,
                    structuredContentJson,
                    variables,
                    pinnedModuleStructures
            );
            return StructuredContentPlainTextProjection.fromDocument(document);
        } catch (IOException ex) {
            throw new DocxAssemblyException(ex);
        }
    }

    static JsonNode resolveRootNodes(JsonNode root) {
        JsonNode nodes = root.path("nodes");
        if (nodes.isArray()) {
            return nodes;
        }
        JsonNode blocks = root.path("blocks");
        if (blocks.isArray()) {
            return blocks;
        }
        return nodes;
    }

    static void clearParagraph(XWPFParagraph paragraph) {
        while (!paragraph.getRuns().isEmpty()) {
            paragraph.removeRun(0);
        }
        CTP ctp = paragraph.getCTP();
        while (ctp.sizeOfFldSimpleArray() > 0) {
            ctp.removeFldSimple(0);
        }
    }
}
