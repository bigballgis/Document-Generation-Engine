package com.bank.docgen.master.service;

import com.bank.docgen.master.api.CreateMasterRequest;
import com.bank.docgen.master.api.MasterDocumentDetailView;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterAnchorEntity;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalog;
import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalogJsonCodec;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/**
 * Package-private create / replace-file mutation bodies for MasterDocumentService.
 */
final class MasterDocumentFileMutationSupport {

    private final MasterDocumentRepository masterDocumentRepository;
    private final MasterRevisionLineRepository masterRevisionLineRepository;
    private final MasterDocxUploadSupport docxUploadSupport;
    private final MasterDocumentAccessSupport access;
    private final MasterDocumentViewSupport views;
    private final MasterRevisionPersistSupport revisions;
    private final ObjectMapper objectMapper;

    MasterDocumentFileMutationSupport(
            MasterDocumentRepository masterDocumentRepository,
            MasterRevisionLineRepository masterRevisionLineRepository,
            MasterDocxUploadSupport docxUploadSupport,
            MasterDocumentAccessSupport access,
            MasterDocumentViewSupport views,
            MasterRevisionPersistSupport revisions,
            ObjectMapper objectMapper
    ) {
        this.masterDocumentRepository = masterDocumentRepository;
        this.masterRevisionLineRepository = masterRevisionLineRepository;
        this.docxUploadSupport = docxUploadSupport;
        this.access = access;
        this.views = views;
        this.revisions = revisions;
        this.objectMapper = objectMapper;
    }

    MasterDocumentDetailView replaceFile(
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
        MasterStyleCatalog styleCatalog = docxUploadSupport.parseStyleCatalog(docxFile);
        String styleCatalogJson = MasterStyleCatalogJsonCodec.write(objectMapper, styleCatalog);
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
                session.username(),
                styleCatalogJson
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

    MasterDocumentDetailView create(
            CreateMasterRequest request,
            MultipartFile docxFile,
            ManagementSessionClaims session
    ) {
        access.assertGroupWritable(session, request.groupCode());
        docxUploadSupport.validateDocxFile(docxFile);
        List<String> anchorIds = docxUploadSupport.extractAnchors(docxFile);
        if (anchorIds.isEmpty()) {
            throw new MasterValidationException("api.error.master.anchorIntegrityFailed");
        }
        MasterStyleCatalog styleCatalog = docxUploadSupport.parseStyleCatalog(docxFile);
        String styleCatalogJson = MasterStyleCatalogJsonCodec.write(objectMapper, styleCatalog);
        UUID masterId = UUID.randomUUID();
        UUID revisionLineId = UUID.randomUUID();
        String revisionStorageKey = docxUploadSupport.revisionStorageKey(
                masterId, revisionLineId, docxFile.getOriginalFilename());
        docxUploadSupport.storeDocx(revisionStorageKey, docxFile);
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
                session.username(),
                styleCatalogJson
        );
        return views.toDetail(master);
    }
}
