package com.bank.docgen.master.service;

import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.api.CreateMasterRequest;
import com.bank.docgen.master.api.DecideMasterReviewRequest;
import com.bank.docgen.master.api.MasterDocumentDetailView;
import com.bank.docgen.master.api.MasterDocumentSummaryView;
import com.bank.docgen.master.api.MasterImpactAnalysisView;
import com.bank.docgen.master.api.SubmitMasterReviewRequest;
import com.bank.docgen.master.api.UpdateMasterRequest;
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
import java.io.InputStream;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MasterDocumentService {

    private static final long DEFAULT_MAX_DOCX_UPLOAD_BYTES = 50L * 1024L * 1024L;

    private final ObjectStoragePort objectStoragePort;
    private final MasterDocumentAccessSupport access;
    private final MasterDocumentViewSupport views;
    private final MasterDocumentCatalogSupport catalog;
    private final MasterDocumentReviewSupport reviews;
    private final MasterDocumentFileMutationSupport fileMutations;
    private final GroupAccessService groupAccessService;
    private final MasterImpactAnalysisService impactAnalysisService;

    public MasterDocumentService(
            MasterDocumentRepository masterDocumentRepository,
            MasterAnchorRepository masterAnchorRepository,
            MasterReviewRecordRepository masterReviewRecordRepository,
            MasterRevisionLineRepository masterRevisionLineRepository,
            ObjectStoragePort objectStoragePort,
            DocxAnchorExtractor docxAnchorExtractor,
            GroupAccessService groupAccessService,
            ManagementUserDisplayService managementUserDisplayService,
            SelfApprovalGuard selfApprovalGuard,
            ObjectMapper objectMapper,
            MasterImpactAnalysisService impactAnalysisService,
            @Value("${docgen.master.max-docx-upload-bytes:" + DEFAULT_MAX_DOCX_UPLOAD_BYTES + "}") long maxDocxUploadBytes
    ) {
        this.objectStoragePort = objectStoragePort;
        this.groupAccessService = groupAccessService;
        this.impactAnalysisService = impactAnalysisService;
        MasterDocxUploadSupport docxUploadSupport = new MasterDocxUploadSupport(
                objectStoragePort,
                docxAnchorExtractor,
                maxDocxUploadBytes
        );
        this.access = new MasterDocumentAccessSupport(masterDocumentRepository, groupAccessService);
        this.views = new MasterDocumentViewSupport(
                masterAnchorRepository,
                masterReviewRecordRepository,
                managementUserDisplayService
        );
        MasterRevisionPersistSupport revisions = new MasterRevisionPersistSupport(masterRevisionLineRepository);
        this.catalog = new MasterDocumentCatalogSupport(
                masterDocumentRepository, groupAccessService, access, views);
        this.reviews = new MasterDocumentReviewSupport(
                masterReviewRecordRepository,
                masterRevisionLineRepository,
                docxUploadSupport,
                access,
                views,
                selfApprovalGuard);
        this.fileMutations = new MasterDocumentFileMutationSupport(
                masterDocumentRepository,
                masterRevisionLineRepository,
                docxUploadSupport,
                access,
                views,
                revisions,
                objectMapper
        );
    }

    @Transactional(readOnly = true)
    public PageView<MasterDocumentSummaryView> list(ManagementSessionClaims session) {
        return list(session, null, null, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public PageView<MasterDocumentSummaryView> list(
            ManagementSessionClaims session,
            Integer page,
            Integer size,
            String search,
            String groupCode,
            String status,
            String sort
    ) {
        return catalog.list(session, page, size, search, groupCode, status, sort);
    }

    @Transactional(readOnly = true)
    public MasterDocumentDetailView get(UUID masterId, ManagementSessionClaims session) {
        MasterDocumentEntity master = access.requireReadableMasterWithAnchors(masterId, session);
        return views.toDetail(master);
    }

    @Transactional(readOnly = true)
    public MasterDownloadArtifact openDownload(UUID masterId, ManagementSessionClaims session) {
        MasterDocumentEntity master = access.requireReadableMaster(masterId, session);
        try {
            InputStream stream = objectStoragePort.get(master.getStorageKey());
            return new MasterDownloadArtifact(
                    stream,
                    master.getOriginalFilename(),
                    MasterDocxUploadSupport.DOCX_CONTENT_TYPE
            );
        } catch (Exception ex) {
            throw new MasterValidationException("api.error.master.downloadFailed");
        }
    }

    @Transactional
    public MasterDocumentDetailView replaceFile(
            UUID masterId,
            MultipartFile docxFile,
            ManagementSessionClaims session
    ) {
        return fileMutations.replaceFile(masterId, docxFile, session);
    }

    @Transactional
    public MasterDocumentDetailView create(
            CreateMasterRequest request,
            MultipartFile docxFile,
            ManagementSessionClaims session
    ) {
        return fileMutations.create(request, docxFile, session);
    }

    /**
     * SYS-NORM Wave 7 / PP-C6 — materialize a letterhead/master from promotion pack DOCX.
     * Landing status is always {@link MasterDocumentStatus#DRAFT}; never APPROVED via pack.
     */
    @Transactional
    public MasterDocumentDetailView materializeDraftFromImport(
            String groupCode,
            String name,
            String description,
            byte[] docxBytes,
            ManagementSessionClaims session
    ) {
        if (docxBytes == null || docxBytes.length == 0) {
            throw new MasterValidationException("api.error.master.docxRequired");
        }
        MultipartFile docxFile = new ByteArrayMultipartFile(
                "file",
                "imported-master.docx",
                MasterDocxUploadSupport.DOCX_CONTENT_TYPE,
                docxBytes
        );
        MasterDocumentDetailView created = fileMutations.create(
                new CreateMasterRequest(groupCode, name, description),
                docxFile,
                session
        );
        if (!MasterDocumentStatus.DRAFT.name().equals(created.status())) {
            throw new MasterValidationException("api.error.master.invalidState");
        }
        return created;
    }

    @Transactional
    public MasterDocumentDetailView updateMetadata(
            UUID masterId,
            UpdateMasterRequest request,
            ManagementSessionClaims session
    ) {
        MasterDocumentEntity master = access.requireWritableMasterWithAnchors(masterId, session);
        if (request.name() != null && !request.name().isBlank()) {
            master.setName(request.name());
        }
        if (request.description() != null) {
            master.setDescription(request.description());
        }
        master.setUpdatedBy(session.username());
        return views.toDetail(master);
    }

    @Transactional
    public MasterDocumentDetailView submitReview(
            UUID masterId,
            SubmitMasterReviewRequest request,
            ManagementSessionClaims session
    ) {
        return reviews.submitReview(masterId, request, session);
    }

    @Transactional
    public MasterDocumentDetailView decideReview(
            UUID masterId,
            DecideMasterReviewRequest request,
            ManagementSessionClaims session
    ) {
        return reviews.decideReview(masterId, request, session);
    }

    @Transactional(readOnly = true)
    public MasterImpactAnalysisView impactAnalysis(UUID masterId, ManagementSessionClaims session) {
        return impactAnalysisService.impactAnalysis(masterId, session);
    }

    public record MasterDownloadArtifact(InputStream contentStream, String filename, String contentType)
            implements AutoCloseable {
        @Override
        public void close() throws java.io.IOException {
            contentStream.close();
        }
    }
}
