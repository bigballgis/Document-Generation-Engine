package com.bank.docgen.master.service;

import com.bank.docgen.master.persistence.MasterAnchorEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineAnchorEntity;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

final class MasterAnchorDocumentSequenceSupport {

    private MasterAnchorDocumentSequenceSupport() {
    }

    static boolean applyMasterAnchorSequencesIfNeeded(
            List<MasterAnchorEntity> anchors,
            List<String> orderedAnchorIds
    ) {
        return applySequencesIfNeeded(
                anchors,
                orderedAnchorIds,
                MasterAnchorEntity::getAnchorId,
                MasterAnchorEntity::getDocumentSequence,
                MasterAnchorEntity::setDocumentSequence
        );
    }

    static boolean applyRevisionLineAnchorSequencesIfNeeded(
            List<MasterRevisionLineAnchorEntity> anchors,
            List<String> orderedAnchorIds
    ) {
        return applySequencesIfNeeded(
                anchors,
                orderedAnchorIds,
                MasterRevisionLineAnchorEntity::getAnchorId,
                MasterRevisionLineAnchorEntity::getDocumentSequence,
                MasterRevisionLineAnchorEntity::setDocumentSequence
        );
    }

    private static <T> boolean applySequencesIfNeeded(
            List<T> anchors,
            List<String> orderedAnchorIds,
            Function<T, String> anchorIdExtractor,
            Function<T, Integer> sequenceExtractor,
            BiConsumer<T, Integer> sequenceSetter
    ) {
        if (anchors.isEmpty() || orderedAnchorIds.isEmpty()) {
            return false;
        }
        Set<String> catalogIds = anchors.stream().map(anchorIdExtractor).collect(Collectors.toCollection(HashSet::new));
        Set<String> extractedIds = new HashSet<>(orderedAnchorIds);
        if (!catalogIds.equals(extractedIds)) {
            return false;
        }
        Map<String, Integer> expectedSequence = IntStream.range(0, orderedAnchorIds.size())
                .boxed()
                .collect(Collectors.toMap(orderedAnchorIds::get, Function.identity()));
        boolean needsUpdate = anchors.stream()
                .anyMatch(anchor -> sequenceExtractor.apply(anchor)
                        != expectedSequence.getOrDefault(anchorIdExtractor.apply(anchor), -1));
        if (!needsUpdate) {
            return false;
        }
        for (T anchor : anchors) {
            sequenceSetter.accept(anchor, expectedSequence.get(anchorIdExtractor.apply(anchor)));
        }
        return true;
    }
}
