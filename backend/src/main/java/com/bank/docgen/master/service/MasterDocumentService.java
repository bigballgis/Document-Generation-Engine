package com.bank.docgen.master.service;

import com.bank.docgen.authorization.management.service.GroupAccessService;
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
import com.bank.docgen.master.persistence.MasterReviewRecordEntity;
import com.bank.docgen.master.persistence.MasterReviewRecordRepository;
import com.bank.docgen.master.rendering.DocxAnchorExtractor;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MasterDocumentService {

    private static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private final MasterDocumentRepository masterDocumentRepository;
    private final MasterAnchorRepository masterAnchorRepository;
    private final MasterReviewRecordRepository masterReviewRecordRepository;
    private final ObjectStoragePort objectStoragePort;
    private final DocxAnchorExtractor docxAnchorExtractor;
    private final GroupAccessService groupAccessService;

    public MasterDocumentService(
            MasterDocumentRepository masterDocumentRepository,
            MasterAnchorRepository masterAnchorRepository,
            MasterReviewRecordRepository masterReviewRecordRepository,
            ObjectStoragePort objectStoragePort,
            DocxAnchorExtractor docxAnchorExtractor,
            GroupAccessService groupAccessService
    ) {
        this.masterDocumentRepository = masterDocumentRepository;
        this.masterAnchorRepository = masterAnchorRepository;
        this.masterReviewRecordRepository = masterReviewRecordRepository;
        this.objectStoragePort = objectStoragePort;
        this.docxAnchorExtractor = docxAnchorExtractor;
        this.groupAccessService = groupAccessService;
    }

    @Transactional(readOnly = true)
    public List<MasterDocumentSummaryView> list(ManagementSessionClaims session) {
        List<String> groupCodes = groupAccessService.accessibleGroupCodes(session);
        List<MasterDocumentEntity> masters;
        if (groupCodes.contains("*")) {
            masters = masterDocumentRepository.findByDeletedAtIsNullOrderByUpdatedAtDesc();
        } else if (groupCodes.isEmpty()) {
            return List.of();
        } else {
            masters = masterDocumentRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(groupCodes);
        }
        Map<UUID, Long> anchorCounts = loadAnchorCounts(masters);
        return masters.stream()
                .map(master -> toSummary(master, anchorCounts.getOrDefault(master.getId(), 0L)))
                .toList();
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
            return new MasterDownloadArtifact(stream, master.getOriginalFilename(), DOCX_CONTENT_TYPE);
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
        validateDocxFile(docxFile);
        String storageKey = "masters/" + masterId + "/" + sanitizeFilename(docxFile.getOriginalFilename());
        storeDocx(storageKey, docxFile);
        Set<String> anchorIds = extractAnchors(docxFile);
        if (anchorIds.isEmpty()) {
            throw new MasterValidationException("api.error.master.anchorIntegrityFailed");
        }
        master.setStorageKey(storageKey);
        master.setOriginalFilename(docxFile.getOriginalFilename());
        master.replaceAnchors(toAnchorEntities(masterId, anchorIds));
        master.getAnchors().forEach(anchor -> anchor.setMaster(master));
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
        validateDocxFile(docxFile);
        UUID masterId = UUID.randomUUID();
        String storageKey = "masters/" + masterId + "/" + sanitizeFilename(docxFile.getOriginalFilename());
        storeDocx(storageKey, docxFile);
        Set<String> anchorIds = extractAnchors(docxFile);
        if (anchorIds.isEmpty()) {
            throw new MasterValidationException("api.error.master.anchorIntegrityFailed");
        }
        MasterDocumentEntity master = new MasterDocumentEntity(
                masterId,
                request.groupCode(),
                request.name(),
                request.description(),
                storageKey,
                docxFile.getOriginalFilename(),
                session.username()
        );
        master.replaceAnchors(toAnchorEntities(masterId, anchorIds));
        master.getAnchors().forEach(anchor -> anchor.setMaster(master));
        masterDocumentRepository.save(master);
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
        assertAnchorIntegrity(master);
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

    private void assertAnchorIntegrity(MasterDocumentEntity master) {
        Set<String> extracted = extractAnchorsFromStorage(master.getStorageKey());
        Set<String> catalog = master.getAnchors().stream()
                .map(MasterAnchorEntity::getAnchorId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (extracted.isEmpty() || !extracted.equals(catalog)) {
            throw new MasterValidationException("api.error.master.anchorIntegrityFailed");
        }
    }

    private Set<String> extractAnchorsFromStorage(String storageKey) {
        try (InputStream inputStream = objectStoragePort.get(storageKey)) {
            return docxAnchorExtractor.extractAnchorIds(inputStream);
        } catch (Exception ex) {
            throw new MasterValidationException("api.error.master.anchorExtractionFailed");
        }
    }

    private void validateDocxFile(MultipartFile docxFile) {
        if (docxFile == null || docxFile.isEmpty()) {
            throw new MasterValidationException("api.error.master.docxRequired");
        }
        String filename = docxFile.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".docx")) {
            throw new MasterValidationException("api.error.master.docxRequired");
        }
    }

    private void storeDocx(String storageKey, MultipartFile docxFile) {
        try (InputStream inputStream = docxFile.getInputStream()) {
            objectStoragePort.put(storageKey, inputStream, docxFile.getSize(), DOCX_CONTENT_TYPE);
        } catch (Exception ex) {
            throw new MasterValidationException("api.error.master.storageFailed");
        }
    }

    private Set<String> extractAnchors(MultipartFile docxFile) {
        try (InputStream inputStream = docxFile.getInputStream()) {
            byte[] bytes = inputStream.readAllBytes();
            try (ByteArrayInputStream extractorStream = new ByteArrayInputStream(bytes)) {
                return docxAnchorExtractor.extractAnchorIds(extractorStream);
            }
        } catch (Exception ex) {
            throw new MasterValidationException("api.error.master.anchorExtractionFailed");
        }
    }

    private List<MasterAnchorEntity> toAnchorEntities(UUID masterId, Set<String> anchorIds) {
        List<MasterAnchorEntity> anchors = new ArrayList<>();
        for (String anchorId : anchorIds) {
            anchors.add(new MasterAnchorEntity(masterId, anchorId, anchorId));
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
                master.getUpdatedAt()
        );
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

    private String sanitizeFilename(String filename) {
        return filename == null ? "master.docx" : filename.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    public record MasterDownloadArtifact(InputStream contentStream, String filename, String contentType)
            implements AutoCloseable {
        @Override
        public void close() throws java.io.IOException {
            contentStream.close();
        }
    }
}
