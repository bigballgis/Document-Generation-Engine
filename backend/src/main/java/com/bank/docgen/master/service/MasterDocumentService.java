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
import com.bank.docgen.master.api.MasterAnchorView;
import com.bank.docgen.master.api.MasterDocumentDetailView;
import com.bank.docgen.master.api.MasterDocumentSummaryView;
import com.bank.docgen.master.api.MasterImpactAnalysisView;
import com.bank.docgen.master.api.MasterReviewRecordView;
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
import com.bank.docgen.master.persistence.MasterRevisionLineAnchorEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.master.rendering.DocxAnchorExtractor;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MasterDocumentService {

    private static final long DEFAULT_MAX_DOCX_UPLOAD_BYTES = 50L * 1024L * 1024L;

    private final MasterDocumentRepository masterDocumentRepository;
    private final MasterAnchorRepository masterAnchorRepository;
    private final MasterReviewRecordRepository masterReviewRecordRepository;
    private final MasterRevisionLineRepository masterRevisionLineRepository;
    private final ObjectStoragePort objectStoragePort;
    private final GroupAccessService groupAccessService;
    private final ManagementUserDisplayService managementUserDisplayService;
    private final MasterDocxUploadSupport docxUploadSupport;

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
        this.masterAnchorRepository = masterAnchorRepository;
        this.masterReviewRecordRepository = masterReviewRecordRepository;
        this.masterRevisionLineRepository = masterRevisionLineRepository;
        this.objectStoragePort = objectStoragePort;
        this.groupAccessService = groupAccessService;
        this.managementUserDisplayService = managementUserDisplayService;
        this.docxUploadSupport = new MasterDocxUploadSupport(
                objectStoragePort,
                docxAnchorExtractor,
                maxDocxUploadBytes
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
        int safePage = CatalogPageSupport.normalizePage(page);
        int safeSize = CatalogPageSupport.normalizeSize(size);
        List<String> groupCodes = groupAccessService.accessibleGroupCodes(session);
        if (groupCodes.isEmpty()) {
            return new PageView<>(List.of(), safePage, safeSize, 0, 0);
        }

        boolean allGroups = groupCodes.contains("*");
        String groupFilter = CatalogPageSupport.blankToNull(groupCode);
        if (groupFilter != null && !groupAccessService.canAccessGroup(session, groupFilter)) {
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
        Map<UUID, Long> anchorCounts = loadAnchorCounts(masterPage.content());
        List<MasterDocumentSummaryView> content = enrichMasterSummaries(masterPage.content().stream()
                .map(master -> toSummary(master, anchorCounts.getOrDefault(master.getId(), 0L)))
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
        MasterDocumentEntity master = requireReadableMasterWithAnchors(masterId, session);
        return toDetail(master);
    }

    @Transactional(readOnly = true)
    public MasterDownloadArtifact openDownload(UUID masterId, ManagementSessionClaims session) {
        MasterDocumentEntity master = requireReadableMaster(masterId, session);
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
        MasterDocumentEntity master = requireWritableMaster(masterId, session);
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
        List<MasterAnchorEntity> anchorEntities = toAnchorEntities(masterId, anchorIds);
        MasterRevisionLineEntity currentLine = persistRevisionLine(
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
        return toDetail(master);
    }

    @Transactional
    public MasterDocumentDetailView create(
            CreateMasterRequest request,
            MultipartFile docxFile,
            ManagementSessionClaims session
    ) {
        assertGroupWritable(session, request.groupCode());
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
        List<MasterAnchorEntity> anchorEntities = toAnchorEntities(masterId, anchorIds);
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
        persistRevisionLine(
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
        return toDetail(master);
    }

    @Transactional
    public MasterDocumentDetailView updateMetadata(
            UUID masterId,
            UpdateMasterRequest request,
            ManagementSessionClaims session
    ) {
        MasterDocumentEntity master = requireWritableMasterWithAnchors(masterId, session);
        if (request.name() != null && !request.name().isBlank()) {
            master.setName(request.name());
        }
        if (request.description() != null) {
            master.setDescription(request.description());
        }
        master.setUpdatedBy(session.username());
        return toDetail(master);
    }

    @Transactional
    public MasterDocumentDetailView submitReview(
            UUID masterId,
            SubmitMasterReviewRequest request,
            ManagementSessionClaims session
    ) {
        MasterDocumentEntity master = requireWritableMasterWithAnchors(masterId, session);
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
        return toDetail(master);
    }

    @Transactional
    public MasterDocumentDetailView decideReview(
            UUID masterId,
            DecideMasterReviewRequest request,
            ManagementSessionClaims session
    ) {
        if (!groupAccessService.canReviewMasters(session)) {
            throw new MasterAccessDeniedException();
        }
        MasterDocumentEntity master = requireReadableMasterWithAnchors(masterId, session);
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
        return toDetail(master);
    }

    @Transactional(readOnly = true)
    public MasterImpactAnalysisView impactAnalysis(UUID masterId, ManagementSessionClaims session) {
        requireReadableMaster(masterId, session);
        return new MasterImpactAnalysisView(masterId.toString(), List.of(), false);
    }

    private MasterDocumentEntity requireReadableMaster(UUID masterId, ManagementSessionClaims session) {
        MasterDocumentEntity master = masterDocumentRepository.findByIdAndDeletedAtIsNull(masterId)
                .orElseThrow(MasterNotFoundException::new);
        if (!groupAccessService.canAccessGroup(session, master.getGroupCode())) {
            throw new MasterAccessDeniedException();
        }
        return master;
    }

    private MasterDocumentEntity requireReadableMasterWithAnchors(UUID masterId, ManagementSessionClaims session) {
        MasterDocumentEntity master = masterDocumentRepository.findWithAnchorsByIdAndDeletedAtIsNull(masterId)
                .orElseThrow(MasterNotFoundException::new);
        if (!groupAccessService.canAccessGroup(session, master.getGroupCode())) {
            throw new MasterAccessDeniedException();
        }
        return master;
    }

    private MasterDocumentEntity requireWritableMaster(UUID masterId, ManagementSessionClaims session) {
        MasterDocumentEntity master = requireReadableMaster(masterId, session);
        if (!groupAccessService.canManageMasters(session)) {
            throw new MasterAccessDeniedException();
        }
        return master;
    }

    private MasterDocumentEntity requireWritableMasterWithAnchors(UUID masterId, ManagementSessionClaims session) {
        MasterDocumentEntity master = requireReadableMasterWithAnchors(masterId, session);
        if (!groupAccessService.canManageMasters(session)) {
            throw new MasterAccessDeniedException();
        }
        return master;
    }

    private void assertGroupWritable(ManagementSessionClaims session, String groupCode) {
        if (!groupAccessService.canManageMasters(session)
                || !groupAccessService.canAccessGroup(session, groupCode)) {
            throw new MasterAccessDeniedException();
        }
    }

    private List<MasterAnchorEntity> toAnchorEntities(UUID masterId, List<String> anchorIds) {
        List<MasterAnchorEntity> anchors = new ArrayList<>();
        for (int sequence = 0; sequence < anchorIds.size(); sequence++) {
            String anchorId = anchorIds.get(sequence);
            anchors.add(new MasterAnchorEntity(masterId, anchorId, anchorId, sequence));
        }
        return anchors;
    }

    private MasterDocumentSummaryView toSummary(MasterDocumentEntity master, long anchorCount) {
        return new MasterDocumentSummaryView(
                master.getId().toString(),
                master.getGroupCode(),
                master.getName(),
                master.getStatus().name(),
                master.getOriginalFilename(),
                Math.toIntExact(anchorCount),
                master.getUpdatedBy(),
                master.getUpdatedAt(),
                null
        );
    }

    private List<MasterDocumentSummaryView> enrichMasterSummaries(List<MasterDocumentSummaryView> summaries) {
        if (summaries.isEmpty()) {
            return summaries;
        }
        Set<String> usernames = summaries.stream()
                .map(MasterDocumentSummaryView::updatedBy)
                .filter(username -> username != null && !username.isBlank())
                .collect(Collectors.toSet());
        Map<String, String> displayNames = managementUserDisplayService.lookupDisplayNames(usernames);
        return summaries.stream()
                .map(summary -> new MasterDocumentSummaryView(
                        summary.id(),
                        summary.groupCode(),
                        summary.name(),
                        summary.status(),
                        summary.originalFilename(),
                        summary.anchorCount(),
                        summary.updatedBy(),
                        summary.updatedAt(),
                        summary.updatedBy() == null ? null : displayNames.get(summary.updatedBy())
                ))
                .toList();
    }

    private Map<UUID, Long> loadAnchorCounts(List<MasterDocumentEntity> masters) {
        if (masters.isEmpty()) {
            return Map.of();
        }
        List<UUID> masterIds = masters.stream().map(MasterDocumentEntity::getId).toList();
        return masterAnchorRepository.countByMasterIdIn(masterIds).stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
    }

    private MasterDocumentDetailView toDetail(MasterDocumentEntity master) {
        List<MasterReviewRecordEntity> reviewRecords =
                masterReviewRecordRepository.findByMasterIdOrderByCreatedAtDesc(master.getId());
        return new MasterDocumentDetailView(
                master.getId().toString(),
                master.getGroupCode(),
                master.getName(),
                master.getDescription(),
                master.getStatus().name(),
                master.getOriginalFilename(),
                master.getChangeSummary(),
                master.getAnchors().stream()
                        .map(anchor -> new MasterAnchorView(anchor.getAnchorId(), anchor.getDisplayLabel()))
                        .toList(),
                reviewRecords.stream()
                        .map(record -> new MasterReviewRecordView(
                                record.getAction().name(),
                                record.getDecision(),
                                record.getChangeSummary(),
                                record.getCommentSummary(),
                                record.getActorUsername(),
                                record.getCreatedAt()))
                        .toList(),
                master.getCreatedBy(),
                master.getUpdatedBy(),
                master.getCreatedAt(),
                master.getUpdatedAt()
        );
    }

    private MasterRevisionLineEntity persistRevisionLine(
            UUID revisionLineId,
            UUID masterId,
            String storageKey,
            String originalFilename,
            List<MasterAnchorEntity> anchors,
            MasterDocumentStatus statusSnapshot,
            int revisionSequence,
            boolean current,
            String changeSummary,
            String actor
    ) {
        MasterRevisionLineEntity line = new MasterRevisionLineEntity(
                revisionLineId,
                masterId,
                storageKey,
                originalFilename,
                anchors.size(),
                statusSnapshot,
                revisionSequence,
                current,
                changeSummary,
                actor
        );
        List<MasterRevisionLineAnchorEntity> snapshotAnchors = anchors.stream()
                .map(anchor -> new MasterRevisionLineAnchorEntity(
                        revisionLineId,
                        anchor.getAnchorId(),
                        anchor.getDisplayLabel(),
                        anchor.getDocumentSequence()))
                .toList();
        line.replaceAnchors(snapshotAnchors);
        line.getAnchors().forEach(anchor -> anchor.setRevisionLine(line));
        return masterRevisionLineRepository.save(line);
    }

    public record MasterDownloadArtifact(InputStream contentStream, String filename, String contentType)
            implements AutoCloseable {
        @Override
        public void close() throws java.io.IOException {
            contentStream.close();
        }
    }
}
