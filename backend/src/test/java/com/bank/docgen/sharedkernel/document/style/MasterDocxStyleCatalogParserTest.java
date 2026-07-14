package com.bank.docgen.sharedkernel.document.style;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

/**
 * BDD-CE-K02-001 / 004 / 006 — parse styles.xml (+ theme) into a durable catalog.
 */
public class MasterDocxStyleCatalogParserTest {

    @Test
    void parsesParagraphStyleTypographyAndDocDefaults() throws Exception {
        byte[] docx = dualFontMasterBytes();

        MasterStyleCatalog catalog = MasterDocxStyleCatalogParser.parse(docx);

        assertThat(catalog.hasDocDefaults()).isTrue();
        assertThat(catalog.docDefaults().eastAsia()).isEqualTo("宋体");
        assertThat(catalog.find("ClauseBody")).isNotNull();
        assertThat(catalog.find("ClauseBody").typography().eastAsia()).isEqualTo("仿宋");
        assertThat(catalog.find("ClauseBody").styleType()).isEqualTo(MasterStyleType.PARAGRAPH);
        assertThat(catalog.themeFonts()).isNotNull();
        assertThat(catalog.themeFonts().minorEastAsia()).isEqualTo("宋体");
        assertThat(catalog.catalogVersion()).startsWith("master-styles-");
    }

    @Test
    void missingStylesXmlFailsClosed() throws Exception {
        byte[] docx = minimalDocxWithoutStyles();

        assertThatThrownBy(() -> MasterDocxStyleCatalogParser.parse(docx))
                .isInstanceOf(MasterDocxStyleCatalogParseException.class)
                .hasMessageContaining("styles.xml");
    }

    public static byte[] dualFontMasterBytes() throws Exception {
        String styles = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:docDefaults>
                    <w:rPrDefault>
                      <w:rPr>
                        <w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:eastAsia="宋体" w:cs="Times New Roman"/>
                        <w:sz w:val="21"/>
                        <w:szCs w:val="21"/>
                      </w:rPr>
                    </w:rPrDefault>
                    <w:pPrDefault><w:pPr/></w:pPrDefault>
                  </w:docDefaults>
                  <w:style w:type="paragraph" w:styleId="Normal" w:default="1">
                    <w:name w:val="Normal"/>
                    <w:qFormat/>
                    <w:rPr>
                      <w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:eastAsia="宋体"/>
                      <w:sz w:val="21"/>
                    </w:rPr>
                  </w:style>
                  <w:style w:type="paragraph" w:styleId="ClauseBody">
                    <w:name w:val="Clause Body"/>
                    <w:basedOn w:val="Normal"/>
                    <w:qFormat/>
                    <w:rPr>
                      <w:rFonts w:ascii="KaiTi" w:hAnsi="KaiTi" w:eastAsia="仿宋"/>
                      <w:sz w:val="21"/>
                    </w:rPr>
                  </w:style>
                  <w:style w:type="paragraph" w:styleId="BankLetterBody">
                    <w:name w:val="Bank Letter Body"/>
                    <w:qFormat/>
                    <w:rPr>
                      <w:rFonts w:eastAsia="宋体"/>
                      <w:sz w:val="22"/>
                    </w:rPr>
                  </w:style>
                </w:styles>
                """;
        String theme = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
                  <a:themeElements>
                    <a:fontScheme name="Office">
                      <a:majorFont>
                        <a:latin typeface="Times New Roman"/>
                        <a:ea typeface="黑体"/>
                        <a:cs typeface="Times New Roman"/>
                      </a:majorFont>
                      <a:minorFont>
                        <a:latin typeface="Times New Roman"/>
                        <a:ea typeface="宋体"/>
                        <a:cs typeface="Times New Roman"/>
                      </a:minorFont>
                    </a:fontScheme>
                  </a:themeElements>
                </a:theme>
                """;
        String document = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:body>
                    <w:p><w:r><w:t>{{anchor:BODY}}</w:t></w:r></w:p>
                    <w:sectPr/>
                  </w:body>
                </w:document>
                """;
        return zipDocx(document, styles, theme);
    }

    public static byte[] masterWithoutDocDefaults(String clauseFont) throws Exception {
        String styles = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:style w:type="paragraph" w:styleId="ClauseBody">
                    <w:name w:val="Clause Body"/>
                    <w:rPr>
                      <w:rFonts w:eastAsia="%s"/>
                      <w:sz w:val="21"/>
                    </w:rPr>
                  </w:style>
                </w:styles>
                """.formatted(clauseFont);
        String document = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:body>
                    <w:p><w:r><w:t>{{anchor:BODY}}</w:t></w:r></w:p>
                    <w:sectPr/>
                  </w:body>
                </w:document>
                """;
        return zipDocx(document, styles, null);
    }

    private static byte[] minimalDocxWithoutStyles() throws Exception {
        String document = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:body><w:p><w:r><w:t>{{anchor:BODY}}</w:t></w:r></w:p><w:sectPr/></w:body>
                </w:document>
                """;
        return zipDocx(document, null, null);
    }

    private static byte[] zipDocx(String documentXml, String stylesXml, String themeXml) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            write(zip, "[Content_Types].xml", contentTypes(stylesXml != null, themeXml != null));
            write(zip, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                    </Relationships>
                    """);
            write(zip, "word/document.xml", documentXml);
            write(zip, "word/_rels/document.xml.rels", documentRels(stylesXml != null, themeXml != null));
            if (stylesXml != null) {
                write(zip, "word/styles.xml", stylesXml);
            }
            if (themeXml != null) {
                write(zip, "word/theme/theme1.xml", themeXml);
            }
        }
        return output.toByteArray();
    }

    private static String contentTypes(boolean styles, boolean theme) {
        StringBuilder builder = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                """);
        if (styles) {
            builder.append("""
                  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
                """);
        }
        if (theme) {
            builder.append("""
                  <Override PartName="/word/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>
                """);
        }
        builder.append("</Types>");
        return builder.toString();
    }

    private static String documentRels(boolean styles, boolean theme) {
        StringBuilder builder = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                """);
        if (styles) {
            builder.append("""
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                """);
        }
        if (theme) {
            builder.append("""
                  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="theme/theme1.xml"/>
                """);
        }
        builder.append("</Relationships>");
        return builder.toString();
    }

    private static void write(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
