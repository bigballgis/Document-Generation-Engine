package com.bank.docgen.master.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.master.api.MasterAnchorSetDeltaView;
import com.bank.docgen.master.api.MasterRenamedAnchorView;
import com.bank.docgen.master.service.MasterAnchorSetDeltaCalculator.AnchorRef;
import java.util.List;
import org.junit.jupiter.api.Test;

class MasterAnchorSetDeltaCalculatorTest {

    @Test
    void detectsAddedAndRemovedAnchors() {
        // Different document sequences so unpaired keys are not treated as renames.
        MasterAnchorSetDeltaView delta = MasterAnchorSetDeltaCalculator.compute(
                List.of(
                        new AnchorRef("HEADER", 0),
                        new AnchorRef("FOOTER", 1),
                        new AnchorRef("KEEP", 2)
                ),
                List.of(
                        new AnchorRef("HEADER", 0),
                        new AnchorRef("KEEP", 2),
                        new AnchorRef("BODY", 3)
                )
        );

        assertThat(delta.addedAnchors()).containsExactly("BODY");
        assertThat(delta.removedAnchors()).containsExactly("FOOTER");
        assertThat(delta.renamedAnchors()).isEmpty();
        assertThat(delta.isEmpty()).isFalse();
    }

    @Test
    void detectsRenameWhenSameSequenceChangesStableKey() {
        MasterAnchorSetDeltaView delta = MasterAnchorSetDeltaCalculator.compute(
                List.of(new AnchorRef("OLD_KEY", 0), new AnchorRef("KEEP", 1)),
                List.of(new AnchorRef("NEW_KEY", 0), new AnchorRef("KEEP", 1))
        );

        assertThat(delta.addedAnchors()).isEmpty();
        assertThat(delta.removedAnchors()).isEmpty();
        assertThat(delta.renamedAnchors()).containsExactly(new MasterRenamedAnchorView("OLD_KEY", "NEW_KEY"));
    }

    @Test
    void ignoresDisplayLabelOnlySemanticsByComparingStableKeysAlone() {
        MasterAnchorSetDeltaView delta = MasterAnchorSetDeltaCalculator.compute(
                List.of(new AnchorRef("HEADER", 0)),
                List.of(new AnchorRef("HEADER", 0))
        );

        assertThat(delta.isEmpty()).isTrue();
    }

    @Test
    void emptyWhenBothSidesEmpty() {
        assertThat(MasterAnchorSetDeltaCalculator.compute(List.of(), List.of()).isEmpty()).isTrue();
    }
}
