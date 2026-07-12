package com.bank.docgen.master.service;

import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterAnchorEntity;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.rendering.DocxAnchorExtractor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

/**
 * Package-private DOCX upload / storage / anchor extraction helpers for master documents.
 */
final class MasterDocxUploadSupport {

    static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private static final Logger LOG = LoggerFactory.getLogger(MasterDocxUploadSupport.class);
    private static final byte[] ZIP_MAGIC_BYTES = {'P', 'K', 3, 4};

    private final ObjectStoragePort objectStoragePort;
    private final DocxAnchorExtractor docxAnchorExtractor;
    private final long maxDocxUploadBytes;

    MasterDocxUploadSupport(
            ObjectStoragePort objectStoragePort,
            DocxAnchorExtractor docxAnchorExtractor,
            long maxDocxUploadBytes
    ) {
        this.objectStoragePort = objectStoragePort;
        this.docxAnchorExtractor = docxAnchorExtractor;
        this.maxDocxUploadBytes = maxDocxUploadBytes;
    }

    void validateDocxFile(MultipartFile docxFile) {
        if (docxFile == null || docxFile.isEmpty()) {
            throw new MasterValidationException("api.error.master.docxRequired");
        }
        if (docxFile.getSize() > maxDocxUploadBytes) {
            throw new MasterValidationException("api.error.master.docxTooLarge");
        }
        String filename = docxFile.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".docx")) {
            throw new MasterValidationException("api.error.master.docxRequired");
        }
        // LR-A3 (CD-PIT-04): reject masquerading uploads by content type. The declared
        // content type must match the OOXML Word document media type; files claiming to be
        // .docx but sending text/html/image/etc. content are rejected before POI parses them.
        String contentType = docxFile.getContentType();
        if (contentType != null
                && !DOCX_CONTENT_TYPE.equals(contentType)
                && !"application/octet-stream".equals(contentType)) {
            throw new MasterValidationException("api.error.master.docxRequired");
        }
        assertDocxPackageStructure(docxFile);
    }

    void storeDocx(String storageKey, MultipartFile docxFile) {
        try (InputStream inputStream = docxFile.getInputStream()) {
            objectStoragePort.put(storageKey, inputStream, docxFile.getSize(), DOCX_CONTENT_TYPE);
        } catch (IOException ex) {
            LOG.warn("Failed to store master DOCX at {}: {}", storageKey, ex.getMessage());
            throw new MasterValidationException("api.error.master.storageFailed");
        }
    }

    List<String> extractAnchors(MultipartFile docxFile) {
        try (InputStream inputStream = docxFile.getInputStream()) {
            byte[] bytes = inputStream.readAllBytes();
            try (ByteArrayInputStream extractorStream = new ByteArrayInputStream(bytes)) {
                return docxAnchorExtractor.extractOrderedAnchorIds(extractorStream);
            }
        } catch (IOException ex) {
            LOG.warn("Failed to extract anchors from uploaded DOCX: {}", ex.getMessage());
            throw new MasterValidationException("api.error.master.anchorExtractionFailed");
        }
    }

    void assertAnchorIntegrity(MasterDocumentEntity master) {
        List<String> extracted = extractAnchorsFromStorage(master.getStorageKey());
        Set<String> catalog = master.getAnchors().stream()
                .map(MasterAnchorEntity::getAnchorId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (extracted.isEmpty() || !new LinkedHashSet<>(extracted).equals(catalog)) {
            throw new MasterValidationException("api.error.master.anchorIntegrityFailed");
        }
    }

    List<String> extractAnchorsFromStorage(String storageKey) {
        try (InputStream inputStream = objectStoragePort.get(storageKey)) {
            return docxAnchorExtractor.extractOrderedAnchorIds(inputStream);
        } catch (IOException | RuntimeException ex) {
            throw new MasterValidationException("api.error.master.anchorExtractionFailed");
        }
    }

    String revisionStorageKey(UUID masterId, UUID revisionLineId, String originalFilename) {
        return "masters/" + masterId + "/revisions/" + revisionLineId + "/"
                + sanitizeFilename(originalFilename);
    }

    private static String sanitizeFilename(String filename) {
        return filename == null ? "master.docx" : filename.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private void assertDocxPackageStructure(MultipartFile docxFile) {
        try (PushbackInputStream inputStream = new PushbackInputStream(docxFile.getInputStream(), ZIP_MAGIC_BYTES.length)) {
            byte[] signature = inputStream.readNBytes(ZIP_MAGIC_BYTES.length);
            if (signature.length < ZIP_MAGIC_BYTES.length || !java.util.Arrays.equals(signature, ZIP_MAGIC_BYTES)) {
                throw new MasterValidationException("api.error.master.docxCorrupt");
            }
            inputStream.unread(signature);
            assertRequiredDocxEntries(inputStream);
        } catch (MasterValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new MasterValidationException("api.error.master.docxCorrupt");
        }
    }

    private void assertRequiredDocxEntries(InputStream inputStream) throws Exception {
        boolean hasContentTypes = false;
        boolean hasRelationships = false;
        boolean hasDocumentXml = false;
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            for (ZipEntry entry = zipInputStream.getNextEntry(); entry != null; entry = zipInputStream.getNextEntry()) {
                String entryName = entry.getName();
                if ("[Content_Types].xml".equals(entryName)) {
                    hasContentTypes = true;
                } else if ("_rels/.rels".equals(entryName)) {
                    hasRelationships = true;
                } else if ("word/document.xml".equals(entryName)) {
                    hasDocumentXml = true;
                }
                zipInputStream.closeEntry();
            }
        }
        if (!hasContentTypes || !hasRelationships || !hasDocumentXml) {
            throw new MasterValidationException("api.error.master.docxCorrupt");
        }
    }
}
