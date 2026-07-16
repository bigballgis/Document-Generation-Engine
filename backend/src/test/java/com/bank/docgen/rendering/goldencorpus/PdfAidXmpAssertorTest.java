package com.bank.docgen.rendering.goldencorpus;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.junit.jupiter.api.Test;

/**
 * BDD-CE-O01-010 — lightweight pdfaid XMP check (part=2, conformance=B).
 */
class PdfAidXmpAssertorTest {

    @Test
    void acceptsPdfA2bXmpIdentifier() throws Exception {
        byte[] pdf = pdfWithXmp("""
                <?xpacket begin='' id='W5M0MpCehiHzreSzNTczkc9d'?>
                <x:xmpmeta xmlns:x='adobe:ns:meta/'>
                  <rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>
                    <rdf:Description xmlns:pdfaid='http://www.aiim.org/pdfa/ns/id/'
                      pdfaid:part='2' pdfaid:conformance='B'/>
                  </rdf:RDF>
                </x:xmpmeta>
                <?xpacket end='w'?>
                """);

        assertThatCode(() -> PdfAidXmpAssertor.assertPdfA2bIdentifier(pdf))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingPdfAid() throws Exception {
        byte[] pdf = pdfWithXmp("""
                <?xpacket begin='' id='W5M0MpCehiHzreSzNTczkc9d'?>
                <x:xmpmeta xmlns:x='adobe:ns:meta/'/>
                <?xpacket end='w'?>
                """);

        assertThatThrownBy(() -> PdfAidXmpAssertor.assertPdfA2bIdentifier(pdf))
                .isInstanceOf(GoldenCorpusException.class)
                .hasMessageContaining("pdfaid");
    }

    private static byte[] pdfWithXmp(String xmp) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            PDMetadata metadata = new PDMetadata(document);
            metadata.importXMPMetadata(xmp.getBytes(StandardCharsets.UTF_8));
            document.getDocumentCatalog().setMetadata(metadata);
            document.save(output);
            return output.toByteArray();
        }
    }
}
