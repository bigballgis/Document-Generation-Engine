package com.bank.docgen.rendering;

import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.internal.PackagePropertiesPart;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

/**
 * LR-A6 / CD-HARD-T03 / ADR-0043: post-assembly OOXML well-formedness gate.
 *
 * <p>Opens the DOCX via {@link OPCPackage} and parses Word XML parts
 * ({@code document.xml}, styles, numbering, headers, footers, and related {@code .xml} parts).
 * Malformed XML / unescaped-ampersand class defects fail closed.
 *
 * <p>Residual: full ECMA-376 XSD schema validation is intentionally deferred; well-formedness
 * plus part walk is the verify Done line for this slice.
 */
@Component
public class OoxmlOutputValidator {

    private static final String MESSAGE_KEY = "api.error.rendering.ooxmlValidationFailed";

    /**
     * Validates assembled DOCX bytes. Does not mutate or persist the package.
     *
     * @throws DocxAssemblyException when the package cannot be opened or any XML part is malformed
     */
    public void validate(byte[] docxBytes) {
        if (docxBytes == null || docxBytes.length == 0) {
            throw failClosed("OOXML package is empty");
        }
        try (OPCPackage pkg = OPCPackage.open(new ByteArrayInputStream(docxBytes))) {
            DocumentBuilderFactory factory = secureDocumentBuilderFactory();
            for (PackagePart part : pkg.getParts()) {
                if (!isValidatableXmlPart(part)) {
                    continue;
                }
                validateXmlPart(factory, part);
            }
        } catch (DocxAssemblyException ex) {
            throw ex;
        } catch (Exception ex) {
            throw failClosed("OOXML package validation failed: " + safeDetail(ex), ex);
        }
    }

    private static void validateXmlPart(DocumentBuilderFactory factory, PackagePart part) {
        String partName = part.getPartName().getName();
        try (InputStream inputStream = part.getInputStream()) {
            factory.newDocumentBuilder().parse(inputStream);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            throw failClosed(
                    "Malformed OOXML part '" + partName + "': " + safeDetail(ex),
                    ex
            );
        }
    }

    /**
     * Word markup + package relationship / content-type XML. Skips {@link PackagePropertiesPart}
     * (core/app props) because POI forbids raw stream access on that part type.
     */
    private static boolean isValidatableXmlPart(PackagePart part) {
        if (part instanceof PackagePropertiesPart) {
            return false;
        }
        String name = part.getPartName().getName();
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".xml") || lower.endsWith(".rels")) {
            return true;
        }
        String contentType = part.getContentType();
        return contentType != null && contentType.toLowerCase(Locale.ROOT).contains("xml");
    }

    private static DocumentBuilderFactory secureDocumentBuilderFactory()
            throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setExpandEntityReferences(false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        return factory;
    }

    private static DocxAssemblyException failClosed(String detail) {
        return failClosed(detail, null);
    }

    private static DocxAssemblyException failClosed(String detail, Throwable cause) {
        if (cause == null) {
            return new DocxAssemblyException(
                    ApiErrorCodes.OOXML_VALIDATION_FAILED,
                    ApiErrorCategories.RENDERING,
                    MESSAGE_KEY,
                    detail
            );
        }
        return new DocxAssemblyException(
                ApiErrorCodes.OOXML_VALIDATION_FAILED,
                ApiErrorCategories.RENDERING,
                MESSAGE_KEY,
                detail,
                cause
        );
    }

    private static String safeDetail(Throwable ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        // Keep messages short; never echo full document payloads.
        return message.length() > 240 ? message.substring(0, 240) : message;
    }
}
