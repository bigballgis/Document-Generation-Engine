package com.bank.docgen.rendering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authoring.structured.FidelityValidationService;
import com.bank.docgen.authoring.structured.RenderProfileService;
import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.rendering.ArtifactSpoolService;
import com.bank.docgen.rendering.DocxAssembler;
import com.bank.docgen.rendering.DocumentArtifactPipeline;
import com.bank.docgen.rendering.GeneratedArtifactSizeGuard;
import com.bank.docgen.rendering.SpooledArtifact;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.sharedkernel.document.RenderProfile;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.port.RenderableTemplateSnapshot;
import com.bank.docgen.template.port.TemplateRenderContextPort;
import com.bank.docgen.template.port.VariableComputePort;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BDD-CE-G02 preview assembly path — watermark only on preview store keys.
 */
@ExtendWith(MockitoExtension.class)
class PreviewSpecimenWatermarkAssemblyTest {

    @Mock
    private AnchorBindingRepository anchorBindingRepository;
    @Mock
    private MasterDocumentRepository masterDocumentRepository;
    @Mock
    private ObjectStoragePort objectStoragePort;
    @Mock
    private DocxAssembler docxAssembler;
    @Mock
    private DocumentArtifactPipeline documentArtifactPipeline;
    @Mock
    private TemplateRenderContextPort renderContextPort;
    @Mock
    private RenderProfileService renderProfileService;
    @Mock
    private FidelityValidationService fidelityValidationService;
    @Mock
    private VariableComputePort variableComputePort;
    @Mock
    private com.bank.docgen.template.port.VariableSchemaValidationPort variableSchemaValidationPort;

    private PreviewGenerationAssemblySupport assembly;
    private ArtifactSpoolService artifactSpoolService;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(variableComputePort.applyCompute(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenAnswer(invocation -> {
            java.util.Map<String, Object> input = invocation.getArgument(1);
            return input == null ? java.util.Map.of() : new java.util.LinkedHashMap<>(input);
        });
        assembly = new PreviewGenerationAssemblySupport(
                anchorBindingRepository,
                masterDocumentRepository,
                objectStoragePort,
                docxAssembler,
                documentArtifactPipeline,
                renderContextPort,
                renderProfileService,
                fidelityValidationService,
                variableComputePort,
                variableSchemaValidationPort,
                new com.bank.docgen.rendering.PaginationDeltaFidelitySupport(
                        new DocgenRenderingProperties(),
                        new com.bank.docgen.rendering.PdfPageCountReader()
                )
        );
        artifactSpoolService = new ArtifactSpoolService(new GeneratedArtifactSizeGuard(new DocgenRenderingProperties()));
    }

    @Test
    void assembleAndStoreAppliesSpecimenToDocxAndPdf() throws Exception {
        UUID masterId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID previewId = UUID.randomUUID();
        byte[] assembledDocx = buildPlainDocx("Preview body");
        byte[] plainPdf = buildPlainPdf("Converted body");

        MasterDocumentEntity master = org.mockito.Mockito.mock(MasterDocumentEntity.class);
        when(master.getStorageKey()).thenReturn("masters/m1.docx");
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(masterId)).thenReturn(Optional.of(master));
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(versionId)).thenReturn(List.of());
        when(renderContextPort.resolvePinnedContentStructures(eq(versionId), any())).thenReturn(Map.of());
        when(objectStoragePort.get("masters/m1.docx")).thenReturn(new ByteArrayInputStream(new byte[] {1}));
        when(docxAssembler.assembleStructured(any(), any(), any(), any())).thenReturn(assembledDocx);

        TemplateVersionEntity version = org.mockito.Mockito.mock(TemplateVersionEntity.class);
        when(version.getId()).thenReturn(versionId);
        RenderProfile profile = org.mockito.Mockito.mock(RenderProfile.class);
        when(renderProfileService.resolveEffectiveProfile(eq(version), any())).thenReturn(profile);

        SpooledArtifact spooled = artifactSpoolService.spool(plainPdf);
        DocumentArtifactPipeline.GeneratedArtifact artifact =
                new DocumentArtifactPipeline.GeneratedArtifact(spooled, "application/pdf", "output.pdf");
        when(documentArtifactPipeline.finalizeArtifact(
                any(byte[].class),
                eq("PDF"),
                any(EncryptionOptionsView.class),
                eq(profile)
        )).thenReturn(artifact);
        when(fidelityValidationService.collectWarningsForVersion(versionId, masterId)).thenReturn(List.of());

