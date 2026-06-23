package com.bank.docgen.runtime.service;

import com.bank.docgen.authorization.management.service.SecurityAuditSummaryService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.runtime.persistence.GenerationIdempotencyEntity;
import com.bank.docgen.runtime.persistence.GenerationIdempotencyRepository;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentDownloadService {

    private final GenerationIdempotencyRepository generationIdempotencyRepository;
    private final ObjectStoragePort objectStoragePort;
    private final SecurityAuditSummaryService securityAuditSummaryService;
    private final TraceIdProvider traceIdProvider;

    public DocumentDownloadService(
            GenerationIdempotencyRepository generationIdempotencyRepository,
            ObjectStoragePort objectStoragePort,
            SecurityAuditSummaryService securityAuditSummaryService,
            TraceIdProvider traceIdProvider
    ) {
        this.generationIdempotencyRepository = generationIdempotencyRepository;
        this.objectStoragePort = objectStoragePort;
        this.securityAuditSummaryService = securityAuditSummaryService;
        this.traceIdProvider = traceIdProvider;
    }

    @Transactional(readOnly = true)
    public DownloadArtifact resolveDownload(String documentId, RuntimeSessionClaims session, HttpServletRequest request) {
        GenerationIdempotencyEntity record = generationIdempotencyRepository.findByDocumentId(documentId)
                .filter(entry -> "COMPLETED".equals(entry.getStatus()))
                .filter(entry -> entry.getResponseStorageKey() != null)
                .orElseThrow(RuntimeDocumentNotFoundException::new);

        if (!record.getTemplateId().equals(session.templateId())) {
            throw new RuntimeAccessDeniedException();
        }
        if (record.getDownloadExpiresAt() == null || !record.getDownloadExpiresAt().isAfter(Instant.now())) {
            throw new RuntimeDownloadExpiredException();
        }

        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        securityAuditSummaryService.recordDocumentDownload(
                session.credentialExternalId(),
                session.accessAccount(),
                documentId,
                session.templateExternalId(),
                auditId,
                traceId
        );

        try {
            InputStream stream = objectStoragePort.get(record.getResponseStorageKey());
            return new DownloadArtifact(
                    stream,
                    ArtifactContentTypes.fromStorageKey(record.getResponseStorageKey()),
                    documentId,
                    auditId,
                    traceId,
                    record.getDownloadExpiresAt()
            );
        } catch (RuntimeException ex) {
            throw new RuntimeDocumentNotFoundException();
        }
    }

    public record DownloadArtifact(
            InputStream contentStream,
            String contentType,
            String documentId,
            String auditId,
            String traceId,
            Instant downloadExpiresAt
    ) implements AutoCloseable {

        @Override
        public void close() throws IOException {
            contentStream.close();
        }
    }
}
