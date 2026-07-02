package com.bank.docgen.master.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.bank.docgen.master.persistence.MasterAnchorEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineAnchorEntity;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MasterAnchorDocumentSequenceSupportTest {

    private static final UUID MASTER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID REVISION_LINE_ID = UUID.fromString("00000000-0000-0000-0000-000000000202");

    @Test
    void appliesMasterAnchorSequencesWhenStoredOrderIsWrong() {
        List<MasterAnchorEntity> anchors = List.of(
                new MasterAnchorEntity(MASTER_ID, "FOL_CLAUSE_01", "FOL_CLAUSE_01", 0),
                new MasterAnchorEntity(MASTER_ID, "FOL_HEADER", "FOL_HEADER", 0)
        );
        List<String> orderedAnchorIds = List.of("FOL_HEADER", "FOL_CLAUSE_01");

        boolean updated = MasterAnchorDocumentSequenceSupport.applyMasterAnchorSequencesIfNeeded(
                anchors,
                orderedAnchorIds
        );

        assertThat(updated).isTrue();
        assertThat(anchors).extracting(MasterAnchorEntity::getAnchorId, MasterAnchorEntity::getDocumentSequence)
                .containsExactly(
                        tuple("FOL_CLAUSE_01", 1),
                        tuple("FOL_HEADER", 0)
                );
    }

    @Test
    void skipsMasterAnchorUpdateWhenSequencesAlreadyMatchDocumentOrder() {
        List<MasterAnchorEntity> anchors = List.of(
                new MasterAnchorEntity(MASTER_ID, "FOL_HEADER", "FOL_HEADER", 0),
                new MasterAnchorEntity(MASTER_ID, "FOL_CLAUSE_01", "FOL_CLAUSE_01", 1)
        );
        List<String> orderedAnchorIds = List.of("FOL_HEADER", "FOL_CLAUSE_01");

        boolean updated = MasterAnchorDocumentSequenceSupport.applyMasterAnchorSequencesIfNeeded(
                anchors,
                orderedAnchorIds
        );

        assertThat(updated).isFalse();
    }

    @Test
    void appliesRevisionLineAnchorSequencesWhenStoredOrderIsWrong() {
        List<MasterRevisionLineAnchorEntity> anchors = List.of(
                new MasterRevisionLineAnchorEntity(REVISION_LINE_ID, "SCHEDULE_B", "SCHEDULE_B", 0),
                new MasterRevisionLineAnchorEntity(REVISION_LINE_ID, "SCHEDULE_A", "SCHEDULE_A", 0)
        );
        List<String> orderedAnchorIds = List.of("SCHEDULE_A", "SCHEDULE_B");

        boolean updated = MasterAnchorDocumentSequenceSupport.applyRevisionLineAnchorSequencesIfNeeded(
                anchors,
                orderedAnchorIds
        );

        assertThat(updated).isTrue();
        assertThat(anchors).extracting(
                        MasterRevisionLineAnchorEntity::getAnchorId,
                        MasterRevisionLineAnchorEntity::getDocumentSequence)
                .containsExactly(
                        tuple("SCHEDULE_B", 1),
                        tuple("SCHEDULE_A", 0)
                );
    }
}
