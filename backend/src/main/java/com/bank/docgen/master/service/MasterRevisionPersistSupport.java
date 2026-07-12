package com.bank.docgen.master.service;

import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterAnchorEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineAnchorEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Package-private anchor entity mapping and revision-line persistence for master documents.
 */
final class MasterRevisionPersistSupport {

    private final MasterRevisionLineRepository masterRevisionLineRepository;

    MasterRevisionPersistSupport(MasterRevisionLineRepository masterRevisionLineRepository) {
        this.masterRevisionLineRepository = masterRevisionLineRepository;
    }

    List<MasterAnchorEntity> toAnchorEntities(UUID masterId, List<String> anchorIds) {
        List<MasterAnchorEntity> anchors = new ArrayList<>();
        for (int sequence = 0; sequence < anchorIds.size(); sequence++) {
            String anchorId = anchorIds.get(sequence);
            anchors.add(new MasterAnchorEntity(masterId, anchorId, anchorId, sequence));
        }
        return anchors;
    }

    MasterRevisionLineEntity persistRevisionLine(
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
}
