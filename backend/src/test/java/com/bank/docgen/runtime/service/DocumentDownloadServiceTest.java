package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.service.SecurityAuditSummaryService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.runtime.persistence.GenerationIdempotencyEntity;
import com.bank.docgen.runtime.persistence.GenerationIdempotencyRepository;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class DocumentDownloadServiceTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final String DOCUMENT_ID = "DOC-12345678";

    @Mock
    private GenerationIdempotencyRepository generationIdempotencyRepository;
    @Mock
    private ObjectStoragePort objectStoragePort;
    @Mock
    private SecurityAuditSummaryService securityAuditSummaryService;

    private DocumentDownloadService service;
    private RuntimeSessionClaims session;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        service = new DocumentDownloadService(
                generationIdempotencyRepository,
                objectStoragePort,
                securityAuditSummaryService,
                new TraceIdProvider()
        );
        session = new RuntimeSessionClaims(
                UUID.randomUUID(),
                "CRED-1",
                TEMPLATE_ID,
                "TPL-001",
                "svc-caller",
                java.util.List.of("grp-a")
        );
        request = new MockHttpServletRequest("GET", "/api/dev/v1/documents/" + DOCUMENT_ID + "/download");
    }

    @Test
    void crossTemplateAccessDenied() {
        GenerationIdempotencyEntity record = completedRecord(UUID.randomUUID(), "generated/DOC-1/output.docx");
        when(generationIdempotencyRepository.findByDocumentId(DOCUMENT_ID)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.resolveDownload(DOCUMENT_ID, session, request))
                .isInstanceOf(RuntimeAccessDeniedException.class);
    }

    @Test
    void expiredDownloadRejected() {
        GenerationIdempotencyEntity record = completedRecord(TEMPLATE_ID, "generated/DOC-1/output.docx");
        record.markDownloadExpired(Instant.now().minusSeconds(30));
        when(generationIdempotencyRepository.findByDocumentId(DOCUMENT_ID)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.resolveDownload(DOCUMENT_ID, session, request))
                .isInstanceOf(RuntimeDownloadExpiredException.class);
    }

    @Test
    void validDownloadReturnsStreamMetadataAndRecordsAudit() throws Exception {
        GenerationIdempotencyEntity record = completedRecord(TEMPLATE_ID, "generated/DOC-1/output.docx");
        ByteArrayInputStream stream = new ByteArrayInputStream(new byte[]{1, 2, 3});
        when(generationIdempotencyRepository.findByDocumentId(DOCUMENT_ID)).thenReturn(Optional.of(record));
        when(objectStoragePort.get("generated/DOC-1/output.docx")).thenReturn(stream);

        try (DocumentDownloadService.DownloadArtifact artifact =
                service.resolveDownload(DOCUMENT_ID, session, request)) {
            assertThat(artifact.contentStream()).isSameAs(stream);
            assertThat(artifact.contentStream().readAllBytes()).containsExactly(1, 2, 3);
            assertThat(artifact.contentType())
                    .isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            assertThat(artifact.documentId()).isEqualTo(DOCUMENT_ID);
            assertThat(artifact.auditId()).startsWith("AUD-");
        }

        verify(securityAuditSummaryService).recordDocumentDownload(
                eq("CRED-1"),
                eq("svc-caller"),
                eq(DOCUMENT_ID),
                eq("TPL-001"),
                any(),
                any()
        );
    }

    @Test
    void pdfDownloadUsesPdfContentType() throws Exception {
        GenerationIdempotencyEntity record = completedRecord(TEMPLATE_ID, "generated/DOC-1/output.pdf");
        when(generationIdempotencyRepository.findByDocumentId(DOCUMENT_ID)).thenReturn(Optional.of(record));
        when(objectStoragePort.get("generated/DOC-1/output.pdf")).thenReturn(new ByteArrayInputStream(new byte[]{9}));

        try (DocumentDownloadService.DownloadArtifact artifact =
                service.resolveDownload(DOCUMENT_ID, session, request)) {
            assertThat(artifact.contentType()).isEqualTo("application/pdf");
        }
    }

    @Test
    void resolveDownloadDoesNotLoadEntireArtifactBeforeReturning() throws Exception {
        GenerationIdempotencyEntity record = completedRecord(TEMPLATE_ID, "generated/DOC-1/output.pdf");
        TrackingInputStream stream = new TrackingInputStream();
        when(generationIdempotencyRepository.findByDocumentId(DOCUMENT_ID)).thenReturn(Optional.of(record));
        when(objectStoragePort.get("generated/DOC-1/output.pdf")).thenReturn(stream);

        try (DocumentDownloadService.DownloadArtifact artifact =
                service.resolveDownload(DOCUMENT_ID, session, request)) {
            assertThat(artifact.contentStream()).isSameAs(stream);
            assertThat(stream.readCount()).isZero();
        }
    }

    private GenerationIdempotencyEntity completedRecord(UUID templateId, String storageKey) {
        GenerationIdempotencyEntity record = new GenerationIdempotencyEntity(
                UUID.randomUUID(),
                "idem-key",
                templateId,
                "hash",
                "COMPLETED",
                Instant.now().plusSeconds(3600)
        );
        record.complete(storageKey, DOCUMENT_ID);
        return record;
    }

    private static final class TrackingInputStream extends InputStream {

        private int readCount;

        @Override
        public int read() throws IOException {
            readCount++;
            return -1;
        }

        int readCount() {
            return readCount;
        }
    }
}
