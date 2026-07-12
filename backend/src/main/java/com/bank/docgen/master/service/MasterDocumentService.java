package com.bank.docgen.master.service;

import com.bank.docgen.authorization.management.api.CatalogPageSupport;
import com.bank.docgen.authorization.management.api.CatalogQueryPage;
import com.bank.docgen.authorization.management.api.CatalogSortKey;
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
import com.bank.docgen.master.domain.MasterReviewAction;
import com.bank.docgen.master.persistence.MasterAnchorEntity;
import com.bank.docgen.master.persistence.MasterAnchorRepository;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterDocumentRepositoryCustom.MasterCatalogFilter;
import com.bank.docgen.master.persistence.MasterReviewRecordEntity;
import com.bank.docgen.master.persistence.MasterReviewRecordRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.master.rendering.DocxAnchorExtractor;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MasterDocumentService {

    private static final long DEFAULT_MAX_DOCX_UPLOAD_BYTES = 50L * 1024L * 1024L;

    private final MasterDocumentRepository masterDocumentRepository;
    private final MasterReviewRecordRepository masterReviewRecordRepository;
    private final MasterRevisionLineRepository masterRevisionLineRepository;
    private final ObjectStoragePort objectStoragePort;
    private final GroupAccessService groupAccessService;
    private final MasterDocxUploadSupport docxUploadSupport;
    private final MasterDocumentAccessSupport access;
    private final MasterDocumentViewSupport views;
    private final MasterRevisionPersistSupport revisions;

    public MasterDocumentService(
            MasterDocumentRepository masterDocumentRepository,
            MasterAnchorRepository masterAnchorRepository,
            MasterReviewRecordRepository masterReviewRecordRepository,
            MasterRevisionLineRepository masterRevisionLineRepository,
            ObjectStoragePort objectStoragePort,
            DocxAnchorExtractor docxAnchorExtractor,
            GroupAccessService groupAccessService,
            ManagementUserDisplayService managementUserDisplayService,
            @Value("${docgen.master.max-docx-upload-bytes:" + DEFAULT_MAX_DOCX_UPLOAD_BYTES + "}") long maxDocxUploadBytes
    ) {
        this.masterDocumentRepository = masterDocumentRepository;
        this.masterReviewRecordRepository = masterReviewRecordRepository;
        this.masterRevisionLineRepository = masterRevisionLineRepository;
        this.objectStoragePort = objectStoragePort;
        this.groupAccessService = groupAccessService;
        this.docxUploadSupport = new MasterDocxUploadSupport(
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
        this.revisions = new MasterRevisionPersistSupport(masterRevisionLineRepository);
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
        int safePage = CatalogPageSupport.normalizePage(page);
        int safeSize = CatalogPageSupport.normalizeSize(size);
        List<String> groupCodes = groupAccessService.accessibleGroupCodes(session);
        if (groupCodes.isEmpty()) {
            return new PageView<>(List.of(), safePage, safeSize, 0, 0);
        }

        boolean allGroups = groupCodes.contains("*");
        String groupFilter = CatalogPageSupport.blankToNull(groupCode);
        if (groupFilter != null && !access.canAccessGroup(session, groupFilter)) {
            return new PageView<>(List.of(), safePage, safeSize, 0, 0);
        }

        MasterDocumentStatus statusFilter = parseStatus(status);
        if (status != null && !status.isBlank() && statusFilter == null) {
            return new PageView<>(List.of(), safePage, safeSize, 0, 0);
        }

        CatalogSortKey sortKey = CatalogSortKey.parse(sort);
        MasterCatalogFilter filter = new MasterCatalogFilter(
                allGroups ? List.of() : List.copyOf(groupCodes),
                allGroups,
                groupFilter,
                CatalogPageSupport.blankToNull(search),
                statusFilter,
                sortKey
        );
        CatalogQueryPage<MasterDocumentEntity> masterPage =
                masterDocumentRepository.searchCatalog(filter, safePage, safeSize);
        Map<UUID, Long> anchorCounts = views.loadAnchorCounts(masterPage.content());
        List<MasterDocumentSummaryView> content = views.enrichMasterSummaries(masterPage.content().stream()
                .map(master -> views.toSummary(master, anchorCounts.getOrDefault(master.getId(), 0L)))
                .toList());
        return new PageView<>(
                content,
                safePage,
                safeSize,
                masterPage.totalElements(),
                masterPage.totalPages()
        );
    }

    private static MasterDocumentStatus parseStatus(String raw) {
        String value = CatalogPageSupport.blankToNull(raw);
        if (value == null) {
            return null;
        }
        try {
            return MasterDocumentStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
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
        MasterDocumentEntity master = access.requireWritableMaster(masterId, session);
        if (master.getStatus() == MasterDocumentStatus.PENDING_REVIEW) {
            throw new MasterValidationException("api.error.master.invalidState");
        }
        docxUploadSupport.validateDocxFile(docxFile);
        List<String> anchorIds = docxUploadSupport.extractAnchors(docxFile);
        if (anchorIds.isEmpty()) {
            throw new MasterValidationException("api.error.master.anchorIntegrityFailed");
        }
        masterRevisionLineRepository.findByMasterIdAndCurrentTrueAndDeletedAtIsNull(masterId)
                .ifPresent(previousLine -> {
                    previousLine.markSuperseded();
                    masterRevisionLineRepository.save(previousLine);
                });
        MasterDocumentStatus statusSnapshot = master.getStatus();
        UUID revisionLineId = UUID.randomUUID();
        String revisionStorageKey = docxUploadSupport.revisionStorageKey(
                masterId, revisionLineId, docxFile.getOriginalFilename());
        docxUploadSupport.storeDocx(revisionStorageKey, docxFile);
        int nextSequence = masterRevisionLineRepository.findMaxRevisionSequence(masterId) + 1;
        List<MasterAnchorEntity> anchorEntities = revisions.toAnchorEntities(masterId, anchorIds);
        MasterRevisionLineEntity currentLine = revisions.persistRevisionLine(
                revisionLineId,
                masterId,
                revisionStorageKey,
                docxFile.getOriginalFilename(),
                anchorEntities,
                statusSnapshot,
                nextSequence,
                true,
                master.getChangeSummary(),
                session.username()
        );
        master.setStorageKey(revisionStorageKey);
        master.setOriginalFilename(docxFile.getOriginalFilename());
        master.replaceAnchors(anchorEntities);
        master.getAnchors().forEach(anchor -> anchor.setMaster(master));
        master.setCurrentRevisionLineId(currentLine.getId());
        if (master.getStatus() != MasterDocumentStatus.DRAFT) {
            master.setStatus(MasterDocumentStatus.DRAFT);
            master.setChangeSummary(null);
        }
        master.setUpdatedBy(session.username());
        masterDocumentRepository.save(master);
        return views.toDetail(master);
    }

    @Transactional
    public MasterDocumentDetailView create(
            CreateMasterRequest request,
            MultipartFile docxFile,
            ManagementSessionClaims session
    ) {
        access.assertGroupWritable(session, request.groupCode());
        docxUploadSupport.validateDocxFile(docxFile);
        UUID masterId = UUID.randomUUID();
        UUID revisionLineId = UUID.randomUUID();
        String revisionStorageKey = docxUploadSupport.revisionStorageKey(
                masterId, revisionLineId, docxFile.getOriginalFilename());
        docxUploadSupport.storeDocx(revisionStorageKey, docxFile);
        List<String> anchorIds = docxUploadSupport.extractAnchors(docxFile);
        if (anchorIds.isEmpty()) {
            throw new MasterValidationException("api.error.master.anchorIntegrityFailed");
        }
        List<MasterAnchorEntity> anchorEntities = revisions.toAnchorEntities(masterId, anchorIds);
        MasterDocumentEntity master = new MasterDocumentEntity(
                masterId,
                request.groupCode(),
                request.name(),
                request.description(),
                revisionStorageKey,
                docxFile.getOriginalFilename(),
                session.username()
        );
        master.setCurrentRevisionLineId(revisionLineId);
        master.replaceAnchors(anchorEntities);
        master.getAnchors().forEach(anchor -> anchor.setMaster(master));
        masterDocumentRepository.save(master);
        revisions.persistRevisionLine(
                revisionLineId,
                masterId,
                revisionStorageKey,
                docxFile.getOriginalFilename(),
                anchorEntities,
                MasterDocumentStatus.DRAFT,
                1,
                true,
                null,
                session.username()
        );
        return views.toDetail(master);
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
        MasterDocumentEntity master = access.requireWritableMasterWithAnchors(masterId, session);
        if (master.getStatus() != MasterDocumentStatus.DRAFT) {
            throw new MasterValidationException("api.error.master.invalidReviewTransition");
        }
        docxUploadSupport.assertAnchorIntegrity(master);
        master.setChangeSummary(request.changeSummary());
        master.setStatus(MasterDocumentStatus.PENDING_REVIEW);
        master.setUpdatedBy(session.username());
        masterReviewRecordRepository.save(new MasterReviewRecordEntity(
                UUID.randomUUID(),
                masterId,
                MasterReviewAction.SUBMITTED,
                null,
                request.changeSummary(),
                null,
                session.username()
        ));
        return views.toDetail(master);
    }

    @Transactional
    public MasterDocumentDetailView decideReview(
            UUID masterId,
            DecideMasterReviewRequest request,
            ManagementSessionClaims session
    ) {
        if (!access.canReviewMasters(session)) {
            throw new MasterAccessDeniedException();
        }
        MasterDocumentEntity master = access.requireReadableMasterWithAnchors(masterId, session);
        if (master.getStatus() != MasterDocumentStatus.PENDING_REVIEW) {
            throw new MasterValidationException("api.error.master.invalidReviewTransition");
        }
        MasterDocumentStatus nextStatus = "APPROVED".equals(request.decision())
                ? MasterDocumentStatus.APPROVED
                : MasterDocumentStatus.DRAFT;
        master.setStatus(nextStatus);
        master.setUpdatedBy(session.username());
        masterReviewRecordRepository.save(new MasterReviewRecordEntity(
                UUID.randomUUID(),
                masterId,
                "APPROVED".equals(request.decision()) ? MasterReviewAction.APPROVED : MasterReviewAction.REJECTED,
                request.decision(),
                master.getChangeSummary(),
                request.commentSummary(),
                session.username()
        ));
        return views.toDetail(master);
    }

    @Transactional(readOnly = true)
    public MasterImpactAnalysisView impactAnalysis(UUID masterId, ManagementSessionClaims session) {
        access.requireReadableMaster(masterId, session);
        return new MasterImpactAnalysisView(masterId.toString(), List.of(), false);
    }

    public record MasterDownloadArtifact(InputStream contentStream, String filename, String contentType)
            implements AutoCloseable {
        @Override
        public void close() throws java.io.IOException {
            contentStream.close();
        }
    }
}
