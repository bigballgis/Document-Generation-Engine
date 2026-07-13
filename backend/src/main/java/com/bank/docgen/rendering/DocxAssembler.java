package com.bank.docgen.rendering;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalog;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DocxAssembler {

    private static final Pattern ANCHOR_PATTERN = Pattern.compile("\\{\\{anchor:([A-Za-z0-9_.-]+)}}");
    private static final String MASTER_FILLER_MARKER =
            "Section-level anchor in the master layout container";

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
        this.styleCatalog = DocxMasterStyleCatalogSupport.loadDefault(objectMapper);
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
            DocxMasterLayoutFillerSupport.removeFillerParagraphs(document, MASTER_FILLER_MARKER);
            DocxPlainAnchorParagraphSupport.replaceAnchorsInDocumentBody(document, anchorContent, ANCHOR_PATTERN);
            DocxPlainAnchorParagraphSupport.replaceInTablesHeadersAndFooters(document, anchorContent, ANCHOR_PATTERN);
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
            DocxMasterLayoutFillerSupport.removeFillerParagraphs(document, MASTER_FILLER_MARKER);
            DocxStructuredAnchorSupport.replaceInDocumentBody(
                    document,
                    bindingJsonByAnchor,
                    variables,
                    pinnedModuleStructures,
                    structuredContentDocxWriter,
                    ANCHOR_PATTERN
            );
            DocxStructuredAnchorSupport.replaceInTablesHeadersAndFooters(
                    document,
                    bindingJsonByAnchor,
                    variables,
                    pinnedModuleStructures,
                    structuredContentDocxWriter,
                    ANCHOR_PATTERN
            );
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
}
