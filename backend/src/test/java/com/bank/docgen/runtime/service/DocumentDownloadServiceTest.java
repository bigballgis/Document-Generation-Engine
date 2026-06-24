package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.runtime.persistence.GenerationIdempotencyEntity;
import com.bank.docgen.runtime.persistence.GenerationIdempotencyRepository;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventEntity;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventRepository;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private TemplateRepository templateRepository;
    @Mock
    private RuntimeGenerationAuditEventRepository runtimeGenerationAuditEventRepository;

    private DocumentDownloadService service;
    private RuntimeSessionClaims session;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        RuntimeGenerationAuditRecorder auditRecorder = new RuntimeGenerationAuditRecorder(
                runtimeGenerationAuditEventRepository,
                new TraceIdProvider()
        );
        service = new DocumentDownloadService(
                generationIdempotencyRepository,
                objectStoragePort,
                templateRepository,
                auditRecorder,
                new TraceIdProvider()
        );
        session = new RuntimeSessionClaims(
                UUID.randomUUID(),
                "CRED-1",
                TEMPLATE_ID,
                "TPL-001",
                "svc-caller",
                List.of("grp-a")
        );
        request = new MockHttpServletRequest("GET", "/api/dev/v1/documents/" + DOCUMENT_ID + "/download");
    }

    @Test
    void crossTemplateAccessDenied() {
        GenerationIdempotencyEntity record = completedRecord(UUID.randomUUID(), "generated/DOC-1/output.docx");
        when(generationIdempotencyRepository.findByDocumentId(DOCUMENT_ID)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.resolveDownload(DOCUMENT_ID, "dev", session, request))
                .isInstanceOf(RuntimeAccessDeniedException.class);
    }

    @Test
    void expiredDownloadRejected() {
        GenerationIdempotencyEntity record = completedRecord(TEMPLATE_ID, "generated/DOC-1/output.docx");
        record.markDownloadExpired(Instant.now().minusSeconds(30));
        when(generationIdempotencyRepository.findByDocumentId(DOCUMENT_ID)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.resolveDownload(DOCUMENT_ID, "dev", session, request))
                .isInstanceOf(RuntimeDownloadExpiredException.class);
    }

    @Test
    void validDownloadReturnsStreamMetadataAndPersistsAudit() throws Exception {
        when(templateRepository.findByIdAndDeletedAtIsNull(TEMPLATE_ID)).thenReturn(Optional.of(templateEntity()));
        GenerationIdempotencyEntity record = completedRecord(TEMPLATE_ID, "generated/DOC-1/output.docx");
        ByteArrayInputStream stream = new ByteArrayInputStream(new byte[]{1, 2, 3});
        when(generationIdempotencyRepository.findByDocumentId(DOCUMENT_ID)).thenReturn(Optional.of(record));
        when(objectStoragePort.get("generated/DOC-1/output.docx")).thenReturn(stream);

        try (DocumentDownloadService.DownloadArtifact artifact =
                service.resolveDownload(DOCUMENT_ID, "dev", session, request)) {
            assertThat(artifact.contentStream()).isSameAs(stream);
            assertThat(artifact.contentStream().readAllBytes()).containsExactly(1, 2, 3);
            assertThat(artifact.contentType())
                    .isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            assertThat(artifact.documentId()).isEqualTo(DOCUMENT_ID);
            assertThat(artifact.auditId()).startsWith("AUD-");
        }

        ArgumentCaptor<RuntimeGenerationAuditEventEntity> captor =
                ArgumentCaptor.forClass(RuntimeGenerationAuditEventEntity.class);
        verify(runtimeGenerationAuditEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType())
                .isEqualTo(RuntimeGenerationAuditRecorder.EVENT_DOCUMENT_DOWNLOAD);
        assertThat(captor.getValue().getEnvironment()).isEqualTo("dev");
    }

    @Test
    void pdfDownloadUsesPdfContentType() throws Exception {
        when(templateRepository.findByIdAndDeletedAtIsNull(TEMPLATE_ID)).thenReturn(Optional.of(templateEntity()));
        GenerationIdempotencyEntity record = completedRecord(TEMPLATE_ID, "generated/DOC-1/output.pdf");
        when(generationIdempotencyRepository.findByDocumentId(DOCUMENT_ID)).thenReturn(Optional.of(record));
        when(objectStoragePort.get("generated/DOC-1/output.pdf")).thenReturn(new ByteArrayInputStream(new byte[]{9}));

        try (DocumentDownloadService.DownloadArtifact artifact =
                service.resolveDownload(DOCUMENT_ID, "dev", session, request)) {
            assertThat(artifact.contentType()).isEqualTo("application/pdf");
        }
    }

    private TemplateEntity templateEntity() {
        return new TemplateEntity(
                TEMPLATE_ID,
                "TPL-001",
                "RETAIL",
                "Sample",
                null,
                UUID.randomUUID(),
                "10000001"
        );
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
}
