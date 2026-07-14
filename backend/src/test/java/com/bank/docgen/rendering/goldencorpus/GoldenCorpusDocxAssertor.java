package com.bank.docgen.rendering.goldencorpus;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Applies DOCX keypath / XPath assertions against an assembled package (K07-C6).
 */
public final class GoldenCorpusDocxAssertor {

    public void assertDocx(byte[] docxBytes, JsonNode assertionRoot) {
        if (assertionRoot.path("deferred").asBoolean(false)) {
            return;
        }
        JsonNode assertions = assertionRoot.path("assertions");
        if (!assertions.isArray() || assertions.isEmpty()) {
            return;
        }
        for (JsonNode assertion : assertions) {
            String type = assertion.path("type").asText().trim().toUpperCase(Locale.ROOT);
            switch (type) {
                case "XML_CONTAINS" -> assertXmlContains(docxBytes, assertion);
                case "XML_NOT_CONTAINS" -> assertXmlNotContains(docxBytes, assertion);
                case "XPATH_EXISTS" -> assertXpathExists(docxBytes, assertion);
                default -> throw new GoldenCorpusException("Unsupported DOCX assertion type: " + type);
            }
        }
    }

    private void assertXmlContains(byte[] docxBytes, JsonNode assertion) {
        String part = assertion.path("part").asText("word/document.xml");
        String substring = assertion.path("substring").asText();
        if (substring.isBlank()) {
            throw new GoldenCorpusException("XML_CONTAINS requires substring");
        }
        String xml = readZipPartAsString(docxBytes, part);
        if (!xml.contains(substring)) {
            String plain = xml.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
            if (plain.length() > 400) {
                plain = plain.substring(0, 400);
            }
            throw new GoldenCorpusException(
                    "DOCX assertion failed: part '" + part + "' does not contain expected substring '"
                            + substring + "'. Plain text snippet: " + plain
            );
        }
    }

    private void assertXmlNotContains(byte[] docxBytes, JsonNode assertion) {
        String part = assertion.path("part").asText("word/document.xml");
        String substring = assertion.path("substring").asText();
        if (substring.isBlank()) {
            throw new GoldenCorpusException("XML_NOT_CONTAINS requires substring");
        }
        String xml = readZipPartAsString(docxBytes, part);
        if (xml.contains(substring)) {
            throw new GoldenCorpusException(
                    "DOCX assertion failed: part '" + part + "' unexpectedly contains forbidden substring"
            );
        }
    }

    private void assertXpathExists(byte[] docxBytes, JsonNode assertion) {
        String part = assertion.path("part").asText("word/document.xml");
        String xpathExpr = assertion.path("xpath").asText();
        if (xpathExpr.isBlank()) {
            throw new GoldenCorpusException("XPATH_EXISTS requires xpath");
        }
        String xml = readZipPartAsString(docxBytes, part);
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList nodes = (NodeList) xpath.evaluate(xpathExpr, document, XPathConstants.NODESET);
            if (nodes == null || nodes.getLength() == 0) {
                throw new GoldenCorpusException("DOCX XPath assertion failed: no nodes for " + xpathExpr);
            }
        } catch (GoldenCorpusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new GoldenCorpusException("Failed evaluating DOCX XPath: " + xpathExpr, ex);
        }
    }

    static String readZipPartAsString(byte[] docxBytes, String partName) {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docxBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (partName.equals(entry.getName())) {
                    return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        } catch (IOException ex) {
            throw new GoldenCorpusException("Failed reading DOCX part " + partName, ex);
        }
        throw new GoldenCorpusException("DOCX part not found: " + partName);
    }
}
