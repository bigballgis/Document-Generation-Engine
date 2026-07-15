package com.bank.docgen.rendering;

import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.IBody;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;

/**
 * Mutable write state for {@link StructuredContentDocxWriter} anchor replacement.
 */
class StructuredContentDocxWriteSession {

    private final ObjectMapper objectMapper;
    private final MasterStyleCatalog styleCatalog;
    private final StructuredContentImageResolver imageResolver;
    private final XWPFDocument document;
    private final IBody body;
    private final DocxListNumberingSupport listSupport;
    private final Map<String, Object> variables;
    private final Map<String, String> pinnedModuleStructures;
    private final int[] numberingCounters;
    private final StructuredContentDocxStyleSupport styles;
    private final StructuredContentDocxTableSupport tableSupport;
    private final StructuredContentDocxInlineSupport inlineSupport;
    private final StructuredContentDocxCursorSupport cursor;
    private final StructuredContentDocxBlockDispatchSupport blockDispatch;
    private final StructuredContentDocxExpandSupport expandSupport;

    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    StructuredContentDocxWriteSession(
            ObjectMapper objectMapper,
            MasterStyleCatalog styleCatalog,
            StructuredContentImageResolver imageResolver,
            XWPFDocument document,
            IBody body,
            DocxListNumberingSupport listSupport,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures,
            int[] numberingCounters
    ) {
        this.objectMapper = objectMapper;
        this.styleCatalog = styleCatalog;
        this.imageResolver = imageResolver;
        this.document = document;
        this.body = body;
        this.listSupport = listSupport;
        this.variables = variables;
        this.pinnedModuleStructures = pinnedModuleStructures;
        this.numberingCounters = numberingCounters;
        this.styles = new StructuredContentDocxStyleSupport(styleCatalog);
        this.tableSupport = new StructuredContentDocxTableSupport(styles);
        this.inlineSupport = new StructuredContentDocxInlineSupport(
                variables,
                styles,
                imageResolver,
                this::rejectIfUnrenderable
        );
        this.cursor = new StructuredContentDocxCursorSupport(document);
        this.expandSupport = new StructuredContentDocxExpandSupport(
                objectMapper,
                document,
                listSupport,
                inlineSupport,
                styles,
                cursor,
                variables,
                pinnedModuleStructures,
                numberingCounters,
                (nodes, paragraph) -> writeBlockNodes(nodes, paragraph, true),
                this::writeBlockNode
        );
        this.blockDispatch = new StructuredContentDocxBlockDispatchSupport(
                styles,
                inlineSupport,
                variables,
                this::writeLoopBlock,
                expandSupport::expandContentModule,
                expandSupport::writeSectionHeading,
                expandSupport::writeInlineOrBlockChildren,
                expandSupport::writeAttachmentListRef
        );
    }

    void writeBlockNodes(JsonNode nodes, XWPFParagraph firstParagraph, boolean reuseFirstParagraph) {
        XWPFParagraph currentParagraph = firstParagraph;
        boolean paragraphAvailable = reuseFirstParagraph;
        for (int index = 0; index < nodes.size(); index++) {
            JsonNode node = nodes.get(index);
            String type = node.path("type").asText("");
            if ("tableComponentRef".equals(type) || "tableComponent".equals(type)) {
                JsonNode tableDefinition = node.has("tableComponent") ? node.get("tableComponent") : node;
                if (!paragraphAvailable) {
                    currentParagraph = cursor.insertParagraphAfter(currentParagraph);
                } else {
                    paragraphAvailable = false;
                    StructuredContentDocxWriter.clearParagraph(currentParagraph);
                }
                XWPFTable table = cursor.insertTableAfter(currentParagraph);
                tableSupport.populateTable(tableDefinition, table, variables);
                continue;
            }
            if ("list".equals(type)) {
                expandSupport.writeList(node, paragraphAvailable ? currentParagraph : null);
                paragraphAvailable = false;
                currentParagraph = body.getParagraphs().get(body.getParagraphs().size() - 1);
                continue;
            }
            if ("imageRef".equals(type) || "sealRef".equals(type)) {
                if (!paragraphAvailable) {
                    currentParagraph = cursor.insertParagraphAfter(currentParagraph);
                }
                inlineSupport.writeReferenceNode(node, currentParagraph);
                paragraphAvailable = false;
                continue;
            }
            if ("qrBarcodeRef".equals(type)) {
                if (!paragraphAvailable) {
                    currentParagraph = cursor.insertParagraphAfter(currentParagraph);
                }
                QrBarcodeRefDocxSupport.writeQrBarcodeRef(node, currentParagraph, variables, styles);
                paragraphAvailable = false;
                continue;
            }
            if ("attachmentListRef".equals(type)) {
                expandSupport.writeAttachmentListRef(
                        node,
                        paragraphAvailable ? currentParagraph : null
                );
                paragraphAvailable = false;
                if (!body.getParagraphs().isEmpty()) {
                    currentParagraph = body.getParagraphs().get(body.getParagraphs().size() - 1);
                }
                continue;
            }
            // LR-A4 (CD-PIT-07): unsupported / writer-missing structured node types must fail-closed.
            // Never silently drop content from published letters.
            rejectIfUnrenderable(type);
            if (!paragraphAvailable) {
                currentParagraph = cursor.insertParagraphAfter(currentParagraph);
            }
            writeBlockNode(node, currentParagraph);
            paragraphAvailable = false;
        }
    }

    private void rejectIfUnrenderable(String type) {
        blockDispatch.rejectIfUnrenderable(type);
    }

    private void writeBlockNode(JsonNode node, XWPFParagraph paragraph) {
        blockDispatch.writeBlockNode(node, paragraph);
    }

    private void writeLoopBlock(JsonNode node, XWPFParagraph paragraph) {
        String loopVariable = node.path("loopVariable").asText("");
        Object rawItems = variables.get(loopVariable);
        if (!(rawItems instanceof List<?> items) || items.isEmpty()) {
            expandSupport.writeInlineOrBlockChildren(node, paragraph);
            return;
        }
        XWPFParagraph current = paragraph;
        for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
            Map<String, Object> scoped = StructuredContentDocxTableSupport.scopedVariables(variables, items.get(itemIndex));
            StructuredContentDocxWriteSession scopedSession = new StructuredContentDocxWriteSession(
                    objectMapper,
                    styleCatalog,
                    imageResolver,
                    document,
                    body,
                    listSupport,
                    scoped,
                    pinnedModuleStructures,
                    numberingCounters
            );
            if (itemIndex == 0) {
                scopedSession.expandSupport.writeInlineOrBlockChildren(node, current);
            } else {
                XWPFParagraph next = cursor.insertParagraphAfter(current);
                scopedSession.expandSupport.writeInlineOrBlockChildren(node, next);
                current = next;
            }
        }
    }
}
