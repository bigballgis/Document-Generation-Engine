package com.bank.docgen.runtime.service;

import com.bank.docgen.authorization.management.service.SecurityAuditSummaryService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.runtime.persistence.GenerationIdempotencyEntity;
import com.bank.docgen.runtime.persistence.GenerationIdempotencyRepository;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.service.TemplateNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentDownloadService {

    private final GenerationIdempotencyRepository generationIdempotencyRepository;
    private final ObjectStoragePort objectStoragePort;
    private final TemplateRepository templateRepository;
    private final RuntimeGenerationAuditRecorder runtimeGenerationAuditRecorder;
    private final TraceIdProvider traceIdProvider;
    private final SecurityAuditSummaryService securityAuditSummaryService;

    public DocumentDownloadService(
            GenerationIdempotencyRepository generationIdempotencyRepository,
            ObjectStoragePort objectStoragePort,
            TemplateRepository templateRepository,
            RuntimeGenerationAuditRecorder runtimeGenerationAuditRecorder,
            TraceIdProvider traceIdProvider,
            SecurityAuditSummaryService securityAuditSummaryService
    ) {
        this.generationIdempotencyRepository = generationIdempotencyRepository;
        this.objectStoragePort = objectStoragePort;
        this.templateRepository = templateRepository;
        this.runtimeGenerationAuditRecorder = runtimeGenerationAuditRecorder;
        this.traceIdProvider = traceIdProvider;
        this.securityAuditSummaryService = securityAuditSummaryService;
    }

    @Transactional(readOnly = true)
    public DownloadArtifact resolveDownload(
            String documentId,
            String environment,
            RuntimeSessionClaims session,
            HttpServletRequest request
    ) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        String groupCode = resolveGroupCode(session.templateId());

        GenerationIdempotencyEntity record = generationIdempotencyRepository.findByDocumentId(documentId)
                .filter(entry -> "COMPLETED".equals(entry.getStatus()))
                .filter(entry -> entry.getResponseStorageKey() != null)
                .orElse(null);

        if (record == null) {
            recordDownloadDenied(
                    session,
                    documentId,
                    groupCode,
                    SecurityAuditSummaryService.REASON_DOWNLOAD_NOT_AVAILABLE,
                    auditId,
                    traceId
            );
            throw new RuntimeDocumentNotFoundException();
        }

        if (!record.getTemplateId().equals(session.templateId())) {
            recordDownloadDenied(
                    session,
                    documentId,
                    groupCode,
                    SecurityAuditSummaryService.REASON_DOWNLOAD_ACCESS_DENIED,
                    auditId,
                    traceId
            );
            throw new RuntimeAccessDeniedException();
        }
        if (record.getDownloadExpiresAt() == null || !record.getDownloadExpiresAt().isAfter(Instant.now())) {
            recordDownloadDenied(
                    session,
                    documentId,
                    groupCode,
                    SecurityAuditSummaryService.REASON_DOWNLOAD_EXPIRED,
                    auditId,
                    traceId
            );
            throw new RuntimeDownloadExpiredException();
        }

        TemplateEntity template = templateRepository.findByIdAndDeletedAtIsNull(session.templateId())
                .orElseThrow(TemplateNotFoundException::new);
        runtimeGenerationAuditRecorder.recordDocumentDownload(
                template,
                session,
                environment,
                documentId,
                auditId,
                traceId
        );
        securityAuditSummaryService.recordDocumentDownload(
                session.credentialExternalId(),
                session.accessAccount(),
                documentId,
                session.templateExternalId(),
                template.getId(),
                template.getGroupCode(),
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
            recordDownloadDenied(
                    session,
                    documentId,
                    template.getGroupCode(),
                    SecurityAuditSummaryService.REASON_DOWNLOAD_NOT_AVAILABLE,
                    auditId,
                    traceId
            );
            throw new RuntimeDocumentNotFoundException();
        }
    }

    private void recordDownloadDenied(
            RuntimeSessionClaims session,
            String documentId,
            String groupCode,
            String reasonCode,
            String auditId,
            String traceId
    ) {
        securityAuditSummaryService.recordDocumentDownloadDenied(
                session.credentialExternalId(),
                session.accessAccount(),
                documentId,
                session.templateExternalId(),
                session.templateId(),
                groupCode,
                reasonCode,
                auditId,
                traceId
        );
    }

    private String resolveGroupCode(UUID templateId) {
        return templateRepository.findByIdAndDeletedAtIsNull(templateId)
                .map(TemplateEntity::getGroupCode)
                .orElse(null);
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
