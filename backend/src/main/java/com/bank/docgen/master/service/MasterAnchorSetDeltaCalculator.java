package com.bank.docgen.master.service;

import com.bank.docgen.master.api.MasterAnchorSetDeltaView;
import com.bank.docgen.master.api.MasterRenamedAnchorView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Computes anchor-set added / removed / renamed between two revisions.
 * Stable key is {@code anchorKey} (anchorId); displayLabel-only changes are ignored.
 * Rename pairing: same {@code documentSequence} among unpaired keys (CE-K05 / K05-C3).
 */
final class MasterAnchorSetDeltaCalculator {

    private MasterAnchorSetDeltaCalculator() {
    }

    static MasterAnchorSetDeltaView compute(List<AnchorRef> baseline, List<AnchorRef> candidate) {
        List<AnchorRef> base = baseline == null ? List.of() : baseline;
        List<AnchorRef> cand = candidate == null ? List.of() : candidate;

        Set<String> baseKeys = new LinkedHashSet<>();
        Map<String, Integer> baseSeq = new HashMap<>();
        for (AnchorRef anchor : base) {
            if (anchor == null || anchor.anchorKey() == null || anchor.anchorKey().isBlank()) {
                continue;
            }
            baseKeys.add(anchor.anchorKey());
            baseSeq.put(anchor.anchorKey(), anchor.documentSequence());
        }

        Set<String> candKeys = new LinkedHashSet<>();
        Map<String, Integer> candSeq = new HashMap<>();
        for (AnchorRef anchor : cand) {
            if (anchor == null || anchor.anchorKey() == null || anchor.anchorKey().isBlank()) {
                continue;
            }
            candKeys.add(anchor.anchorKey());
            candSeq.put(anchor.anchorKey(), anchor.documentSequence());
        }

        Set<String> removedCandidates = new LinkedHashSet<>(baseKeys);
        removedCandidates.removeAll(candKeys);
        Set<String> addedCandidates = new LinkedHashSet<>(candKeys);
        addedCandidates.removeAll(baseKeys);

        Map<Integer, String> removedBySeq = indexBySequence(removedCandidates, baseSeq);
        Map<Integer, String> addedBySeq = indexBySequence(addedCandidates, candSeq);

        List<MasterRenamedAnchorView> renamed = new ArrayList<>();
        Set<String> consumedRemoved = new HashSet<>();
        Set<String> consumedAdded = new HashSet<>();
        for (Integer sequence : new TreeSet<>(removedBySeq.keySet())) {
            String from = removedBySeq.get(sequence);
            String to = addedBySeq.get(sequence);
            if (from == null || to == null || Objects.equals(from, to)) {
                continue;
            }
            renamed.add(new MasterRenamedAnchorView(from, to));
            consumedRemoved.add(from);
            consumedAdded.add(to);
        }

        List<String> removed = removedCandidates.stream()
                .filter(key -> !consumedRemoved.contains(key))
                .sorted()
                .toList();
        List<String> added = addedCandidates.stream()
                .filter(key -> !consumedAdded.contains(key))
                .sorted()
                .toList();

        return new MasterAnchorSetDeltaView(added, removed, List.copyOf(renamed));
    }

    private static Map<Integer, String> indexBySequence(Set<String> keys, Map<String, Integer> seqByKey) {
        Map<Integer, String> bySeq = new HashMap<>();
        for (String key : keys) {
            Integer sequence = seqByKey.get(key);
            if (sequence == null) {
                continue;
            }
            // First unpaired key wins when sequences collide.
            bySeq.putIfAbsent(sequence, key);
        }
        return bySeq;
    }

    record AnchorRef(String anchorKey, int documentSequence) {
    }
}