        RenderableTemplateSnapshot template = new RenderableTemplateSnapshot(templateId, masterId, "RETAIL");

        ArgumentCaptor<byte[]> finalizeDocxCaptor = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<java.io.InputStream> pdfStreamCaptor = ArgumentCaptor.forClass(java.io.InputStream.class);

        assembly.assembleAndStore(template, version, previewId, Map.of());

        verify(documentArtifactPipeline).finalizeArtifact(
                finalizeDocxCaptor.capture(),
                eq("PDF"),
                any(EncryptionOptionsView.class),
                eq(profile)
        );
        byte[] watermarkedDocx = finalizeDocxCaptor.getValue();
        assertThat(anyZipPartContains(watermarkedDocx, "word/header", "SPECIMEN")).isTrue();
        assertThat(anyZipPartContains(watermarkedDocx, "word/footer", "SPECIMEN")).isTrue();

        verify(objectStoragePort).put(
                eq("previews/" + previewId + "/output.pdf"),
                pdfStreamCaptor.capture(),
                anyLong(),
                eq("application/pdf")
        );
        byte[] storedPdf = pdfStreamCaptor.getValue().readAllBytes();
        try (PDDocument document = Loader.loadPDF(storedPdf)) {
            assertThat(new PDFTextStripper().getText(document).replaceAll("\\s+", "")).contains("SPECIMEN");
        }
    }

    @Test
    void pdfStampFailureDoesNotStoreSucceededPdf() throws Exception {
        UUID masterId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID previewId = UUID.randomUUID();
        byte[] assembledDocx = buildPlainDocx("Preview body");

        MasterDocumentEntity master = org.mockito.Mockito.mock(MasterDocumentEntity.class);
        when(master.getStorageKey()).thenReturn("masters/m1.docx");
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(masterId)).thenReturn(Optional.of(master));
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(versionId)).thenReturn(List.of());
        when(renderContextPort.resolvePinnedContentStructures(eq(versionId), any())).thenReturn(Map.of());
        when(objectStoragePort.get("masters/m1.docx")).thenReturn(new ByteArrayInputStream(new byte[] {1}));
        when(docxAssembler.assembleStructured(any(), any(), any(), any())).thenReturn(assembledDocx);

        TemplateVersionEntity version = org.mockito.Mockito.mock(TemplateVersionEntity.class);
        when(version.getId()).thenReturn(versionId);
        RenderProfile profile = org.mockito.Mockito.mock(RenderProfile.class);
        when(renderProfileService.resolveEffectiveProfile(eq(version), any())).thenReturn(profile);

        SpooledArtifact spooled = artifactSpoolService.spool(new byte[] {1, 2, 3});
        DocumentArtifactPipeline.GeneratedArtifact artifact =
                new DocumentArtifactPipeline.GeneratedArtifact(spooled, "application/pdf", "output.pdf");
        when(documentArtifactPipeline.finalizeArtifact(
                any(byte[].class),
                eq("PDF"),
                any(EncryptionOptionsView.class),
                eq(profile)
        )).thenReturn(artifact);

        RenderableTemplateSnapshot template = new RenderableTemplateSnapshot(templateId, masterId, "RETAIL");

        assertThatThrownBy(() -> assembly.assembleAndStore(template, version, previewId, Map.of()))
                .isInstanceOf(RuntimeException.class);

        verify(objectStoragePort, never()).put(
                eq("previews/" + previewId + "/output.pdf"),
                any(),
                anyLong(),
                anyString()
        );
    }

    private static byte[] buildPlainDocx(String body) throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText(body);
            document.write(output);
            return output.toByteArray();
        }
    }

    private static byte[] buildPlainPdf(String body) throws Exception {
        try (PDDocument source = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            source.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(source, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText(body);
                contentStream.endText();
            }
            source.save(output);
            return output.toByteArray();
        }
    }

    private static boolean anyZipPartContains(byte[] docxBytes, String namePrefix, String substring) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docxBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.startsWith(namePrefix) && name.endsWith(".xml")) {
                    String xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                    if (xml.contains(substring)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
