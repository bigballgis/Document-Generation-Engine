package com.bank.docgen.rendering;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.sharedkernel.document.style.MasterDocxStyleCatalogParseException;
import com.bank.docgen.sharedkernel.document.style.MasterDocxStyleCatalogParser;
import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalog;
import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalogMergeSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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

    private final ObjectMapper objectMapper;
    private final MasterStyleCatalog platformMetadataCatalog;
    private final StructuredContentImageResolver imageResolver;
    private final OoxmlOutputValidator ooxmlOutputValidator;
    private final DocgenRenderingProperties renderingProperties;
    private final List<String> lastAssemblyFidelityWarnings = new ArrayList<>();

    @Autowired
    public DocxAssembler(
            ObjectMapper objectMapper,
            StructuredContentImageResolver imageResolver,
            OoxmlOutputValidator ooxmlOutputValidator,
            DocgenRenderingProperties renderingProperties
    ) {
        this.objectMapper = objectMapper;
        this.platformMetadataCatalog = DocxMasterStyleCatalogSupport.loadDefault(objectMapper);
        this.imageResolver = imageResolver;
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

    /**
     * CE-K02: fidelity warning codes from the most recent structured assembly (e.g. MASTER_STYLE_FALLBACK).
     */
    public List<String> lastAssemblyFidelityWarnings() {
        return List.copyOf(lastAssemblyFidelityWarnings);
    }

    public byte[] assemble(InputStream masterDocx, Map<String, String> anchorContent) {
        try {
            byte[] masterBytes = masterDocx.readAllBytes();
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(masterBytes));
                    ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                DocxMasterLayoutFillerSupport.removeFillerParagraphs(document, MASTER_FILLER_MARKER);
                DocxPlainAnchorParagraphSupport.replaceAnchorsInDocumentBody(document, anchorContent, ANCHOR_PATTERN);
                DocxPlainAnchorParagraphSupport.replaceInTablesHeadersAndFooters(document, anchorContent, ANCHOR_PATTERN);
                DocxWordCompatibilitySupport.ensureWordCompatiblePackage(document);
                document.write(output);
                return validatedBytes(output.toByteArray());
            }
        } catch (IOException ex) {
            throw new DocxAssemblyException(ex);
        }
    }

    public String renderStructuredContent(String structuredContentJson, Map<String, Object> variables) {
        return renderStructuredContent(structuredContentJson, variables, Map.of(), null);
    }

    public String renderStructuredContent(
            String structuredContentJson,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures
    ) {
        return renderStructuredContent(structuredContentJson, variables, pinnedModuleStructures, null);
    }

    public String renderStructuredContent(
            String structuredContentJson,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures,
            String owningGroupCode
    ) {
        return AssetResolveGroupContext.callWithGroup(owningGroupCode, () -> {
            StructuredContentDocxWriter writer = new StructuredContentDocxWriter(
                    objectMapper,
                    platformMetadataCatalog,
                    imageResolver
            );
            return writer.renderPlainTextProjection(
                    structuredContentJson,
                    variables,
                    pinnedModuleStructures
            );
        });
    }

    public Map<String, String> buildAnchorReplacements(
            Map<String, String> bindingJsonByAnchor,
            Map<String, Object> variables
    ) {
        return buildAnchorReplacements(bindingJsonByAnchor, variables, Map.of(), null);
    }

    public Map<String, String> buildAnchorReplacements(
            Map<String, String> bindingJsonByAnchor,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures
    ) {
        return buildAnchorReplacements(bindingJsonByAnchor, variables, pinnedModuleStructures, null);
    }

    public Map<String, String> buildAnchorReplacements(
            Map<String, String> bindingJsonByAnchor,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures,
            String owningGroupCode
    ) {
        return bindingJsonByAnchor.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> renderStructuredContent(
                                entry.getValue(),
                                variables,
                                pinnedModuleStructures,
                                owningGroupCode
                        )
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
        return assembleStructured(masterDocx, bindingJsonByAnchor, variables, pinnedModuleStructures, null);
    }

    public byte[] assembleStructured(
            InputStream masterDocx,
            Map<String, String> bindingJsonByAnchor,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures,
            String owningGroupCode
    ) {
        try {
            byte[] masterBytes = masterDocx.readAllBytes();
            return assembleStructuredFromBytes(
                    masterBytes,
                    bindingJsonByAnchor,
                    variables,
                    pinnedModuleStructures,
                    owningGroupCode
            );
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
        return assembleStructuredFromBytes(
                masterBytes,
                bindingJsonByAnchor,
                variables,
                pinnedModuleStructures,
                null
        );
    }

    public byte[] assembleStructuredFromBytes(
            byte[] masterBytes,
            Map<String, String> bindingJsonByAnchor,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures,
            String owningGroupCode
    ) {
        return AssetResolveGroupContext.callWithGroup(owningGroupCode, () -> {
            lastAssemblyFidelityWarnings.clear();
            MasterStyleCatalog assemblyCatalog = resolveAssemblyCatalog(masterBytes);
            StructuredContentDocxWriter writer = new StructuredContentDocxWriter(
                    objectMapper,
                    assemblyCatalog,
                    imageResolver
            );
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(masterBytes));
                    ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                DocxMasterLayoutFillerSupport.removeFillerParagraphs(document, MASTER_FILLER_MARKER);
                DocxStructuredAnchorSupport.replaceInDocumentBody(
                        document,
                        bindingJsonByAnchor,
                        variables,
                        pinnedModuleStructures,
                        writer,
                        ANCHOR_PATTERN
                );
                DocxStructuredAnchorSupport.replaceInTablesHeadersAndFooters(
                        document,
                        bindingJsonByAnchor,
                        variables,
                        pinnedModuleStructures,
                        writer,
                        ANCHOR_PATTERN
                );
                DocxWordCompatibilitySupport.ensureWordCompatiblePackage(document);
                if (!assemblyCatalog.hasDocDefaults()) {
                    lastAssemblyFidelityWarnings.add("MASTER_STYLE_FALLBACK");
                }
                document.write(output);
                return validatedBytes(output.toByteArray());
            } catch (IOException ex) {
                throw new DocxAssemblyException(ex);
            }
        });
    }

    /**
     * CE-K02: assembly catalog is parsed from the master package opened for this render
     * (pinned revision bytes for published generation; current revision for preview / golden).
     */
    private MasterStyleCatalog resolveAssemblyCatalog(byte[] masterBytes) {
        try {
            MasterStyleCatalog parsed = MasterDocxStyleCatalogParser.parse(masterBytes);
            return MasterStyleCatalogMergeSupport.mergeWithPlatformMetadata(parsed, platformMetadataCatalog);
        } catch (MasterDocxStyleCatalogParseException ex) {
            // Masters without styles.xml (legacy golden skeletons): platform metadata + no docDefaults
            // → system baseline path + MASTER_STYLE_FALLBACK (K02-C6/C7).
            return platformMetadataCatalog;
        }
    }

    private byte[] validatedBytes(byte[] assembledBytes) {
        if (renderingProperties.isOoxmlValidationEnabled()) {
            ooxmlOutputValidator.validate(assembledBytes);
        }
        return assembledBytes;
    }
}
