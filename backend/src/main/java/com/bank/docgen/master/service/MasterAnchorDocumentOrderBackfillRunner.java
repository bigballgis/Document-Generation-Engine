package com.bank.docgen.master.service;

import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterAnchorRepository;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.master.rendering.DocxAnchorExtractor;
import java.io.InputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(10)
public class MasterAnchorDocumentOrderBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MasterAnchorDocumentOrderBackfillRunner.class);

    private final MasterDocumentRepository masterDocumentRepository;
    private final MasterAnchorRepository masterAnchorRepository;
    private final MasterRevisionLineRepository masterRevisionLineRepository;
    private final ObjectStoragePort objectStoragePort;
    private final DocxAnchorExtractor docxAnchorExtractor;

    public MasterAnchorDocumentOrderBackfillRunner(
            MasterDocumentRepository masterDocumentRepository,
            MasterAnchorRepository masterAnchorRepository,
            MasterRevisionLineRepository masterRevisionLineRepository,
            ObjectStoragePort objectStoragePort,
            DocxAnchorExtractor docxAnchorExtractor
    ) {
        this.masterDocumentRepository = masterDocumentRepository;
        this.masterAnchorRepository = masterAnchorRepository;
        this.masterRevisionLineRepository = masterRevisionLineRepository;
        this.objectStoragePort = objectStoragePort;
        this.docxAnchorExtractor = docxAnchorExtractor;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        backfillMasterAnchors();
        backfillRevisionLineAnchors();
    }

    private void backfillMasterAnchors() {
        List<MasterDocumentEntity> masters = masterDocumentRepository.findByDeletedAtIsNull();
        for (MasterDocumentEntity master : masters) {
            List<String> orderedAnchorIds = extractOrderedAnchors(master.getStorageKey());
            if (orderedAnchorIds.isEmpty()) {
                continue;
            }
            boolean updated = MasterAnchorDocumentSequenceSupport.applyMasterAnchorSequencesIfNeeded(
                    master.getAnchors(),
                    orderedAnchorIds
            );
            if (updated) {
                masterAnchorRepository.saveAll(master.getAnchors());
                log.info("Backfilled master anchor document order for master {}", master.getId());
            }
        }
    }

    private void backfillRevisionLineAnchors() {
        List<MasterRevisionLineEntity> revisionLines = masterRevisionLineRepository.findByDeletedAtIsNull();
        for (MasterRevisionLineEntity revisionLine : revisionLines) {
            List<String> orderedAnchorIds = extractOrderedAnchors(revisionLine.getStorageKey());
            if (orderedAnchorIds.isEmpty()) {
                continue;
            }
            boolean updated = MasterAnchorDocumentSequenceSupport.applyRevisionLineAnchorSequencesIfNeeded(
                    revisionLine.getAnchors(),
                    orderedAnchorIds
            );
            if (updated) {
                masterRevisionLineRepository.save(revisionLine);
                log.info("Backfilled revision line anchor document order for revision {}", revisionLine.getId());
            }
        }
    }

    private List<String> extractOrderedAnchors(String storageKey) {
        try (InputStream inputStream = objectStoragePort.get(storageKey)) {
            return docxAnchorExtractor.extractOrderedAnchorIds(inputStream);
        } catch (Exception ex) {
            log.warn("Skipping anchor order backfill for storage key {}: {}", storageKey, ex.getMessage());
            return List.of();
        }
    }
}
