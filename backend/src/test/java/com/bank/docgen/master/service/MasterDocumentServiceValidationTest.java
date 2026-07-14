package com.bank.docgen.master.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.api.CreateMasterRequest;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterAnchorRepository;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterReviewRecordRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.master.rendering.DocxAnchorExtractor;
import com.bank.docgen.sharedkernel.lifecycle.SelfApprovalGuard;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class MasterDocumentServiceValidationTest {

    private static final ManagementSessionClaims GLOBAL_ADMIN = new ManagementSessionClaims(
            "10000001",
            "Admin",
            "admin@example.com",
            AuthSource.LOCAL,
            List.of("GLOBAL_ADMIN"),
            List.of("*"),
            "route.global-governance-home",
            List.of(),
            Instant.now().plusSeconds(3600)
    );

    @Mock
    private MasterDocumentRepository masterDocumentRepository;

    @Mock
    private MasterAnchorRepository masterAnchorRepository;

    @Mock
    private MasterReviewRecordRepository masterReviewRecordRepository;

    @Mock
    private MasterRevisionLineRepository masterRevisionLineRepository;

    @Mock
    private ObjectStoragePort objectStoragePort;

    @Mock
    private DocxAnchorExtractor docxAnchorExtractor;
    @Mock
    private ManagementUserDisplayService managementUserDisplayService;

    private MasterDocumentService service;

    @BeforeEach
    void setUp() {
        service = new MasterDocumentService(
                masterDocumentRepository,
                masterAnchorRepository,
                masterReviewRecordRepository,
                masterRevisionLineRepository,
                objectStoragePort,
                docxAnchorExtractor,
                new GroupAccessService(),
                managementUserDisplayService,
                new SelfApprovalGuard(),
                new ObjectMapper(),
                4096
        );
    }

    @Test
    void rejectsDocxLargerThanConfiguredLimitBeforeStorage() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "master.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[4097]
        );

        assertThatThrownBy(() -> service.create(request(), file, GLOBAL_ADMIN))
                .isInstanceOf(MasterValidationException.class)
                .extracting(ex -> ((MasterValidationException) ex).messageKey())
                .isEqualTo("api.error.master.docxTooLarge");

        verify(objectStoragePort, never()).put(any(), any(), any(Long.class), any());
        verify(docxAnchorExtractor, never()).extractOrderedAnchorIds(any());
    }

    @Test
    void rejectsNonDocxExtensionBeforeStorage() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "master.txt",
                "text/plain",
                new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> service.create(request(), file, GLOBAL_ADMIN))
                .isInstanceOf(MasterValidationException.class)
                .extracting(ex -> ((MasterValidationException) ex).messageKey())
                .isEqualTo("api.error.master.docxRequired");

        verify(objectStoragePort, never()).put(any(), any(), any(Long.class), any());
        verify(docxAnchorExtractor, never()).extractOrderedAnchorIds(any());
    }

    @Test
    void rejectsCorruptDocxPackageBeforeStorage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "master.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                zipBytes(Set.of("word/styles.xml"))
        );

        assertThatThrownBy(() -> service.create(request(), file, GLOBAL_ADMIN))
                .isInstanceOf(MasterValidationException.class)
                .extracting(ex -> ((MasterValidationException) ex).messageKey())
                .isEqualTo("api.error.master.docxCorrupt");

        verify(objectStoragePort, never()).put(any(), any(), any(Long.class), any());
        verify(docxAnchorExtractor, never()).extractOrderedAnchorIds(any());
    }

    @Test
    void rejectsWrongMagicBytesBeforeStorage() {
        // A1: .docx name + OOXML MIME but plain-text / non-PK signature → docxCorrupt
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "evil.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "not-a-zip-package".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> service.create(request(), file, GLOBAL_ADMIN))
                .isInstanceOf(MasterValidationException.class)
                .extracting(ex -> ((MasterValidationException) ex).messageKey())
                .isEqualTo("api.error.master.docxCorrupt");

        verify(objectStoragePort, never()).put(any(), any(), any(Long.class), any());
        verify(docxAnchorExtractor, never()).extractOrderedAnchorIds(any());
    }

    @Test
    void rejectsMasqueradingContentTypeBeforeStorage() {
        // LR-A3 (CD-PIT-04): a file claiming to be .docx but sending text/html content is
        // rejected before POI/LibreOffice ever parse it.
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "master.docx",
                "text/html",
                new byte[]{'<', 'h', 't', 'm', 'l', '>'}
        );

        assertThatThrownBy(() -> service.create(request(), file, GLOBAL_ADMIN))
                .isInstanceOf(MasterValidationException.class)
                .extracting(ex -> ((MasterValidationException) ex).messageKey())
                .isEqualTo("api.error.master.docxRequired");

        verify(objectStoragePort, never()).put(any(), any(), any(Long.class), any());
        verify(docxAnchorExtractor, never()).extractOrderedAnchorIds(any());
    }

    @Test
    void replaceFileRejectsWrongMagicBytesWithSameContractAsCreate() {
        // A7: replace shares validateDocxFile — wrong magic → docxCorrupt, no storage
        UUID masterId = UUID.randomUUID();
        MasterDocumentEntity master = new MasterDocumentEntity(
                masterId, "RETAIL", "Retail Letter Master", "Sample",
                "masters/" + masterId + "/file.docx", "file.docx", "admin"
        );
        master.setStatus(MasterDocumentStatus.DRAFT);
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(masterId)).thenReturn(Optional.of(master));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "evil.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "plain-text-masquerade".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> service.replaceFile(masterId, file, GLOBAL_ADMIN))
                .isInstanceOf(MasterValidationException.class)
                .extracting(ex -> ((MasterValidationException) ex).messageKey())
                .isEqualTo("api.error.master.docxCorrupt");

        verify(objectStoragePort, never()).put(any(), any(), any(Long.class), any());
        verify(docxAnchorExtractor, never()).extractOrderedAnchorIds(any());
    }

    @Test
    void replaceFileRejectsOversizedDocxWithSameContractAsCreate() {
        UUID masterId = UUID.randomUUID();
        MasterDocumentEntity master = new MasterDocumentEntity(
                masterId, "RETAIL", "Retail Letter Master", "Sample",
                "masters/" + masterId + "/file.docx", "file.docx", "admin"
        );
        master.setStatus(MasterDocumentStatus.DRAFT);
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(masterId)).thenReturn(Optional.of(master));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "master.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[4097]
        );

        assertThatThrownBy(() -> service.replaceFile(masterId, file, GLOBAL_ADMIN))
                .isInstanceOf(MasterValidationException.class)
                .extracting(ex -> ((MasterValidationException) ex).messageKey())
                .isEqualTo("api.error.master.docxTooLarge");

        verify(objectStoragePort, never()).put(any(), any(), any(Long.class), any());
        verify(docxAnchorExtractor, never()).extractOrderedAnchorIds(any());
    }

    private CreateMasterRequest request() {
        return new CreateMasterRequest("RETAIL", "Retail Letter Master", "Sample");
    }

    private byte[] zipBytes(Set<String> entries) throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            for (String entry : entries) {
                zipOutputStream.putNextEntry(new ZipEntry(entry));
                zipOutputStream.write("x".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zipOutputStream.closeEntry();
            }
            zipOutputStream.finish();
            return outputStream.toByteArray();
        }
    }
}
